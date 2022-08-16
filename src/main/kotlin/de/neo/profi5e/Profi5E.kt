package de.neo.profi5e

import de.neo.profi5e.instructions.AsmArg
import de.neo.profi5e.instructions.AsmInstruction
import de.neo.profi5e.instructions.AsmVariation
import de.neo.profi5e.instructions.InstructionRegister
import org.json.JSONObject
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class Profi5E {}

fun loadInstructions() {
    val instructionStream = Profi5E::class.java.getResourceAsStream("/instructions.json")
        ?: throw IllegalStateException("Could not load instructions")
    val instructionContent = String(instructionStream.readAllBytes(), StandardCharsets.UTF_8)
    instructionStream.close()
    val instructionSet = JSONObject(instructionContent)
    instructionSet.keySet()
        .stream()
        .forEach { name ->
            val instruction = instructionSet.getJSONArray(name)
            val variations = instruction.map { variation ->
                if (variation !is JSONObject) throw IllegalArgumentException("Invalid instruction variation: $variation")
                val args = variation.getJSONArray("args").map { arg -> AsmArg.fromString(arg)!! }
                val code = variation.getString("code").toUByte(16)
                println("0x${code.toString(16)}: $name(${args.joinToString(", ")})")
                return@map AsmVariation(args, code)
            }
            InstructionRegister.register(AsmInstruction(name, variations))
        }
}

fun main(args: Array<String>) {
    var sourcePath = ""
    var outputPath = ""
    var shrink = false
    var handled = false
    when (args.size) {
        1 -> {
            sourcePath = args[0]
            outputPath = sourcePath.substring(0, sourcePath.lastIndexOf('.')) + ".5e"
            shrink = args.any { it == "-s" || it == "--shrink" }
            handled = true
        }

        2 -> {
            sourcePath = args[0]
            outputPath = args[1]
            shrink = args.any { it == "-s" || it == "--shrink" }
            handled = true
        }

        else -> {}
    }
    if (!handled && args.isNotEmpty()) {
        sourcePath = args[0]
        outputPath = args[1]
        shrink = args.any { it == "-s" || it == "--shrink" }
    } else if (!handled) {
        if (System.getenv("DEV") == null) {
            println("Usage: java -jar Profi5ECompiler <source> [output] [-s|--shrink]")
            return
        }
        sourcePath = "test.asm"
        outputPath = "test.5e"
        shrink = false
    }
    loadInstructions()
    println()
    val instructions = Parser.parse(Path.of(sourcePath)).parseContent()
    println("Printing instructions... (${instructions.size} instructions)")
    val hexContent = StringBuilder()
    instructions.forEach {
        if (shrink && it.opcode == "00".toUByte(16)) return@forEach
        hexContent.append("${it.memoryAddress.toString(16)} ${it.opcode.toString(16).padStart(2, '0')} ")
        it.operands.forEach { it0 ->
            hexContent.append("${it0.toString(16).padStart(2, '0')} ")
        }
        hexContent.deleteCharAt(hexContent.length - 1)
        hexContent.append("\n")
    }
    val hexString = hexContent.toString().dropLast(1)
    println(hexString)

    val hexPathString = outputPath.substring(0, outputPath.lastIndexOf('.')) + ".hex"
    val hexPath = Path.of(hexPathString)
    println("Writing hex file to $hexPathString")
    Files.write(hexPath, hexString.toByteArray())

    println("Writing to $outputPath...")
    val binPath = Path.of(outputPath)
    val byteCount = instructions.sumOf { it.byteSize }
    val bin = ByteArray(byteCount)
    var offset = 0
    for (inst in instructions) {
        val bytes = inst.toBytes()
        for (byte in bytes) {
            bin[offset++] = byte.toByte()
        }
    }
    Files.write(binPath, bin)

    println("Compiled to $binPath")
}