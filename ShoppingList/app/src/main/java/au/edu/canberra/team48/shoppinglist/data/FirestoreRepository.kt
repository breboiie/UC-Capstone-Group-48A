package au.edu.canberra.team48.shoppinglist.data

import android.util.Log
import au.edu.canberra.team48.shoppinglist.domain.Category
import au.edu.canberra.team48.shoppinglist.domain.ListItem
import au.edu.canberra.team48.shoppinglist.domain.ListLogic
import au.edu.canberra.team48.shoppinglist.domain.Member
import au.edu.canberra.team48.shoppinglist.domain.Pairing
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date

/**
 * The shared list in Cloud Firestore.
 *
 * Each phone signs in anonymously, so nobody types an email or password. Pairing
 * works without Cloud Functions (which need a paid Firebase plan): the joining
 * phone adds only itself to the household, and the security rules in
 * firestore.rules check that it holds a live pairing number for that household.
 *
 *   households/{hid}              memberUids, memberNames, createdAt, joinCode
 *   households/{hid}/items/{id}   name, key, category, ticked, addedAt, addedBy, addedByUid
 *   pairing/{code}                householdId, ownerName, expiresAt
 *
 * Changes reach the other phones straight away: both phones keep a live listener
 * open on the list, so a tick or a new item appears on the other screen in about
 * a second. On opening, the list shows at once from the phone's own copy while the
 * listener catches up, and [refresh] reconnects immediately when the app comes back
 * to the front or the internet returns, instead of waiting for Firebase to retry.
 *
 * It repairs itself: if this phone's household was never created on the server
 * (for example the app ran before the security rules were published), a new one
 * is made instead of failing silently for ever. Every refused save is reported
 * through [status] and [problem], never swallowed.
 */
