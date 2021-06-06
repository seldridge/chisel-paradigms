// SPDX-License-Identifier: Apache-2.0
package paradigms

import chisel3._
import chisel3.experimental.FixedPoint

object NoSourceInfo {
  implicit val noInfo = chisel3.internal.sourceinfo.UnlocatableSourceInfo
}
import NoSourceInfo._

/** A simple ALU coded like Verilog */
class ALUSimple extends RawModule {
  val a = IO(Input(UInt(32.W)))
  val b = IO(Input(UInt(32.W)))
  val opcode = IO(Input(UInt(2.W)))
  val f_lo = IO(Output(UInt(32.W)))
  val f_hi = IO(Output(UInt(32.W)))

  val result = Wire(UInt(64.W))
  result := DontCare
  when (opcode === 0.U) {
    result := a + b
  }.elsewhen(opcode === 1.U) {
    result := a - b
  }.elsewhen(opcode === 2.U) {
    result := a * b
  }.elsewhen(opcode === 3.U) {
    result := b.abs() ## a.abs()
  }

  f_hi := result(63, 32)
  f_lo := result(31, 0)
}

/** An ALU that uses a Chisel standard library function "MuxLookup" */
class ALULibraries extends RawModule {
  val a = IO(Input(UInt(32.W)))
  val b = IO(Input(UInt(32.W)))
  val opcode = IO(Input(UInt(2.W)))
  val f_lo = IO(Output(UInt(32.W)))
  val f_hi = IO(Output(UInt(32.W)))

  val result = util.MuxLookup(
    opcode, 0.U,
    Seq(
      0.U -> (a + b),
      1.U -> (a - b),
      2.U -> (a * b),
      3.U -> (b.abs() ## a.abs())
    )
  ).asUInt

  f_hi := result(63, 32)
  f_lo := result(31, 0)
}

/** A parametric ALU */
class ALUParametric(width: Int) extends RawModule {
  val a = IO(Input(UInt(width.W)))
  val b = IO(Input(UInt(width.W)))
  val f_lo = IO(Output(UInt(width.W)))
  val f_hi = IO(Output(UInt(width.W)))

  val ops = Seq(
    0.U -> (a + b),
    1.U -> (a - b),
    2.U -> (a * b),
    3.U -> (b.abs() ## a.abs())
  )

  val opcode = IO(Input(UInt(util.log2Up(ops.size).W)))

  val result = util.MuxLookup(opcode, 0.U, ops).asUInt

  f_hi := result(width * 2 - 1, width)
  f_lo := result(width - 1, 0)
}

class ALUParametric16 extends ALUParametric(16)
class ALUParametric8 extends ALUParametric(8)

/** A polymorphic ALU */
class ALUPolymorphic[A <: Bits with Num[A]](gen: => A) extends RawModule {
  val a, b = IO(Input(gen))
  val width = a.getWidth
  val f_lo, f_hi = IO(Output(UInt(width.W)))

  val ops = Seq(
    0.U -> (a + b),
    1.U -> (a - b),
    2.U -> (a * b),
    3.U -> (b.abs() ## a.abs()).asTypeOf(a)
  )

  val opcode = IO(Input(UInt(util.log2Up(ops.size).W)))

  val result = util.MuxLookup(opcode, 0.U, ops).asUInt

  f_hi := result(width * 2 - 1, width)
  f_lo := result(width - 1, 0)
}

class ALUPolymorphic_U128 extends ALUPolymorphic(UInt(128.W))
class ALUPolymorphic_S64 extends ALUPolymorphic(SInt(64.W))
class ALUPolymorphic_FP8 extends ALUPolymorphic(FixedPoint(8.W, 4.BP))

/** A totally generic ALU */
class ALUGeneric[A <: Bits with Num[A]](gen: => A, ops: Seq[(A, A) => UInt]) extends RawModule {
  val a, b = IO(Input(gen))
  val width = a.getWidth
  val f_lo, f_hi = IO(Output(UInt(width.W)))

  val opcode = IO(Input(UInt(util.log2Up(ops.size).W)))

  val result = if (ops.size == 1) {
    ops(0)(a, b).asUInt
  } else {
    val _ops = ops.zipWithIndex.map{ case (f, op) => (op.U, f(a, b)) }
    util.MuxLookup(opcode, 0.U, _ops).asUInt
  }

  f_hi := result(width * 2 - 1, width)
  f_lo := result(width - 1, 0)
}

object Ops {
  val Ops2 = Seq(
    (a: UInt, b: UInt) => a + b,
    (a: UInt, b: UInt) => a << b
  )
  val Ops8 = Seq(
    (a: SInt, b: SInt) => (a % b).asUInt,
    (a: SInt, b: SInt) => (a * b).asUInt,
    (a: SInt, b: SInt) => (a + b).asUInt,
    (a: SInt, b: SInt) => (a - b).asUInt,
    (a: SInt, b: SInt) => (a / b).asUInt,
    (a: SInt, b: SInt) => (b.abs() ## a.abs()).asUInt,
    (a: SInt, b: SInt) => a.max(b).asUInt,
    (a: SInt, b: SInt) => a.min(b).asUInt
  )
}

class ALUGeneric_U4_2op extends ALUGeneric(UInt(4.W), Ops.Ops2)
class ALUGeneric_S8_8op extends ALUGeneric(SInt(8.W), Ops.Ops8)
