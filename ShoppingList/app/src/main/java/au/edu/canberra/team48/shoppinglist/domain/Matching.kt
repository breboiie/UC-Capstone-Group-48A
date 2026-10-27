package au.edu.canberra.team48.shoppinglist.domain

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/**
 * Turns what someone typed, said, or what the camera read off a packet into
 * catalogue items. Speech-to-text is imperfect, so every caller shows the
 * result as pictures to choose from rather than adding anything directly.
 */
object Matching {

    /** Words people say around an item name that are not the item itself. */
    private val filler = setOf(
        "add", "please", "some", "a", "an", "the", "to", "my", "list", "shopping", "of", "and",
        "i", "need", "want", "get", "buy", "for", "me", "on", "into", "in", "we", "our", "more",
        "put", "also", "too", "can", "you", "um", "uh",
    )

    /** Quantities and pack sizes. "Two litres of milk" should match "Milk". */
    private val quantities = setOf(
        "litre", "litres", "liter", "liters", "l", "ml", "kg", "kilo", "kilos", "g", "gram", "grams",
        "pack", "packet", "packets", "bottle", "bottles", "carton", "cartons", "box", "boxes",
        "bag", "bags", "dozen", "tin", "tins", "can", "cans", "jar", "jars", "loaf", "loaves",
        "bunch", "bunches", "tub", "tubs", "punnet", "punnets", "one", "two", "three", "four",
        "five", "six", "seven", "eight", "nine", "ten", "half", "x", "large", "small", "big",
    )

    /** Lower case, accents removed, punctuation to spaces, single spaced. "Mjölk!" becomes "mjolk". */
    fun normalize(s: String): String {
        val noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return noAccents.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    /** Strips filler words and quantities from something spoken. */
    fun cleanSpoken(s: String): String =
        normalize(s).split(" ")
            .filter { it.isNotEmpty() && it !in filler && it !in quantities && it.toIntOrNull() == null }
            .joinToString(" ")

    /** Crude plural stemming, enough for grocery words: tomatoes to tomato, apples to apple. */
    fun stem(word: String): String = when {
        word.length > 4 && word.endsWith("oes") -> word.dropLast(2)
        word.length > 4 && word.endsWith("ies") -> word.dropLast(3) + "y"
        word.length > 3 && word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)
        else -> word
    }

    private fun stemPhrase(p: String) = p.split(" ").joinToString(" ") { stem(it) }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** 1.0 identical, 0.0 nothing in common. */
    fun similarity(a: String, b: String): Double {
        val len = max(a.length, b.length)
        return if (len == 0) 1.0 else 1.0 - levenshtein(a, b).toDouble() / len
    }

    /** How well [query] describes [item], from 0 to 1. */
    fun score(query: String, item: CatalogItem): Double {
        val q = normalize(query)
        if (q.isEmpty()) return 0.0
        val qs = stemPhrase(q)
        val qWords = q.split(" ")
        var best = 0.0
        for (raw in listOf(item.name) + item.aliases) {
            val n = normalize(raw)
            if (n.isEmpty()) continue
            val ns = stemPhrase(n)
            val nWords = n.split(" ")
            val s = when {
                n == q -> 1.0
                ns == qs -> 0.97
                // the query contains the whole name: "full cream milk" contains "milk"
                nWords.size == 1 && qWords.any { stem(it) == ns } -> 0.9
                nWords.size > 1 && " $qs ".contains(" $ns ") -> 0.92
                // typing ahead: "cus" to "custard", "sweet p" to "sweet potato"
                q.length >= 2 && n.startsWith(q) -> 0.7 + 0.25 * q.length / n.length
                q.length >= 3 && nWords.any { it.startsWith(q) } -> 0.62 + 0.2 * q.length / n.length
                // misheard or misspelt: "bananna", "tomatoe"
                else -> {
                    val whole = similarity(qs, ns)
                    val perWord = qWords.maxOf { w -> nWords.maxOf { similarity(stem(w), stem(it)) } }
                    max(whole, perWord * 0.95) * 0.9
                }
            }
            best = max(best, s)
        }
        return best
    }

