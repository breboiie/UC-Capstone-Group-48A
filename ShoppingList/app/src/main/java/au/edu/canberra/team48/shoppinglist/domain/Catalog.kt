package au.edu.canberra.team48.shoppinglist.domain

import au.edu.canberra.team48.shoppinglist.domain.Category.BREAD
import au.edu.canberra.team48.shoppinglist.domain.Category.CARDS_GIFTS
import au.edu.canberra.team48.shoppinglist.domain.Category.CLEANING
import au.edu.canberra.team48.shoppinglist.domain.Category.DAIRY
import au.edu.canberra.team48.shoppinglist.domain.Category.DRINKS
import au.edu.canberra.team48.shoppinglist.domain.Category.FROZEN
import au.edu.canberra.team48.shoppinglist.domain.Category.FRUIT_VEG
import au.edu.canberra.team48.shoppinglist.domain.Category.MEAT
import au.edu.canberra.team48.shoppinglist.domain.Category.PANTRY

/**
 * Everyday grocery items with Australian names. Every item has a picture.
 *
 * Fruit, vegetables and the milk, yoghurt, sour cream and juice cartons are real
 * photographs from the Grocery Store Dataset (Klasson et al. 2019, MIT licence),
 * taken by the dataset authors in store. The other items use 3D pictures from
 * Microsoft Fluent Emoji (MIT licence). A household's own photo of a product
 * replaces the picture for that item.
 */
object Catalog {

    private fun p(key: String, name: String, cat: Category, rank: Int, vararg aliases: String) =
        CatalogItem(key, name, cat, hasPhoto = true, aliases = aliases.toList(), rank = rank)

