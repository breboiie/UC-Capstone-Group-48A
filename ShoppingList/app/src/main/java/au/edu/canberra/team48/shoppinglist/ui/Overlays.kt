package au.edu.canberra.team48.shoppinglist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import au.edu.canberra.team48.shoppinglist.AppViewModel
import au.edu.canberra.team48.shoppinglist.Overlay
import au.edu.canberra.team48.shoppinglist.R
import au.edu.canberra.team48.shoppinglist.domain.Catalog
import au.edu.canberra.team48.shoppinglist.domain.ProfileRules
import kotlinx.coroutines.delay

/** Draws whichever pop-up is open, over the dimmed screen. Tapping the dimmed area closes it. */
@Composable
fun OverlayHost(vm: AppViewModel) {
    val o = vm.overlay ?: return
    val p = LocalPalette.current
    val dismissable = !(o is Overlay.JoinConfirm && o.busy)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.scrim)
            .pointerInput(o) { detectTapGestures { if (dismissable) vm.closeOverlay() } },
    ) {
        when (o) {
            is Overlay.ConfirmRemove -> PopUp("Remove item") {
                ItemPicture(o.item.key, o.item.photoPath, Modifier.size(96.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.size(14.dp))
                PopUpTitle("Remove ${o.item.name.lowercase()}?")
                Spacer(Modifier.size(20.dp))
                BigButton("Remove", onClick = vm::confirmRemove)
                BigButton("Keep", onClick = vm::closeOverlay, look = Look.Outline)
            }

            is Overlay.ConfirmRemovePrevious -> PopUp("Delete item") {
                ItemPicture(o.item.key, o.item.photoPath, Modifier.size(96.dp).align(Alignment.CenterHorizontally), dim = true)
                Spacer(Modifier.size(14.dp))
                PopUpTitle("Delete ${o.item.name.lowercase()} from this old list?")
                Spacer(Modifier.size(20.dp))
                BigButton("Delete", onClick = vm::confirmRemovePrevious)
                BigButton("Keep", onClick = vm::closeOverlay, look = Look.Outline)
            }

            is Overlay.ConfirmRemovePastList -> PopUp("Delete old list") {
                Icon(painterResource(R.drawable.ic_folder), contentDescription = null, tint = p.primary, modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.size(10.dp))
                PopUpTitle("Delete the list from ${pastListDate(o.past.clearedAt)}?")
                Spacer(Modifier.size(8.dp))
                PopUpText(if (o.past.items.size == 1) "It has 1 item." else "It has ${o.past.items.size} items.")
                Spacer(Modifier.size(20.dp))
                BigButton("Delete", onClick = vm::confirmRemovePastList)
                BigButton("Keep", onClick = vm::closeOverlay, look = Look.Outline)
            }

            is Overlay.ConfirmClear -> PopUp(if (o.allBought) "Start a new list" else "Bought") {
                val n = o.items.size
                PopUpTitle(
                    when {
                        o.allBought -> "Start a new list?"
                        n == 1 -> "Clear 1 ticked item?"
                        else -> "Clear $n ticked items?"
                    }
                )
                Spacer(Modifier.size(8.dp))
                PopUpText((if (o.allBought) "Everything has been bought. " else "Things still to buy stay on the list. ") + "Cleared things move to Bought before, under the list.")
                val others = vm.others.map { it.name }
                if (others.isNotEmpty()) {
                    Spacer(Modifier.size(4.dp))
                    PopUpText("${ProfileRules.joinNames(others)}'s phone too.")
                }
                Spacer(Modifier.size(20.dp))
                BigButton(if (o.allBought) "Start a new list" else "Clear", onClick = vm::confirmClear)
                BigButton("Keep", onClick = vm::closeOverlay, look = Look.Outline)
            }

            is Overlay.Suggest -> Sheet("Things you often buy") {
                Heading("Things you often buy")
                Spacer(Modifier.size(14.dp))
                ChoiceGrid(o.keys, o.added, vm::toggleSuggested)
                Spacer(Modifier.size(16.dp))
                BigButton("Done", onClick = vm::closeOverlay)
            }

            Overlay.OfferPresent -> PopUp("Present") {
                ItemPicture("present", null, Modifier.size(120.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.size(14.dp))
                PopUpTitle("Buy a present too?")
                Spacer(Modifier.size(20.dp))
                BigButton("Yes, add a present", onClick = vm::addPresent)
                BigButton("No, thanks", onClick = vm::closeOverlay, look = Look.Outline)
            }

            is Overlay.JoinConfirm -> PopUp("Join a list") {
                Avatar(ProfileRules.initial(o.owner), 80.dp)
                Spacer(Modifier.size(14.dp))
                PopUpTitle("Join ${o.owner}'s list?")
                Spacer(Modifier.size(20.dp))
                if (o.busy) {
                    CircularProgressIndicator(color = p.primary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.size(8.dp))
                    PopUpText("Joining")
                } else {
                    BigButton("Join", onClick = vm::confirmJoin)
                    BigButton("Cancel", onClick = vm::closeOverlay, look = Look.Outline)
                }
            }

            is Overlay.PairingError -> PopUp(o.title) {
                PopUpTitle(o.title)
                Spacer(Modifier.size(8.dp))
                PopUpText(o.detail)
                Spacer(Modifier.size(20.dp))
                BigButton("OK", onClick = vm::closeOverlay)
            }

            is Overlay.ConfirmStopSharing -> PopUp("Stop sharing") {
                Avatar(ProfileRules.initial(o.member.name), 80.dp)
                Spacer(Modifier.size(14.dp))
                PopUpTitle("Stop sharing with ${o.member.name}?")
                Spacer(Modifier.size(8.dp))
                PopUpText("${o.member.name}'s phone won't show this list any more. You can connect again any time.")
                Spacer(Modifier.size(20.dp))
                BigButton("Stop sharing", onClick = vm::confirmStopSharing)
                BigButton("Keep sharing", onClick = vm::closeOverlay, look = Look.Outline)
            }

            Overlay.ProfileSaved -> PopUp("Changed successfully") {
                PopUpTitle("Changed successfully!")
                Spacer(Modifier.size(8.dp))
                PopUpText("Your name and phone number are saved.")
                Spacer(Modifier.size(20.dp))
                BigButton("OK", onClick = vm::closeProfileSaved)
            }

            Overlay.SharingNotSetUp -> PopUp("Sharing is not set up") {
                PopUpTitle("Sharing is not set up")
                Spacer(Modifier.size(8.dp))
                PopUpText("This copy of the app has no online list yet. The list still works on this phone.")
                if (vm.showSyncDetail) {
                    Spacer(Modifier.size(8.dp))
                    PopUpText("For the team: add app/google-services.json from your Firebase project and build again. See README, Sharing between phones.")
                }
                Spacer(Modifier.size(20.dp))
                BigButton("OK", onClick = vm::closeOverlay)
            }
        }
    }
}

/** Swallows taps so pressing inside a pop-up does not close it. */
private val swallowTaps = Modifier.pointerInput(Unit) { detectTapGestures { } }

@Composable
private fun BoxScope.PopUp(title: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = p.surface,
        contentColor = p.ink,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(20.dp)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .then(swallowTaps)
            .semantics { paneTitle = title },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun BoxScope.Sheet(title: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = p.surface,
        contentColor = p.ink,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .then(swallowTaps)
            .semantics { paneTitle = title },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 48.dp, height = 6.dp)
                    .background(p.lineStrong, RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.size(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) { content() }
        }
    }
}

