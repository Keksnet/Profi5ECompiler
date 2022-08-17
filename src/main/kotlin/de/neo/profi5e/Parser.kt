package de.neo.profi5e

import de.neo.profi5e.extensions.prefixedHexString
import de.neo.profi5e.instructions.AsmArg
import de.neo.profi5e.instructions.Instruction
import de.neo.profi5e.instructions.InstructionRegister
import de.neo.profi5e.util.PerformanceTimer
import java.nio.file.Files
import java.nio.file.Path
import java.text.ParseException
import java.util.Timer

class Parser(private val content: String) {

    private val labels = mutableMapOf<String, Int>()

    var cursorResetAddress = 0x8000
    var lineResetCount = 0

    var currentAddress = 8000
    var lineCount = 0

    fun parseContent(): List<Instruction> {
        val timer = PerformanceTimer() // for performance measurement
        timer.start()
        println("Started preprocessing at 0x${currentAddress.prefixedHexString(4)}")

        currentAddress = cursorResetAddress // reset address
        content.split("\n")
            .filter { return@filter !it.startsWith(";") && it.isNotBlank() }
            .map { it.split(";")[0] }
            .map { setLabelPlaceholder(it) }
            .map { parseLine(it) }
        currentAddress = cursorResetAddress // reset address to original value
        lineCount = lineResetCount // reset line count to original value

        timer.stop()
        println("Finished preprocessing at 0x${currentAddress.prefixedHexString(4)} in ${timer.elapsedTime}\n")

        timer.start()
        try {
            println("Started compiling at 0x${currentAddress.prefixedHexString(4)}")
            val ret = content.split("\n")
                .filter { lineCount++; return@filter !it.startsWith(";") && it.isNotBlank() }
                .map { it.split(";")[0] }
                .mapNotNull { parseLine(it) }
            timer.stop()
            println("Finished compiling at 0x${currentAddress.prefixedHexString(4)} in ${timer.elapsedTime}\n")
            return ret
        }catch (e: Exception) {
            val parserException = ParseException("Error in line $lineCount", lineCount)
            parserException.initCause(e)
            throw parserException
        }finally {
            if (timer.running.isLocked) timer.stop()
        }
    }

    fun parseLine(line: String): Instruction? {

        if (line.startsWith("@")) {
            labels[line.substring(1)] = currentAddress
            if (Profi5E.cmd.hasOption("vv"))
                println("Set label ${line.substring(1)} to 0x${currentAddress.prefixedHexString(4)}")
            return null
        }

        if (line.endsWith(":")) {
            val address = line.dropLast(1)
            currentAddress = address.toInt(16)
            if (Profi5E.cmd.hasOption("vv"))
                println("Set address to 0x${currentAddress.prefixedHexString(4)}")
            return null
        }

        val tokens = line.split(" ")
        val args = run {
            var token1 = tokens.getOrElse(1) { "" }
            if (labels.keys.any { token1.contains(it) }) {
                val label = token1.substring(token1.indexOf('@') + 1).split(" ")[0]
                if (!labels.containsKey(label)) throw IllegalArgumentException("Label $label not found")
                val address = labels[label]!!.toString(16)
                if (Profi5E.cmd.hasOption("vv")) println("Found label $label at $address")
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
            if (Profi5E.cmd.hasOption("vv")) println("$argIndex $argType $arg")
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
            if (Profi5E.cmd.hasOption("vv")) println("Set label ${line.substring(1)}")
        }
        return line
    }

    companion object {

        fun parse(file: Path): Parser {
            return Parser(Files.readString(file))
        }

    }

}