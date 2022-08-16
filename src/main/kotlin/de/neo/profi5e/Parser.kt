package de.neo.profi5e

import de.neo.profi5e.instructions.AsmArg
import de.neo.profi5e.instructions.Instruction
import de.neo.profi5e.instructions.InstructionRegister
import java.nio.file.Files
import java.nio.file.Path

class Parser(private val content: String) {

    private val labels = mutableMapOf<String, Int>()
    private var currentAddress = 8000
    private var lineCount = 0

    fun parseContent(): List<Instruction> {
        currentAddress = "8000".toInt(16)
        lineCount = 0
        content.split("\n")
            .filter { return@filter !it.startsWith(";") && it.isNotBlank() }
            .map { it.split(";")[0] }
            .map { setLabelPlaceholder(it) }
            .map { parseLine(it) }
        currentAddress = "8000".toInt(16)
        lineCount = 0
        return content.split("\n")
            .filter { lineCount++; return@filter !it.startsWith(";") && it.isNotBlank() }
            .map { it.split(";")[0] }
            .map { parseLine(it) }
    }

    fun parseLine(line: String): Instruction {

        if (line.startsWith("@")) {
            val nopInstruction = Instruction.getNop(currentAddress)
            currentAddress += nopInstruction.byteSize
            labels[line.substring(1)] = currentAddress
            println("Set label ${line.substring(1)} to $currentAddress")
            return nopInstruction
        }

        val tokens = line.split(" ")
        val args = run {
            var token1 = tokens.getOrElse(1) { "" }
            if (labels.keys.any { token1.contains(it) }) {
                val label = token1.substring(token1.indexOf('@') + 1).split(" ")[0]
                if (!labels.containsKey(label)) throw IllegalArgumentException("Label $label not found")
                val address = labels[label]!!.toString(16)
                println("Found label $label at $address")
                token1 = token1.replace("@$label", address)
            }
            val args = token1.split(",")
            if (args.size == 1 && args[0].isBlank()) return@run listOf<String>()
            args
        }

        val asmInstruction = InstructionRegister.getByName(tokens[0]).getVariation(args)

        val operands = if (asmInstruction.containsUserArguments()) {
            val argIndex = asmInstruction.getUserArgumentIndex()
            val argType = asmInstruction.getUserArgument()
            val arg = args[argIndex]
            // println("$argIndex $argType $arg")
            val a = when (argType) {
                AsmArg.ADR -> {
                    var localArg = arg.toInt(16).toString(16)
                    while(localArg.length != 4) localArg = "0$localArg"
                    listOf(localArg.substring(2).toUByte(16), localArg.substring(0, 2).toUByte(16))
                }

                AsmArg.KO -> listOf(arg.toUByte(16))

                AsmArg.KA -> listOf(arg.toUByte(16))

                else -> listOf(arg.toUByte(16))
            }
            a
        } else {
            listOf()
        }

        val instruction = Instruction(asmInstruction.code, operands, currentAddress)
        currentAddress += instruction.byteSize
        return instruction
    }

    private fun setLabelPlaceholder(line: String): String {
        if (line.startsWith("@")) {
            labels[line.substring(1)] = currentAddress
            println("Set label ${line.substring(1)} to $currentAddress")
        }
        return line
    }

    companion object {

        fun parse(file: Path): Parser {
            return Parser(Files.readString(file))
        }

    }

}