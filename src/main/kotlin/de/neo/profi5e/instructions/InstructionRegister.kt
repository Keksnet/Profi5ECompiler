package de.neo.profi5e.instructions

object InstructionRegister {

    private val instructions = HashMap<String, AsmInstruction>()

    fun register(instruction: AsmInstruction) {
        instructions[instruction.name] = instruction
    }

    fun getByName(name: String): AsmInstruction {
        return instructions[name] ?: throw IllegalArgumentException("No instruction with name $name")
    }

}