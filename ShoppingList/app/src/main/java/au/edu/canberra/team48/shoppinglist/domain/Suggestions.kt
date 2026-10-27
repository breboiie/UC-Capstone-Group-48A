package au.edu.canberra.team48.shoppinglist.domain

/**
 * The "AI" in the proposal, kept deliberately explainable: a frequency rule.
 * Something bought at least twice in the last twelve weeks, and not on the list,
 * is offered once a day. No model, no training data, and the Family member can
 * switch it off.
 */
object Suggestions {
    const val DAY_MS = 86_400_000L
    const val WINDOW_DAYS = 84 // twelve weeks
    const val MIN_TIMES = 2

    fun usual(
        history: List<Purchase>,
        onList: Set<String>,
        now: Long,
        limit: Int = 3,
        windowDays: Int = WINDOW_DAYS,
        minTimes: Int = MIN_TIMES,
    ): List<String> {
        val from = now - windowDays * DAY_MS
        return history.asSequence()
            .filter { it.at in from..now }
            .groupingBy { it.key }
            .eachCount()
            .filter { (key, count) -> count >= minTimes && key !in onList }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key }
    }

    fun timesBought(history: List<Purchase>, key: String, now: Long, windowDays: Int = WINDOW_DAYS): Int {
        val from = now - windowDays * DAY_MS
        return history.count { it.key == key && it.at in from..now }
    }

    /** Day number in local time, so "once a day" means once per calendar day. */
    fun dayIndex(epochMs: Long, utcOffsetMs: Long): Long = Math.floorDiv(epochMs + utcOffsetMs, DAY_MS)

    fun shouldOffer(enabled: Boolean, candidates: Int, lastShownDay: Long, today: Long): Boolean =
        enabled && candidates > 0 && lastShownDay != today

    /** Keeps history small: drops anything older than the window. */
    fun prune(history: List<Purchase>, now: Long, windowDays: Int = WINDOW_DAYS): List<Purchase> =
        history.filter { it.at >= now - windowDays * DAY_MS }

    // ---------------------------------------------------------------- birthday cards

    /**
     * Things usually bought together. Adding the key on the left asks about the item on the
     * right: a birthday card asks "Buy a present too?". One question, Yes or No, kept simple
     * on purpose; wrapping paper and candles can still be added like anything else.
     */
    val companions: Map<String, List<String>> = mapOf(
        "birthday_card" to listOf("present"),
    )

    /** Companions not already waiting on the list. */
    fun companionsFor(key: String, onList: Set<String>): List<String> =
        companions[key].orEmpty().filter { it !in onList }
}

/**
 * Number-of-days settings (Remind me, and how long Bought before keeps things):
 * a minus and plus button, and quick choices of 3, 7 and 14 days.
 */
object Days {
    const val MIN = 1
    const val MAX = 60
    val QUICK = listOf(3, 7, 14)
    const val KEEP_BOUGHT_DEFAULT = 7

    fun clamp(days: Int): Int = days.coerceIn(MIN, MAX)
    fun more(days: Int): Int = clamp(days + 1)
    fun fewer(days: Int): Int = clamp(days - 1)
    fun label(days: Int): String = if (days == 1) "1 day" else "$days days"
}

object Reminders {
    val DAY_CHOICES = Days.QUICK
    const val DEFAULT_DAYS = 7

    /**
     * "Remind me if I have not shopped for N days."
     * [lastShopAt] is the last time anything was ticked. Before the first shop,
     * the oldest waiting item stands in for it. Nothing waiting means no reminder.
     */
    fun shouldRemind(enabled: Boolean, days: Int, lastShopAt: Long, oldestWaitingAt: Long, waiting: Int, now: Long): Boolean {
        if (!enabled || waiting <= 0) return false
        val since = if (lastShopAt > 0) lastShopAt else oldestWaitingAt
        if (since <= 0) return false
        return now - since >= days * Suggestions.DAY_MS
    }
}
