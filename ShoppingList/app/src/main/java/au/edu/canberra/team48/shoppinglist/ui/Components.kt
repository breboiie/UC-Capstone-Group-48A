package au.edu.canberra.team48.shoppinglist.ui

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.edu.canberra.team48.shoppinglist.domain.PastList
import androidx.compose.ui.draw.rotate
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.LocalDate
import java.time.Instant
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import au.edu.canberra.team48.shoppinglist.domain.Days
import au.edu.canberra.team48.shoppinglist.R
import au.edu.canberra.team48.shoppinglist.domain.Catalog
import au.edu.canberra.team48.shoppinglist.domain.CatalogItem
import au.edu.canberra.team48.shoppinglist.domain.ListItem
import au.edu.canberra.team48.shoppinglist.platform.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Height in pixels of the current screen's bottom button area, so messages sit just above it. */
val LocalBottomBar = staticCompositionLocalOf<MutableIntState> { mutableIntStateOf(0) }

private val Rounded = RoundedCornerShape(12.dp)

// ---------------------------------------------------------------- page frame

/**
 * True on short screens: a phone turned sideways, the keyboard open, or the phone's
 * text at its largest. Screens use it to put buttons side by side instead of stacked.
 */
val LocalCompactHeight = staticCompositionLocalOf { false }

/**
 * Every screen uses this: top bar, content in the middle, and all action buttons
 * at the bottom. Only back and the profile button live at the top.
 *
 * On tall screens the buttons are pinned to the bottom. On short screens they
 * would squeeze the middle to nothing, so for scrolling screens they move to the
 * end of the page and scroll with it; nothing can end up hidden or unreachable.
 * [reportBottomBar] is false for anything drawn on top of a screen, so only the
 * screen itself decides where messages appear.
 */
@Composable
fun ScreenFrame(
    top: @Composable () -> Unit,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    scroll: Boolean = true,
    reportBottomBar: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    val bottomBar = LocalBottomBar.current
    val fontScale = LocalDensity.current.fontScale
    val tablet = LocalFormFactor.current != FormFactor.PHONE
    BoxWithConstraints(Modifier.fillMaxSize().background(p.paper)) {
        val compact = maxHeight < 480.dp || (fontScale >= 1.5f && maxHeight < 640.dp)
        CompositionLocalProvider(LocalCompactHeight provides compact) {
            // Forms and settings on a tablet, and any page on a short screen: the buttons sit
            // right under what they act on and scroll with it, instead of far away at the bottom
            // of a big page. Lists keep their buttons pinned at the bottom.
            // One structure in every case, so opening the keyboard never rebuilds the page.
            val inline = scroll && (compact || tablet)
            Column(Modifier.fillMaxSize()) {
                top()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                        .padding(horizontal = if (tablet) 28.dp else 16.dp, vertical = if (compact) 8.dp else if (tablet) 20.dp else 12.dp),
                ) {
                    content()
                    if (inline && bottom != null) {
                        Spacer(Modifier.size(if (tablet) 32.dp else 20.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(if (tablet) 14.dp else 10.dp),
                            modifier = Modifier
                                .then(if (tablet) Modifier.widthIn(max = 560.dp) else Modifier)
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                            content = bottom,
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                }
                if (!inline && bottom != null) {
                    HorizontalDivider(thickness = 1.dp, color = p.line)
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(p.paper)
                            .then(if (reportBottomBar) Modifier.onSizeChanged { bottomBar.intValue = it.height } else Modifier)
                            .padding(
                                start = if (tablet) 28.dp else 16.dp, end = if (tablet) 28.dp else 16.dp,
                                top = if (compact) 8.dp else 12.dp, bottom = if (compact) 8.dp else if (tablet) 20.dp else 14.dp,
                            ),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.then(if (tablet) Modifier.widthIn(max = 760.dp) else Modifier).fillMaxWidth(),
                            content = bottom,
                        )
                    }
                } else if (reportBottomBar) {
                    SideEffect { bottomBar.intValue = 0 }
                }
            }
        }
    }
}


@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    avatar: String? = null,
    onAvatar: () -> Unit = {},
) {
    val p = LocalPalette.current
    val compact = LocalCompactHeight.current
    Column(Modifier.background(p.paper)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 60.dp else 72.dp)
                .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                SquareIconButton(R.drawable.ic_arrow_back, "Back", onBack)
                Spacer(Modifier.width(14.dp))
            }
            Text(
                text = title,
                style = Type.screenTitle,
                color = p.ink,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (avatar != null) {
                Spacer(Modifier.width(12.dp))
                Avatar(avatar, 56.dp, onClick = onAvatar, description = "Profile and settings")
            }
        }
        HorizontalDivider(thickness = 2.dp, color = p.line)
    }
}

