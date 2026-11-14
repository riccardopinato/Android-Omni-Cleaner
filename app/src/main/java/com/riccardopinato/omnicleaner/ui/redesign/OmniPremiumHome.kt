package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.DeepStorageAccessStatus
import com.riccardopinato.omnicleaner.ui.CleanerUiState
import com.riccardopinato.omnicleaner.ui.theme.OmniBlue
import com.riccardopinato.omnicleaner.ui.theme.OmniMint
import java.util.Locale
import kotlin.math.roundToInt

/* Premium Light & Dark: first-party Compose reproduction of the approved mockup.
 * All storage and scan metrics are device-derived; no mock percentages or savings.
 */
@Composable
internal fun OmniPremiumWaveBackground(dark: Boolean) {
    Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(if (dark) listOf(Color(0xFF0A112C), Color(0xFF0A1025))
            else listOf(Color(0xFFEAF2FF), Color(0xFFF8FAFF))))
        val top = Path().apply {
            moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h * .21f)
            cubicTo(w * .60f, h * .11f, w * .42f, h * .19f, 0f, h * .04f); close()
        }
        drawPath(top, Brush.linearGradient(
            if (dark) listOf(Color(0xFF2D165A), Color(0xFF173B8A), Color(0xFF0AADEB))
            else listOf(Color(0xFFCDDFFF), Color(0xFFAAD7FF), Color(0xFF79B9FF)),
            Offset.Zero, Offset(w, h * .22f),
        ))
        val bottom = Path().apply {
            moveTo(0f, h * .76f)
            cubicTo(w * .4f, h * .82f, w * .5f, h, w, h * .94f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(bottom, Brush.linearGradient(
            if (dark) listOf(Color(0xFF5019B5), Color(0xFF233ADE), Color(0xFF0CA8E6))
            else listOf(Color(0xFF9CCBFF), Color(0xFF4388F8), Color(0xFFDFEBFF)),
            Offset(0f, h * .76f), Offset(w, h),
        ))
    }
}

@Composable
internal fun OmniPremiumBroomMark(modifier: Modifier = Modifier, framed: Boolean = true) {
    Box(
        modifier = modifier.size(54.dp).then(
            if (framed) Modifier.background(
                Brush.linearGradient(listOf(Color(0xFF02D8FB), Color(0xFF1B62FF), Color(0xFF771DFF))),
                RoundedCornerShape(16.dp),
            ) else Modifier,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val w = size.width; val h = size.height
            drawLine(Color(0xFFF3FEFF), Offset(w * .61f, h * .20f), Offset(w * .48f, h * .56f),
                w * .12f, cap = StrokeCap.Round)
            val p = Path().apply {
                moveTo(w * .29f, h * .51f)
                quadraticTo(w * .47f, h * .59f, w * .67f, h * .50f)
                lineTo(w * .73f, h * .69f)
                quadraticTo(w * .67f, h * .78f, w * .61f, h * .78f)
                lineTo(w * .55f, h * .69f); lineTo(w * .49f, h * .81f)
                lineTo(w * .43f, h * .70f); lineTo(w * .35f, h * .78f)
                quadraticTo(w * .28f, h * .72f, w * .27f, h * .65f); close()
            }
            drawPath(p, Brush.linearGradient(listOf(Color.White, Color(0xFFB0F7FF), Color(0xFF37AFFF))))
            drawLine(Color.White, Offset(w * .22f, h * .17f), Offset(w * .22f, h * .32f), w * .022f)
            drawLine(Color.White, Offset(w * .15f, h * .245f), Offset(w * .29f, h * .245f), w * .022f)
            drawCircle(Color(0xFFB9FCFF), w * .027f, Offset(w * .78f, h * .34f))
        }
    }
}

private fun premiumBytes(bytes: Long): String {
    if (bytes < 1024L) return bytes.toString() + " B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var amount = bytes.toDouble()
    var index = -1
    while (amount >= 1024.0 && index < units.lastIndex) { amount /= 1024.0; index++ }
    return String.format(Locale.getDefault(), "%.1f %s", amount, units[index])
}

