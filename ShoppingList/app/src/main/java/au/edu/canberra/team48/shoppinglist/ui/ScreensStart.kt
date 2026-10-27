package au.edu.canberra.team48.shoppinglist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import au.edu.canberra.team48.shoppinglist.AppViewModel
import au.edu.canberra.team48.shoppinglist.FieldErrors
import au.edu.canberra.team48.shoppinglist.R
import au.edu.canberra.team48.shoppinglist.Screen
import au.edu.canberra.team48.shoppinglist.domain.ProfileRules
import au.edu.canberra.team48.shoppinglist.platform.SpeechProblem

// ---------------------------------------------------------------- Welcome

@Composable
fun WelcomeScreen(vm: AppViewModel) {
    ScreenFrame(
        top = { Spacer(Modifier.size(24.dp)) },
        bottom = { BigButton("Start", onClick = { vm.go(Screen.SignUp) }) },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = if (isTablet()) 24.dp else 40.dp, bottom = 8.dp),
        ) {
            // The logo carries the name, My List
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "My List",
                modifier = Modifier
                    .size(if (LocalCompactHeight.current) 150.dp else 260.dp)
                    .clip(RoundedCornerShape(32.dp)),
            )
            Spacer(Modifier.size(24.dp))
            Text(
                "Your shared shopping list",
                style = Type.heading.copy(textAlign = TextAlign.Center),
                color = LocalPalette.current.ink,
            )
        }
    }
}

// ---------------------------------------------------------------- Sign up and Profile

@Composable
fun SignUpScreen(vm: AppViewModel) {
    ProfileForm(
        vm = vm,
        title = "Sign up",
        startName = "",
        startPhone = "",
        submitLabel = "Done",
        showAvatar = false,
        finishAfterVoice = true,
        onSubmit = vm::signUp,
    )
}

@Composable
fun ProfileScreen(vm: AppViewModel) {
    ProfileForm(
        vm = vm,
        title = "Profile",
        startName = vm.name,
        startPhone = vm.phone,
        submitLabel = "Save",
        showAvatar = true,
        finishAfterVoice = false,
        onSubmit = vm::saveProfile,
    )
}

/**
 * Steps of "Say it" on the sign-up form: a short guided conversation.
 * Name, confirm, number, confirm. Listening ends by itself when the person stops
 * talking, so nobody has to press Stop.
 */
private enum class Talk { OFF, LISTEN_NAME, CONFIRM_NAME, MISSED_NAME, LISTEN_PHONE, CONFIRM_PHONE, MISSED_PHONE }