/** A button that sits inside a row, sized to its words rather than the full width. */
@Composable
fun RowButton(text: String, onClick: () -> Unit, description: String? = null) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = p.surface,
        contentColor = p.ink,
        border = BorderStroke(2.5.dp, p.ink),
        modifier = Modifier.heightIn(min = 56.dp).then(
            if (description != null) Modifier.semantics { contentDescription = description } else Modifier
        ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(text, style = Type.label, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SquareIconButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = p.surface,
        contentColor = p.ink,
        border = BorderStroke(2.dp, p.ink),
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun Avatar(initial: String, size: Dp, onClick: (() -> Unit)? = null, description: String? = null) {
    val p = LocalPalette.current
    val inner: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                initial,
                style = Type.heading.copy(fontSize = Type.heading.fontSize * (size.value / 52f)),
                color = p.ink,
                modifier = if (description != null) Modifier.clearAndSetSemantics { } else Modifier,
            )
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = p.iconBg,
            border = BorderStroke(2.5.dp, p.ink),
            modifier = Modifier.size(size).semantics { if (description != null) contentDescription = description },
            content = inner,
        )
    } else {
        Surface(shape = CircleShape, color = p.iconBg, border = BorderStroke(2.5.dp, p.ink), modifier = Modifier.size(size), content = inner)
    }
}

// ---------------------------------------------------------------- buttons

enum class Look { Filled, Outline, Quiet }

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    look: Look = Look.Filled,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val p = LocalPalette.current
    val (bg, fg, border) = when (look) {
        Look.Filled -> Triple(p.primary, p.onPrimary, BorderStroke(2.5.dp, p.primary))
        Look.Outline -> Triple(p.surface, p.ink, BorderStroke(2.5.dp, p.ink))
        Look.Quiet -> Triple(p.surface, p.muted, BorderStroke(2.dp, p.lineStrong))
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = Rounded,
        color = bg,
        contentColor = fg,
        border = border,
        modifier = modifier.fillMaxWidth().heightIn(min = 62.dp).alpha(if (enabled) 1f else 0.45f),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = Type.button, textAlign = TextAlign.Center)
        }
    }
}

/** A square button with the icon above the word, used three across on Add items. */
@Composable
fun ActionTile(text: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        shape = Rounded,
        color = p.surface,
        contentColor = p.ink,
        border = BorderStroke(2.5.dp, p.ink),
        modifier = modifier.heightIn(min = 76.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.size(4.dp))
            Text(text, style = Type.label, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// ---------------------------------------------------------------- pictures

/**
 * Picture for an item: the household's own photo if they took one, otherwise the
 * bundled picture for that item, otherwise a shopping-bags picture for anything
 * typed or said that is not in the catalogue. Never a plain icon.
 */
@Composable
fun ItemPicture(key: String, photoPath: String?, modifier: Modifier, dim: Boolean = false) {
    val shaped = modifier.clip(RoundedCornerShape(10.dp)).alpha(if (dim) 0.45f else 1f)
    val own by produceState<Bitmap?>(initialValue = null, photoPath) {
        value = photoPath?.let { path -> withContext(Dispatchers.IO) { PhotoStore.load(path) } }
    }
    // older lists may hold an item that has since been merged, such as "oat milk" into Milk
    val bundled = Pictures.photo(Catalog.find(key)?.key ?: key) ?: Pictures.custom
    val ownBitmap = own
    if (ownBitmap != null) {
        Image(ownBitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = shaped)
    } else {
        Image(painterResource(bundled), contentDescription = null, contentScale = ContentScale.Crop, modifier = shaped)
    }
}

// ---------------------------------------------------------------- the list

/**
 * One line of the list. Bin at the far left, tick box at the far right, as the
 * participants asked, so a slip cannot delete something that was meant to be ticked.
 * Tapping anywhere on the picture or the word also ticks. The word is never struck through.
 */
@Composable
fun ItemRow(item: ListItem, highlighted: Boolean, onTick: () -> Unit, onRemove: () -> Unit) {
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // On narrow phones, or with the phone's text very large, the picture shrinks
        // a little so the item's name keeps room to be read in whole words.
        val narrow = maxWidth < 380.dp || LocalDensity.current.fontScale >= 1.5f
        val gap = if (narrow) 8.dp else 12.dp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (item.ticked) p.tickedRow else p.surface)
                .then(if (highlighted) Modifier.border(3.dp, p.highlight) else Modifier)
                .heightIn(min = 80.dp)
                .padding(horizontal = if (narrow) 4.dp else 8.dp, vertical = 8.dp),
        ) {
            Surface(
                onClick = onRemove,
                shape = RoundedCornerShape(10.dp),
                color = p.surface,
                contentColor = p.delete,
                border = BorderStroke(2.dp, p.delete),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = "Remove ${item.name}", modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(gap))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .toggleable(value = item.ticked, role = Role.Checkbox, onValueChange = { onTick() })
                    .semantics { stateDescription = if (item.ticked) "Ticked" else "Not ticked" }
                    .padding(vertical = 4.dp),
            ) {
                ItemPicture(item.key, item.photoPath, Modifier.size(if (narrow) 48.dp else if (isTablet()) 72.dp else 62.dp), dim = item.ticked)
                Spacer(Modifier.width(gap))
                Text(
                    item.name,
                    style = Type.item,
                    color = if (item.ticked) p.muted else p.ink,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(gap))
                TickBox(item.ticked)
            }
        }
    }
}

