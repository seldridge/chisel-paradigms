// SPDX-License-Identifier: Apache-2.0
package paradigms

import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

case class ChiselExample(generator: () => RawModule, directory: String)

object Main extends App {

  val stage = new ChiselStage

  Seq(
    ChiselExample(() => new ALUSimple, "build/ALUSimple"),
    ChiselExample(() => new ALULibraries, "build/ALULibraries"),
    ChiselExample(() => new ALUParametric16, "build/ALUParametric-16"),
    ChiselExample(() => new ALUParametric8, "build/ALUParametric-8"),
    ChiselExample(() => new ALUPolymorphic_U128, "build/ALUPolymorphic_U128"),
    ChiselExample(() => new ALUPolymorphic_S64, "build/ALUPolymorphic_S64"),
    ChiselExample(() => new ALUPolymorphic_FP8, "build/ALUPolymorphic_FP8"),
    ChiselExample(() => new ALUGeneric_U4_2op, "build/ALUGeneric_U4_2op"),
    ChiselExample(() => new ALUGeneric_S8_8op, "build/ALUGeneric_S8_8op"),
  ).foreach{ case ChiselExample(f, dir) =>
      stage.execute(
        Array("-X", "verilog",
              "-E", "high", "-E", "middle", "-E", "low", "-E", "verilog",
              "-td", dir),
        Seq(ChiselGeneratorAnnotation(f))
      )
  }
}
