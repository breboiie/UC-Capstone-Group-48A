package au.edu.canberra.team48.shoppinglist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import au.edu.canberra.team48.shoppinglist.AppViewModel
import au.edu.canberra.team48.shoppinglist.BuildConfig
import au.edu.canberra.team48.shoppinglist.R
import au.edu.canberra.team48.shoppinglist.Screen
import au.edu.canberra.team48.shoppinglist.data.SyncStatus
import au.edu.canberra.team48.shoppinglist.domain.Days
import au.edu.canberra.team48.shoppinglist.domain.ProfileRules
import au.edu.canberra.team48.shoppinglist.domain.Role

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    ScreenFrame(
        top = { TopBar("Settings", onBack = { vm.back() }) },
        bottom = { BigButton("Back to shopping list", onClick = vm::backToList, look = Look.Outline) },
    ) {
        // Profile header: the details from sign up. Tapping opens Profile.
        Surface(onClick = { vm.go(Screen.Profile) }, color = p.paper, contentColor = p.ink) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(vertical = 10.dp),
            ) {
                Avatar(ProfileRules.initial(vm.name), 64.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(vm.name, style = Type.heading, color = p.ink)
                    if (vm.phone.isNotBlank()) Text(vm.phone, style = Type.body, color = p.muted)
                }
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Edit profile", tint = p.muted, modifier = Modifier.size(32.dp))
            }
        }
        HorizontalDivider(thickness = 2.dp, color = p.line)
        // Both are AI suggestions. The labels say what each one does, so neither needs explaining.
        SwitchRow("Often Bought", vm.suggestionsOn, vm::setSuggestions)
        SwitchRow("Birthday Gift Option", vm.birthdayOn, vm::setBirthday)
        SwitchRow("Categories Option", vm.categoriesOn, vm::setCategories)
        NavRow("Remind me", value = if (vm.remindOn) Days.label(vm.remindDays) else "Off") { vm.go(Screen.RemindMe) }
        NavRow("Delete old lists after", value = Days.label(vm.keepBoughtDays)) { vm.go(Screen.KeepBought) }
        NavRow("Family member tools", value = vm.role.label) { vm.go(Screen.FamilyTools) }
    }
}

// ---------------------------------------------------------------- Remind me

@Composable
fun RemindMeScreen(vm: AppViewModel) {
    val actions = LocalActions.current
    ScreenFrame(
        top = { TopBar("Remind me", onBack = { vm.back() }) },
        bottom = { BigButton("Done", onClick = { vm.back() }) },
    ) {
        SwitchRow("Remind me", vm.remindOn) { on ->
            if (on) {
                actions.withNotifications { granted ->
                    vm.setRemind(true)
                    if (!granted) vm.showToast("Notifications are off for this app")
                }
            } else {
                vm.setRemind(false)
            }
        }
        if (vm.remindOn) {
            Spacer(Modifier.size(20.dp))
            Heading("If I have not shopped for", style = Type.bodyBold)
            Spacer(Modifier.size(12.dp))
            DaysPicker(vm.remindDays, vm::chooseRemindDays, what = "Remind me after")
            if (BuildConfig.DEMO_MODE) {
                Spacer(Modifier.size(12.dp))
                BigButton(
                    "Show a test reminder",
                    onClick = { actions.withNotifications { granted -> if (granted) vm.sendTestReminder() else vm.showToast("Notifications are off for this app") } },
                    look = Look.Quiet,
                    icon = R.drawable.ic_notifications,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Bought before

/** How long cleared items stay under the list, greyed out, before they are deleted. */
@Composable
fun KeepBoughtScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    ScreenFrame(
        top = { TopBar("Old lists", onBack = { vm.back() }) },
        bottom = { BigButton("Done", onClick = { vm.back() }) },
    ) {
        Heading("Delete old lists after", style = Type.bodyBold)
        Spacer(Modifier.size(12.dp))
        DaysPicker(vm.keepBoughtDays, vm::setKeepBought, what = "Delete old lists after")
        Spacer(Modifier.size(20.dp))
        Text(
            "Each time the list is cleared, the bought things are kept under it as an old list, for this long. Every phone sharing the list uses the same setting.",
            style = Type.body,
            color = p.muted,
        )
    }
}

// ---------------------------------------------------------------- Family member tools

@Composable
fun FamilyToolsScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    ScreenFrame(
        top = { TopBar("Family member tools", onBack = { vm.back() }) },
        bottom = { BigButton("Connect a phone", onClick = vm::openConnect, icon = R.drawable.ic_qr_code_2) },
    ) {
        Heading("I am", style = Type.bodyBold)
        Spacer(Modifier.size(12.dp))
        Role.entries.forEach { r ->
            RadioRow(r.label, selected = vm.role == r) { vm.chooseRole(r) }
        }
        Spacer(Modifier.size(16.dp))
        HorizontalDivider(thickness = 2.dp, color = p.line)
        Spacer(Modifier.size(14.dp))
        SharedListStatus(vm)
        val others = vm.others
        if (others.isNotEmpty()) {
            Spacer(Modifier.size(16.dp))
            HorizontalDivider(thickness = 2.dp, color = p.line)
            Spacer(Modifier.size(14.dp))
            Heading("Sharing the list with", style = Type.bodyBold)
            Spacer(Modifier.size(6.dp))
            others.forEach { m ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(vertical = 8.dp),
                ) {
                    Avatar(ProfileRules.initial(m.name), 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(m.name, style = Type.item, color = p.ink, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    RowButton("Stop sharing", onClick = { vm.askStopSharing(m) }, description = "Stop sharing with ${m.name}")
                }
                HorizontalDivider(thickness = 1.5.dp, color = p.line)
            }
        }
    }
}

/** One plain line saying whether the list is reaching the other phones. */
@Composable
private fun SharedListStatus(vm: AppViewModel) {
    val p = LocalPalette.current
    val (word, colour) = when (vm.syncStatus) {
        SyncStatus.CONNECTED -> "Working" to p.ink
        SyncStatus.CONNECTING -> "Connecting\u2026" to p.muted
        SyncStatus.NOT_SET_UP -> "Only on this phone" to p.muted
        SyncStatus.PROBLEM -> "Not working" to p.danger
    }
    Heading("Shared list", style = Type.bodyBold)
    Spacer(Modifier.size(4.dp))
    Text(word, style = Type.body, color = colour, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    if (vm.syncStatus == SyncStatus.PROBLEM) {
        Text("The list is still saved on this phone.", style = Type.body, color = p.ink)
    }
    if (vm.showSyncDetail) {
        val detail = when (vm.syncStatus) {
            SyncStatus.NOT_SET_UP -> "For the team: app/google-services.json is missing, so this build has no Firebase. See README, Sharing between phones."
            SyncStatus.PROBLEM -> "For the team: " + (vm.syncProblem ?: "see Logcat, tag ShoppingList.")
            SyncStatus.CONNECTING -> "For the team: waiting for Firebase. Needs internet."
            SyncStatus.CONNECTED -> null
        }
        if (detail != null) {
            Spacer(Modifier.size(6.dp))
            Text(detail, style = Type.label, color = p.muted)
        }
    }
}