@Composable
private fun ProfileForm(
    vm: AppViewModel,
    title: String,
    startName: String,
    startPhone: String,
    submitLabel: String,
    showAvatar: Boolean,
    finishAfterVoice: Boolean,
    onSubmit: (String, String) -> FieldErrors?,
) {
    val p = LocalPalette.current
    val actions = LocalActions.current
    var name by rememberSaveable { mutableStateOf(startName) }
    var phone by rememberSaveable { mutableStateOf(startPhone) }
    var errors by remember { mutableStateOf(FieldErrors()) }
    var field by rememberSaveable { mutableIntStateOf(0) } // 0 name, 1 phone
    val phoneFocus = remember { FocusRequester() }

    // guided voice
    var talk by rememberSaveable { mutableStateOf(Talk.OFF) }
    var heard by rememberSaveable { mutableStateOf("") }
    var problem by rememberSaveable { mutableStateOf(SpeechProblem.NOT_HEARD) }

    DisposableEffect(Unit) { onDispose { vm.speech.cancel() } }

    fun submit() {
        vm.speech.cancel()
        talk = Talk.OFF
        errors = onSubmit(name, phone) ?: FieldErrors()
    }

    fun listen(forPhone: Boolean) {
        heard = ""
        talk = if (forPhone) Talk.LISTEN_PHONE else Talk.LISTEN_NAME
        vm.speech.start(
            onPartial = { heard = it },
            onResults = { guesses ->
                val said = guesses.first()
                if (forPhone) {
                    val digits = ProfileRules.digitsFromSpeech(said)
                    if (digits.length >= 8) {
                        heard = ProfileRules.formatPhone(digits); talk = Talk.CONFIRM_PHONE
                    } else {
                        problem = SpeechProblem.NOT_HEARD; talk = Talk.MISSED_PHONE
                    }
                } else {
                    val n = ProfileRules.cleanName(said).split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
                    if (n.isNotBlank()) {
                        heard = n; talk = Talk.CONFIRM_NAME
                    } else {
                        problem = SpeechProblem.NOT_HEARD; talk = Talk.MISSED_NAME
                    }
                }
            },
            onFailed = { why ->
                problem = why
                talk = if (forPhone) Talk.MISSED_PHONE else Talk.MISSED_NAME
            },
        )
    }

    fun stopTalking() {
        vm.speech.cancel()
        talk = Talk.OFF
    }

    fun fillFromPhone() {
        actions.phoneFromDevice(
            { number ->
                phone = ProfileRules.formatPhone(number)
                errors = errors.copy(phone = null)
            },
            {
                vm.showToast("Please type your number")
                field = 1
                runCatching { phoneFocus.requestFocus() }
            },
        )
    }

    // Sign up: offer this phone's own number straight away (one tap, nothing to type).
    var offeredNumber by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (finishAfterVoice && phone.isBlank() && !offeredNumber) {
            offeredNumber = true
            // if this phone cannot offer its number, say nothing: the person starts with their name
            actions.phoneFromDevice({ number -> phone = ProfileRules.formatPhone(number); errors = errors.copy(phone = null) }, {})
        }
    }

    // Turning the phone stops the microphone. If that happened while listening, listen
    // again, so the screen never sits on "Listening" with nothing listening.
    LaunchedEffect(Unit) {
        when (talk) {
            Talk.LISTEN_NAME -> listen(forPhone = false)
            Talk.LISTEN_PHONE -> listen(forPhone = true)
            else -> {}
        }
    }

    // While talking, back returns to the form rather than leaving the screen.
    BackHandler(enabled = talk != Talk.OFF) { stopTalking() }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = p.ink,
        unfocusedBorderColor = p.lineStrong,
        focusedTextColor = p.ink,
        unfocusedTextColor = p.ink,
        focusedLabelColor = p.ink,
        unfocusedLabelColor = p.muted,
        cursorColor = p.primary,
        errorBorderColor = p.danger,
        errorLabelColor = p.danger,
        errorSupportingTextColor = p.danger,
        focusedContainerColor = p.surface,
        unfocusedContainerColor = p.surface,
        errorContainerColor = p.surface,
    )

    ScreenFrame(
        top = { TopBar(title, onBack = { if (talk != Talk.OFF) stopTalking() else vm.back() }) },
        bottom = {
            when (talk) {
                Talk.OFF -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Sign up talks through name then number. Profile fills only the box being edited.
                        ActionTile("Say it", R.drawable.ic_mic, onClick = { actions.withMic { listen(forPhone = !finishAfterVoice && field == 1) } }, modifier = Modifier.weight(1f))
                        ActionTile("My number", R.drawable.ic_dialpad, onClick = ::fillFromPhone, modifier = Modifier.weight(1f))
                    }
                    BigButton(submitLabel, onClick = ::submit)
                }
                Talk.LISTEN_NAME -> BigButton("Type instead", onClick = ::stopTalking, look = Look.Outline, icon = R.drawable.ic_keyboard)
                Talk.LISTEN_PHONE -> BigButton("Type instead", onClick = ::stopTalking, look = Look.Outline, icon = R.drawable.ic_keyboard)
                Talk.CONFIRM_NAME -> {
                    BigButton("Yes", onClick = {
                        name = heard
                        errors = errors.copy(name = null)
                        if (finishAfterVoice && phone.isBlank()) listen(forPhone = true) else talk = Talk.OFF
                    })
                    BigButton("No, say it again", onClick = { listen(forPhone = false) }, look = Look.Outline, icon = R.drawable.ic_mic)
                }
                Talk.CONFIRM_PHONE -> {
                    BigButton("Yes", onClick = {
                        phone = heard
                        errors = errors.copy(phone = null)
                        talk = Talk.OFF
                        if (finishAfterVoice && ProfileRules.isValidName(name)) submit()
                    })
                    BigButton("No, say it again", onClick = { listen(forPhone = true) }, look = Look.Outline, icon = R.drawable.ic_mic)
                }
                Talk.MISSED_NAME, Talk.MISSED_PHONE -> {
                    if (problem != SpeechProblem.UNAVAILABLE) {
                        BigButton("Try again", onClick = { listen(forPhone = talk == Talk.MISSED_PHONE) }, icon = R.drawable.ic_mic)
                    }
                    BigButton("Type instead", onClick = ::stopTalking, look = if (problem == SpeechProblem.UNAVAILABLE) Look.Filled else Look.Outline, icon = R.drawable.ic_keyboard)
                }
            }
        },
    ) {
        if (talk != Talk.OFF) {
            TalkPanel(talk, heard, problem)
        } else {
            val nameErr = errors.name
            val phoneErr = errors.phone
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().align(Alignment.CenterHorizontally)) {
                if (showAvatar) {
                    Avatar(ProfileRules.initial(name.ifBlank { startName }), 88.dp)
                    Spacer(Modifier.size(18.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(ProfileRules.MAX_NAME); errors = errors.copy(name = null) },
                    label = { Text("Name", style = Type.label) },
                    textStyle = Type.item,
                    singleLine = true,
                    isError = nameErr != null,
                    supportingText = if (nameErr != null) { { Text(nameErr, style = Type.bodyBold) } } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { runCatching { phoneFocus.requestFocus() } }),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) field = 0 },
                )
                Spacer(Modifier.size(14.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(20); errors = errors.copy(phone = null) },
                    label = { Text("Phone number", style = Type.label) },
                    textStyle = Type.item,
                    singleLine = true,
                    isError = phoneErr != null,
                    supportingText = if (phoneErr != null) { { Text(phoneErr, style = Type.bodyBold) } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().focusRequester(phoneFocus).onFocusChanged { if (it.isFocused) field = 1 },
                )
            }
        }
    }
}

