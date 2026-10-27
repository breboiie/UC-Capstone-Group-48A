package au.edu.canberra.team48.shoppinglist.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import au.edu.canberra.team48.shoppinglist.AppViewModel
import au.edu.canberra.team48.shoppinglist.Screen
import au.edu.canberra.team48.shoppinglist.platform.PhotoStore
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity

/**
 * Things screens ask the phone to do. Each one handles its own permission,
 * so a screen never has to know whether it was already granted.
 */
class PlatformActions(
    val withMic: (onGranted: () -> Unit) -> Unit,
    val takePhoto: () -> Unit,
    val scanCode: () -> Unit,
    val withNotifications: (onDone: (granted: Boolean) -> Unit) -> Unit,
    /**
     * Offers the phone's own number with Google's Phone Number Hint: one tap, no typing
     * and no permission. [onNumber] gets it; [onNone] runs if the phone cannot offer one
     * (no SIM, an emulator, no Google Play). Backing out of the picker calls neither.
     */
    val phoneFromDevice: (onNumber: (String) -> Unit, onNone: () -> Unit) -> Unit,
)

val LocalActions = staticCompositionLocalOf<PlatformActions> { error("PlatformActions not provided") }

/**
 * Phones, and tablets held either way. Tablets still show one screen at a time,
 * exactly like a phone, so there is only ever one thing to look at.
 */
enum class FormFactor { PHONE, TABLET, TABLET_LANDSCAPE }

val LocalFormFactor = staticCompositionLocalOf { FormFactor.PHONE }

@Composable
fun isTablet(): Boolean = LocalFormFactor.current != FormFactor.PHONE

/** A phone screen never grows wider than this, so lines stay easy to read when it is turned sideways. */
private val PhoneMaxWidth = 720.dp

/** Shortest side at which a screen counts as a tablet (Android's own definition). */
private val TabletShortSide = 600.dp

@Composable
fun AppRoot(vm: AppViewModel) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val bottomBar = remember { mutableIntStateOf(0) }

    // ---- microphone
    var afterMic by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) afterMic?.invoke() else vm.showToast("The microphone is off. You can type instead.")
        afterMic = null
    }

    // ---- notifications (Android 13 and later ask first)
    var afterNotify by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        afterNotify?.invoke(granted)
        afterNotify = null
    }

    // ---- camera: the phone's own camera app takes the picture, so no camera permission is needed.
    // The file location survives the app being closed while the camera is open.
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhoto
        pendingPhoto = null
        vm.pendingCapture = null
        if (saved && uri != null) vm.onPhotoCaptured(Uri.parse(uri))
    }

    // ---- Photo: the phone's own camera app. The app declares CAMERA for the scanner, and
    // then Android requires it to be granted before handing over to the camera app too.
    fun openCamera() {
        val uri = PhotoStore.newCaptureUri(ctx)
        pendingPhoto = uri.toString()
        vm.pendingCapture = uri
        try {
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingPhoto = null
            vm.showToast("No camera app on this phone")
        } catch (e: SecurityException) {
            pendingPhoto = null
            vm.showToast("The camera is off for this app")
        }
    }
    val photoPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else vm.showToast("The camera is off for this app. You can search instead.")
    }

    // ---- camera for scanning the other phone's code (asked the first time only)
    val scanPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.openScanner() else vm.scannerUnavailable()
    }

    // ---- the phone's own number (sign up and profile)
    var afterPhone by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var noPhone by remember { mutableStateOf<(() -> Unit)?>(null) }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val onNumber = afterPhone
        val onNone = noPhone
        afterPhone = null
        noPhone = null
        if (result.resultCode == Activity.RESULT_OK) {
            val number = runCatching { Identity.getSignInClient(ctx).getPhoneNumberFromIntent(result.data) }.getOrNull()
            if (!number.isNullOrBlank()) onNumber?.invoke(number) else onNone?.invoke()
        }
    }

    val actions = remember {
        PlatformActions(
            withMic = { onGranted ->
                if (has(ctx, Manifest.permission.RECORD_AUDIO)) {
                    onGranted()
                } else {
                    afterMic = onGranted
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            takePhoto = {
                if (has(ctx, Manifest.permission.CAMERA)) openCamera() else photoPermission.launch(Manifest.permission.CAMERA)
            },
            scanCode = {
                if (has(ctx, Manifest.permission.CAMERA)) {
                    vm.openScanner()
                } else {
                    scanPermission.launch(Manifest.permission.CAMERA)
                }
            },
            withNotifications = { onDone ->
                if (Build.VERSION.SDK_INT < 33 || has(ctx, Manifest.permission.POST_NOTIFICATIONS)) {
                    onDone(true)
                } else {
                    afterNotify = onDone
                    notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            phoneFromDevice = { onNumber, onNone ->
                afterPhone = onNumber
                noPhone = onNone
                val failed = {
                    afterPhone = null
                    noPhone = null
                    onNone()
                }
                runCatching {
                    Identity.getSignInClient(ctx)
                        .getPhoneNumberHintIntent(GetPhoneNumberHintIntentRequest.builder().build())
                        .addOnSuccessListener { pending ->
                            runCatching { phoneLauncher.launch(IntentSenderRequest.Builder(pending).build()) }
                                .onFailure { failed() }
                        }
                        .addOnFailureListener { failed() }
                }.onFailure { failed() }
            },
        )
    }

    // System back: close a pop-up first, then go back a screen. On the list, leave the app as normal.
    BackHandler(enabled = vm.overlay != null || vm.stack.size > 1) { vm.back() }

    CompositionLocalProvider(LocalActions provides actions, LocalBottomBar provides bottomBar) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(p.paper)
                // the keyboard is left out here on purpose: phone or tablet must not change
                // when the keyboard opens, or the screen would be rebuilt mid-typing
                .systemBarsPadding()
                .displayCutoutPadding(),
        ) {
            val shortSide = if (maxWidth < maxHeight) maxWidth else maxHeight
            val form = when {
                shortSide < TabletShortSide -> FormFactor.PHONE
                maxWidth > maxHeight -> FormFactor.TABLET_LANDSCAPE
                else -> FormFactor.TABLET
            }
            val screen = vm.screen
            // While a pop-up is open, screen readers see only the pop-up.
            val hideBehind = if (vm.overlay != null) Modifier.clearAndSetSemantics { } else Modifier

            Box(Modifier.fillMaxSize().imePadding()) {   // the keyboard only makes the page shorter
            if (form == FormFactor.PHONE) {
                Box(Modifier.fillMaxSize().then(hideBehind), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = PhoneMaxWidth).fillMaxSize()) {
                        CurrentScreen(vm, screen)
                    }
                    ToastHost(vm)
                }
                OverlayHost(vm)
            } else {
                // On a tablet everything is drawn a little larger, up to a fifth, so text,
                // pictures and buttons are easy to see and hit from arm's length.
                val d = LocalDensity.current
                val scale = (shortSide.value / 620f).coerceIn(1f, 1.2f)
                CompositionLocalProvider(
                    LocalDensity provides Density(d.density * scale, d.fontScale),
                    LocalFormFactor provides form,
                ) {
                    TabletPage(vm, screen, form, hideBehind)
                    OverlayHost(vm)
                }
            }
            }
        }
    }
}