@Composable
private fun PremiumStorage(state: CleanerUiState, dark: Boolean) {
    val info = state.storageInfo
    val used = info?.usedPercent?.coerceIn(0f, 1f)
    val ink = if (dark) Color.White else Color(0xFF17213A)
    val secondary = if (dark) Color(0xFFBECAE8) else Color(0xFF51617E)
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, if (dark) Color(0xFF596DAD) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    if (dark) listOf(Color(0xFF162548), Color(0xFF23335D))
                    else listOf(Color.White, Color(0xFFEAF1FF))))
                .padding(horizontal = 14.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(108.dp).semantics {
                    contentDescription = used?.let {
                        (it * 100).roundToInt().toString() + "% storage used"
                    } ?: "Storage value unavailable"
                },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
                    val stroke = 11.dp.toPx()
                    val rect = Size(size.minDimension - stroke, size.minDimension - stroke)
                    val offset = Offset(stroke / 2f, stroke / 2f)
                    drawArc(if (dark) Color(0xFF33466C) else Color(0xFFD3DFF4),
                        -90f, 360f, false, offset, rect, style = Stroke(stroke))
                    if (used != null && used > 0f) {
                        drawArc(
                            Brush.sweepGradient(listOf(
                                Color(0xFF05D8F5), Color(0xFF2474FF),
                                Color(0xFF7B2DFA), Color(0xFFEB49E8), Color(0xFF05D8F5))),
                            -90f, 360f * used, false, offset, rect,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(used?.let { (it * 100).roundToInt().toString() + "%" } ?: "—",
                        fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                    Text(stringResource(R.string.storage), fontSize = 10.sp, color = secondary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(stringResource(R.string.storage), color = secondary, fontSize = 13.sp)
                Text(
                    info?.let { premiumBytes(it.usedBytes) + " / " + premiumBytes(it.totalBytes) }
                        ?: stringResource(R.string.redesign_storage_waiting),
                    color = ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                )
                Box(Modifier.fillMaxWidth().height(8.dp)
                    .background(if (dark) Color(0xFF44547D) else Color(0xFFD0DDF4), CircleShape)) {
                    if (used != null && used > 0f) {
                        Box(Modifier.fillMaxWidth(used).height(8.dp)
                            .background(Brush.horizontalGradient(listOf(
                                Color(0xFF00D4F8), Color(0xFF3579FF), Color(0xFFDF42EA))), CircleShape))
                    }
                }
                Text(
                    info?.let { stringResource(
                        R.string.redesign_storage_detail, premiumBytes(it.freeBytes), premiumBytes(it.totalBytes),
                    ) } ?: "—",
                    color = secondary, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PremiumQuickAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(
                    Color(0xFF04CBF0), Color(0xFF256FFF), Color(0xFF6A31F9), Color(0xFFAF26E5))),
                RoundedCornerShape(99.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("✦", color = Color.White, fontSize = 25.sp)
        Text(stringResource(R.string.redesign_quick_analyze), color = Color.White,
            fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text("›", color = Color.White, fontSize = 30.sp)
    }
}

@Composable
private fun PremiumTile(
    symbol: OmniNavigationSymbol, title: String, subtitle: String,
    dark: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val accent = when (symbol) {
        OmniNavigationSymbol.CLEAN -> Color(0xFF02CBF6)
        OmniNavigationSymbol.GALLERY -> Color(0xFF9459FF)
        OmniNavigationSymbol.PRIVACY -> Color(0xFF15CAA2)
        OmniNavigationSymbol.PRO -> Color(0xFFFFAE2C)
        else -> Color(0xFF357BFF)
    }
    val lightTint = when (symbol) {
        OmniNavigationSymbol.CLEAN -> Color(0xFFD8EAFF)
        OmniNavigationSymbol.GALLERY -> Color(0xFFF1E4FF)
        OmniNavigationSymbol.PRIVACY -> Color(0xFFDAF6F5)
        else -> Color(0xFFFFEFE8)
    }
    val card = if (dark) listOf(accent.copy(alpha = .21f), Color(0xFF17213E))
        else listOf(lightTint, Color(0xFFFDFDFF))
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, accent.copy(alpha = if (dark) .53f else .25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().height(165.dp)
                .background(Brush.linearGradient(card)).padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (symbol == OmniNavigationSymbol.CLEAN) {
                OmniPremiumBroomMark(framed = false)
            } else {
                Box(Modifier.size(54.dp).background(accent.copy(alpha = .16f),
                    RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    OmniNavigationIcon(symbol, accent, Modifier.size(30.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = if (dark) Color.White else Color(0xFF101933),
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = if (dark) Color(0xFFD8E0F5) else Color(0xFF34425F),
                    fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun OmniPremiumHome(
    state: CleanerUiState,
    premium: Boolean,
    modifier: Modifier,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onEnableDeepClean: () -> Unit,
    onOpenClean: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPro: () -> Unit,
    onAnalyzeStorage: () -> Unit,
    onFindDuplicates: () -> Unit,
) {
    val ink = if (dark) Color.White else Color(0xFF101831)
    val muted = if (dark) Color(0xFFCED8F0) else Color(0xFF465574)
    val small = LocalConfiguration.current.screenWidthDp < 360 ||
        LocalDensity.current.fontScale > 1.25f
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(21.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OmniPremiumBroomMark()
                Column(Modifier.weight(1f)) {
                    Text("Android", color = muted, fontSize = 13.sp)
                    Text("Omni Cleaner", color = ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    Modifier.size(48.dp).background(
                        if (dark) Color(0xFF48288B) else Color(0xFFE0E9FF),
                        RoundedCornerShape(16.dp))
                        .clickable(onClick = onToggleTheme)
                        .semantics { contentDescription = "Toggle light or dark theme" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (dark) "☀" else "☾",
                        fontSize = 25.sp,
                        color = if (dark) Color.White else Color(0xFF325ED7))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.redesign_home_eyebrow),
                    color = ink, fontSize = 27.sp, lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.redesign_home_subtitle),
                    color = muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { PremiumStorage(state, dark) }
        item { PremiumQuickAction(onAnalyzeStorage) }
        item {
            if (small) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumTile(OmniNavigationSymbol.CLEAN, stringResource(R.string.nav_clean),
                        stringResource(R.string.redesign_quick_analyze_subtitle), dark, onOpenClean)
                    PremiumTile(OmniNavigationSymbol.GALLERY, stringResource(R.string.nav_gallery),
                        stringResource(R.string.redesign_quick_gallery_subtitle), dark, onOpenGallery)
                    PremiumTile(OmniNavigationSymbol.PRIVACY, stringResource(R.string.nav_privacy),
                        stringResource(R.string.redesign_quick_privacy_subtitle), dark, onOpenPrivacy)
                    PremiumTile(OmniNavigationSymbol.PRO, stringResource(R.string.nav_pro),
                        stringResource(R.string.redesign_pro_card_body), dark, onOpenPro)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PremiumTile(OmniNavigationSymbol.CLEAN, stringResource(R.string.nav_clean),
                            stringResource(R.string.redesign_quick_analyze_subtitle),
                            dark, onOpenClean, Modifier.weight(1f))
                        PremiumTile(OmniNavigationSymbol.GALLERY, stringResource(R.string.nav_gallery),
                            stringResource(R.string.redesign_quick_gallery_subtitle),
                            dark, onOpenGallery, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PremiumTile(OmniNavigationSymbol.PRIVACY, stringResource(R.string.nav_privacy),
                            stringResource(R.string.redesign_quick_privacy_subtitle),
                            dark, onOpenPrivacy, Modifier.weight(1f))
                        PremiumTile(OmniNavigationSymbol.PRO, stringResource(R.string.nav_pro),
                            stringResource(R.string.redesign_pro_card_body),
                            dark, onOpenPro, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor =
                    if (dark) Color(0xFF1D294C) else Color(0xFFFFFFFF)),
                border = BorderStroke(1.dp,
                    if (dark) Color(0xFF4B6092) else Color(0xFFE3EAF7)),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.redesign_access_title), color = ink,
                        style = MaterialTheme.typography.titleMedium)
                    OmniStatusPill(
                        text = when {
                            state.hasBroadAccess -> stringResource(R.string.access_ready)
                            state.storageCapability.standardCleaningAvailable ->
                                stringResource(R.string.redesign_standard_ready)
                            else -> stringResource(R.string.standard_cleaning_unavailable)
                        },
                        accent = when {
                            state.hasBroadAccess -> OmniMint
                            state.storageCapability.standardCleaningAvailable -> OmniBlue
                            else -> MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        when (state.storageCapability.status) {
                            DeepStorageAccessStatus.READY -> stringResource(R.string.redesign_deep_ready)
                            DeepStorageAccessStatus.NEEDS_ALL_FILES_ACCESS,
                            DeepStorageAccessStatus.NEEDS_LEGACY_PERMISSION ->
                                stringResource(R.string.redesign_deep_optional)
                            DeepStorageAccessStatus.SCOPED_ONLY_ANDROID_10 ->
                                stringResource(R.string.deep_access_android10)
                            DeepStorageAccessStatus.STORAGE_UNAVAILABLE ->
                                stringResource(R.string.deep_access_unavailable)
                        },
                        color = muted, style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.storageCapability.canRequestDeepAccess) {
                            Button(onClick = onEnableDeepClean) {
                                Text(stringResource(R.string.grant_deep_access))
                            }
                        }
                        OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
                    }
                    OutlinedButton(onClick = onFindDuplicates, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.redesign_quick_duplicates))
                    }
                }
            }
        }
        item {
            Card(
                onClick = onOpenPro,
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor =
                    if (dark) Color(0xFF35255F) else Color(0xFFF0E9FF)),
            ) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(if (premium) stringResource(R.string.pro_active)
                        else stringResource(R.string.redesign_pro_card_title),
                        color = ink, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.redesign_pro_card_body),
                        color = muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
