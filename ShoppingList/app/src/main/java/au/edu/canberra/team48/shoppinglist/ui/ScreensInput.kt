package au.edu.canberra.team48.shoppinglist.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.edu.canberra.team48.shoppinglist.AppViewModel
import au.edu.canberra.team48.shoppinglist.PhotoState
import au.edu.canberra.team48.shoppinglist.R
import au.edu.canberra.team48.shoppinglist.Screen
import au.edu.canberra.team48.shoppinglist.VoiceState
import au.edu.canberra.team48.shoppinglist.domain.CatalogItem
import au.edu.canberra.team48.shoppinglist.platform.PhotoStore
import au.edu.canberra.team48.shoppinglist.platform.SpeechProblem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------- 18 to 20 Say it

/**
 * Built on the assumption that speech recognition will sometimes be wrong: it offers
 * pictures to choose from and adds nothing until they are confirmed. Several things
 * can be said at once ("milk, bread and eggs"); each shows as a ticked picture.
 * Listening stops by itself when the person goes quiet.
 */
@Composable
fun VoiceScreen(vm: AppViewModel) {
    // Start listening on arrival. After the phone is turned, keep any pictures already
    // on screen rather than making the person say it again; only an interrupted
    // "Listening" starts over.
    DisposableEffect(Unit) {
        if (vm.voice is VoiceState.Listening) vm.startVoice()
        onDispose { if (vm.voice is VoiceState.Listening) vm.cancelVoice() }
    }
    val typeInstead = { vm.typeInstead() }
    when (val v = vm.voice) {
        is VoiceState.Listening -> ScreenFrame(
            top = { TopBar("Say it", onBack = { vm.back() }) },
            bottom = { BigButton("Stop listening", onClick = vm::stopVoice, look = Look.Outline) },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                MicBadge(active = true)
                Spacer(Modifier.size(24.dp))
                Heading("Listening")
                Spacer(Modifier.size(14.dp))
                Text(
                    if (v.heard.isBlank()) "Say what you need, like \u201Cmilk and bread\u201D" else "\u201C${v.heard}\u201D",
                    style = Type.item.copy(textAlign = TextAlign.Center),
                    color = LocalPalette.current.muted,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        is VoiceState.Choices -> ScreenFrame(
            top = { TopBar("Say it", onBack = { vm.back() }) },
            bottom = {
                BigButton("Say it again", onClick = vm::startVoice, look = Look.Outline, icon = R.drawable.ic_mic)
                BigButton("Add to list", onClick = vm::addVoiceChoice)
            },
        ) {
            Heading(if (v.options.size == 1) "Is this it?" else "Did you mean?")
            Spacer(Modifier.size(16.dp))
            ChoiceRow(v.options, v.selected, vm::pickVoice)
        }

        is VoiceState.Several -> ScreenFrame(
            top = { TopBar("Say it", onBack = { vm.back() }) },
            bottom = {
                val n = v.chosen.size
                BigButton("Say it again", onClick = vm::startVoice, look = Look.Outline, icon = R.drawable.ic_mic)
                BigButton(if (n == 1) "Add 1 item" else "Add $n items", onClick = vm::addVoiceChoice, enabled = n > 0)
            },
        ) {
            Heading("Add these?")
            Spacer(Modifier.size(16.dp))
            TickGrid(v.items, v.chosen, vm::pickVoice)
        }

        is VoiceState.Failed -> ScreenFrame(
            top = { TopBar("Say it", onBack = { vm.back() }) },
            bottom = {
                if (v.problem != SpeechProblem.UNAVAILABLE) {
                    BigButton("Try again", onClick = vm::startVoice, icon = R.drawable.ic_mic)
                }
                BigButton(
                    "Type instead",
                    onClick = typeInstead,
                    look = if (v.problem == SpeechProblem.UNAVAILABLE) Look.Filled else Look.Outline,
                    icon = R.drawable.ic_keyboard,
                )
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                MicBadge(active = false)
                Spacer(Modifier.size(24.dp))
                Heading(
                    when (v.problem) {
                        SpeechProblem.NOT_HEARD -> "Didn't catch that"
                        SpeechProblem.NO_INTERNET -> "Voice needs the internet"
                        SpeechProblem.UNAVAILABLE -> "Voice is not set up on this phone"
                    },
                    style = Type.heading.copy(textAlign = TextAlign.Center),
                )
            }
        }
    }
}

/** Several pictures, three to a row, each ticked while chosen. */
@Composable
private fun TickGrid(items: List<CatalogItem>, chosen: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 560.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { c -> PickTile(c, c.key in chosen, onClick = { onToggle(c.key) }, modifier = Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun MicBadge(active: Boolean) {
    val p = LocalPalette.current
    val pulse = rememberInfiniteTransition(label = "mic")
    val s by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "micScale",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
        Box(
            Modifier
                .size(160.dp)
                .scale(s)
                .clip(CircleShape)
                .background(if (active) p.iconBg else p.tickedRow),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(p.surface)
                .border(3.dp, if (active) p.primary else p.lineStrong, CircleShape),
        ) {
            Icon(
                painterResource(R.drawable.ic_mic),
                contentDescription = null,
                tint = if (active) p.primary else p.muted,
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

/** Up to three large picture choices; the selected one has a thick border and a tick. */
@Composable
private fun ChoiceRow(options: List<CatalogItem>, selected: String, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.widthIn(max = 560.dp)) {
        options.take(3).forEach { c ->
            PickTile(c, selected = c.key == selected, onClick = { onPick(c.key) }, modifier = Modifier.weight(1f))
        }
        if (options.size == 1) Spacer(Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------- 22 and 23 Photo

@Composable
fun PhotoScreen(vm: AppViewModel, uri: String) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val actions = LocalActions.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { PhotoStore.decodeUpright(ctx, Uri.parse(uri), 1000) }
    }
    var typed by rememberSaveable(uri) { mutableStateOf("") }
    var typedError by rememberSaveable(uri) { mutableStateOf<String?>(null) }

    ScreenFrame(
        top = { TopBar("Photo", onBack = { vm.back() }) },
        bottom = {
            when (vm.photo) {
                is PhotoState.Found -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Yes", onClick = vm::addPhotoChoice, modifier = Modifier.weight(1f))
                    BigButton("No", onClick = actions.takePhoto, look = Look.Outline, modifier = Modifier.weight(1f))
                }
                PhotoState.NotFound -> {
                    BigButton("Add to list", onClick = { typedError = vm.addPhotoNamed(typed) }, enabled = typed.isNotBlank())
                    BigButton("Take again", onClick = actions.takePhoto, look = Look.Outline, icon = R.drawable.ic_photo_camera)
                }
                PhotoState.Reading -> BigButton("Take again", onClick = actions.takePhoto, look = Look.Outline, icon = R.drawable.ic_photo_camera)
            }
        },
    ) {
        val bmp = bitmap
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(p.tickedRow)
                .align(Alignment.CenterHorizontally),
        ) {
            if (bmp != null) {
                Image(bmp.asImageBitmap(), contentDescription = "Your photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f))
            } else {
                CircularProgressIndicator(color = p.primary)
            }
        }
        Spacer(Modifier.size(18.dp))
        when (val s = vm.photo) {
            PhotoState.Reading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = p.primary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.size(14.dp))
                Heading("Looking at your photo")
            }

            is PhotoState.Found -> {
                Heading("Is this it?")
                Spacer(Modifier.size(14.dp))
                ChoiceRow(s.options, s.selected, vm::pickPhoto)
            }

            PhotoState.NotFound -> {
                Heading("What is it?")
                Spacer(Modifier.size(4.dp))
                Text(
                    if (vm.online) "The photo could not be recognised. Please type what it is." else "Recognising a photo needs the internet. Please type what it is.",
                    style = Type.body,
                    color = p.muted,
                )
                Spacer(Modifier.size(12.dp))
                val err = typedError
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(40); typedError = null },
                    placeholder = { Text("For example, jam", style = Type.item, color = p.muted) },
                    textStyle = Type.item,
                    singleLine = true,
                    isError = err != null,
                    supportingText = if (err != null) { { Text(err, style = Type.bodyBold) } } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { typedError = vm.addPhotoNamed(typed) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.ink,
                        unfocusedBorderColor = p.lineStrong,
                        focusedTextColor = p.ink,
                        unfocusedTextColor = p.ink,
                        cursorColor = p.primary,
                        focusedContainerColor = p.surface,
                        unfocusedContainerColor = p.surface,
                        errorBorderColor = p.danger,
                        errorSupportingTextColor = p.danger,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                )
            }
        }
    }
}
