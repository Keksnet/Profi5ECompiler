package de.neo.profi5e.extensions

fun Int.prefixedHexString(length: Int) = toString(16).padStart(length, '0')
fun UByte.prefixedHexString(length: Int) = toString(16).padStart(length, '0')

fun String.changeFileExtension(newExtension: String) = substring(0, lastIndexOf('.')) + newExtension