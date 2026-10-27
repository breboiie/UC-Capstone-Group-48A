package au.edu.canberra.team48.shoppinglist

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.edu.canberra.team48.shoppinglist.data.ListRepository
import au.edu.canberra.team48.shoppinglist.data.PairingException
import au.edu.canberra.team48.shoppinglist.data.PairingException.Kind
import au.edu.canberra.team48.shoppinglist.data.Prefs
import au.edu.canberra.team48.shoppinglist.data.RepoEvent
import au.edu.canberra.team48.shoppinglist.data.Repositories
import au.edu.canberra.team48.shoppinglist.data.SyncStatus
import au.edu.canberra.team48.shoppinglist.domain.Catalog
import au.edu.canberra.team48.shoppinglist.domain.CatalogItem
import au.edu.canberra.team48.shoppinglist.domain.Days
import au.edu.canberra.team48.shoppinglist.domain.ListItem
import au.edu.canberra.team48.shoppinglist.domain.ListLogic
import au.edu.canberra.team48.shoppinglist.domain.Matching
import au.edu.canberra.team48.shoppinglist.domain.PastList
import au.edu.canberra.team48.shoppinglist.domain.Member
import au.edu.canberra.team48.shoppinglist.domain.Pairing
import au.edu.canberra.team48.shoppinglist.domain.ProfileRules
import au.edu.canberra.team48.shoppinglist.domain.Purchase
import au.edu.canberra.team48.shoppinglist.domain.Role
import au.edu.canberra.team48.shoppinglist.domain.Suggestions
import au.edu.canberra.team48.shoppinglist.platform.Connectivity
import au.edu.canberra.team48.shoppinglist.platform.ItemRecognizer
import au.edu.canberra.team48.shoppinglist.platform.Notifications
import au.edu.canberra.team48.shoppinglist.platform.PhotoReader
import au.edu.canberra.team48.shoppinglist.platform.PhotoStore
import au.edu.canberra.team48.shoppinglist.platform.ReminderWorker
import au.edu.canberra.team48.shoppinglist.platform.SpeechController
import au.edu.canberra.team48.shoppinglist.platform.SpeechProblem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.TimeZone
import java.util.UUID
import kotlin.math.max

// ---------------------------------------------------------------- screens, pop-ups, messages

sealed interface Screen {
    data object Welcome : Screen
    data object SignUp : Screen
    data object Home : Screen
    data object AddItems : Screen
    data object Voice : Screen
    data class Photo(val uri: String) : Screen
    data object Settings : Screen
    data object Profile : Screen
    data object RemindMe : Screen
    data object KeepBought : Screen
    data object FamilyTools : Screen
    data object Connect : Screen
    data object TypeNumber : Screen
    data object Scan : Screen
}

sealed interface Overlay {
    data class ConfirmRemove(val item: ListItem) : Overlay
    /** Delete something from an old list. */
    data class ConfirmRemovePrevious(val item: ListItem) : Overlay
    /** Delete a whole old list. */
    data class ConfirmRemovePastList(val past: PastList) : Overlay
    /** "Clear ticked items", or "Start a new list" when everything has been bought. */
    data class ConfirmClear(val items: List<ListItem>, val allBought: Boolean) : Overlay
    /** Things you often buy. A tap adds straight away; [added] shows the ticks. */
    data class Suggest(val keys: List<String>, val added: Set<String> = emptySet()) : Overlay
    /** After a birthday card: one question, "Buy a present too?", Yes or No. */
    data object OfferPresent : Overlay
    data class JoinConfirm(val code: String, val owner: String, val busy: Boolean = false) : Overlay
    data class PairingError(val title: String, val detail: String) : Overlay
    data class ConfirmStopSharing(val member: Member) : Overlay
    data object SharingNotSetUp : Overlay
    /** After Save in Profile. */
    data object ProfileSaved : Overlay
}

/** [undo] puts removed items back; [unclear] moves cleared items back from Bought before. */
data class Toast(val text: String, val undo: List<ListItem> = emptyList(), val unclear: List<String> = emptyList(), val id: Long = System.nanoTime()) {
    val canUndo: Boolean get() = undo.isNotEmpty() || unclear.isNotEmpty()
}