/**
 * A tablet shows the screen as one page in the middle, on a soft background, rather
 * than stretching a phone layout edge to edge. Each kind of screen gets a page width
 * that suits it; buttons always stay with the page, never in a separate column.
 */
@Composable
private fun TabletPage(vm: AppViewModel, screen: Screen, form: FormFactor, hideBehind: Modifier) {
    val p = LocalPalette.current
    val landscape = form == FormFactor.TABLET_LANDSCAPE
    // One page in the middle: wide enough for pictures and the list, narrow enough that a
    // form, its buttons and the words never spread across the whole tablet.
    val maxWidth = when (screen) {
        Screen.AddItems -> if (landscape) 1100.dp else 920.dp
        Screen.Home -> if (landscape) 940.dp else 820.dp
        else -> 720.dp
    }
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.backdrop)
            .padding(horizontal = 24.dp, vertical = if (landscape) 14.dp else 24.dp)
            .then(hideBehind),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize()
                .shadow(8.dp, shape)
                .clip(shape)
                .background(p.paper)
                .border(1.5.dp, p.line, shape),
        ) {
            CurrentScreen(vm, screen)
            ToastHost(vm)
        }
    }
}

@Composable
private fun CurrentScreen(vm: AppViewModel, screen: Screen) {
    when (screen) {
        Screen.Welcome -> WelcomeScreen(vm)
        Screen.SignUp -> SignUpScreen(vm)
        Screen.Home -> HomeScreen(vm)
        Screen.AddItems -> AddItemsScreen(vm)
        Screen.Voice -> VoiceScreen(vm)
        is Screen.Photo -> PhotoScreen(vm, screen.uri)
        Screen.Settings -> SettingsScreen(vm)
        Screen.Profile -> ProfileScreen(vm)
        Screen.RemindMe -> RemindMeScreen(vm)
        Screen.KeepBought -> KeepBoughtScreen(vm)
        Screen.FamilyTools -> FamilyToolsScreen(vm)
        Screen.Connect -> ConnectScreen(vm)
        Screen.TypeNumber -> TypeNumberScreen(vm)
        Screen.Scan -> ScanScreen(vm)
    }
}

private fun has(ctx: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
