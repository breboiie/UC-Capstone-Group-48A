package au.edu.canberra.team48.shoppinglist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun item(id: String, name: String, ticked: Boolean = false, addedAt: Long = 0L) =
    ListItem(id = id, name = name, key = Catalog.items.firstOrNull { it.name == name }?.key ?: Catalog.customKey(name),
        category = Category.OTHER, ticked = ticked, addedAt = addedAt)

class ListLogicTest {

    @Test fun newItemsGoAtTheBottomLikePaper() {
        val r = ListLogic.add(listOf(item("1", "Milk")), item("2", "Bread"))
        assertTrue(r is ListLogic.AddResult.Added)
        assertEquals(listOf("Milk", "Bread"), r.list.map { it.name })
    }

    @Test fun addingSomethingAlreadyWaitingChangesNothing() {
        val list = listOf(item("1", "Milk"))
        val r = ListLogic.add(list, item("2", "milk"))
        assertTrue(r is ListLogic.AddResult.AlreadyThere)
        assertEquals(1, r.list.size)
    }

    @Test fun addingSomethingAlreadyTickedUnticksItInPlace() {
        val list = listOf(item("1", "Milk", ticked = true), item("2", "Eggs"))
        val r = ListLogic.add(list, item("9", "Milk"))
        assertTrue(r is ListLogic.AddResult.Unticked)
        assertEquals(listOf("1", "2"), r.list.map { it.id })
        assertFalse(r.list[0].ticked)
    }

    @Test fun tickedItemsKeepTheirPlace() {
        val list = listOf(item("1", "Milk"), item("2", "Bread"), item("3", "Eggs"))
        val after = ListLogic.toggle(list, "2")
        assertEquals(listOf("1", "2", "3"), after.map { it.id })
        assertTrue(after[1].ticked)
        assertEquals(2, ListLogic.remaining(after))
    }

    @Test fun undoPutsARemovedItemBackWhereItWas() {
        val list = listOf(item("1", "Milk"), item("2", "Bread"), item("3", "Eggs"))
        val (after, removed) = ListLogic.remove(list, "2")
        assertEquals(listOf("1", "3"), after.map { it.id })
        assertNotNull(removed)
        assertEquals(listOf("1", "2", "3"), ListLogic.undo(after, removed!!).map { it.id })
    }

    @Test fun removingSomethingMissingIsHarmless() {
        val list = listOf(item("1", "Milk"))
        val (after, removed) = ListLogic.remove(list, "nope")
        assertEquals(list, after)
        assertNull(removed)
    }

    @Test fun undoTwiceDoesNotDuplicate() {
        val list = listOf(item("1", "Milk"), item("2", "Bread"))
        val (after, removed) = ListLogic.remove(list, "1")
        val once = ListLogic.undo(after, removed!!)
        assertEquals(once, ListLogic.undo(once, removed))
    }

    @Test fun ticksFromAnotherPhoneAreSeenOnce() {
        val before = listOf(item("1", "Milk"), item("2", "Bread", ticked = true))
        val after = listOf(item("1", "Milk", ticked = true), item("2", "Bread", ticked = true), item("3", "Eggs", ticked = true))
        assertEquals(listOf("1"), ListLogic.newlyTicked(before, after).map { it.id })
    }

    @Test fun oldestWaitingIgnoresTickedItems() {
        val list = listOf(item("1", "Milk", ticked = true, addedAt = 10), item("2", "Bread", addedAt = 50), item("3", "Eggs", addedAt = 40))
        assertEquals(40L, ListLogic.oldestWaiting(list))
        assertEquals(0L, ListLogic.oldestWaiting(emptyList()))
    }
}

class MatchingTest {

    private fun top(q: String) = Matching.search(q).firstOrNull()?.key

    @Test fun normalizeRemovesAccentsAndPunctuation() {
        assertEquals("mjolk", Matching.normalize("Mjölk!"))
        assertEquals("sweet potato", Matching.normalize("  Sweet   Potato. "))
    }

    @Test fun typingFindsItemsByPrefix() {
        assertEquals("bananas", top("ban"))
        assertEquals("sweet_potato", top("sweet p"))
        assertEquals("milk", top("milk"))
    }

    @Test fun aliasesUseAustralianAndEverydayWords() {
        assertEquals("capsicum", top("bell pepper"))
        assertEquals("eggplant", top("aubergine"))
        assertEquals("rockmelon", top("cantaloupe"))
        assertEquals("tea", top("tea"))
    }

    @Test fun spellingMistakesStillMatch() {
        assertEquals("bananas", top("bananna"))
        assertEquals("tomatoes", top("tomatoe"))
    }