data class FieldErrors(val name: String? = null, val phone: String? = null)

sealed interface VoiceState {
    data class Listening(val heard: String) : VoiceState
    /** One thing was said: up to two pictures to choose from. */
    data class Choices(val options: List<CatalogItem>, val selected: String) : VoiceState
    /** Several things were said: all of them, ticked, each can be unticked. */
    data class Several(val items: List<CatalogItem>, val chosen: Set<String>) : VoiceState
    data class Failed(val problem: SpeechProblem) : VoiceState
}

sealed interface PhotoState {
    data object Reading : PhotoState
    data class Found(val options: List<CatalogItem>, val selected: String) : PhotoState
    data object NotFound : PhotoState
}

sealed interface CodeState {
    data object Loading : CodeState
    data class Ready(val code: String, val expiresAt: Long) : CodeState
    data class Failed(val message: String) : CodeState
}

// ---------------------------------------------------------------- view model

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Application get() = getApplication()
    private val prefs = Prefs(app)
    val repo: ListRepository = Repositories.create(app, prefs, viewModelScope)
    private val connectivity = Connectivity(app)
    val speech = SpeechController(app)
    private val demo = BuildConfig.DEMO_MODE

    // navigation
    val stack = mutableStateListOf<Screen>(if (prefs.signedUp) Screen.Home else Screen.Welcome)
    val screen: Screen get() = stack.last()
    var overlay by mutableStateOf<Overlay?>(null); private set
    var toast by mutableStateOf<Toast?>(null); private set
    var highlightId by mutableStateOf<String?>(null); private set

    /** The item the list should scroll to when it next shows, so a new item is never off-screen. */
    var scrollToId by mutableStateOf<String?>(null); private set

    // shared data
    /** The current list. */
    var items by mutableStateOf<List<ListItem>>(emptyList()); private set
    /** Bought before: one folder per clearing of the list, newest first. */
    var pastLists by mutableStateOf<List<PastList>>(emptyList()); private set
    private var allRows: List<ListItem> = emptyList()
    var keepBoughtDays by mutableStateOf(prefs.keepBoughtDays); private set
    var members by mutableStateOf<List<Member>>(emptyList()); private set
    var online by mutableStateOf(true); private set
    var syncStatus by mutableStateOf(SyncStatus.NOT_SET_UP); private set
    var syncProblem by mutableStateOf<String?>(null); private set

    /** Shown only in the debug build, so the team can see why sharing is off. */
    val showSyncDetail: Boolean get() = demo
    private var history: List<Purchase> = prefs.history

    // profile and settings
    var name by mutableStateOf(prefs.name); private set
    var phone by mutableStateOf(prefs.phone); private set
    var role by mutableStateOf(prefs.role); private set
    var suggestionsOn by mutableStateOf(prefs.suggestionsOn(default = demo)); private set
    var birthdayOn by mutableStateOf(prefs.birthdayOn(default = demo)); private set
    var categoriesOn by mutableStateOf(prefs.categoriesOn); private set
    var remindOn by mutableStateOf(prefs.remindOn); private set
    var remindDays by mutableStateOf(prefs.remindDays); private set

    // voice, photo and pairing
    var voice by mutableStateOf<VoiceState>(VoiceState.Listening("")); private set
    var photo by mutableStateOf<PhotoState>(PhotoState.Reading); private set
    var code by mutableStateOf<CodeState>(CodeState.Loading); private set
    var typedNumber by mutableStateOf(""); private set
    /** True while a pairing number is being looked up, so Join shows "Checking" and cannot be pressed twice. */
    var checkingCode by mutableStateOf(false); private set
    var pendingCapture: Uri? = null

    /** Catalogue keys added during the current visit to Add items, for "Back to shopping list (3 added)". */
    private val addedThisVisit = mutableStateListOf<String>()

    val others: List<Member> get() = members.filterNot { it.isMe }
    val canShare: Boolean get() = repo.canShare
    val tickedCount: Int get() = items.count { it.ticked }
    val allBought: Boolean get() = ListLogic.allBought(items)
    val addedThisVisitCount: Int get() = addedThisVisit.count { isWaiting(it) }

    init {
        // Earlier test builds filled in two months of made-up shopping, so a brand-new person was
        // offered "Things you often buy". Suggestions now come only from real shopping: clear it once.
        if (prefs.demoSeeded) {
            history = emptyList(); prefs.history = history; prefs.demoSeeded = false
        }
        if (prefs.signedUp) repo.start(name)
        viewModelScope.launch { repo.items.collect { onItems(it) } }
        viewModelScope.launch { repo.members.collect { members = it } }
        viewModelScope.launch { repo.events.collect { onEvent(it) } }
        viewModelScope.launch { repo.status.collect { syncStatus = it } }
        viewModelScope.launch { repo.problem.collect { syncProblem = it } }
        // another phone changed "Delete bought items after": use the list's shared value
        viewModelScope.launch {
            repo.keepBoughtDays.collect { shared ->
                if (shared != null && Days.clamp(shared) != keepBoughtDays) {
                    keepBoughtDays = Days.clamp(shared); prefs.keepBoughtDays = keepBoughtDays
                    deleteExpired()
                }
            }
        }
        viewModelScope.launch {
            connectivity.online.collect { now ->
                val cameBack = now && !online
                online = now
                // internet back: catch up with the other phones at once
                if (cameBack && prefs.signedUp) repo.refresh()
            }
        }
    }

    private var stoppedAt = 0L

    /** The app went into the background. */
    fun onAppHidden() {
        stoppedAt = System.currentTimeMillis()
    }

    /**
     * The app is on screen again. If it was away for more than a few seconds, reconnect
     * now so changes made on the other phone in the meantime show straight away.
     */
    fun onAppShown() {
        val away = stoppedAt > 0 && System.currentTimeMillis() - stoppedAt > 5_000
        stoppedAt = 0L
        if (away && prefs.signedUp) repo.refresh()
    }

    override fun onCleared() {
        speech.cancel()
        super.onCleared()
    }

    // ================================================================ navigation

    fun go(s: Screen) {
        if (s == Screen.AddItems) addedThisVisit.clear()
        stack.add(s)
    }

    private fun replaceAll(s: Screen) {
        stack.clear()
        stack.add(s)
    }

    /** Back arrow or system back. Returns false when there is nowhere left to go. */
    fun back(): Boolean {
        if (overlay != null) {
            closeOverlay(); return true
        }
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex); return true
        }
        return false
    }

    fun backToList() {
        overlay = null
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        if (stack.last() != Screen.Home) replaceAll(Screen.Home)
    }

    fun listShown() {
        scrollToId = null
    }

    // ================================================================ messages and pop-ups

    fun showToast(text: String, undo: List<ListItem> = emptyList(), unclear: List<String> = emptyList()) {
        toast = Toast(text, undo, unclear)
    }

    fun dismissToast(id: Long) {
        if (toast?.id == id) toast = null
    }

    fun undo() {
        toast?.undo?.takeIf { it.isNotEmpty() }?.let { repo.restoreAll(it) }
        toast?.unclear?.takeIf { it.isNotEmpty() }?.let { repo.setCleared(it, 0L) }
        toast = null
    }

    /** Closes whatever pop-up is open. Suggestion sheets report what they added. */
    fun closeOverlay() {
        if (overlay == Overlay.ProfileSaved) return closeProfileSaved()
        val o = overlay
        overlay = null
        val added = when (o) {
            is Overlay.Suggest -> { prefs.lastSuggestionDay = today(); o.added.size }
            else -> 0
        }
        if (added > 0 && screen != Screen.AddItems) showToast(if (added == 1) "1 item added" else "$added items added")
    }

    private fun today(): Long {
        val now = System.currentTimeMillis()
        return Suggestions.dayIndex(now, TimeZone.getDefault().getOffset(now).toLong())
    }

    // ================================================================ sign up and profile

    fun signUp(rawName: String, rawPhone: String): FieldErrors? {
        validate(rawName, rawPhone)?.let { return it }
        val n = ProfileRules.cleanName(rawName)
        prefs.name = n
        prefs.phone = if (rawPhone.isBlank()) "" else ProfileRules.formatPhone(rawPhone)
        prefs.signedUp = true
        name = n
        phone = prefs.phone
        repo.start(n)
        replaceAll(Screen.Home)
        return null
    }

    fun saveProfile(rawName: String, rawPhone: String): FieldErrors? {
        validate(rawName, rawPhone)?.let { return it }
        val n = ProfileRules.cleanName(rawName)
        prefs.name = n
        prefs.phone = if (rawPhone.isBlank()) "" else ProfileRules.formatPhone(rawPhone)
        name = n
        phone = prefs.phone
        repo.updateMyName(n)
        overlay = Overlay.ProfileSaved   // "Changed successfully!", then OK goes back to Settings
        return null
    }

    /** "OK" on "Changed successfully!": back to Settings. */
    fun closeProfileSaved() {
        overlay = null
        if (screen == Screen.Profile) back()
    }

    /** Both are needed. "My number" fills the number in from the phone with one tap. */
    private fun validate(rawName: String, rawPhone: String): FieldErrors? {
        val nameErr = if (!ProfileRules.isValidName(rawName)) "Please add your name" else null
        val phoneErr = when {
            rawPhone.isBlank() -> "Please add your phone number, or press My number"
            !ProfileRules.isValidPhone(rawPhone) -> "Please check the number"
            else -> null
        }
        return if (nameErr != null || phoneErr != null) FieldErrors(nameErr, phoneErr) else null
    }

    // ================================================================ the list

    private fun onItems(all: List<ListItem>) {
        allRows = all
        val new = ListLogic.current(all)
        pastLists = ListLogic.pastLists(all)
        val bought = ListLogic.newlyTicked(items, new)
        if (bought.isNotEmpty()) {
            val now = System.currentTimeMillis()
            history = Suggestions.prune(history + bought.map { Purchase(it.key, now) }, now)
            prefs.history = history
            prefs.lastShopAt = now
        }
        items = new
        prefs.waitingCount = ListLogic.remaining(new)
        prefs.oldestWaitingAt = ListLogic.oldestWaiting(new)
        deleteExpired()
    }

    /** Bought before keeps things for the number of days set in Settings, then deletes them. */
    private fun deleteExpired() {
        val old = ListLogic.expired(allRows, System.currentTimeMillis(), keepBoughtDays)
        if (old.isNotEmpty()) repo.removeAll(old.map { it.id })
    }

    /** Tap on a Bought before row: it goes back on the list. */
    fun addAgain(item: ListItem) {
        val c = Catalog.find(item.key) ?: Catalog.custom(item.name)
        add(c, offerGifts = false)
    }

    fun askRemovePrevious(item: ListItem) {
        overlay = Overlay.ConfirmRemovePrevious(item)
    }

    /** Deletes one item from an old list. */
    fun confirmRemovePrevious() {
        val o = overlay as? Overlay.ConfirmRemovePrevious ?: return
        overlay = null
        repo.remove(o.item.id)
        showToast("${o.item.name} deleted", undo = listOf(o.item))
    }

    fun askRemovePastList(past: PastList) {
        overlay = Overlay.ConfirmRemovePastList(past)
    }

    /** Deletes a whole old list. */
    fun confirmRemovePastList() {
        val o = overlay as? Overlay.ConfirmRemovePastList ?: return
        overlay = null
        repo.removeAll(o.past.items.map { it.id })
        showToast("Old list deleted", undo = o.past.items)
    }

    /** "Add all again": everything from an old list that is not already on the list. */
    fun addAllAgain(past: PastList) {
        val todo = past.items.filterNot { isWaiting(it.key) }
        todo.forEach { item -> add(Catalog.find(item.key) ?: Catalog.custom(item.name), quiet = true, offerGifts = false) }
        showToast(
            when (todo.size) {
                0 -> "All of these are already on the list"
                1 -> "1 item added"
                else -> "${todo.size} items added"
            }
        )
    }

    fun setKeepBought(days: Int) {
        keepBoughtDays = Days.clamp(days); prefs.keepBoughtDays = keepBoughtDays
        repo.setKeepBoughtDays(keepBoughtDays)   // the same for every phone on the list
        deleteExpired()
    }

    fun toggleTick(item: ListItem) = repo.setTicked(item.id, !item.ticked)

    fun askRemove(item: ListItem) {
        overlay = Overlay.ConfirmRemove(item)
    }

    fun confirmRemove() {
        val o = overlay as? Overlay.ConfirmRemove ?: return
        overlay = null
        repo.remove(o.item.id)
        showToast("${o.item.name} removed", undo = listOf(o.item))
    }

    /** The button under the list: clears what has been bought, never what is still to buy. */
    fun askClear() {
        val ticked = ListLogic.ticked(items)
        if (ticked.isNotEmpty()) overlay = Overlay.ConfirmClear(ticked, ListLogic.allBought(items))
    }

    fun confirmClear() {
        val o = overlay as? Overlay.ConfirmClear ?: return
        overlay = null
        // Not deleted: moved to Bought before, under the list, until the days set in Settings pass.
        repo.setCleared(o.items.map { it.id }, System.currentTimeMillis())
        val n = o.items.size
        showToast(
            when {
                o.allBought -> "List cleared"
                n == 1 -> "1 item cleared"
                else -> "$n items cleared"
            },
            unclear = o.items.map { it.id },
        )
    }

    fun isWaiting(key: String): Boolean = items.any { it.key == key && !it.ticked }

    /** How often something was bought lately, used to put usual items first in the grid. */
    fun timesBought(key: String): Int = Suggestions.timesBought(history, key, System.currentTimeMillis())

    private fun newItem(c: CatalogItem, photoPath: String?) = ListItem(
        id = UUID.randomUUID().toString(),
        name = c.name,
        key = c.key,
        category = c.category,
        addedAt = System.currentTimeMillis(),
        addedBy = name,
        addedByUid = repo.myUid,
        photoPath = photoPath,
    )

    /**
     * Adds to the list. Returns false if it was already waiting.
     * [quiet] skips the message, for screens that show the tick instead.
     */
    fun add(c: CatalogItem, photoPath: String? = null, quiet: Boolean = false, offerGifts: Boolean = true): Boolean {
        val id = when (val r = ListLogic.add(items, newItem(c, photoPath))) {
            is ListLogic.AddResult.Added -> { repo.add(r.item); r.item.id }
            is ListLogic.AddResult.Unticked -> { repo.setTicked(r.item.id, false); r.item.id }
            is ListLogic.AddResult.AlreadyThere -> {
                if (!quiet) showToast("${c.name} is already on the list")
                return false
            }
        }
        scrollToId = id
        addedThisVisit.add(c.key)
        if (!quiet) showToast("${c.name} added")
        if (offerGifts && c.key == BIRTHDAY_CARD && birthdayOn) offerBirthday()
        return true
    }

    /** A picture on Add items: adds it, or takes it off again. The tick on the picture is the feedback. */
    fun tapTile(c: CatalogItem) {
        val waiting = items.firstOrNull { it.key == c.key && !it.ticked }
        if (waiting != null) {
            repo.remove(waiting.id)
            addedThisVisit.remove(c.key)
        } else {
            add(c, quiet = true)
        }
    }

    fun addCustom(text: String, quiet: Boolean = false): Boolean {
        val clean = ProfileRules.cleanName(text)
        if (clean.isEmpty()) return false
        val exact = Matching.search(clean, limit = 1, minScore = 0.97).firstOrNull()
        return add(exact ?: Catalog.custom(clean), quiet = quiet)
    }

    /** Set by "Type instead" on Say it: Add items opens the keyboard on its search bar. */
    var focusSearch by mutableStateOf(false); private set

    fun searchFocused() {
        focusSearch = false
    }

    /** From Say it or Photo: back to Add items with the keyboard ready in the search bar. */
    fun typeInstead() {
        if (screen != Screen.AddItems) back()
        if (screen != Screen.AddItems) go(Screen.AddItems)
        focusSearch = true
    }

    // ================================================================ suggestions and birthday gifts

    fun maybeOfferSuggestions() {
        if (overlay != null || screen != Screen.Home) return
        val now = System.currentTimeMillis()
        val keys = Suggestions.usual(history, ListLogic.waitingKeys(items), now).filter { Catalog.find(it) != null }
        if (Suggestions.shouldOffer(suggestionsOn, keys.size, prefs.lastSuggestionDay, today())) {
            overlay = Overlay.Suggest(keys)
        }
    }

    /** Asks once, simply, whether a present is needed too. Not asked if one is already on the list. */
    private fun offerBirthday() {
        if (Suggestions.companionsFor(BIRTHDAY_CARD, ListLogic.waitingKeys(items)).isNotEmpty()) overlay = Overlay.OfferPresent
    }

    /**
     * "Yes, add a present": the present goes on the current list, and the person is shown it
     * on the Add items page (opened if they were elsewhere), scrolled to its ticked picture.
     */
    fun addPresent() {
        overlay = null
        val present = Suggestions.companionsFor(BIRTHDAY_CARD, emptySet()).firstOrNull()?.let { Catalog.find(it) } ?: return
        add(present, quiet = true, offerGifts = false)
        if (screen != Screen.AddItems) {
            backToList()
            go(Screen.AddItems)
        }
        revealKey = present.key
        showToast("Present added to your list")
    }

    /** A picture Add items should scroll to and show, such as the present just added. */
    var revealKey by mutableStateOf<String?>(null); private set

    fun revealed() {
        revealKey = null
    }

    /** One tap on a suggested picture adds it; a second tap takes it off again. */
    fun toggleSuggested(key: String) {
        val c = Catalog.find(key) ?: return
        val added = when (val o = overlay) {
            is Overlay.Suggest -> o.added
            else -> return
        }
        val nowAdded = if (key in added) {
            items.firstOrNull { it.key == key && !it.ticked }?.let { repo.remove(it.id) }
            addedThisVisit.remove(key)
            added - key
        } else {
            add(c, quiet = true, offerGifts = false)
            added + key
        }
        overlay = when (val o = overlay) {
            is Overlay.Suggest -> o.copy(added = nowAdded)
            else -> o
        }
    }

    // ================================================================ voice

    /** Opens Say it fresh, so a previous result never shows for a moment. */
    fun openVoice() {
        voice = VoiceState.Listening("")
        go(Screen.Voice)
    }

    fun startVoice() {
        voice = VoiceState.Listening("")
        speech.start(
            onPartial = { voice = VoiceState.Listening(it) },
            onResults = { guesses ->
                val several = Matching.severalFromSpeech(guesses.first())
                voice = if (several != null) {
                    VoiceState.Several(several, several.map { it.key }.toSet())
                } else {
                    val options = Matching.voiceCandidates(guesses).ifEmpty {
                        val spoken = Matching.cleanSpoken(guesses.first())
                        if (spoken.isBlank()) emptyList() else listOf(Catalog.custom(spoken))
                    }
                    if (options.isEmpty()) VoiceState.Failed(SpeechProblem.NOT_HEARD) else VoiceState.Choices(options, options.first().key)
                }
            },
            onFailed = { voice = VoiceState.Failed(it) },
        )
    }

    fun stopVoice() = speech.finish()

    fun cancelVoice() = speech.cancel()

    fun pickVoice(key: String) {
        when (val v = voice) {
            is VoiceState.Choices -> voice = v.copy(selected = key)
            is VoiceState.Several -> voice = v.copy(chosen = if (key in v.chosen) v.chosen - key else v.chosen + key)
            else -> {}
        }
    }

    fun addVoiceChoice() {
        when (val v = voice) {
            is VoiceState.Choices -> {
                val item = v.options.firstOrNull { it.key == v.selected } ?: return
                speech.cancel()
                back()
                add(item)
            }
            is VoiceState.Several -> {
                val chosen = v.items.filter { it.key in v.chosen }
                if (chosen.isEmpty()) return
                speech.cancel()
                back()
                val n = chosen.count { add(it, quiet = true) }
                showToast(if (n == 1) "1 item added" else "$n items added")
            }
            else -> {}
        }
    }

    // ================================================================ photo

    private var photoJob: Job? = null
    private var photoBusy = false

    fun onPhotoCaptured(uri: Uri) {
        photo = PhotoState.Reading
        photoBusy = false
        val s = Screen.Photo(uri.toString())
        if (screen is Screen.Photo) stack[stack.lastIndex] = s else go(s)
        // a newer photo replaces the old one: its answer, when it comes, must not show
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            // Two at once: the AI says what the photo shows (a mandarin, a loaf of bread),
            // and the phone reads any words on a packet. Neither may keep "Looking" up for ever.
            val seen = async(Dispatchers.IO) {
                try { withTimeout(25_000) { ItemRecognizer.nameOf(ctx, uri) } } catch (e: Exception) { null }
            }
            val words = async(Dispatchers.IO) {
                try { withTimeout(15_000) { PhotoReader.readText(ctx, uri) } } catch (e: Exception) { "" }
            }
            val options = Matching.photoChoices(seen.await(), words.await())
            photo = if (options.isEmpty()) PhotoState.NotFound else PhotoState.Found(options, options.first().key)
        }
    }

    fun pickPhoto(key: String) {
        (photo as? PhotoState.Found)?.let { photo = it.copy(selected = key) }
    }

    private suspend fun keepCurrentPhoto(): String? {
        val uri = (screen as? Screen.Photo)?.uri?.let(Uri::parse) ?: return null
        return withContext(Dispatchers.IO) { PhotoStore.keep(ctx, uri, "item_" + UUID.randomUUID()) }
    }

    fun addPhotoChoice() {
        val f = photo as? PhotoState.Found ?: return
        val c = f.options.firstOrNull { it.key == f.selected } ?: return
        if (photoBusy) return   // a second tap must not go back twice
        photoBusy = true
        viewModelScope.launch {
            val path = keepCurrentPhoto()
            if (screen is Screen.Photo) back()
            add(c, path)
        }
    }

    /** Nothing recognised: the person types what it is, and their photo becomes its picture. */
    fun addPhotoNamed(text: String): String? {
        val clean = ProfileRules.cleanName(text)
        if (clean.isEmpty()) return "Please type what it is"
        val c = Matching.search(clean, limit = 1, minScore = 0.97).firstOrNull() ?: Catalog.custom(clean)
        if (photoBusy) return null
        photoBusy = true
        viewModelScope.launch {
            val path = keepCurrentPhoto()
            if (screen is Screen.Photo) back()
            add(c, path)
        }
        return null
    }

    // ================================================================ settings

    fun setSuggestions(on: Boolean) {
        suggestionsOn = on; prefs.setSuggestionsOn(on)
    }

    fun setBirthday(on: Boolean) {
        birthdayOn = on; prefs.setBirthdayOn(on)
    }

    /** Off: Add items shows every picture in one list, with no category buttons. */
    fun setCategories(on: Boolean) {
        categoriesOn = on; prefs.categoriesOn = on
    }

    fun setRemind(on: Boolean) {
        remindOn = on
        prefs.remindOn = on
        if (on) ReminderWorker.schedule(ctx) else ReminderWorker.cancel(ctx)
    }

    fun chooseRemindDays(days: Int) {
        remindDays = Days.clamp(days); prefs.remindDays = remindDays
    }

    fun sendTestReminder() = Notifications.showReminder(ctx, max(1, ListLogic.remaining(items)))

    fun chooseRole(r: Role) {
        role = r; prefs.role = r
    }

    // ================================================================ connecting phones

    fun openConnect() {
        if (!repo.canShare) overlay = Overlay.SharingNotSetUp else go(Screen.Connect)
    }

    fun makeCode() {
        code = CodeState.Loading
        viewModelScope.launch {
            code = try {
                val c = withTimeout(NETWORK_WAIT_MS) { repo.createCode() }
                CodeState.Ready(c, System.currentTimeMillis() + Pairing.TTL_MS)
            } catch (e: TimeoutCancellationException) {
                CodeState.Failed("Could not reach the internet. Please try again.")
            } catch (e: PairingException) {
                CodeState.Failed(if (e.kind == Kind.OFFLINE) "No internet. Try again when connected." else "Could not make a code. Please try again.")
            }
        }
    }

    fun onScanned(raw: String?) {
        if (raw == null) return // the person backed out of the scanner; stay where they are
        val c = Pairing.codeFromScan(raw)
        if (c == null) {
            overlay = Overlay.PairingError("That is not a list code", "Scan the code on the other phone.")
            return
        }
        lookup(c)
    }

    /** The camera may not be used: go straight to typing. */
    fun scannerUnavailable() {
        if (screen == Screen.Scan) back()
        if (screen != Screen.TypeNumber) go(Screen.TypeNumber)
        showToast("Type the number from the other phone")
    }

    /** From the phone's own number keyboard: digits only, at most six. */
    fun onNumberTyped(s: String) {
        typedNumber = Pairing.typed(s)
    }

    /** "Type the number instead" on the Scan screen. */
    fun typeNumberInstead() {
        if (screen == Screen.Scan) back()
        go(Screen.TypeNumber)
    }

    /** "Scan the code": the camera opens inside the app once it may be used. */
    fun openScanner() {
        if (screen != Screen.Scan) go(Screen.Scan)
    }

    fun submitNumber() {
        if (Pairing.isWellFormed(typedNumber) && !checkingCode) lookup(typedNumber)
    }

    private fun lookup(c: String) {
        if (checkingCode) return
        checkingCode = true
        viewModelScope.launch {
            overlay = try {
                Overlay.JoinConfirm(c, withTimeout(NETWORK_WAIT_MS) { repo.lookupCode(c) })
            } catch (e: TimeoutCancellationException) {
                errorOverlay(Kind.OFFLINE)
            } catch (e: PairingException) {
                errorOverlay(e.kind)
            } finally {
                checkingCode = false
            }
        }
    }

    fun confirmJoin() {
        val o = overlay as? Overlay.JoinConfirm ?: return
        if (o.busy) return
        overlay = o.copy(busy = true)
        viewModelScope.launch {
            try {
                withTimeout(NETWORK_WAIT_MS) { repo.join(o.code, name) }
                typedNumber = ""
                backToList()
                showToast("Connected to ${o.owner}'s list")
            } catch (e: TimeoutCancellationException) {
                overlay = errorOverlay(Kind.OFFLINE)
            } catch (e: PairingException) {
                overlay = errorOverlay(e.kind)
            }
        }
    }

    /** Called on the phone showing its code when another phone joins. */
    fun someoneJoined(who: String) {
        backToList()
        showToast("$who joined your list")
    }

    fun askStopSharing(m: Member) {
        overlay = Overlay.ConfirmStopSharing(m)
    }

    fun confirmStopSharing() {
        val o = overlay as? Overlay.ConfirmStopSharing ?: return
        overlay = null
        viewModelScope.launch {
            try {
                withTimeout(NETWORK_WAIT_MS) { repo.removeMember(o.member.uid) }
                showToast("Stopped sharing with ${o.member.name}")
            } catch (e: TimeoutCancellationException) {
                overlay = errorOverlay(Kind.OFFLINE)
            } catch (e: PairingException) {
                overlay = errorOverlay(e.kind)
            }
        }
    }

    private fun errorOverlay(kind: Kind): Overlay = when (kind) {
        Kind.NOT_SET_UP -> Overlay.SharingNotSetUp
        Kind.WRONG_NUMBER -> Overlay.PairingError("That number did not work", "Check it and try again.")
        Kind.EXPIRED -> Overlay.PairingError("That number has run out", "Ask for a new one.")
        Kind.OWN_LIST -> Overlay.PairingError("That is this phone's own list", "Scan the code on the other phone.")
        Kind.OFFLINE -> Overlay.PairingError("No internet", "Connect to the internet and try again.")
        Kind.FULL -> Overlay.PairingError("That list is full", "A list can be shared by ${Pairing.MAX_PHONES} phones.")
        Kind.REFUSED -> Overlay.PairingError(
            "Sharing is not working",
            "The list is still saved on this phone." + (if (demo) "\n\n" + (repo.problem.value ?: "") else ""),
        )
        Kind.UNKNOWN -> Overlay.PairingError("Something went wrong", "Please try again.")
    }

    // ================================================================ sync events

    private fun onEvent(e: RepoEvent) {
        when (e) {
            is RepoEvent.AddedByOther -> {
                showToast("${e.who} added ${e.itemName.lowercase()}")
                highlightId = e.itemId
                scrollToId = e.itemId
                viewModelScope.launch {
                    delay(5_000)
                    if (highlightId == e.itemId) highlightId = null
                }
            }
            RepoEvent.RemovedFromSharedList -> showToast("This phone has stopped sharing a list")
            is RepoEvent.SharingProblem -> showToast("The list could not be shared. It is still saved on this phone.")
        }
    }

    private companion object {
        const val BIRTHDAY_CARD = "birthday_card"
        const val NETWORK_WAIT_MS = 20_000L
    }
}