    /** Catalogue search for the Type screen, best first. */
    fun search(query: String, catalog: List<CatalogItem> = Catalog.items, limit: Int = 6, minScore: Double = 0.62): List<CatalogItem> {
        if (normalize(query).isEmpty()) return emptyList()
        return catalog.map { it to score(query, it) }
            .filter { it.second >= minScore }
            .sortedWith(compareByDescending<Pair<CatalogItem, Double>> { it.second }.thenBy { it.first.rank })
            .take(limit)
            .map { it.first }
    }

    /**
     * The search bar on Add items. Updates with every letter: items whose name (or another
     * word for them) starts with what was typed come first, so "ma" shows Mandarins and
     * Mangoes; then close spellings, so "bannana" still finds Bananas.
     */
    fun browse(query: String, catalog: List<CatalogItem> = Catalog.items, limit: Int = 30): List<CatalogItem> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        fun startsWith(text: String) = normalize(text).let { t -> t.startsWith(q) || t.split(' ').any { it.startsWith(q) } }
        val prefix = catalog.filter { c -> startsWith(c.name) || c.aliases.any(::startsWith) }.sortedBy { it.rank }
        val close = if (q.length >= 3) search(query, catalog, limit = limit, minScore = 0.6) else emptyList()
        return (prefix + close).distinctBy { it.key }.take(limit)
    }

    /** True when the search bar text is exactly one catalogue item, so no "Add …" button is needed for it. */
    fun isExactly(query: String, item: CatalogItem): Boolean {
        val q = normalize(query)
        return normalize(item.name) == q || item.aliases.any { normalize(it) == q }
    }

    /**
     * What to offer after someone speaks. Speech engines return several guesses,
     * so every guess is scored and the best items overall are kept.
     */
    fun voiceCandidates(alternatives: List<String>, catalog: List<CatalogItem> = Catalog.items, limit: Int = 2): List<CatalogItem> {
        val best = HashMap<String, Double>()
        for ((idx, alt) in alternatives.withIndex()) {
            val cleaned = cleanSpoken(alt)
            if (cleaned.isEmpty()) continue
            val weight = 1.0 - idx * 0.05 // the engine's first guess is usually its best
            for (item in catalog) {
                val s = score(cleaned, item) * weight
                if (s >= 0.66 && s > (best[item.key] ?: 0.0)) best[item.key] = s
            }
        }
        return best.entries
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { Catalog.find(it.key)?.rank ?: 999 })
            .take(limit)
            .mapNotNull { e -> catalog.firstOrNull { it.key == e.key } }
    }

    /** Joining words and marks people use when they say several things at once. */
    private val joiners = Regex("\\s*(?:,|;|&|\\+|\\band\\b|\\bthen\\b|\\bplus\\b|\\balso\\b|\\bas well as\\b)\\s*")

    /**
     * "Milk, bread and eggs" said in one go. Returns the items in the order spoken,
     * or null when only one thing was said, so the single-item screen is used.
     * Anything not in the catalogue is kept as a custom item rather than dropped.
     */
    fun severalFromSpeech(spoken: String, catalog: List<CatalogItem> = Catalog.items): List<CatalogItem>? {
        val whole = " ${normalize(spoken)} "
        // an item whose own name has "and" in it ("fish and chips") is one thing, not two
        val namesWithAnd = catalog.flatMap { listOf(it.name) + it.aliases }.map { normalize(it) }.filter { it.contains(" and ") }
        if (namesWithAnd.any { whole.contains(" $it ") }) return null
        val parts = spoken.lowercase().split(joiners).map { cleanSpoken(it) }.filter { it.isNotEmpty() }
        val found = parts.flatMap { itemsInPart(it, catalog) }.distinctBy { it.key }
        return if (found.size >= 2) found else null
    }

    /**
     * One chunk of speech. Speech engines rarely add commas, so "milk bread and eggs"
     * arrives as "milk bread" + "eggs". A chunk that closely matches one item stays
     * one item ("oat milk", "sour cream"); otherwise each word that clearly names an
     * item becomes its own item. Anything left unmatched becomes a custom item.
     */
    private fun itemsInPart(part: String, catalog: List<CatalogItem>): List<CatalogItem> {
        fun best(q: String): Pair<CatalogItem, Double>? =
            catalog.map { it to score(q, it) }.maxWithOrNull(compareBy<Pair<CatalogItem, Double>> { it.second }.thenByDescending { -it.first.rank })
        val whole = best(part)
        // above 0.9 means the chunk IS a name ("oat milk"); exactly 0.9 only means it
        // contains a one-word name ("milk bread" contains "bread"), so try the words first
        if (whole != null && whole.second > 0.9) return listOf(whole.first)
        val words = part.split(" ").filter { it.length >= 3 }
        if (words.size >= 2) {
            val each = words.mapNotNull { w -> best(w)?.takeIf { it.second >= 0.85 }?.first }.distinctBy { it.key }
            if (each.size >= 2) return each
        }
        return listOf(if (whole != null && whole.second >= 0.66) whole.first else Catalog.custom(part))
    }

    /**
     * What the camera read off a packet. Packets carry a lot of other words, so
     * only whole-word matches count, plus close misreads of longer words.
     */
    fun photoCandidates(recognisedText: String, catalog: List<CatalogItem> = Catalog.items, limit: Int = 3): List<CatalogItem> {
        val text = " ${normalize(recognisedText)} "
        if (text.isBlank()) return emptyList()
        val tokens = text.trim().split(" ").filter { it.length >= 4 }
        val scored = catalog.mapNotNull { item ->
            var s = 0.0
            for (raw in listOf(item.name) + item.aliases) {
                val n = normalize(raw)
                if (n.length < 3) continue
                s = when {
                    text.contains(" $n ") -> max(s, if (n.contains(' ')) 1.0 else 0.95)
                    text.contains(" ${stemPhrase(n)} ") -> max(s, 0.9)
                    !n.contains(' ') && n.length >= 5 && tokens.any { similarity(it, n) >= 0.8 } -> max(s, 0.75)
                    else -> s
                }
            }
            if (s > 0) item to s else null
        }
        return scored
            .sortedWith(compareByDescending<Pair<CatalogItem, Double>> { it.second }
                .thenByDescending { it.first.name.length } // "Oat milk" beats "Milk" when both appear
                .thenBy { it.first.rank })
            .take(limit)
            .map { it.first }
    }

    /**
     * Cleans what the AI said a photo shows ("A mandarin." -> "Mandarin"). Null when it
     * could not tell, or answered with something that is not a short item name.
     */
    fun cleanAiAnswer(raw: String?): String? {
        if (raw == null) return null
        var t = raw.trim().lines().firstOrNull()?.trim() ?: return null
        t = t.trim('"', '\'', '*', '.', '!', ' ', '\u201C', '\u201D')
        t = t.replace(Regex("^(it is|it's|this is|that is|a photo of|an image of)\\s+", RegexOption.IGNORE_CASE), "")
        t = t.replace(Regex("^(a|an|some|the)\\s+", RegexOption.IGNORE_CASE), "").trim()
        val n = normalize(t)
        if (n.isEmpty() || n == "unknown" || n == "none" || n.startsWith("not ") || n.startsWith("i ") || n.split(' ').size > 4) return null
        return t.split(' ').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercaseChar() } }
    }

    /**
     * Choices after a photo: what the AI saw comes first (as the catalogue item, or as
     * typed if the catalogue has no such thing), then anything read off the packet.
     */
    fun photoChoices(aiAnswer: String?, recognisedText: String, catalog: List<CatalogItem> = Catalog.items, limit: Int = 3): List<CatalogItem> {
        val seen = cleanAiAnswer(aiAnswer)
        val fromAi = seen?.let { name ->
            val exact = catalog.firstOrNull { c -> normalize(c.name) == normalize(name) || c.aliases.any { normalize(it) == normalize(name) } }
            exact ?: search(name, catalog, limit = 1, minScore = 0.9).firstOrNull() ?: Catalog.custom(name)
        }
        return (listOfNotNull(fromAi) + photoCandidates(recognisedText, catalog, limit)).distinctBy { it.key }.take(limit)
    }
}
