package de.neo.profi5e

import de.neo.profi5e.extensions.changeFileExtension
import de.neo.profi5e.extensions.getValue
import de.neo.profi5e.extensions.prefixedHexString
import de.neo.profi5e.instructions.*
import de.neo.profi5e.util.PerformanceTimer
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.json.JSONObject
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.min

const val DEFAULT_SOURCE_FILE = "test.asm"
const val DEFAULT_MEM_START = 0x8000
const val DEFAULT_MEM_END = 0x87FF
const val DEFAULT_PROGRAM_START = 0x8000

class Profi5E {
    companion object {
        var cmd = CommandLine.Builder().build()
    }
}

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
                if (Profi5E.cmd.hasOption("vv"))
                    println("0x${code.toString(16)}: $name(${args.joinToString(", ")})")
                return@map AsmVariation(args, code)
            }
            InstructionRegister.register(AsmInstruction(name, variations))
        }
}

fun main(args: Array<String>) {
    val startTime = System.currentTimeMillis()

    run(args)

    val endTime = System.currentTimeMillis()
    println("Done in ${endTime - startTime}ms")
}

fun run(args: Array<String>) {
    // Init command line options
    val cmdParser = DefaultParser(false)
    val options = Options()
    options.addOption("in", "source", true, "Source file (required)")
    options.addOption("o", "output", true, "Output file (default: same as source but with .5e extension)")
    options.addOption("x", "saveHex", false, "Save hex and binary output (default: false)")
    options.addOption("ms", "mem-start", true, "Memory offset of the binary (default: 0x8000)")
    options.addOption("me", "mem-end", true, "Memory end of the binary (default: 0x87FF)")
    options.addOption("ma", "mem-auto", false, "Automatically determine memory range [WARNING: experimental] (default: false)")
    options.addOption("ps", "program-start", true, "Program start address (default: 0x8000)")
    options.addOption("v", "verbose", false, "Verbose output (default: false)")
    options.addOption("vv", "very-verbose", false, "Very verbose output (default: false)")
    options.addOption("h", "help", false, "Show help")
    Profi5E.cmd = cmdParser.parse(options, args)

    if (Profi5E.cmd.hasOption("help")) {
        HelpFormatter().printHelp("java -jar Profi5ECompiler.jar [options] -in <source> [-o output]", options)
        return
    }

    if (!Profi5E.cmd.hasOption("source") && System.getenv("DEV") == null) {
        println("ERR: No source file specified")
        HelpFormatter().printHelp("java -jar Profi5ECompiler.jar [options] -in <source> [-o output]", options)
        return
    }

    // Load asm instruction definitions
    logVeryVerbose("Loading instructions")
    loadInstructions()
    logVeryVerbose("Loaded ${InstructionRegister.instructionCount} instructions")
    println()

    // Parse source file
    logVeryVerbose("Parsing source file")
    val sourceFile = Profi5E.cmd.getValue("source", DEFAULT_SOURCE_FILE)
    val asmParser = Parser.parse(Path.of(sourceFile))
    asmParser.currentAddress = Profi5E.cmd.getValue("ms", DEFAULT_PROGRAM_START) // set default memory start address
    var instructions = asmParser.parseContent()
    logVeryVerbose("Parsed ${instructions.size} instructions")

    val memRange = getMemoryRange(instructions)

    // Compile instructions to binary
    logVeryVerbose("Compiling instructions")
    val outputPath = Profi5E.cmd.getValue("output", sourceFile.changeFileExtension(".5e"))
    val binPath = Path.of(outputPath)
    val bin = ByteArray(memRange.second - memRange.first + 1) { 0x00 }
    logVeryVerbose("Allocated 0x${bin.size.prefixedHexString(4)} bytes for binary")
    logVeryVerbose("Memory range: 0x${memRange.first.prefixedHexString(4)} - 0x${memRange.second.prefixedHexString(4)}")
    var offset = Profi5E.cmd.getValue("ps", DEFAULT_PROGRAM_START) - memRange.first
    var warnings = 0
    for (inst in instructions) {
        val bytes = inst.toBytes()
        for (byte in bytes) {
            logVeryVerbose("0x${offset.prefixedHexString(4)}: 0x${byte.prefixedHexString(2)}")
            if (offset >= bin.size) {
                println("WARN: Instruction at 0x${offset.prefixedHexString(4)} outside memory range (overflow)")
                warnings += 1
                continue
            }
            if (offset < 0) {
                println("WARN: Instruction at 0x${offset.prefixedHexString(4)} outside memory range (underflow)")
                warnings += 1
                continue
            }
            bin[offset++] = byte.toByte()
        }
    }
    logVeryVerbose("Compiled ${instructions.size} instructions to ${bin.size} bytes")

    val hexString = buildHexString(instructions)

    if (Profi5E.cmd.hasOption("v")) {
        println("Printing instructions... (${instructions.size} instructions)")
        println(hexString)
    }

    if (Profi5E.cmd.hasOption("x")) {
        val hexPathString = outputPath.changeFileExtension(".hex")
        val hexPath = Path.of(hexPathString)
        println("Writing hex file to $hexPathString...")
        Files.write(hexPath, hexString.toByteArray())
    }

    println("Writing to $outputPath...")
    Files.write(binPath, bin)
    println("Compiled to $binPath with $warnings warnings")
}

fun getMemoryRange(instructions: List<Instruction>): Pair<Int, Int> {
    val auto = Profi5E.cmd.hasOption("ma")
    val ps = Profi5E.cmd.getValue("ps", DEFAULT_PROGRAM_START)
    val ms = Profi5E.cmd.getValue("ms", if (auto) instructions.minOf(Instruction::memoryAddress) else DEFAULT_MEM_START)
    val me = Profi5E.cmd.getValue("me", if (auto) instructions.maxOf(Instruction::memoryAddress) else DEFAULT_MEM_END)
    val programLen = me - ms
    return Pair(
        min(ms, ps),
        min(me, ps + programLen)
    )
}

fun buildHexString(instructions: List<Instruction>): String {
    val memRange = getMemoryRange(instructions)

    // Create a map of all instructions by address
    logVeryVerbose("Creating instruction map")
    val memoryKeyMap = mutableMapOf<Int, Instruction>()
    for (inst in instructions) {
        memoryKeyMap[inst.memoryAddress] = inst
    }
    logVeryVerbose("Created instruction map with ${memoryKeyMap.size} entries")

    // Building the actual hex string
    logVeryVerbose("Building hex string")
    val hexContent = StringBuilder()
    var address = memRange.first
    while (address <= memRange.second) {
        val inst = memoryKeyMap[address] ?: Instruction.getNop(address) // get nop if no instruction found
        hexContent.append("${address.prefixedHexString(4)} ${inst.opcode.prefixedHexString(2)} ")
        inst.operands.forEach { it0 ->
            hexContent.append("${it0.prefixedHexString(2)} ")
        }
        hexContent.deleteCharAt(hexContent.length - 1) // remove last space
        hexContent.append("\n") // newline
        address += inst.byteSize // next address
    }
    return hexContent.toString().dropLast(1) // remove last newline
}

fun logVeryVerbose(msg: String) {
    if (Profi5E.cmd.hasOption("vv")) println(msg)
}