/** What "Say it" shows in place of the form: listening, a confirmation, or a problem. */
@Composable
private fun TalkPanel(talk: Talk, heard: String, problem: SpeechProblem) {
    val p = LocalPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        when (talk) {
            Talk.LISTEN_NAME, Talk.LISTEN_PHONE -> {
                MicBadge(active = true)
                Spacer(Modifier.size(20.dp))
                Heading(if (talk == Talk.LISTEN_NAME) "Say your name" else "Say your phone number", style = Type.heading.copy(textAlign = TextAlign.Center))
                Spacer(Modifier.size(12.dp))
                Text(
                    if (heard.isBlank()) "Listening" else "“$heard”",
                    style = Type.item.copy(textAlign = TextAlign.Center),
                    color = p.muted,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Talk.CONFIRM_NAME, Talk.CONFIRM_PHONE -> {
                Spacer(Modifier.size(16.dp))
                Text(
                    heard,
                    style = Type.hero.copy(textAlign = TextAlign.Center),
                    color = p.ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(3.dp, p.primary, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 22.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
                Spacer(Modifier.size(20.dp))
                Heading("Is that right?", style = Type.heading.copy(textAlign = TextAlign.Center))
            }
            Talk.MISSED_NAME, Talk.MISSED_PHONE -> {
                MicBadge(active = false)
                Spacer(Modifier.size(20.dp))
                Heading(
                    when (problem) {
                        SpeechProblem.NOT_HEARD -> "Didn't catch that"
                        SpeechProblem.NO_INTERNET -> "Voice needs the internet"
                        SpeechProblem.UNAVAILABLE -> "Voice is not set up on this phone"
                    },
                    style = Type.heading.copy(textAlign = TextAlign.Center),
                )
            }
            Talk.OFF -> {}
        }
    }
}
