package au.edu.canberra.team48.shoppinglist.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import au.edu.canberra.team48.shoppinglist.domain.Category
import au.edu.canberra.team48.shoppinglist.domain.Days
import au.edu.canberra.team48.shoppinglist.domain.ListItem
import au.edu.canberra.team48.shoppinglist.domain.Purchase
import au.edu.canberra.team48.shoppinglist.domain.Reminders
import au.edu.canberra.team48.shoppinglist.domain.Role
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything the app remembers on this phone. SharedPreferences plus the JSON
 * support built into Android, so there are no database dependencies to set up.
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("shopping_list", Context.MODE_PRIVATE)

    // ---------------------------------------------------------------- profile
    var signedUp: Boolean
        get() = sp.getBoolean("signed_up", false)
        set(v) = sp.edit { putBoolean("signed_up", v) }

    var name: String
        get() = sp.getString("name", "") ?: ""
        set(v) = sp.edit { putString("name", v) }

    var phone: String
        get() = sp.getString("phone", "") ?: ""
        set(v) = sp.edit { putString("phone", v) }

    var role: Role
        get() = Role.fromName(sp.getString("role", null))
        set(v) = sp.edit { putString("role", v.name) }

    // ---------------------------------------------------------------- settings
    fun suggestionsOn(default: Boolean) = sp.getBoolean("suggestions_on", default)
    fun setSuggestionsOn(v: Boolean) = sp.edit { putBoolean("suggestions_on", v) }

    /** The category buttons (Fruit and veg, Dairy...) above the pictures in Add items. On unless switched off. */
    var categoriesOn: Boolean
        get() = sp.getBoolean("categories_on", true)
        set(v) = sp.edit { putBoolean("categories_on", v) }

    fun birthdayOn(default: Boolean) = sp.getBoolean("birthday_on", default)
    fun setBirthdayOn(v: Boolean) = sp.edit { putBoolean("birthday_on", v) }

    var remindOn: Boolean
        get() = sp.getBoolean("remind_on", false)
        set(v) = sp.edit { putBoolean("remind_on", v) }

    /** How many days Bought before keeps things before deleting them. */
    var keepBoughtDays: Int
        get() = sp.getInt("keep_bought_days", Days.KEEP_BOUGHT_DEFAULT)
        set(v) = sp.edit { putInt("keep_bought_days", v) }

    var remindDays: Int
        get() = sp.getInt("remind_days", Reminders.DEFAULT_DAYS)
        set(v) = sp.edit { putInt("remind_days", v) }

    var lastSuggestionDay: Long
        get() = sp.getLong("last_suggestion_day", -1L)
        set(v) = sp.edit { putLong("last_suggestion_day", v) }

    // ---------------------------------------------------------------- read by the reminder worker
    var lastShopAt: Long
        get() = sp.getLong("last_shop_at", 0L)
        set(v) = sp.edit { putLong("last_shop_at", v) }

    var waitingCount: Int
        get() = sp.getInt("waiting_count", 0)
        set(v) = sp.edit { putInt("waiting_count", v) }

    var oldestWaitingAt: Long
        get() = sp.getLong("oldest_waiting_at", 0L)
        set(v) = sp.edit { putLong("oldest_waiting_at", v) }

    // ---------------------------------------------------------------- sync
    var householdId: String?
        get() = sp.getString("household_id", null)
        set(v) = sp.edit { if (v == null) remove("household_id") else putString("household_id", v) }

    var demoSeeded: Boolean
        get() = sp.getBoolean("demo_seeded", false)
        set(v) = sp.edit { putBoolean("demo_seeded", v) }

    // ---------------------------------------------------------------- photos of items
    /** A photo taken on this phone for an item. Kept here because photos are not shared. */
    fun photoFor(itemId: String): String? = sp.getString("photo_$itemId", null)

    fun setPhotoFor(itemId: String, path: String?) =
        sp.edit { if (path == null) remove("photo_$itemId") else putString("photo_$itemId", path) }

    // ---------------------------------------------------------------- the list (used when not sharing)
    var localItems: List<ListItem>
        get() = decodeItems(sp.getString("items", null))
        set(v) = sp.edit { putString("items", encodeItems(v)) }

    // ---------------------------------------------------------------- purchase history
    var history: List<Purchase>
        get() {
            val raw = sp.getString("history", null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                List(arr.length()) { i -> arr.getJSONObject(i).let { Purchase(it.getString("k"), it.getLong("t")) } }
            }.getOrDefault(emptyList())
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(JSONObject().put("k", it.key).put("t", it.at)) }
            sp.edit { putString("history", arr.toString()) }
        }

    companion object {
        fun encodeItems(items: List<ListItem>): String {
            val arr = JSONArray()
            items.forEach { i ->
                arr.put(
                    JSONObject()
                        .put("id", i.id).put("name", i.name).put("key", i.key)
                        .put("cat", i.category.name).put("ticked", i.ticked).put("at", i.addedAt)
                        .put("by", i.addedBy).put("byUid", i.addedByUid)
                        .put("photo", i.photoPath ?: JSONObject.NULL).put("cleared", i.clearedAt)
                )
            }
            return arr.toString()
        }

        fun decodeItems(raw: String?): List<ListItem> {
            if (raw.isNullOrEmpty()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
                    ListItem(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        key = o.getString("key"),
                        category = Category.fromName(o.optString("cat")),
                        ticked = o.optBoolean("ticked"),
                        addedAt = o.optLong("at"),
                        addedBy = o.optString("by"),
                        addedByUid = o.optString("byUid"),
                        photoPath = if (o.isNull("photo")) null else o.optString("photo"),
                        clearedAt = o.optLong("cleared"),
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}
