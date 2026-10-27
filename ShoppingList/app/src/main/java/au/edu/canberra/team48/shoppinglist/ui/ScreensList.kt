package au.edu.canberra.team48.shoppinglist.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import au.edu.canberra.team48.shoppinglist.domain.Days
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import au.edu.canberra.team48.shoppinglist.AppViewModel
import au.edu.canberra.team48.shoppinglist.R
import au.edu.canberra.team48.shoppinglist.Screen
import au.edu.canberra.team48.shoppinglist.domain.Catalog
import au.edu.canberra.team48.shoppinglist.domain.CatalogItem
import au.edu.canberra.team48.shoppinglist.domain.Category
import au.edu.canberra.team48.shoppinglist.domain.Matching
import au.edu.canberra.team48.shoppinglist.domain.ProfileRules
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- Shopping list

/** Home: the list, with Add items underneath. Phones and tablets alike show it on its own. */
@Composable
fun HomeScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) {
        delay(700)
        vm.maybeOfferSuggestions()
    }
    val listState = rememberLazyListState()
    // which old lists are open; all start closed so the page stays tidy
    var openLists by remember { mutableStateOf(setOf<Long>()) }
    // A newly added item may be below the bottom of the screen: bring it into view.
    val target = vm.scrollToId
    LaunchedEffect(target, vm.items.size) {
        if (target != null) {
            val i = vm.items.indexOfFirst { it.id == target }
            if (i >= 0) {
                listState.animateScrollToItem(i)
                vm.listShown()
            }
        }
    }
    val showClear = vm.tickedCount > 0
    ScreenFrame(
        top = {
            TopBar(
                "Shopping list",
                avatar = ProfileRules.initial(vm.name),
                onAvatar = { vm.go(Screen.Settings) },
            )
        },
        bottom = { ListButtons(vm, showClear = showClear) },
        scroll = false,
    ) {
        if (!vm.online && vm.canShare) OfflineBanner()
        if (vm.items.isEmpty() && vm.pastLists.isEmpty()) {
            CentreMessage(R.drawable.ic_shopping_basket, "Start a new list")
        } else {
            val p = LocalPalette.current
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (vm.items.isEmpty()) {
                    item(key = "empty") { CentreMessage(R.drawable.ic_shopping_basket, "Start a new list") }
                }
                items(vm.items, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        highlighted = item.id == vm.highlightId,
                        onTick = { vm.toggleTick(item) },
                        onRemove = { vm.askRemove(item) },
                    )
                    HorizontalDivider(thickness = 1.5.dp, color = p.line)
                }
                // Old lists: each clearing of the list is one folder, closed until tapped.
                if (vm.pastLists.isNotEmpty()) {
                    item(key = "past") { PastListsHeader(vm.keepBoughtDays) }
                    vm.pastLists.forEach { past ->
                        val open = past.clearedAt in openLists
                        item(key = "folder-${past.clearedAt}") {
                            PastListFolder(
                                past = past,
                                open = open,
                                onToggle = { openLists = if (open) openLists - past.clearedAt else openLists + past.clearedAt },
                                onRemove = { vm.askRemovePastList(past) },
                            )
                        }
                        if (open) {
                            items(past.items, key = { "was-" + it.id }) { item ->
                                Column(Modifier.padding(start = 24.dp)) {
                                    PreviousRow(
                                        item = item,
                                        onList = vm.isWaiting(item.key),
                                        onAdd = { vm.addAgain(item) },
                                        onRemove = { vm.askRemovePrevious(item) },
                                    )
                                    HorizontalDivider(thickness = 1.dp, color = p.line)
                                }
                            }
                            item(key = "again-${past.clearedAt}") {
                                Column(Modifier.padding(start = 24.dp, top = 10.dp, bottom = 6.dp)) {
                                    BigButton("Add all again", onClick = { vm.addAllAgain(past) }, look = Look.Outline, icon = R.drawable.ic_add)
                                }
                            }
                        }
                        item(key = "gap-${past.clearedAt}") { Spacer(Modifier.size(10.dp)) }
                    }
                }
            }
        }
    }
}

