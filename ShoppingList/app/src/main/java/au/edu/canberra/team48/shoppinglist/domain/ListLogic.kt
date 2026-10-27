package au.edu.canberra.team48.shoppinglist.domain

/**
 * The rules of the paper-like list.
 *
 * New items go at the bottom, the way someone writes on paper. Ticked items stay
 * exactly where they are and turn grey. Nothing moves on its own.
 */
object ListLogic {

    sealed interface AddResult {
        val list: List<ListItem>
        val item: ListItem

        /** A new row was written at the bottom. */
        data class Added(override val list: List<ListItem>, override val item: ListItem) : AddResult

        /** Already on the list and not yet bought, so nothing changes. */
        data class AlreadyThere(override val list: List<ListItem>, override val item: ListItem) : AddResult

        /** Was ticked off earlier, so it is simply unticked again in its old place. */
        data class Unticked(override val list: List<ListItem>, override val item: ListItem) : AddResult
    }

    private fun same(a: ListItem, b: ListItem): Boolean =
        a.key == b.key || Matching.normalize(a.name) == Matching.normalize(b.name)

    fun add(list: List<ListItem>, item: ListItem): AddResult {
        // only the current list counts: something bought before is written again as new
        val existing = list.firstOrNull { it.clearedAt == 0L && same(it, item) }
        return when {
            existing == null -> AddResult.Added(list + item, item)
            !existing.ticked -> AddResult.AlreadyThere(list, existing)
            else -> {
                val again = existing.copy(ticked = false)
                AddResult.Unticked(list.map { if (it.id == existing.id) again else it }, again)
            }
        }
    }

    fun setTicked(list: List<ListItem>, id: String, ticked: Boolean): List<ListItem> =
        list.map { if (it.id == id) it.copy(ticked = ticked) else it }

    fun toggle(list: List<ListItem>, id: String): List<ListItem> =
        list.map { if (it.id == id) it.copy(ticked = !it.ticked) else it }

    /** Where a removed item was, so Undo can put it back in the same place. */
    data class Removed(val item: ListItem, val index: Int)

    fun remove(list: List<ListItem>, id: String): Pair<List<ListItem>, Removed?> {
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return list to null
        return list.filterIndexed { i, _ -> i != index } to Removed(list[index], index)
    }

    fun undo(list: List<ListItem>, removed: Removed): List<ListItem> {
        if (list.any { it.id == removed.item.id }) return list
        val at = removed.index.coerceIn(0, list.size)
        return list.take(at) + removed.item + list.drop(at)
    }

    fun remaining(list: List<ListItem>): Int = list.count { !it.ticked }

    /** Ticked items, the ones "Clear ticked items" takes away. Unticked items always stay. */
    fun ticked(list: List<ListItem>): List<ListItem> = list.filter { it.ticked }

    /** True when there is something on the list and every item has been bought. */
    fun allBought(list: List<ListItem>): Boolean = list.isNotEmpty() && list.all { it.ticked }

    /** Puts items back after an undo, in their original order, never twice. */
    fun restore(list: List<ListItem>, items: List<ListItem>): List<ListItem> {
        val have = list.map { it.id }.toSet()
        return (list + items.filter { it.id !in have }).sortedBy { it.addedAt }
    }

    /** Keys of items that are on the list and still to buy. */
    fun waitingKeys(list: List<ListItem>): Set<String> = list.filter { !it.ticked }.map { it.key }.toSet()

    /** Oldest time an unbought item was added, used by the reminder. 0 when nothing is waiting. */
    fun oldestWaiting(list: List<ListItem>): Long = list.filter { !it.ticked }.minOfOrNull { it.addedAt } ?: 0L

    // ---------------------------------------------------------------- Bought before

    /** The current list: everything not yet cleared away. */
    fun current(list: List<ListItem>): List<ListItem> = list.filter { it.clearedAt == 0L }

    /**
     * Bought before, as folders: everything cleared at the same moment ("Clear ticked items"
     * or "Start a new list") is one past list, newest first, items in the order they were added.
     */
    fun pastLists(list: List<ListItem>): List<PastList> =
        list.filter { it.clearedAt > 0L }
            .groupBy { it.clearedAt }
            .map { (at, rows) -> PastList(at, rows.sortedBy { it.addedAt }) }
            .sortedByDescending { it.clearedAt }

    /** Cleared rows older than [keepDays] days, to be deleted. */
    fun expired(list: List<ListItem>, now: Long, keepDays: Int): List<ListItem> {
        val cutoff = now - keepDays * DAY_MS
        return list.filter { it.clearedAt in 1 until cutoff }
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /**
     * Items that went from not ticked to ticked between two snapshots of the list.
     * Ticks from any connected phone arrive this way, so purchases are recorded once.
     */
    fun newlyTicked(before: List<ListItem>, after: List<ListItem>): List<ListItem> {
        val was = before.associateBy { it.id }
        return after.filter { it.ticked && was[it.id]?.ticked == false }
    }
}
