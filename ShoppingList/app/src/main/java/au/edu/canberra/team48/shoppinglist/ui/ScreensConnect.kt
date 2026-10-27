package au.edu.canberra.team48.shoppinglist.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.edu.canberra.team48.shoppinglist.platform.QrCamera
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import au.edu.canberra.team48.shoppinglist.AppViewModel
import au.edu.canberra.team48.shoppinglist.CodeState
import au.edu.canberra.team48.shoppinglist.R
import au.edu.canberra.team48.shoppinglist.Screen
import au.edu.canberra.team48.shoppinglist.domain.Pairing
import au.edu.canberra.team48.shoppinglist.domain.ProfileRules
import au.edu.canberra.team48.shoppinglist.platform.Qr
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- 32 and 33 Connect a phone

/** The WhatsApp pattern: one phone shows a code, the other scans it. */
@Composable
fun ConnectScreen(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val actions = LocalActions.current

    // When another phone joins while this code is showing, go back to the list and say so.
    // No separate "Connected" screen: the message is the confirmation.
    val startCount = remember { vm.others.size }
    LaunchedEffect(vm.others.size) {
        if (vm.others.size > startCount) vm.someoneJoined(vm.others.last().name)
    }

    ScreenFrame(
        top = { TopBar("Connect a phone", onBack = { vm.back() }) },
        bottom = if (tab == 1) {
            {
                BigButton("Scan the code", onClick = actions.scanCode, icon = R.drawable.ic_qr_code_scanner)
                BigButton("Type the number instead", onClick = { vm.go(Screen.TypeNumber) }, look = Look.Outline, icon = R.drawable.ic_dialpad)
            }
        } else {
            null
        },
    ) {
        Tabs(tab) { tab = it }
        Spacer(Modifier.size(20.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (tab == 0) MyCode(vm) else ScanHelp()
        }
    }
}

@Composable
private fun Tabs(current: Int, onPick: (Int) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.5.dp, p.ink, RoundedCornerShape(12.dp)),
    ) {
        listOf("My code", "Scan code").forEachIndexed { i, label ->
            val on = i == current
            Surface(
                onClick = { onPick(i) },
                color = if (on) p.primary else p.surface,
                contentColor = if (on) p.onPrimary else p.ink,
                modifier = Modifier
                    .weight(1f)
                    .semantics { selected = on },
            ) {
                Text(
                    label,
                    style = Type.button.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.heightIn(min = 56.dp).padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun MyCode(vm: AppViewModel) {
    val p = LocalPalette.current
    LaunchedEffect(Unit) { vm.makeCode() }

    when (val c = vm.code) {
        CodeState.Loading -> {
            Spacer(Modifier.size(60.dp))
            CircularProgressIndicator(color = p.primary, modifier = Modifier.size(56.dp))
        }

        is CodeState.Failed -> {
            CentreMessage(R.drawable.ic_wifi_off, c.message)
            BigButton("Try again", onClick = vm::makeCode, modifier = Modifier.widthIn(max = 420.dp))
        }

        is CodeState.Ready -> {
            // A fresh code replaces this one as soon as it runs out.
            LaunchedEffect(c.expiresAt) {
                delay((c.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L))
                vm.makeCode()
            }
            val qr = remember(c.code) { Qr.bitmap(Pairing.qrPayload(c.code)) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.dp, p.line, RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .semantics { contentDescription = "Code for ${vm.name}'s list" },
            ) {
                Image(qr.asImageBitmap(), contentDescription = null, filterQuality = FilterQuality.None, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Spacer(Modifier.size(16.dp))
            Heading(vm.name)
            Spacer(Modifier.size(6.dp))
            Text(Pairing.display(c.code), style = Type.code, color = p.ink)
            Spacer(Modifier.size(12.dp))
            Body("On the other phone, choose Scan code.", muted = true)
        }
    }
}

@Composable
private fun ScanHelp() {
    val p = LocalPalette.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp).clip(RoundedCornerShape(24.dp)).background(p.iconBg),
    ) {
        Icon(painterResource(R.drawable.ic_qr_code_scanner), contentDescription = null, tint = p.primary, modifier = Modifier.size(100.dp))
    }
    Spacer(Modifier.size(20.dp))
    Heading("Scan the code on the other phone", style = Type.heading.copy(textAlign = TextAlign.Center))
}

// ---------------------------------------------------------------- 34 Type the number

@Composable
fun TypeNumberScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // The phone's own number keyboard opens straight away.
    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }
    ScreenFrame(
        top = { TopBar("Type the number", onBack = { vm.back() }) },
        bottom = {
            BigButton(
                if (vm.checkingCode) "Checking" else "Join",
                onClick = vm::submitNumber,
                enabled = Pairing.isWellFormed(vm.typedNumber) && !vm.checkingCode,
            )
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("The 6-digit number on the other phone", style = Type.bodyBold, color = p.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.size(16.dp))
            // An ordinary text box underneath, drawn as six boxes. Tapping anywhere on them opens the keyboard.
            BasicTextField(
                value = vm.typedNumber,
                onValueChange = vm::onNumberTyped,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.submitNumber() }),
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .semantics { contentDescription = "Number from the other phone" },
                decorationBox = { inner ->
                    Box {
                        Box(Modifier.size(1.dp).alpha(0f)) { inner() }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            repeat(Pairing.LENGTH) { i ->
                                val d = vm.typedNumber.getOrNull(i)
                                val next = i == vm.typedNumber.length
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 68.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(p.surface)
                                        .border(if (d != null || next) 3.dp else 2.dp, if (next) p.primary else if (d != null) p.ink else p.lineStrong, RoundedCornerShape(10.dp)),
                                ) {
                                    Text(d?.toString() ?: "", style = Type.code, color = p.ink)
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}


// ---------------------------------------------------------------- Scan

/**
 * Scans the code on the other phone with this phone's camera, inside the app.
 * If the camera cannot be used, "Type the number instead" is always there.
 */
@Composable
fun ScanScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val camera = remember { QrCamera(ctx) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { camera.stop() } }
    ScreenFrame(
        top = { TopBar("Scan the code", onBack = { vm.back() }) },
        bottom = { BigButton("Type the number instead", onClick = vm::typeNumberInstead, look = Look.Outline, icon = R.drawable.ic_dialpad) },
        scroll = false,
    ) {
        Text("Point the camera at the code on the other phone.", style = Type.bodyBold, color = p.ink)
        Spacer(Modifier.size(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.Black),
        ) {
            if (failed) {
                Text(
                    "The camera could not start. Please type the number instead.",
                    style = Type.bodyBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                AndroidView(
                    factory = { c ->
                        PreviewView(c).also { view ->
                            camera.start(
                                owner = owner,
                                view = view,
                                ready = { vm.overlay == null && !vm.checkingCode && vm.screen == Screen.Scan },
                                onCode = vm::onScanned,
                                onError = { failed = true },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                // where to hold the code
                Box(Modifier.fillMaxWidth(0.62f).aspectRatio(1f).border(4.dp, Color.White, RoundedCornerShape(24.dp)))
                if (vm.checkingCode) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(56.dp))
            }
        }
    }
}