/**
 * Under the list. "Clear ticked items" appears only once something is ticked, and
 * becomes "Start a new list" when everything has been bought. On short screens and
 * tablets the two buttons sit side by side so the list keeps its room.
 */
@Composable
private fun ColumnScope.ListButtons(vm: AppViewModel, showClear: Boolean) {
    val compact = LocalCompactHeight.current
    val allBought = vm.allBought
    val clearLabel = if (allBought) "Start a new list" else "Clear ticked items"
    val clearLook = if (allBought) Look.Filled else Look.Outline
    val addLook = if (allBought) Look.Outline else Look.Filled
    if ((compact || isTablet()) && showClear) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton(clearLabel, onClick = vm::askClear, look = clearLook, icon = R.drawable.ic_check, modifier = Modifier.weight(1f))
            BigButton("Add items", onClick = { vm.go(Screen.AddItems) }, look = addLook, icon = R.drawable.ic_add, modifier = Modifier.weight(1f))
        }
    } else {
        if (showClear) BigButton(clearLabel, onClick = vm::askClear, look = clearLook, icon = R.drawable.ic_check)
        BigButton("Add items", onClick = { vm.go(Screen.AddItems) }, look = addLook, icon = R.drawable.ic_add)
    }
}

@Composable
private fun PastListsHeader(keepDays: Int) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp).semantics { heading() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_history), contentDescription = null, tint = p.muted, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Text("Old lists", style = Type.heading, color = p.ink)
        }
        Spacer(Modifier.size(4.dp))
        Text("Tap a list to open it. Each is kept for ${Days.label(keepDays)}.", style = Type.body, color = p.muted)
    }
}

// ---------------------------------------------------------------- Add items

/**
 * Pictures first, buttons at the bottom. A tap adds an item and ticks its picture;
 * another tap takes it off. No messages pop up over the buttons: the ticks and the
 * count on "Back to shopping list" show what has been added.
 * On tablets the pictures are bigger rather than more numerous.
 */
