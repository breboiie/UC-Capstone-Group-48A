package au.edu.canberra.team48.shoppinglist.domain

/**
 * Plain Kotlin models. Nothing in the domain package imports Android, so all of
 * the app's rules can be unit tested on a normal JVM.
 */

enum class Category(val label: String) {
    FRUIT_VEG("Fruit and veg"),
    DAIRY("Dairy"),
    BREAD("Bread"),
    MEAT("Meat"),
    PANTRY("Pantry"),
    DRINKS("Drinks"),
    FROZEN("Frozen"),
    CLEANING("Cleaning"),
    CARDS_GIFTS("Cards and gifts"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/**
 * A product the app knows about.
 * [hasPhoto] true means a real photograph is bundled as drawable `photo_<key>`.
 * Otherwise [icon] names a vector drawable used as a plain picture.
 * [rank] orders the "All" grid; lower numbers are more common purchases.
 */
data class CatalogItem(
    val key: String,
    val name: String,
    val category: Category,
    val hasPhoto: Boolean,
    val icon: String = "ic_grocery",
    val aliases: List<String> = emptyList(),
    val rank: Int = 50,
)

/** One line on the shopping list. [key] links back to the catalogue, or starts with "custom:". */
data class ListItem(
    val id: String,
    val name: String,
    val key: String,
    val category: Category,
    val ticked: Boolean = false,
    val addedAt: Long = 0L,
    val addedBy: String = "",
    val addedByUid: String = "",
    val photoPath: String? = null,
    /**
     * When "Clear ticked items" (or "Start a new list") moved it to Bought before.
     * 0 means it is on the current list. Bought-before items are deleted after a set number of days.
     */
    val clearedAt: Long = 0L,
)

/** One folder of Bought before: the items cleared together at [clearedAt]. */
data class PastList(val clearedAt: Long, val items: List<ListItem>)

/** A record that something was bought, used only by the suggestion rule. */
data class Purchase(val key: String, val at: Long)

enum class Role(val label: String) {
    PRIMARY("Primary user"),
    FAMILY("Family member"),
    CARER("Carer");

    companion object {
        fun fromName(name: String?): Role = entries.firstOrNull { it.name == name } ?: PRIMARY
    }
}

/** A phone connected to the shared list. */
data class Member(val uid: String, val name: String, val isMe: Boolean)