    @Test fun nonsenseMatchesNothing() {
        assertTrue(Matching.search("xqzv").isEmpty())
        assertTrue(Matching.search("   ").isEmpty())
    }

    @Test fun spokenQuantitiesAreIgnored() {
        assertEquals("milk", Matching.cleanSpoken("Add two litres of milk to my list please"))
        assertEquals("milk", Matching.voiceCandidates(listOf("two litres of milk")).first().key)
    }

    @Test fun voiceUsesEveryGuessFromTheSpeechEngine() {
        // first guess is wrong, second is right
        val got = Matching.voiceCandidates(listOf("banner", "bananas", "bandanas")).map { it.key }
        assertTrue(got.contains("bananas"))
    }

    @Test fun voiceNeverOffersMoreThanTwoChoices() {
        assertTrue(Matching.voiceCandidates(listOf("milk", "oat milk", "soy milk", "light milk")).size <= 2)
    }

    @Test fun voiceWithNothingUsefulOffersNothing() {
        assertTrue(Matching.voiceCandidates(listOf("um please add the")).isEmpty())
    }

    @Test fun photoReadsTheNameOffThePacket() {
        val text = "OATLY\nTHE ORIGINAL\nOAT MILK\n1 L\nKeep refrigerated after opening"
        assertEquals("milk", Matching.photoCandidates(text).first().key)
    }

    @Test fun photoPrefersTheLongerMatchingName() {
        val text = "SWEET POTATO  Product of Australia  Potato growers"
        // both "potato" and "sweet potato" appear; the more specific wins
        assertEquals("sweet_potato", Matching.photoCandidates(text).first().key)
    }

    @Test fun photoWithNoKnownWordsFindsNothing() {
        assertTrue(Matching.photoCandidates("Best before 12 OCT").isEmpty())
    }

    @Test fun photoToleratesAnOcrMisread() {
        // "BANANAS" misread as "BANANAB"
        assertEquals("bananas", Matching.photoCandidates("ORGANIC BANANAB 1KG").firstOrNull()?.key)
    }
}

class SuggestionsTest {
    private val day = Suggestions.DAY_MS
    private val now = 1_000 * day

    @Test fun offersThingsBoughtAtLeastTwice() {
        val h = listOf(Purchase("milk", now - day), Purchase("milk", now - 8 * day), Purchase("bread", now - 2 * day))
        assertEquals(listOf("milk"), Suggestions.usual(h, emptySet(), now))
    }

    @Test fun mostOftenBoughtComesFirst() {
        val h = List(3) { Purchase("eggs", now - it * day) } + List(5) { Purchase("milk", now - it * day) } + List(2) { Purchase("tea", now - it * day) }
        assertEquals(listOf("milk", "eggs", "tea"), Suggestions.usual(h, emptySet(), now))
    }

    @Test fun neverOffersWhatIsAlreadyOnTheList() {
        val h = List(4) { Purchase("milk", now - it * day) } + List(3) { Purchase("eggs", now - it * day) }
        assertEquals(listOf("eggs"), Suggestions.usual(h, setOf("milk"), now))
    }

    @Test fun forgetsPurchasesOlderThanTwelveWeeks() {
        val h = listOf(Purchase("milk", now - 100 * day), Purchase("milk", now - 90 * day))
        assertTrue(Suggestions.usual(h, emptySet(), now).isEmpty())
        assertTrue(Suggestions.prune(h, now).isEmpty())
    }

    @Test fun offeredAtMostOnceADay() {
        val today = Suggestions.dayIndex(now, 0)
        assertTrue(Suggestions.shouldOffer(true, 2, today - 1, today))
        assertFalse(Suggestions.shouldOffer(true, 2, today, today))
        assertFalse(Suggestions.shouldOffer(false, 2, today - 1, today))
        assertFalse(Suggestions.shouldOffer(true, 0, today - 1, today))
    }

    @Test fun birthdayCardAsksOnlyAboutAPresent() {
        assertEquals(listOf("present"), Suggestions.companionsFor("birthday_card", emptySet()))
    }

    @Test fun noPresentQuestionWhenOneIsAlreadyOnTheList() {
        assertTrue(Suggestions.companionsFor("birthday_card", setOf("present")).isEmpty())
        assertTrue(Suggestions.companionsFor("milk", emptySet()).isEmpty())
    }

    @Test fun everyCompanionIsARealCatalogueItem() {
        Suggestions.companions.forEach { (k, v) ->
            assertNotNull(Catalog.find(k))
            v.forEach { assertNotNull("missing $it", Catalog.find(it)) }
        }
    }
}

class RemindersTest {
    private val day = Suggestions.DAY_MS
    private val now = 1_000 * day

