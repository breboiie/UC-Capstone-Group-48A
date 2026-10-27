package au.edu.canberra.team48.shoppinglist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rules added after the client's review of the final wireframes. */
class SeveralItemsTest {

    @Test fun threeThingsInOneSentence() {
        val got = Matching.severalFromSpeech("milk, bread and eggs")!!.map { it.key }
        assertEquals(listOf("milk", "bread", "eggs"), got)
    }

    @Test fun fillerWordsAroundEachItemAreIgnored() {
        val got = Matching.severalFromSpeech("add some milk and a bag of bananas please")!!.map { it.key }
        assertEquals(listOf("milk", "bananas"), got)
    }

    @Test fun oneThingUsesTheSingleItemScreen() {
        assertNull(Matching.severalFromSpeech("two litres of milk"))
        assertNull(Matching.severalFromSpeech("milk"))
    }

    @Test fun somethingUnknownIsKeptNotDropped() {
        val got = Matching.severalFromSpeech("milk and pikelets")!!
        assertEquals("milk", got[0].key)
        assertEquals("Pikelets", got[1].name)
        assertTrue(Catalog.isCustom(got[1].key))
    }

    @Test fun sayingTheSameThingTwiceIsOneItem() {
        assertNull(Matching.severalFromSpeech("milk and milk"))
    }

    @Test fun otherJoiningWordsWork() {
        val got = Matching.severalFromSpeech("bread then butter plus tea")!!.map { it.key }
        assertEquals(listOf("bread", "butter", "tea"), got)
    }

    @Test fun fillerOnlyPartsAreSkipped() {
        val got = Matching.severalFromSpeech("milk and um and bread")!!.map { it.key }
        assertEquals(listOf("milk", "bread"), got)
    }
}

class ClearTickedTest {
    private fun item(id: String, ticked: Boolean, at: Long) =
        ListItem(id = id, name = id, key = id, category = Category.OTHER, ticked = ticked, addedAt = at)

    private val list = listOf(item("a", true, 1), item("b", false, 2), item("c", true, 3))

    @Test fun onlyTickedItemsAreCleared() {
        assertEquals(listOf("a", "c"), ListLogic.ticked(list).map { it.id })
    }

    @Test fun allBoughtNeedsEveryItemTicked() {
        assertFalse(ListLogic.allBought(list))
        assertTrue(ListLogic.allBought(ListLogic.ticked(list)))
        assertFalse(ListLogic.allBought(emptyList()))
    }

    @Test fun undoPutsThemBackInOrderOnce() {
        val left = list.filterNot { it.ticked }
        val back = ListLogic.restore(left, ListLogic.ticked(list))
        assertEquals(listOf("a", "b", "c"), back.map { it.id })
        assertEquals(3, ListLogic.restore(back, ListLogic.ticked(list)).size)
    }
}

class PhoneFromDeviceTest {
    @Test fun internationalFormFromThePhoneIsShownLocally() {
        assertEquals("0412 345 678", ProfileRules.formatPhone("+61412345678"))
        assertEquals("0412 345 678", ProfileRules.formatPhone("+61 412 345 678"))
    }

    @Test fun otherCountriesAreLeftAsGiven() {
        assertEquals("+64211234567", ProfileRules.formatPhone("+64211234567"))
    }

    @Test fun internationalFormIsValid() {
        assertTrue(ProfileRules.isValidPhone("+61412345678"))
    }
}

class SpeechWithoutCommasTest {
    @Test fun speechEnginesLeaveOutCommas() {
        val got = Matching.severalFromSpeech("milk bread and eggs")!!.map { it.key }
        assertEquals(listOf("milk", "bread", "eggs"), got)
    }

    @Test fun twoWordNamesStayTogether() {
        val got = Matching.severalFromSpeech("sweet potato and sour cream")!!.map { it.key }
        assertEquals(listOf("sweet_potato", "sour_cream"), got)
    }

    @Test fun noJoiningWordAtAll() {
        val got = Matching.severalFromSpeech("apples bananas carrots")!!.map { it.key }
        assertEquals(listOf("apples", "bananas", "carrots"), got)
    }

    @Test fun oneItemWithAnExtraWordIsStillOneItem() {
        assertNull(Matching.severalFromSpeech("chicken breast"))
        assertNull(Matching.severalFromSpeech("some fresh milk please"))
    }
}

/** "Just milk is enough, we don't need its type." */
class OneItemPerProductTest {
    @Test fun everyKindOfMilkIsJustMilk() {
        for (said in listOf("skim milk", "oat milk", "lactose free milk", "soy milk", "light milk", "full cream milk")) {
            assertEquals(said, "milk", Matching.voiceCandidates(listOf(said)).first().key)
            assertEquals(said, "milk", Matching.search(said, limit = 1).first().key)
        }
    }

    @Test fun anyJuiceIsJustJuice() {
        assertEquals("juice", Matching.voiceCandidates(listOf("orange juice")).first().key)
        assertEquals("juice", Matching.voiceCandidates(listOf("apple juice")).first().key)
    }

    @Test fun thereIsOnlyOneMilkAndOneJuice() {
        assertEquals(1, Catalog.items.count { it.name.lowercase().contains("milk") })
        assertEquals(1, Catalog.items.count { it.name.lowercase().contains("juice") })
    }

    @Test fun itemsSavedUnderOldKindsStillShowAsTheNewItem() {
        assertEquals("milk", Catalog.find("oat_milk")!!.key)
        assertEquals("juice", Catalog.find("orange_juice")!!.key)
        assertEquals("bread", Catalog.find("bread_rolls")!!.key)
    }
}
