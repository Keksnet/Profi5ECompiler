package de.neo.profi5e.instructions

data class Instruction(
    val opcode: UByte,
    val operands: List<UByte>
) {
    val byteSize = operands.size + 1

    fun toBytes(): List<UByte> {
        val bytes = mutableListOf<UByte>()
        bytes.add(opcode)
        bytes.addAll(operands)
        return bytes
    }

    companion object {
        val NOP = Instruction("00".toUByte(16), listOf())
    }
}