    @Test fun remindsAfterTheChosenNumberOfDays() {
        assertTrue(Reminders.shouldRemind(true, 7, now - 7 * day, 0, 3, now))
        assertFalse(Reminders.shouldRemind(true, 7, now - 6 * day, 0, 3, now))
    }

    @Test fun neverRemindsWhenOffOrNothingIsWaiting() {
        assertFalse(Reminders.shouldRemind(false, 7, now - 30 * day, 0, 3, now))
        assertFalse(Reminders.shouldRemind(true, 7, now - 30 * day, 0, 0, now))
    }

    @Test fun beforeTheFirstShopUsesTheOldestItem() {
        assertTrue(Reminders.shouldRemind(true, 3, 0, now - 4 * day, 2, now))
        assertFalse(Reminders.shouldRemind(true, 3, 0, 0, 2, now))
    }
}

class PairingTest {

    @Test fun codesAreAlwaysSixDigitsWithNoLeadingZero() {
        val r = kotlin.random.Random(42)
        repeat(2_000) {
            val c = Pairing.newCode(r)
            assertTrue(c, Pairing.isWellFormed(c))
            assertTrue(c[0] != '0')
        }
    }

    @Test fun qrRoundTripsAndRejectsStrangers() {
        assertEquals("492713", Pairing.codeFromScan(Pairing.qrPayload("492713")))
        assertEquals("492713", Pairing.codeFromScan(" 492713 "))
        assertNull(Pairing.codeFromScan("https://example.com"))
        assertNull(Pairing.codeFromScan("SHOPLIST:12"))
        assertNull(Pairing.codeFromScan(null))
    }

    @Test fun expiryIsTenMinutes() {
        val made = 5_000L
        assertFalse(Pairing.isExpired(made + Pairing.TTL_MS, made + Pairing.TTL_MS - 1))
        assertTrue(Pairing.isExpired(made + Pairing.TTL_MS, made + Pairing.TTL_MS))
    }

    @Test fun displayAndTyping() {
        assertEquals("492 713", Pairing.display("492713"))
        assertEquals("492713", Pairing.typed("49-27 13 99"))
    }
}

class ProfileRulesTest {

    @Test fun namesAreTidied() {
        assertEquals("Mary Smith", ProfileRules.cleanName("  Mary    Smith "))
        assertFalse(ProfileRules.isValidName("   "))
        assertEquals("M", ProfileRules.initial("mary"))
    }

    @Test fun phoneNumbersAcceptTheWaysPeopleTypeThem() {
        assertTrue(ProfileRules.isValidPhone("0412 345 678"))
        assertTrue(ProfileRules.isValidPhone("+61 412-345-678"))
        assertTrue(ProfileRules.isValidPhone("(02) 6201 5111"))
        assertFalse(ProfileRules.isValidPhone("1234"))
        assertFalse(ProfileRules.isValidPhone("0412 abc 678"))
    }

    @Test fun mobilesAreFormattedTheAustralianWay() {
        assertEquals("0412 345 678", ProfileRules.formatPhone("0412345678"))
        assertEquals("(02) 6201 5111", ProfileRules.formatPhone("0262015111"))
    }

    @Test fun spokenNumbersBecomeDigits() {
        assertEquals("0412345678", ProfileRules.digitsFromSpeech("zero four one two three four five six seven eight"))
        assertEquals("0412345678", ProfileRules.digitsFromSpeech("0412 345 678"))
        assertEquals("04412", ProfileRules.digitsFromSpeech("oh double four one two"))
    }

    @Test fun namesJoinNaturally() {
        assertEquals("Mary and Frank", ProfileRules.joinNames(listOf("Mary", "Frank")))
        assertEquals("Mary, Frank and Sam", ProfileRules.joinNames(listOf("Mary", "Frank", "Sam")))
    }
}

class CatalogTest {

    @Test fun keysAreUniqueAndResourceSafe() {
        val keys = Catalog.items.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        keys.forEach { assertTrue(it, it.matches(Regex("[a-z][a-z0-9_]*"))) }
    }

    @Test fun everyItemCanBeFoundByItsOwnName() {
        Catalog.items.forEach { assertEquals(it.name, it.key, Matching.search(it.name).firstOrNull()?.key) }
    }

    @Test fun customItemsGetAStableKey() {
        assertEquals("custom:pikelets", Catalog.customKey("Pikelets!"))
        assertEquals("Pikelets", Catalog.custom("  pikelets ").name)
        assertTrue(Catalog.isCustom(Catalog.customKey("x")))
    }

    @Test fun everyItemHasAPicture() {
        Catalog.items.forEach { assertTrue(it.name, it.hasPhoto) }
    }
}
