package au.edu.canberra.team48.shoppinglist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY = 24L * 60 * 60 * 1000

private fun row(id: String, key: String, ticked: Boolean = false, clearedAt: Long = 0L, name: String = key) =
    ListItem(id = id, name = name, key = key, category = Category.OTHER, ticked = ticked, addedAt = id.hashCode().toLong(), clearedAt = clearedAt)

/** Bought before: cleared items stay, greyed out, until they are deleted after a set number of days. */
class BoughtBeforeTest {

    private val now = 1_000 * DAY

    @Test fun clearedItemsLeaveTheCurrentListButAreKept() {
        val all = listOf(row("1", "milk"), row("2", "bread", ticked = true, clearedAt = now))
        assertEquals(listOf("1"), ListLogic.current(all).map { it.id })
        assertEquals(listOf("2"), ListLogic.pastLists(all).single().items.map { it.id })
    }

    @Test fun eachClearingIsOneFolderNewestFirst() {
        val week = now - 7 * DAY
        val all = listOf(
            row("a", "bananas", ticked = true, clearedAt = week),
            row("b", "bread", ticked = true, clearedAt = week),
            row("c", "milk", ticked = true, clearedAt = now),
            row("d", "eggs"),
        )
        val folders = ListLogic.pastLists(all)
        assertEquals(listOf(now, week), folders.map { it.clearedAt })
        assertEquals(setOf("c"), folders[0].items.map { it.id }.toSet())
        assertEquals(setOf("a", "b"), folders[1].items.map { it.id }.toSet())
    }

    @Test fun theSameThingCanBeInTwoOldLists() {
        val all = listOf(
            row("a", "milk", ticked = true, clearedAt = now - 7 * DAY),
            row("b", "milk", ticked = true, clearedAt = now - DAY),
        )
        assertEquals(2, ListLogic.pastLists(all).size)
    }

    @Test fun olderThanTheSettingIsDeletedNewerIsKept() {
        val all = listOf(
            row("old", "milk", ticked = true, clearedAt = now - 8 * DAY),
            row("new", "eggs", ticked = true, clearedAt = now - 6 * DAY),
            row("list", "bread"),
        )
        assertEquals(listOf("old"), ListLogic.expired(all, now, keepDays = 7).map { it.id })
        assertTrue(ListLogic.expired(all, now, keepDays = 14).isEmpty())
        assertEquals(setOf("old", "new"), ListLogic.expired(all, now, keepDays = 3).map { it.id }.toSet())
    }

    @Test fun addingSomethingBoughtBeforeWritesItAgainOnTheList() {
        val all = listOf(row("old", "milk", ticked = true, clearedAt = now))
        val r = ListLogic.add(all, row("new", "milk"))
        assertTrue(r is ListLogic.AddResult.Added)
    }

}

/** Remind me and Bought before: minus and plus, and quick choices of 3, 7 and 14 days. */
class DaysTest {
    @Test fun quickChoices() = assertEquals(listOf(3, 7, 14), Days.QUICK)

    @Test fun minusAndPlusStayInRange() {
        assertEquals(1, Days.fewer(1))
        assertEquals(6, Days.fewer(7))
        assertEquals(8, Days.more(7))
        assertEquals(Days.MAX, Days.more(Days.MAX))
        assertEquals(Days.MIN, Days.clamp(-5))
    }

    @Test fun labelsReadNaturally() {
        assertEquals("1 day", Days.label(1))
        assertEquals("7 days", Days.label(7))
    }
}

/** The search bar on Add items. */
class SearchBarTest {
    @Test fun firstLettersShowMatchingPictures() {
        val keys = Matching.browse("ma").map { it.key }
        assertTrue("mandarins" in keys)
        assertTrue("mangoes" in keys)
        assertFalse("milk" in keys)
    }

    @Test fun otherWordsForAnItemAlsoMatch() {
        assertTrue("milk" in Matching.browse("oat").map { it.key })
    }

    @Test fun aSpellingMistakeStillFindsIt() {
        assertTrue("bananas" in Matching.browse("bannana").map { it.key })
    }

    @Test fun exactNameNeedsNoAddButton() {
        val milk = Catalog.find("milk")!!
        assertTrue(Matching.isExactly("Milk", milk))
        assertTrue(Matching.isExactly("oat milk", milk))
        assertFalse(Matching.isExactly("mil", milk))
    }

    @Test fun emptySearchShowsNothingSpecial() = assertTrue(Matching.browse("  ").isEmpty())
}

/** Photo: what the AI says it sees becomes the first choice. */
class PhotoAiTest {
    @Test fun aMandarinIsRecognised() {
        assertEquals("mandarins", Matching.photoChoices("Mandarin", "").first().key)
        assertEquals("mandarins", Matching.photoChoices("A mandarin.", "").first().key)
    }

    @Test fun answersAreCleanedUp() {
        assertEquals("Mandarin", Matching.cleanAiAnswer("\"Mandarin.\""))
        assertEquals("Bread", Matching.cleanAiAnswer("It is a bread"))
        assertEquals(null, Matching.cleanAiAnswer("unknown"))
        assertEquals(null, Matching.cleanAiAnswer("I cannot tell what this is from the photo"))
    }

    @Test fun somethingNotInTheCatalogueIsOfferedAsSaid() {
        assertEquals("Dragon Fruit", Matching.photoChoices("dragon fruit", "").first().name)
    }

    @Test fun packetWordsStillCountAndComeAfter() {
        val keys = Matching.photoChoices("Milk", "ARLA OAT MILK 1L").map { it.key }
        assertEquals("milk", keys.first())
        assertEquals(1, keys.count { it == "milk" })
    }

    @Test fun withoutTheAiThePacketWordsAreUsed() {
        assertEquals("milk", Matching.photoChoices(null, "FULL CREAM MILK").first().key)
    }
}

class PhotoAiPluralsTest {
    @Test fun singularOrPluralBothFindTheItem() {
        for ((said, key) in listOf("Banana" to "bananas", "Tomato" to "tomatoes", "Egg" to "eggs", "Potato" to "potatoes", "Loaf of bread" to "bread", "Carrots" to "carrots")) {
            assertEquals(said, key, Matching.photoChoices(said, "").first().key)
        }
    }
}
