package au.edu.canberra.team48.shoppinglist.data

import android.content.Context
import android.util.Log
import au.edu.canberra.team48.shoppinglist.domain.ListItem
import au.edu.canberra.team48.shoppinglist.domain.ListLogic
import au.edu.canberra.team48.shoppinglist.domain.Member
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Things that happen on the shared list that the person should be told about. */
sealed interface RepoEvent {
    data class AddedByOther(val who: String, val itemName: String, val itemId: String) : RepoEvent
    data object RemovedFromSharedList : RepoEvent
    /** Sharing has stopped working. The list is still kept on this phone. */
    data class SharingProblem(val detail: String) : RepoEvent
}

/** How the shared list is doing, shown in Family member tools. */
enum class SyncStatus {
    /** No Firebase in this build: the list is kept on this phone only. */
    NOT_SET_UP,
    /** Signing in and finding the list. Also shown while there is no internet. */
    CONNECTING,
    /** The server has the list and is sharing it. */
    CONNECTED,
    /** Firebase is there but refuses; [ListRepository.problem] says why. */
    PROBLEM,
}

/** Why a pairing step failed, in words that can go straight on screen. */
class PairingException(val kind: Kind) : Exception(kind.name) {
    enum class Kind { NOT_SET_UP, OFFLINE, WRONG_NUMBER, EXPIRED, OWN_LIST, FULL, REFUSED, UNKNOWN }
}

/**
 * Where the list lives. The screens only ever talk to this interface, so the
 * app runs identically whether the list is on this phone or shared in the cloud.
 */
interface ListRepository {
    val items: StateFlow<List<ListItem>>
    val members: StateFlow<List<Member>>
    val events: SharedFlow<RepoEvent>
    val status: StateFlow<SyncStatus>
    /** Why sharing is not working, in words a developer can act on. Null when all is well. */
    val problem: StateFlow<String?>

    /**
     * "Delete bought items after", shared by every phone on the list so they all delete at the
     * same time. Null when the list has no shared value (on this phone only, or not set yet).
     */
    val keepBoughtDays: StateFlow<Int?>
    fun setKeepBoughtDays(days: Int)

    /** True when Firebase is configured and phones can be connected. */
    val canShare: Boolean

    /** Id written into items this phone adds, so other phones can say who added them. */
    val myUid: String

    fun start(myName: String)
    fun add(item: ListItem)
    fun setTicked(id: String, ticked: Boolean)
    fun remove(id: String)
    /** Takes several items off at once: "Clear ticked items", or a finished list. */
    fun removeAll(ids: Collection<String>)
    /** Moves items to Bought before ([at] > 0), or back onto the current list ([at] = 0, for Undo). */
    fun setCleared(ids: Collection<String>, at: Long)
    /** Puts removed items back, for Undo. Items already there are left alone. */
    fun restoreAll(items: List<ListItem>)
    fun updateMyName(name: String)
    /** Reconnects to the shared list straight away. Nothing to do when the list is only on this phone. */
    fun refresh() {}

    suspend fun createCode(): String
    suspend fun lookupCode(code: String): String
    suspend fun join(code: String, myName: String)
    suspend fun removeMember(uid: String)
}

/** The whole list on this phone only. Used when Firebase has not been added. */
class LocalRepository(private val prefs: Prefs) : ListRepository {
    private val _items = MutableStateFlow(prefs.localItems)
    override val items: StateFlow<List<ListItem>> = _items
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    override val members: StateFlow<List<Member>> = _members
    override val events: SharedFlow<RepoEvent> = MutableSharedFlow()
    override val status: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.NOT_SET_UP)
    override val problem: StateFlow<String?> = MutableStateFlow(null)
    override val canShare = false
    override val keepBoughtDays: StateFlow<Int?> = MutableStateFlow(null)
    override fun setKeepBoughtDays(days: Int) {}
    override val myUid = "this-phone"

    private fun save(list: List<ListItem>) {
        _items.value = list
        prefs.localItems = list
    }

    override fun start(myName: String) {
        _members.value = listOf(Member(myUid, myName, isMe = true))
    }

    override fun add(item: ListItem) = save(ListLogic.add(_items.value, item).list)
    override fun setTicked(id: String, ticked: Boolean) = save(ListLogic.setTicked(_items.value, id, ticked))
    override fun remove(id: String) = save(ListLogic.remove(_items.value, id).first)

    override fun removeAll(ids: Collection<String>) {
        val gone = ids.toSet()
        save(_items.value.filterNot { it.id in gone })
    }

    override fun restoreAll(items: List<ListItem>) = save(ListLogic.restore(_items.value, items))

    override fun setCleared(ids: Collection<String>, at: Long) {
        val which = ids.toSet()
        save(_items.value.map { if (it.id in which) it.copy(clearedAt = at) else it })
    }

    override fun updateMyName(name: String) {
        _members.value = listOf(Member(myUid, name, isMe = true))
    }

    override suspend fun createCode(): String = throw PairingException(PairingException.Kind.NOT_SET_UP)
    override suspend fun lookupCode(code: String): String = throw PairingException(PairingException.Kind.NOT_SET_UP)
    override suspend fun join(code: String, myName: String) = throw PairingException(PairingException.Kind.NOT_SET_UP)
    override suspend fun removeMember(uid: String) = throw PairingException(PairingException.Kind.NOT_SET_UP)
}

object Repositories {
    /**
     * Uses Firestore when google-services.json has been added to the app module,
     * and the on-device list otherwise. The app is fully usable either way.
     */
    fun create(context: Context, prefs: Prefs, scope: CoroutineScope): ListRepository {
        val firebaseReady = runCatching {
            FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
        }.getOrDefault(false)
        Log.i("ShoppingList", if (firebaseReady) "Sharing on: using Firestore" else "Sharing off: list kept on this phone")
        return if (firebaseReady) FirestoreRepository(prefs, scope) else LocalRepository(prefs)
    }
}

/** Waits for a Google Play services Task without extra libraries. */
@Suppress("UNCHECKED_CAST")
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { t ->
        if (t.isSuccessful) cont.resume(t.result as T)
        else cont.resumeWithException(t.exception ?: RuntimeException("Task failed"))
    }
}
