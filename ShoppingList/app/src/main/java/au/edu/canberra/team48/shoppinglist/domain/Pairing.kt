package au.edu.canberra.team48.shoppinglist.domain

import kotlin.random.Random

/**
 * Connecting phones, WhatsApp style. One phone shows a QR code that holds a
 * six digit number; the other scans it or types the number. Numbers last ten
 * minutes and are deleted once used.
 */
object Pairing {
    const val TTL_MS = 10 * 60 * 1000L
    const val MAX_PHONES = 5
    const val LENGTH = 6
    private const val QR_PREFIX = "SHOPLIST:"

    /** Never starts with 0, so it is always six digits when read aloud. */
    fun newCode(random: Random = Random.Default): String = (100_000 + random.nextInt(900_000)).toString()

    fun isWellFormed(code: String): Boolean = code.length == LENGTH && code.all { it in '0'..'9' }

    fun isExpired(expiresAt: Long, now: Long): Boolean = now >= expiresAt

    fun qrPayload(code: String): String = QR_PREFIX + code

    /** Accepts our own QR payload or a bare number. Anything else is rejected. */
    fun codeFromScan(raw: String?): String? {
        val t = raw?.trim() ?: return null
        val candidate = if (t.startsWith(QR_PREFIX)) t.removePrefix(QR_PREFIX) else t
        return candidate.takeIf(::isWellFormed)
    }

    /** "492713" shown as "492 713". */
    fun display(code: String): String = if (code.length == LENGTH) code.substring(0, 3) + " " + code.substring(3) else code

    /** Keeps digits only, at most six, for the number pad. */
    fun typed(input: String): String = input.filter { it in '0'..'9' }.take(LENGTH)
}

object ProfileRules {
    const val MAX_NAME = 40

    fun cleanName(s: String): String = s.trim().replace(Regex("\\s+"), " ").take(MAX_NAME)

    fun isValidName(s: String): Boolean = cleanName(s).isNotEmpty()

    fun phoneDigits(s: String): String = s.filter { it in '0'..'9' }

    /** 8 to 15 digits, allowing the spaces, dashes, brackets and + that people type. */
    fun isValidPhone(s: String): Boolean {
        val allowed = s.all { it in '0'..'9' || it in " +-()" }
        return allowed && phoneDigits(s).length in 8..15
    }

    /** Australian mobiles shown the familiar way: 0412 345 678. */
    fun formatPhone(s: String): String {
        val raw = phoneDigits(s)
        // numbers read from the phone arrive in international form: +61 412 345 678
        val d = if (raw.length == 11 && raw.startsWith("61") && s.trim().startsWith("+")) "0" + raw.substring(2) else raw
        return when {
            d.length == 10 && d.startsWith("04") -> "${d.substring(0, 4)} ${d.substring(4, 7)} ${d.substring(7)}"
            d.length == 10 && d.startsWith("0") -> "(${d.substring(0, 2)}) ${d.substring(2, 6)} ${d.substring(6)}"
            else -> s.trim()
        }
    }

    private val spokenDigits = mapOf(
        "zero" to "0", "oh" to "0", "o" to "0", "nought" to "0",
        "one" to "1", "two" to "2", "to" to "2", "too" to "2", "three" to "3", "four" to "4", "for" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
    )

    /**
     * Voice for the phone field. Speech engines return "0412 345 678", or
     * "zero four one two", or "double four". All become digits.
     */
    fun digitsFromSpeech(s: String): String {
        val words = s.lowercase().replace(Regex("[^a-z0-9 ]"), " ").split(Regex("\\s+")).filter { it.isNotEmpty() }
        val out = StringBuilder()
        var repeat = 1
        for (w in words) {
            when {
                w == "double" -> repeat = 2
                w == "triple" -> repeat = 3
                w.all { it.isDigit() } -> { out.append(w.repeat(1)); repeat = 1 }
                spokenDigits.containsKey(w) -> { out.append(spokenDigits.getValue(w).repeat(repeat)); repeat = 1 }
            }
        }
        return out.toString()
    }

    fun initial(name: String): String = cleanName(name).firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    /** "Mary and Frank", "Mary, Frank and Sam". */
    fun joinNames(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> names.dropLast(1).joinToString(", ") + " and " + names.last()
    }
}