    val items: List<CatalogItem> = listOf(
        // ---- fruit and veg, real photos
        p("apples", "Apples", FRUIT_VEG, 5, "apple", "pink lady", "granny smith", "royal gala", "red apples", "green apples"),
        p("bananas", "Bananas", FRUIT_VEG, 3, "banana"),
        p("oranges", "Oranges", FRUIT_VEG, 12, "orange"),
        p("mandarins", "Mandarins", FRUIT_VEG, 25, "mandarin", "satsuma", "satsumas"),
        p("lemons", "Lemons", FRUIT_VEG, 20, "lemon"),
        p("limes", "Limes", FRUIT_VEG, 40, "lime"),
        p("kiwi_fruit", "Kiwi fruit", FRUIT_VEG, 35, "kiwi", "kiwis", "kiwifruit"),
        p("mangoes", "Mangoes", FRUIT_VEG, 38, "mango", "mangos"),
        p("avocados", "Avocados", FRUIT_VEG, 22, "avocado", "avo", "avos"),
        p("pears", "Pears", FRUIT_VEG, 28, "pear"),
        p("peaches", "Peaches", FRUIT_VEG, 42, "peach"),
        p("nectarines", "Nectarines", FRUIT_VEG, 45, "nectarine"),
        p("plums", "Plums", FRUIT_VEG, 48, "plum"),
        p("pineapple", "Pineapple", FRUIT_VEG, 50, "pineapples"),
        p("watermelon", "Watermelon", FRUIT_VEG, 46, "water melon", "melon"),
        p("rockmelon", "Rockmelon", FRUIT_VEG, 52, "rock melon", "cantaloupe"),
        p("grapefruit", "Grapefruit", FRUIT_VEG, 60, "grapefruits", "grape fruit"),
        p("carrots", "Carrots", FRUIT_VEG, 6, "carrot"),
        p("potatoes", "Potatoes", FRUIT_VEG, 8, "potato", "spuds", "spud"),
        p("sweet_potato", "Sweet potato", FRUIT_VEG, 36, "sweet potatoes", "kumara"),
        p("onions", "Onions", FRUIT_VEG, 9, "onion", "brown onion", "brown onions"),
        p("tomatoes", "Tomatoes", FRUIT_VEG, 7, "tomato"),
        p("capsicum", "Capsicum", FRUIT_VEG, 30, "capsicums", "red capsicum", "bell pepper", "peppers"),
        p("cucumber", "Cucumber", FRUIT_VEG, 18, "cucumbers"),
        p("mushrooms", "Mushrooms", FRUIT_VEG, 24, "mushroom"),
        p("garlic", "Garlic", FRUIT_VEG, 27, "garlic bulb"),
        p("ginger", "Ginger", FRUIT_VEG, 55),
        p("zucchini", "Zucchini", FRUIT_VEG, 33, "zucchinis", "courgette"),
        p("cabbage", "Cabbage", FRUIT_VEG, 44),
        p("eggplant", "Eggplant", FRUIT_VEG, 54, "eggplants", "aubergine"),
        p("leek", "Leek", FRUIT_VEG, 58, "leeks"),
        p("asparagus", "Asparagus", FRUIT_VEG, 56),
        p("beetroot", "Beetroot", FRUIT_VEG, 59, "beet", "beets", "beetroots"),

        // ---- dairy and drinks. One item each, as people write it on paper: "Milk", not
        // "2L light milk". The kinds are kept as other words for the same item, so saying
        // or photographing "oat milk" still finds Milk.
        p("milk", "Milk", DAIRY, 1, "full cream milk", "whole milk", "mjolk", "light milk", "lite milk", "low fat milk",
            "skim milk", "skinny milk", "reduced fat milk", "lactose free milk", "lactose free", "oat milk", "oatmilk",
            "oat drink", "soy milk", "soya milk", "almond milk"),
        p("yoghurt", "Yoghurt", DAIRY, 11, "yogurt", "yoghurts", "yogurts", "natural yoghurt"),
        p("sour_cream", "Sour cream", DAIRY, 47),
        p("juice", "Juice", DRINKS, 16, "orange juice", "apple juice", "oj", "fruit juice"),

        // ---- pictures from Microsoft Fluent Emoji (MIT licence); see ATTRIBUTION.md
        p("bread", "Bread", BREAD, 2, "loaf", "white bread", "wholemeal bread", "bread rolls", "rolls", "bread roll"),
        p("eggs", "Eggs", DAIRY, 4, "egg", "dozen eggs"),
        p("cheese", "Cheese", DAIRY, 10, "cheddar", "tasty cheese"),
        p("butter", "Butter", DAIRY, 13, "margarine"),
        p("cream", "Cream", DAIRY, 49, "thickened cream"),
        p("chicken", "Chicken", MEAT, 14, "chicken breast", "chicken thighs"),
        p("mince", "Mince", MEAT, 21, "beef mince", "mince meat"),
        p("sausages", "Sausages", MEAT, 31, "sausage", "snags"),
        p("fish", "Fish", MEAT, 43, "salmon", "fish fillets"),
        p("tea", "Tea", PANTRY, 17, "tea bags", "teabags"),
        p("coffee", "Coffee", PANTRY, 19, "instant coffee"),
        p("sugar", "Sugar", PANTRY, 41),
        p("rice", "Rice", PANTRY, 23),
        p("pasta", "Pasta", PANTRY, 39, "spaghetti", "noodles"),
        p("flour", "Flour", PANTRY, 53, "plain flour", "self raising flour"),
        p("cereal", "Cereal", PANTRY, 51, "weetbix", "weet bix", "cornflakes", "porridge", "oats"),
        p("biscuits", "Biscuits", PANTRY, 57, "biscuit", "cookies"),
        p("water", "Water", DRINKS, 61, "bottled water", "sparkling water"),
        p("frozen_peas", "Frozen peas", FROZEN, 62, "peas", "frozen vegetables"),
        p("ice_cream", "Ice cream", FROZEN, 63, "icecream"),
        p("toilet_paper", "Toilet paper", CLEANING, 64, "toilet rolls", "loo paper"),
        p("dish_soap", "Dishwashing liquid", CLEANING, 65, "dish soap", "detergent", "washing up liquid"),
        p("laundry", "Laundry powder", CLEANING, 66, "washing powder", "laundry liquid"),
        p("birthday_card", "Birthday card", CARDS_GIFTS, 70, "card", "birthday cards"),
        p("present", "Present", CARDS_GIFTS, 71, "gift", "birthday present"),
        p("wrapping_paper", "Wrapping paper", CARDS_GIFTS, 72, "gift wrap"),
        p("candles", "Candles", CARDS_GIFTS, 73, "birthday candles", "candle"),
    )

    /**
     * Items from earlier versions that are now one item. Lists saved on a phone may
     * still hold the old keys; they show and behave as the new item.
     */
    private val renamed = mapOf(
        "light_milk" to "milk", "lactose_free_milk" to "milk", "oat_milk" to "milk", "soy_milk" to "milk",
        "orange_juice" to "juice", "apple_juice" to "juice", "bread_rolls" to "bread",
    )

    private val byKey: Map<String, CatalogItem> = items.associateBy { it.key }

    fun find(key: String): CatalogItem? = byKey[renamed[key] ?: key]

    /** Categories in the order the chips appear, only those that have items. */
    val categories: List<Category> = Category.entries.filter { c -> items.any { it.category == c } }

    const val CUSTOM_PREFIX = "custom:"

    fun customKey(name: String): String = CUSTOM_PREFIX + Matching.normalize(name).replace(' ', '_')

    fun isCustom(key: String): Boolean = key.startsWith(CUSTOM_PREFIX)

    /** Builds a catalogue-shaped entry for something the household typed or said. */
    fun custom(name: String): CatalogItem {
        val clean = ProfileRules.cleanName(name).replaceFirstChar { it.uppercase() }
        return CatalogItem(customKey(clean), clean, Category.OTHER, hasPhoto = false, icon = "ic_grocery", rank = 999)
    }

    /** Looks up by key, or rebuilds a custom entry from a list item. */
    fun forListItem(item: ListItem): CatalogItem =
        find(item.key) ?: CatalogItem(item.key, item.name, item.category, false, "ic_grocery", rank = 999)
}
