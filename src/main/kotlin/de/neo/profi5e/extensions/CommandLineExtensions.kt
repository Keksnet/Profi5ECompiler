package de.neo.profi5e.extensions

import org.apache.commons.cli.CommandLine

fun CommandLine.getValue(option: String, default: String): String {
    return if (hasOption(option)) getOptionValue(option) else default
}

fun CommandLine.getValue(option: Char, default: String): String {
    return if (hasOption(option)) getOptionValue(option) else default
}


fun CommandLine.getValue(option: String, default: Int, radix: Int = 16): Int {
    return if (hasOption(option)) getOptionValue(option).replace("0x", "").toInt(radix) else default
}

fun CommandLine.getValue(option: Char, default: Int, radix: Int = 16): Int {
    return if (hasOption(option)) getOptionValue(option).replace("0x", "").toInt(radix) else default
}