class FirestoreRepository(
    private val prefs: Prefs,
    private val scope: CoroutineScope,
) : ListRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _items = MutableStateFlow<List<ListItem>>(emptyList())
    override val items: StateFlow<List<ListItem>> = _items
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    override val members: StateFlow<List<Member>> = _members
    private val _events = MutableSharedFlow<RepoEvent>(extraBufferCapacity = 8)
    override val events: SharedFlow<RepoEvent> = _events
    private val _status = MutableStateFlow(SyncStatus.CONNECTING)
    override val status: StateFlow<SyncStatus> = _status
    private val _problem = MutableStateFlow<String?>(null)
    override val problem: StateFlow<String?> = _problem
    override val canShare = true
    private val _keepDays = MutableStateFlow<Int?>(null)
    override val keepBoughtDays: StateFlow<Int?> = _keepDays

    override fun setKeepBoughtDays(days: Int) {
        _keepDays.value = days
        write { h ->
            db.document("households/$h").update("keepBoughtDays", days)
                // rules from before this setting refuse it: keep using it on this phone, and say so in Logcat
                .addOnFailureListener { e -> Log.w(TAG, "Could not share 'Delete bought items after'. Publish the new firestore.rules.", e) }
        }
    }

    override val myUid: String get() = auth.currentUser?.uid ?: ""

    private var myName = ""
    private var hid: String? = null
    private var itemsReg: ListenerRegistration? = null
    private var houseReg: ListenerRegistration? = null
    private var firstItemsSnapshot = true
    private var started = false
    private var connecting: Job? = null

    /** Set once this phone has been seen in the member list, so a denial means "removed", not "rules missing". */
    private var membershipConfirmed = false

    /** Cuts a wait between connection attempts short. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /** Writes made before the household is ready, applied once it is. */
    private val pending = mutableListOf<(String) -> Unit>()

    override fun start(myName: String) {
        this.myName = myName
        if (started) return
        started = true
        connect()
    }

    // ---------------------------------------------------------------- connecting

    /** Signs in and finds (or makes) this phone's household, retrying with growing gaps until it works. */
    private fun connect() {
        if (connecting?.isActive == true) return
        connecting = scope.launch {
            var wait = 2_000L
            while (true) {
                if (_status.value != SyncStatus.PROBLEM) _status.value = SyncStatus.CONNECTING
                try {
                    val uid = signIn()
                    // The saved list is opened straight away, from the phone's own copy if
                    // need be; its listener checks with the server in the background.
                    val house = prefs.householdId ?: createHousehold(uid)
                    attach(house, uid)
                    return@launch
                } catch (e: FirebaseNetworkException) {
                    Log.w(TAG, "No internet yet, will retry", e) // normal on a first launch offline
                } catch (e: FirebaseFirestoreException) {
                    if (e.code == Code.PERMISSION_DENIED) {
                        report(PROBLEM_RULES, e)
                    } else {
                        Log.w(TAG, "Firestore not ready yet, will retry", e)
                    }
                } catch (e: SignInRefused) {
                    report(PROBLEM_SIGN_IN, e.cause)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not connect yet, will retry", e)
                }
                withTimeoutOrNull(wait) { wake.receive() } // or sooner, when refresh() is called
                wait = (wait * 2).coerceAtMost(60_000L)
            }
        }
    }

    private class SignInRefused(cause: Exception) : Exception(cause)

    private suspend fun signIn(): String {
        auth.currentUser?.uid?.let { return it }
        return try {
            auth.signInAnonymously().awaitResult().user?.uid ?: throw IllegalStateException("No user after sign in")
        } catch (e: FirebaseNetworkException) {
            throw e
        } catch (e: Exception) {
            // Anything other than "no internet" here almost always means Anonymous
            // sign-in has not been switched on in the Firebase console.
            throw SignInRefused(e)
        }
    }

    /**
     * Makes a household with only this phone in it. Waits for the server so a refusal
     * is noticed straight away; offline, Firestore keeps the write and sends it later.
     */
    private suspend fun createHousehold(uid: String): String {
        val ref = db.collection("households").document()
        val write = ref.set(
            mapOf(
                "memberUids" to listOf(uid),
                "memberNames" to mapOf(uid to myName),
                "createdAt" to FieldValue.serverTimestamp(),
            )
        )
        withTimeoutOrNull(8_000) { write.awaitResult() } // throws PERMISSION_DENIED if the rules refuse
        prefs.householdId = ref.id
        return ref.id
    }

    private fun memberUids(snap: DocumentSnapshot): List<String> =
        (snap.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()

    private fun attach(house: String, uid: String) {
        detach()
        hid = house
        membershipConfirmed = false
        firstItemsSnapshot = true

        houseReg = db.document("households/$house").addSnapshotListener { snap, err ->
            if (err != null) {
                if (err.code == Code.PERMISSION_DENIED) {
                    // Confirmed before: another phone has stopped sharing with this one.
                    // Never confirmed: the saved list was never made on the server (or this
                    // phone was taken off while it was closed). Start a fresh one quietly;
                    // if the rules really are missing, making it will say so.
                    if (membershipConfirmed) onRemovedFromList() else startOver()
                } else {
                    Log.w(TAG, "The list stopped updating; reconnecting", err)
                    scope.launch { delay(2_000); refresh() }
                }
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            if (!snap.exists()) {
                if (!snap.metadata.isFromCache) startOver()
                return@addSnapshotListener
            }
            val uids = memberUids(snap)
            val names = (snap.get("memberNames") as? Map<*, *>).orEmpty()
            snap.getLong("keepBoughtDays")?.let { _keepDays.value = it.toInt() }
            if (uid !in uids && !snap.metadata.isFromCache) {
                onRemovedFromList()
                return@addSnapshotListener
            }
            if (uid in uids) {
                membershipConfirmed = true
                if (!snap.metadata.isFromCache) markConnected()
            }
            _members.value = uids.map { Member(it, (names[it] as? String) ?: "Someone", it == uid) }
                .sortedByDescending { it.isMe }
        }

        itemsReg = db.collection("households/$house/items").orderBy("addedAt")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    // Before membership is confirmed the household listener sorts out a refusal.
                    if (err.code == Code.PERMISSION_DENIED && membershipConfirmed) report(PROBLEM_RULES, err)
                    else Log.w(TAG, "Items stopped updating", err)
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                _items.value = snap.documents.map(::toItem)
                if (!firstItemsSnapshot && !snap.metadata.hasPendingWrites()) {
                    for (ch in snap.documentChanges) {
                        if (ch.type != DocumentChange.Type.ADDED) continue
                        val it = toItem(ch.document)
                        // only things put on the current list count (not a Bought-before row coming back)
                        if (it.clearedAt == 0L && it.addedByUid.isNotEmpty() && it.addedByUid != uid) {
                            _events.tryEmit(RepoEvent.AddedByOther(it.addedBy.ifBlank { "Someone" }, it.name, it.id))
                        }
                    }
                }
                firstItemsSnapshot = false
            }

        // make sure our name is current, then send anything added before the household was ready
        watch(db.document("households/$house").update("memberNames.$uid", myName))
        val queued = pending.toList()
        pending.clear()
        queued.forEach { it(house) }
    }

    private fun detach() {
        itemsReg?.remove(); itemsReg = null
        houseReg?.remove(); houseReg = null
    }

    /** The saved household cannot be used: start again with a fresh one, keeping what is still to buy. */
    private fun startOver() {
        Log.w(TAG, "Household $hid is not usable from this phone; making a new one")
        val keep = _items.value.filter { !it.ticked }
        detach()
        hid = null
        prefs.householdId = null
        keep.forEach { item -> pending += { h -> watch(db.document("households/$h/items/${item.id}").set(toMap(item))) } }
        connect()
    }

    /**
     * Reconnects now rather than when Firebase next retries by itself, which after a
     * phone has slept or lost signal can take a minute. Called when the app comes back
     * to the front and when the internet returns. Nothing typed in the meantime is lost:
     * changes made offline are kept and sent as soon as the connection is back.
     */
    override fun refresh() {
        if (hid == null) {
            wake.trySend(Unit)
            connect()
            return
        }
        db.disableNetwork().addOnCompleteListener { db.enableNetwork() }
    }

    private fun onRemovedFromList() {
        membershipConfirmed = false
        detach()
        hid = null
        prefs.householdId = null
        _items.value = emptyList()
        _members.value = emptyList()
        _events.tryEmit(RepoEvent.RemovedFromSharedList)
        connect()
    }

    // ---------------------------------------------------------------- status and problems

    private fun markConnected() {
        _status.value = SyncStatus.CONNECTED
        _problem.value = null
    }

    /** Records why sharing is not working, so it can be shown, and tells the screen once. */
    private fun report(why: String, e: Throwable?) {
        Log.e(TAG, why, e)
        val wasFine = _status.value != SyncStatus.PROBLEM
        _status.value = SyncStatus.PROBLEM
        _problem.value = why
        if (wasFine) _events.tryEmit(RepoEvent.SharingProblem(why))
    }

    /** Every write reports a refusal instead of failing silently. */
    private fun <T> watch(task: Task<T>) {
        task.addOnFailureListener { e ->
            if (e is FirebaseFirestoreException && e.code == Code.PERMISSION_DENIED) {
                report(PROBLEM_RULES, e)
            } else {
                Log.w(TAG, "A change could not be saved yet", e)
            }
        }
    }

    // ---------------------------------------------------------------- items

    private fun toItem(d: DocumentSnapshot) = ListItem(
        id = d.id,
        name = d.getString("name") ?: "",
        key = d.getString("key") ?: "",
        category = Category.fromName(d.getString("category")),
        ticked = d.getBoolean("ticked") ?: false,
        addedAt = d.getLong("addedAt") ?: 0L,
        addedBy = d.getString("addedBy") ?: "",
        addedByUid = d.getString("addedByUid") ?: "",
        // photos stay on the phone that took them; other phones show the catalogue picture
        photoPath = prefs.photoFor(d.id),
        clearedAt = d.getLong("clearedAt") ?: 0L,
    )

    private fun toMap(i: ListItem) = mapOf(
        "name" to i.name, "key" to i.key, "category" to i.category.name, "ticked" to i.ticked,
        "addedAt" to i.addedAt, "addedBy" to i.addedBy, "addedByUid" to i.addedByUid,
        "clearedAt" to i.clearedAt,
    )

    /** Runs now if the household is ready, otherwise as soon as it is. */
    private fun write(op: (String) -> Unit) {
        val h = hid
        if (h != null) op(h) else pending += op
    }

    override fun add(item: ListItem) {
        if (item.photoPath != null) prefs.setPhotoFor(item.id, item.photoPath)
        _items.value = _items.value + item // shows instantly; the listener confirms
        write { h -> watch(db.document("households/$h/items/${item.id}").set(toMap(item))) }
    }

    override fun setTicked(id: String, ticked: Boolean) {
        _items.value = _items.value.map { if (it.id == id) it.copy(ticked = ticked) else it }
        write { h -> watch(db.document("households/$h/items/$id").update("ticked", ticked)) }
    }

    override fun remove(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
        write { h -> watch(db.document("households/$h/items/$id").delete()) }
    }

    override fun restoreAll(items: List<ListItem>) {
        val have = _items.value.map { it.id }.toSet()
        val back = items.filter { it.id !in have }
        if (back.isEmpty()) return
        _items.value = ListLogic.restore(_items.value, back) // shows instantly; the listener confirms
        write { h ->
            back.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.set(db.document("households/$h/items/${it.id}"), toMap(it)) }
                watch(batch.commit())
            }
        }
    }

    override fun setCleared(ids: Collection<String>, at: Long) {
        val which = ids.toSet()
        if (which.isEmpty()) return
        _items.value = _items.value.map { if (it.id in which) it.copy(clearedAt = at) else it } // shows instantly
        write { h ->
            which.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.update(db.document("households/$h/items/$it"), "clearedAt", at) }
                watch(batch.commit())
            }
        }
    }

    override fun removeAll(ids: Collection<String>) {
        val gone = ids.toSet()
        if (gone.isEmpty()) return
        _items.value = _items.value.filterNot { it.id in gone }
        write { h ->
            gone.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(db.document("households/$h/items/$it")) }
                watch(batch.commit())
            }
        }
    }

    override fun updateMyName(name: String) {
        myName = name
        val uid = myUid
        if (uid.isNotEmpty()) write { h -> watch(db.document("households/$h").update("memberNames.$uid", name)) }
    }

    // ---------------------------------------------------------------- pairing

    private fun mapError(e: Exception): PairingException = when {
        e is PairingException -> e
        e is FirebaseNetworkException -> PairingException(PairingException.Kind.OFFLINE)
        e is FirebaseFirestoreException && e.code == Code.UNAVAILABLE -> PairingException(PairingException.Kind.OFFLINE)
        e is FirebaseFirestoreException && e.code == Code.PERMISSION_DENIED ->
            PairingException(if (_status.value == SyncStatus.PROBLEM) PairingException.Kind.REFUSED else PairingException.Kind.WRONG_NUMBER)
        else -> PairingException(PairingException.Kind.UNKNOWN)
    }

    /** Fails with a clear reason while the household is not ready, instead of an endless wait. */
    private fun readyHousehold(): String = hid ?: throw PairingException(
        if (_status.value == SyncStatus.PROBLEM) PairingException.Kind.REFUSED else PairingException.Kind.OFFLINE
    )

    override suspend fun createCode(): String {
        val house = readyHousehold()
        repeat(3) {
            val code = Pairing.newCode()
            try {
                db.document("pairing/$code").set(
                    mapOf(
                        "householdId" to house,
                        "ownerName" to myName,
                        "expiresAt" to Timestamp(Date(System.currentTimeMillis() + Pairing.TTL_MS)),
                    )
                ).awaitResult()
                return code
            } catch (e: FirebaseFirestoreException) {
                // PERMISSION_DENIED here usually means the number is already in use: try another
                if (e.code != Code.PERMISSION_DENIED) throw mapError(e)
            }
        }
        // three refusals in a row is not bad luck: the rules are refusing this phone
        report(PROBLEM_RULES, null)
        throw PairingException(PairingException.Kind.REFUSED)
    }

    override suspend fun lookupCode(code: String): String {
        try {
            val snap = db.document("pairing/$code").get().awaitResult()
            if (!snap.exists()) throw PairingException(PairingException.Kind.WRONG_NUMBER)
            val expires = snap.getTimestamp("expiresAt")?.toDate()?.time ?: 0L
            if (Pairing.isExpired(expires, System.currentTimeMillis())) throw PairingException(PairingException.Kind.EXPIRED)
            if (snap.getString("householdId") == hid) throw PairingException(PairingException.Kind.OWN_LIST)
            return snap.getString("ownerName")?.ifBlank { null } ?: "Someone"
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    override suspend fun join(code: String, myName: String) {
        this.myName = myName
        val uid = myUid.ifEmpty { throw PairingException(PairingException.Kind.OFFLINE) }
        try {
            val p = db.document("pairing/$code").get().awaitResult()
            val target = p.getString("householdId") ?: throw PairingException(PairingException.Kind.WRONG_NUMBER)
            db.document("households/$target").update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "memberNames.$uid" to myName,
                    "joinCode" to code,
                )
            ).awaitResult()

            // bring across anything still to buy from this phone's own list, so nothing is lost
            val carry = _items.value.filter { !it.ticked }
            if (carry.isNotEmpty()) {
                val batch = db.batch()
                carry.forEach { batch.set(db.document("households/$target/items/${it.id}"), toMap(it)) }
                watch(batch.commit())
            }
            watch(db.document("pairing/$code").delete()) // one use only

            prefs.householdId = target
            attach(target, uid)
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    override suspend fun removeMember(uid: String) {
        val house = readyHousehold()
        try {
            db.document("households/$house").update(
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(uid),
                    "memberNames.$uid" to FieldValue.delete(),
                )
            ).awaitResult()
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    private companion object {
        const val TAG = "ShoppingList"
        const val PROBLEM_RULES =
            "The server refused to save the list. In the Firebase console, open Firestore Database, Rules, " +
                "paste in firestore.rules from the project and press Publish."
        const val PROBLEM_SIGN_IN =
            "This phone could not sign in. In the Firebase console, open Authentication, Sign-in method, " +
                "and turn on Anonymous."
    }
}
