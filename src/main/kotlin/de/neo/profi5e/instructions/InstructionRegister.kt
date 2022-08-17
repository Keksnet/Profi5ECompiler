package de.neo.profi5e.instructions

object InstructionRegister {

    private val instructions = HashMap<String, AsmInstruction>()

    var instructionCount: Int = 0
        private set

    fun register(instruction: AsmInstruction) {
        instructions[instruction.name] = instruction
        instructionCount = instructions.size
    }

    fun getByName(name: String): AsmInstruction {
        return instructions[name] ?: throw IllegalArgumentException("No instruction with name $name")
    }

}