@Composable
fun AddItemsScreen(vm: AppViewModel) {
    val actions = LocalActions.current
    val p = LocalPalette.current
    var picked by rememberSaveable { mutableStateOf<String?>(null) } // null means All
    var query by rememberSaveable { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val searching = query.isNotBlank()
    // With categories switched off in Settings, every picture is shown in one list.
    val category = if (vm.categoriesOn) picked else null
    val shown: List<CatalogItem> = remember(category, query) {
        val c = category
        when {
            query.isNotBlank() -> Matching.browse(query)
            c == null -> Catalog.items.sortedWith(compareByDescending<CatalogItem> { vm.timesBought(it.key) }.thenBy { it.rank })
            else -> Catalog.items.filter { it.category.name == c }.sortedBy { it.rank }
        }
    }
    // Show a picture just added from elsewhere (the present after a birthday card).
    val gridState = rememberLazyGridState()
    val latestShown by rememberUpdatedState(shown)
    LaunchedEffect(vm.revealKey) {
        val k = vm.revealKey ?: return@LaunchedEffect
        if (query.isNotEmpty()) query = ""
        if (picked != null && Catalog.find(k)?.category?.name != picked) picked = null
        delay(150) // let the pictures update after clearing the search or category
        val i = latestShown.indexOfFirst { it.key == k }
        if (i >= 0) gridState.animateScrollToItem(i)
        vm.revealed()
    }
    // Coming from "Type instead" on Say it: open the keyboard on the search bar.
    LaunchedEffect(vm.focusSearch) {
        if (vm.focusSearch) {
            runCatching { searchFocus.requestFocus() }
            vm.searchFocused()
        }
    }
    val added = vm.addedThisVisitCount
    val backLabel = if (added > 0) "Back to shopping list ($added added)" else "Back to shopping list"
    val cleanQuery = ProfileRules.cleanName(query)
    // Something typed that is not exactly a catalogue item can still be added as typed.
    val offerTyped = searching && cleanQuery.isNotEmpty() && shown.none { Matching.isExactly(cleanQuery, it) }

    ScreenFrame(
        top = { TopBar("Add items", onBack = { vm.back() }) },
        bottom = {
            val compact = LocalCompactHeight.current
            if (isTablet()) {
                // tablet: the three buttons in one row, so the pictures keep the height
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BigButton("Say it", onClick = { actions.withMic { vm.openVoice() } }, look = Look.Outline, icon = R.drawable.ic_mic, modifier = Modifier.weight(1f))
                    BigButton("Photo", onClick = actions.takePhoto, look = Look.Outline, icon = R.drawable.ic_photo_camera, modifier = Modifier.weight(1f))
                    BigButton(if (added > 0) "Back to list ($added added)" else "Back to list", onClick = { vm.backToList() }, modifier = Modifier.weight(1.3f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile("Say it", R.drawable.ic_mic, onClick = { actions.withMic { vm.openVoice() } }, modifier = Modifier.weight(1f))
                    ActionTile("Photo", R.drawable.ic_photo_camera, onClick = actions.takePhoto, modifier = Modifier.weight(1f))
                    if (compact) {
                        ActionTile(if (added > 0) "List ($added)" else "List", R.drawable.ic_arrow_back, onClick = { vm.backToList() }, modifier = Modifier.weight(1f))
                    }
                }
                if (!compact) BigButton(backLabel, onClick = { vm.backToList() })
            }
        },
        scroll = false,
        reportBottomBar = true,
    ) {
        val wide = isTablet()
        SearchBar(query, onChange = { query = it.take(ProfileRules.MAX_NAME) }, focus = searchFocus, onDone = { keyboard?.hide() })
        Spacer(Modifier.size(10.dp))
        if (vm.categoriesOn && !searching) {
            CategoryChips(category) { picked = it }
            Spacer(Modifier.size(12.dp))
        }
        if (offerTyped) {
            BigButton(
                "Add \u201C$cleanQuery\u201D",
                onClick = {
                    if (vm.addCustom(cleanQuery, quiet = true)) query = "" else vm.showToast("$cleanQuery is already on the list")
                },
                look = Look.Outline,
                icon = R.drawable.ic_add,
            )
            Spacer(Modifier.size(10.dp))
        }
        if (searching && shown.isEmpty()) {
            Text("No picture for that yet. Press Add to put it on the list as typed.", style = Type.body, color = p.muted)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (wide) 140.dp else 112.dp),
            horizontalArrangement = Arrangement.spacedBy(if (wide) 14.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (wide) 14.dp else 10.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            state = gridState,
            modifier = Modifier.weight(1f),
        ) {
            items(shown, key = { it.key }) { c ->
                PickTile(c, selected = vm.isWaiting(c.key), onClick = { vm.tapTile(c) })
            }
        }
    }
}

/** The search bar at the top of Add items: type a word and the pictures narrow down as you go. */
@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit, focus: FocusRequester, onDone: () -> Unit) {
    val p = LocalPalette.current
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search, or type something new", style = Type.body, color = p.muted) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = p.ink, modifier = Modifier.size(28.dp)) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                Surface(onClick = { onChange("") }, color = p.surface, contentColor = p.ink, shape = RoundedCornerShape(10.dp)) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search", modifier = Modifier.padding(10.dp).size(26.dp))
                }
            }
        } else null,
        textStyle = Type.item,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.primary,
            unfocusedBorderColor = p.lineStrong,
            focusedTextColor = p.ink,
            unfocusedTextColor = p.ink,
            cursorColor = p.primary,
            focusedContainerColor = p.surface,
            unfocusedContainerColor = p.surface,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).focusRequester(focus).semantics { contentDescription = "Search items" },
    )
}

@Composable
private fun CategoryChips(current: String?, onPick: (String?) -> Unit) {
    val options: List<Pair<String?, String>> = listOf<Pair<String?, String>>(null to "All") +
        Catalog.categories.filter { it != Category.OTHER }.map { it.name to it.label }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it.first ?: "all" }) { (key, label) ->
            Chip(label, selected = key == current, onClick = { onPick(key) })
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) p.primary else p.surface,
        contentColor = if (selected) p.onPrimary else p.ink,
        border = BorderStroke(2.dp, if (selected) p.primary else p.lineStrong),
        modifier = Modifier.semantics { this.selected = selected },
    ) {
        Text(label, style = Type.label, modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 18.dp, vertical = 12.dp))
    }
}