@Composable
private fun PopUpTitle(text: String) {
    Heading(text, style = Type.heading.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PopUpText(text: String) {
    Text(text, style = Type.body.copy(textAlign = TextAlign.Center), color = LocalPalette.current.ink, modifier = Modifier.fillMaxWidth())
}

/** Picture choices, three to a row. A tick shows each one that has been added. */
@Composable
private fun ChoiceGrid(keys: List<String>, added: Set<String>, onToggle: (String) -> Unit) {
    val items = keys.mapNotNull(Catalog::find)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 560.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { c ->
                    PickTile(c, c.key in added, onClick = { onToggle(c.key) }, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------------------------------------------------------------- messages

/** Short message just above the bottom buttons, never on top of them. Goes away on its own. */
@Composable
fun ToastHost(vm: AppViewModel) {
    val t = vm.toast ?: return
    val p = LocalPalette.current
    val density = LocalDensity.current
    val bottomPx = LocalBottomBar.current.intValue
    val bottom = with(density) { bottomPx.toDp() } + 12.dp

    LaunchedEffect(t.id) {
        delay(if (t.canUndo) 6_000 else 4_000)
        vm.dismissToast(t.id)
    }

    Box(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = bottom), contentAlignment = Alignment.BottomCenter) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .background(p.toast, RoundedCornerShape(12.dp))
                .border(2.dp, p.toast, RoundedCornerShape(12.dp))
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(t.text, style = Type.bodyBold, color = p.onToast, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            if (t.canUndo) {
                Surface(onClick = vm::undo, color = p.toast, contentColor = p.onToast, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        "Undo",
                        style = Type.button.copy(textDecoration = TextDecoration.Underline),
                        modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

/** Shown on the list when the phone has no internet and the list is shared. */
@Composable
fun OfflineBanner() {
    val p = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(2.5.dp, p.highlight, RoundedCornerShape(12.dp))
            .background(p.surface, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_wifi_off), contentDescription = null, tint = p.ink, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(12.dp))
        Text("No internet. Saved on this phone.", style = Type.bodyBold, color = p.ink)
    }
}