/** "Today", "Yesterday" or "Tuesday 14 October": when an old list was cleared. */
fun pastListDate(at: Long): String {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (day) {
        today -> "today"
        today.minusDays(1) -> "yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
    }
}

/**
 * One old list, like a folder: the day it was cleared and how many things were on it.
 * Tap to open or close it. The red bin deletes the whole list.
 */
@Composable
fun PastListFolder(past: PastList, open: Boolean, onToggle: () -> Unit, onRemove: () -> Unit) {
    val p = LocalPalette.current
    val n = past.items.size
    val time = Instant.ofEpochMilli(past.clearedAt).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())).lowercase()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.surface)
            .border(2.dp, if (open) p.primary else p.line, RoundedCornerShape(14.dp))
            .heightIn(min = 84.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Surface(
            onClick = onRemove,
            shape = RoundedCornerShape(10.dp),
            color = p.surface,
            contentColor = p.delete,
            border = BorderStroke(2.dp, p.delete),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete the list from ${pastListDate(past.clearedAt)}", modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(onClickLabel = if (open) "Close" else "Open", onClick = onToggle)
                .semantics { stateDescription = if (open) "Open" else "Closed" }
                .padding(vertical = 4.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(p.iconBg),
            ) {
                Icon(painterResource(R.drawable.ic_folder), contentDescription = null, tint = p.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("List from ${pastListDate(past.clearedAt)}", style = Type.item, color = p.ink)
                Text(
                    (if (n == 1) "1 item" else "$n items") + ", cleared at $time",
                    style = Type.body,
                    color = p.muted,
                )
            }
            Icon(
                painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = p.ink,
                modifier = Modifier.size(36.dp).rotate(if (open) 180f else 0f),
            )
        }
    }
}

/**
 * A row inside an old list: greyed out like a ticked item, with the same red bin at the left.
 * Tapping the row (or Add) puts it back on the list.
 */
@Composable
fun PreviousRow(item: ListItem, onList: Boolean, onAdd: () -> Unit, onRemove: () -> Unit) {
    val p = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(p.tickedRow)
            .heightIn(min = 80.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Surface(
            onClick = onRemove,
            shape = RoundedCornerShape(10.dp),
            color = p.surface,
            contentColor = p.delete,
            border = BorderStroke(2.dp, p.delete),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete ${item.name} from Bought before", modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = !onList, onClickLabel = "Add again", onClick = onAdd)
                .padding(vertical = 4.dp),
        ) {
            ItemPicture(item.key, item.photoPath, Modifier.size(if (isTablet()) 64.dp else 52.dp), dim = true)
            Spacer(Modifier.width(12.dp))
            Text(item.name, style = Type.item, color = p.muted, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            if (onList) {
                Text("On the list", style = Type.label, color = p.muted)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .border(2.dp, p.lineStrong, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = p.ink, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", style = Type.label, color = p.ink)
                }
            }
        }
    }
}

@Composable
fun TickBox(ticked: Boolean) {
    val p = LocalPalette.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (ticked) p.primary else p.surface)
            .border(3.dp, if (ticked) p.primary else p.ink, RoundedCornerShape(8.dp)),
    ) {
        if (ticked) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = p.onPrimary, modifier = Modifier.size(34.dp))
    }
}

