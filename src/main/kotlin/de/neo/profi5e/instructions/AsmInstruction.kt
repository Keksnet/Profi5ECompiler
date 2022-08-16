package de.neo.profi5e.instructions

data class AsmInstruction(
    val name: String,
    val variations: List<AsmVariation>
) {
    fun getVariation(args: List<String>): AsmVariation {
        return variations
            .filter {
                return@filter it.args.size == args.size
            }
            .firstOrNull {
                println("Checking ${it.args} against $args")
                for (i in it.args.indices) {
                    val arg = AsmArg.fromString(args[i])
                    val itArg = it.args[i]
                    if (itArg == arg) continue
                    if (arg != null) return@firstOrNull false
                    if (itArg != AsmArg.ADR && itArg != AsmArg.KO && itArg != AsmArg.KA) return@firstOrNull false
                }
                return@firstOrNull true
            } ?: throw IllegalArgumentException("Invalid instruction: '$name' '${args.joinToString(",")}'")
    }
}

data class AsmVariation(
    val args: List<AsmArg>,
    val code: UByte
) {
    fun containsUserArguments(): Boolean {
        return args.any { it == AsmArg.KA || it == AsmArg.KO || it == AsmArg.ADR }
    }

    fun getUserArgumentIndex(): Int {
        return args.indexOfFirst { it == AsmArg.KA || it == AsmArg.KO || it == AsmArg.ADR }
    }

    fun getUserArgument(): AsmArg {
        return args.first { it == AsmArg.KA || it == AsmArg.KO || it == AsmArg.ADR }
    }
}

enum class AsmArg {
    A, B, C, D, E, H, L, M, F,
    KO, KA, ADR, SP,
    INDEX_0, INDEX_1, INDEX_2, INDEX_3, INDEX_4, INDEX_5, INDEX_6, INDEX_7;

    companion object {
        fun fromString(arg: Any): AsmArg? {
            return fromString(arg as String)
        }

        private fun fromString(arg: String): AsmArg? {
            val asmArg = try {
                AsmArg.valueOf(arg.uppercase())
            }catch (_: IllegalArgumentException) {
                try {
                    AsmArg.valueOf("INDEX_${arg}")
                }catch (_: IllegalArgumentException) {
                    null
                }
            }
            return asmArg
        }
    }
}