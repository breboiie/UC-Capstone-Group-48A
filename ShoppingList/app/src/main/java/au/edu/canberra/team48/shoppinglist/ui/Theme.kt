package au.edu.canberra.team48.shoppinglist.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import au.edu.canberra.team48.shoppinglist.R

/**
 * Sky-blue theme to match the My List logo. Light values were checked against
 * WCAG 2.1: body text 15.6:1, button text 5.9:1, muted text 6.9:1, the red bin 5.6:1.
 */
@Immutable
data class Palette(
    val paper: Color,        // screen background
    val surface: Color,      // rows, cards, pop-ups
    val ink: Color,          // main text, borders
    val muted: Color,        // secondary text
    val line: Color,         // dividers
    val lineStrong: Color,   // inactive borders
    val primary: Color,      // filled buttons
    val onPrimary: Color,
    val tickedRow: Color,    // background of a ticked row
    val accent: Color,       // highlights, the logo tick
    val highlight: Color,    // outline on an item just added by someone else
    val danger: Color,       // errors
    val delete: Color,       // the bin on each row
    val scrim: Color,
    val toast: Color,
    val onToast: Color,
    val iconBg: Color,       // behind icon pictures
    val backdrop: Color,     // around the page on tablets
)

val LightPalette = Palette(
    paper = Color(0xFFEEF7FD),      // light sky blue
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF0F1E29),
    muted = Color(0xFF44576A),
    line = Color(0xFFCFE1EE),
    lineStrong = Color(0xFF6B8599),
    primary = Color(0xFF0369A1),    // deep sky blue: white text on it is 5.9:1
    onPrimary = Color(0xFFFFFFFF),
    tickedRow = Color(0xFFE2EDF4),
    accent = Color(0xFF7DD3FC),
    highlight = Color(0xFF9A5B00),
    danger = Color(0xFFB3261E),
    delete = Color(0xFFC62828),     // the bin: red, 5.6:1 on white
    scrim = Color(0x99101A22),
    toast = Color(0xFF0F1E29),
    onToast = Color(0xFFFFFFFF),
    iconBg = Color(0xFFD6EEFB),
    backdrop = Color(0xFFD3EAF9),
)

val DarkPalette = Palette(
    paper = Color(0xFF0A1520),
    surface = Color(0xFF122230),
    ink = Color(0xFFEAF4FB),
    muted = Color(0xFFA9BDCC),
    line = Color(0xFF24384A),
    lineStrong = Color(0xFF7393AB),
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF082F49),
    tickedRow = Color(0xFF1A2B38),
    accent = Color(0xFF7DD3FC),
    highlight = Color(0xFFF2B84B),
    danger = Color(0xFFF2B8B5),
    delete = Color(0xFFFF8A80),
    scrim = Color(0xB3000000),
    toast = Color(0xFFEAF4FB),
    onToast = Color(0xFF0A1520),
    iconBg = Color(0xFF15344A),
    backdrop = Color(0xFF050C12),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

/**
 * Every size is in sp, so the phone's own text size setting scales the whole
 * app. There is deliberately no text size control inside the app.
 */
object Type {
    val screenTitle = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp)
    val hero = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp)
    val heading = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
    val item = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp)
    val button = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
    val body = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 26.sp)
    val bodyBold = body.copy(fontWeight = FontWeight.Bold)
    val tile = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
    val label = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp)
    val code = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 3.sp)
}

@Composable
fun ShoppingTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.primary, onPrimary = p.onPrimary, background = p.paper, onBackground = p.ink,
            surface = p.surface, onSurface = p.ink, outline = p.lineStrong, error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.primary, onPrimary = p.onPrimary, background = p.paper, onBackground = p.ink,
            surface = p.surface, onSurface = p.ink, outline = p.lineStrong, error = p.danger,
        )
    }
    val typography = Typography(
        bodyLarge = Type.body, bodyMedium = Type.body, titleLarge = Type.screenTitle, labelLarge = Type.button,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