/** A picture tile on Add items and in the suggestion pop-ups. */
@Composable
fun PickTile(item: CatalogItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) p.tickedRow else p.surface,
        contentColor = p.ink,
        border = if (selected) BorderStroke(3.5.dp, p.primary) else BorderStroke(2.dp, p.line),
        modifier = modifier.semantics { stateDescription = if (selected) "On the list" else "Not on the list" },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Box {
                ItemPicture(item.key, null, Modifier.fillMaxWidth().aspectRatio(1f))
                if (selected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(p.primary),
                    ) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = p.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.size(6.dp))
            Text(
                item.name,
                style = Type.tile,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------- settings rows

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp),
    ) {
        Text(label, style = Type.item, color = p.ink, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = p.primary,
                checkedThumbColor = p.onPrimary,
                uncheckedBorderColor = p.lineStrong,
                uncheckedThumbColor = p.lineStrong,
                uncheckedTrackColor = p.surface,
            ),
        )
    }
    HorizontalDivider(thickness = 1.5.dp, color = p.line)
}

@Composable
fun NavRow(label: String, value: String? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Surface(onClick = onClick, color = p.paper, contentColor = p.ink) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 10.dp),
        ) {
            Text(label, style = Type.item, modifier = Modifier.weight(1f))
            if (value != null) {
                Text(value, style = Type.body, color = p.muted)
                Spacer(Modifier.width(6.dp))
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = p.muted, modifier = Modifier.size(32.dp))
        }
    }
    HorizontalDivider(thickness = 1.5.dp, color = p.line)
}

@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(Rounded)
            .background(if (selected) p.tickedRow else p.surface)
            .border(if (selected) 3.dp else 2.dp, if (selected) p.primary else p.lineStrong, Rounded)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 68.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = p.primary, unselectedColor = p.ink),
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = Type.item, color = p.ink)
    }
}

// ---------------------------------------------------------------- text helpers

@Composable
fun Heading(text: String, modifier: Modifier = Modifier, style: TextStyle = Type.heading) {
    Text(text, style = style, color = LocalPalette.current.ink, modifier = modifier.semantics { heading() })
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    val p = LocalPalette.current
    Text(text, style = Type.body, color = if (muted) p.muted else p.ink, modifier = modifier)
}

/** Centred picture and words, used for empty and error states. */
@Composable
fun CentreMessage(@DrawableRes icon: Int, title: String, detail: String? = null) {
    val p = LocalPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(112.dp).clip(CircleShape).background(p.iconBg),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = p.primary, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.size(20.dp))
        Heading(title, style = Type.heading.copy(textAlign = TextAlign.Center))
        if (detail != null) {
            Spacer(Modifier.size(8.dp))
            Body(detail, muted = true, modifier = Modifier.padding(horizontal = 12.dp))
        }
    }
}


// ---------------------------------------------------------------- number of days

/**
 * Number of days, for Remind me and for how long Bought before keeps things:
 * big minus and plus buttons around the number, and quick choices of 3, 7 and 14 days.
 */
@Composable
fun DaysPicker(days: Int, onChange: (Int) -> Unit, what: String) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        StepButton(R.drawable.ic_remove, "One day fewer", enabled = days > Days.MIN) { onChange(Days.fewer(days)) }
        Text(
            Days.label(days),
            style = Type.hero.copy(textAlign = TextAlign.Center),
            color = p.ink,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "$what: ${Days.label(days)}" },
        )
        StepButton(R.drawable.ic_add, "One day more", enabled = days < Days.MAX) { onChange(Days.more(days)) }
    }
    Spacer(Modifier.size(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Days.QUICK.forEach { d ->
            val on = d == days
            Surface(
                onClick = { onChange(d) },
                shape = RoundedCornerShape(12.dp),
                color = if (on) p.primary else p.surface,
                contentColor = if (on) p.onPrimary else p.ink,
                border = BorderStroke(2.5.dp, if (on) p.primary else p.ink),
                modifier = Modifier.weight(1f).heightIn(min = 60.dp).semantics { selected = on },
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                    Text(Days.label(d), style = Type.button, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun StepButton(@DrawableRes icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = p.surface,
        contentColor = p.ink,
        border = BorderStroke(2.5.dp, p.ink),
        modifier = Modifier.size(76.dp).alpha(if (enabled) 1f else 0.35f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(40.dp))
        }
    }
}
