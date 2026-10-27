package au.edu.canberra.team48.shoppinglist.ui

import au.edu.canberra.team48.shoppinglist.R

/**
 * Generated lookup from catalogue keys to bundled pictures. Explicit R references
 * (rather than getIdentifier) keep the pictures safe from resource shrinking.
 */
object Pictures {
    /** The picture for a catalogue key, or null for something typed that is not in the catalogue. */
    fun photo(key: String): Int? = when (key) {
        "apples" -> R.drawable.photo_apples
        "asparagus" -> R.drawable.photo_asparagus
        "avocados" -> R.drawable.photo_avocados
        "bananas" -> R.drawable.photo_bananas
        "beetroot" -> R.drawable.photo_beetroot
        "birthday_card" -> R.drawable.photo_birthday_card
        "biscuits" -> R.drawable.photo_biscuits
        "bread" -> R.drawable.photo_bread
        "butter" -> R.drawable.photo_butter
        "cabbage" -> R.drawable.photo_cabbage
        "candles" -> R.drawable.photo_candles
        "capsicum" -> R.drawable.photo_capsicum
        "carrots" -> R.drawable.photo_carrots
        "cereal" -> R.drawable.photo_cereal
        "cheese" -> R.drawable.photo_cheese
        "chicken" -> R.drawable.photo_chicken
        "coffee" -> R.drawable.photo_coffee
        "cream" -> R.drawable.photo_cream
        "cucumber" -> R.drawable.photo_cucumber
        "dish_soap" -> R.drawable.photo_dish_soap
        "eggplant" -> R.drawable.photo_eggplant
        "eggs" -> R.drawable.photo_eggs
        "fish" -> R.drawable.photo_fish
        "flour" -> R.drawable.photo_flour
        "frozen_peas" -> R.drawable.photo_frozen_peas
        "garlic" -> R.drawable.photo_garlic
        "ginger" -> R.drawable.photo_ginger
        "grapefruit" -> R.drawable.photo_grapefruit
        "ice_cream" -> R.drawable.photo_ice_cream
        "juice" -> R.drawable.photo_juice
        "kiwi_fruit" -> R.drawable.photo_kiwi_fruit
        "laundry" -> R.drawable.photo_laundry
        "leek" -> R.drawable.photo_leek
        "lemons" -> R.drawable.photo_lemons
        "limes" -> R.drawable.photo_limes
        "mandarins" -> R.drawable.photo_mandarins
        "mangoes" -> R.drawable.photo_mangoes
        "milk" -> R.drawable.photo_milk
        "mince" -> R.drawable.photo_mince
        "mushrooms" -> R.drawable.photo_mushrooms
        "nectarines" -> R.drawable.photo_nectarines
        "onions" -> R.drawable.photo_onions
        "oranges" -> R.drawable.photo_oranges
        "pasta" -> R.drawable.photo_pasta
        "peaches" -> R.drawable.photo_peaches
        "pears" -> R.drawable.photo_pears
        "pineapple" -> R.drawable.photo_pineapple
        "plums" -> R.drawable.photo_plums
        "potatoes" -> R.drawable.photo_potatoes
        "present" -> R.drawable.photo_present
        "rice" -> R.drawable.photo_rice
        "rockmelon" -> R.drawable.photo_rockmelon
        "sausages" -> R.drawable.photo_sausages
        "sour_cream" -> R.drawable.photo_sour_cream
        "sugar" -> R.drawable.photo_sugar
        "sweet_potato" -> R.drawable.photo_sweet_potato
        "tea" -> R.drawable.photo_tea
        "toilet_paper" -> R.drawable.photo_toilet_paper
        "tomatoes" -> R.drawable.photo_tomatoes
        "water" -> R.drawable.photo_water
        "watermelon" -> R.drawable.photo_watermelon
        "wrapping_paper" -> R.drawable.photo_wrapping_paper
        "yoghurt" -> R.drawable.photo_yoghurt
        "zucchini" -> R.drawable.photo_zucchini
        else -> null
    }

    /** Shown for anything the household typed or said that is not in the catalogue. */
    val custom: Int = R.drawable.photo_custom

    /** Vector icon by name, for buttons. */
    fun icon(name: String): Int = when (name) {
        "ic_ac_unit" -> R.drawable.ic_ac_unit
        "ic_add" -> R.drawable.ic_add
        "ic_arrow_back" -> R.drawable.ic_arrow_back
        "ic_bakery_dining" -> R.drawable.ic_bakery_dining
        "ic_breakfast_dining" -> R.drawable.ic_breakfast_dining
        "ic_cake" -> R.drawable.ic_cake
        "ic_celebration" -> R.drawable.ic_celebration
        "ic_check" -> R.drawable.ic_check
        "ic_chevron_right" -> R.drawable.ic_chevron_right
        "ic_close" -> R.drawable.ic_close
        "ic_coffee" -> R.drawable.ic_coffee
        "ic_cookie" -> R.drawable.ic_cookie
        "ic_delete" -> R.drawable.ic_delete
        "ic_dialpad" -> R.drawable.ic_dialpad
        "ic_egg" -> R.drawable.ic_egg
        "ic_emoji_food_beverage" -> R.drawable.ic_emoji_food_beverage
        "ic_featured_seasonal_and_gifts" -> R.drawable.ic_featured_seasonal_and_gifts
        "ic_grain" -> R.drawable.ic_grain
        "ic_grocery" -> R.drawable.ic_grocery
        "ic_icecream" -> R.drawable.ic_icecream
        "ic_inventory_2" -> R.drawable.ic_inventory_2
        "ic_kebab_dining" -> R.drawable.ic_kebab_dining
        "ic_keyboard" -> R.drawable.ic_keyboard
        "ic_launcher_background" -> R.drawable.ic_launcher_background
        "ic_launcher_foreground" -> R.drawable.ic_launcher_foreground
        "ic_launcher_monochrome" -> R.drawable.ic_launcher_monochrome
        "ic_liquor" -> R.drawable.ic_liquor
        "ic_local_laundry_service" -> R.drawable.ic_local_laundry_service
        "ic_lunch_dining" -> R.drawable.ic_lunch_dining
        "ic_mail" -> R.drawable.ic_mail
        "ic_medication" -> R.drawable.ic_medication
        "ic_mic" -> R.drawable.ic_mic
        "ic_notification" -> R.drawable.ic_notification
        "ic_notifications" -> R.drawable.ic_notifications
        "ic_nutrition" -> R.drawable.ic_nutrition
        "ic_person" -> R.drawable.ic_person
        "ic_pets" -> R.drawable.ic_pets
        "ic_photo_camera" -> R.drawable.ic_photo_camera
        "ic_qr_code_2" -> R.drawable.ic_qr_code_2
        "ic_qr_code_scanner" -> R.drawable.ic_qr_code_scanner
        "ic_ramen_dining" -> R.drawable.ic_ramen_dining
        "ic_redeem" -> R.drawable.ic_redeem
        "ic_rice_bowl" -> R.drawable.ic_rice_bowl
        "ic_set_meal" -> R.drawable.ic_set_meal
        "ic_shopping_basket" -> R.drawable.ic_shopping_basket
        "ic_soap" -> R.drawable.ic_soap
        "ic_sync_alt" -> R.drawable.ic_sync_alt
        "ic_water_drop" -> R.drawable.ic_water_drop
        "ic_water_full" -> R.drawable.ic_water_full
        "ic_wc" -> R.drawable.ic_wc
        "ic_wifi_off" -> R.drawable.ic_wifi_off
        else -> R.drawable.ic_grocery
    }
}
