package de.neo.profi5e.instructions

data class Instruction(
    val opcode: UByte,
    val operands: List<UByte>,
    val memoryAddress: Int,
    val comment: String = ""
) {
    val byteSize = operands.size + 1

    fun toBytes(): List<UByte> {
        val bytes = mutableListOf<UByte>()
        bytes.add(opcode)
        bytes.addAll(operands)
        return bytes
    }

    companion object {
        fun getNop(memoryAddress: Int): Instruction {
            return Instruction("00".toUByte(16), listOf(), memoryAddress)
        }
    }
}
