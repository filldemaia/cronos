package com.filldemaia.cronos.radio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filldemaia.cronos.R
import com.filldemaia.cronos.radio.data.model.AlarmOutcome
import com.filldemaia.cronos.radio.data.model.ReliabilityLevel
import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.ui.theme.EgeuMarble
import com.filldemaia.cronos.radio.ui.theme.RadioBlue
import com.filldemaia.cronos.radio.ui.theme.RadioGreen
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioNight
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioRed
import com.filldemaia.cronos.radio.ui.theme.RadioSurfaceHigh
import com.filldemaia.cronos.radio.ui.theme.RadioSurfaceHighest
import com.filldemaia.cronos.radio.ui.theme.RadioText
import com.filldemaia.cronos.radio.ui.theme.RadioYellow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun rememberNow(intervalMs: Long = 30_000L): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            now = Instant.now()
            delay(intervalMs)
        }
    }
    return now
}

@Composable
fun CronosMark(size: Dp = 46.dp) {
    Surface(
        shape = CircleShape,
        color = RadioSurfaceHighest,
        border = BorderStroke(1.dp, RadioYellow.copy(alpha = 0.55f)),
        modifier = Modifier.size(size)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Mateixa geometria que la icona adaptativa (lienzo de 108 unitats),
            // centrada perquè la ràdio respiri dins el distintiu.
            val canvasSize = this.size
            val unit = canvasSize.minDimension / 108f
            fun x(value: Float) = canvasSize.width / 2f + (value - 54f) * unit + 1f * unit
            fun y(value: Float) = canvasSize.height / 2f + (value - 54f) * unit - 3f * unit
            fun point(px: Float, py: Float) = Offset(x(px), y(py))

            // Antena, de l'esquerra cap a la dreta
            drawLine(
                color = RadioYellow,
                start = point(31f, 54f),
                end = point(79f, 34f),
                strokeWidth = 2.6f * unit,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = RadioYellow,
                radius = 2.2f * unit,
                center = point(79f, 34f)
            )
            // Cos de la ràdio
            drawRoundRect(
                color = RadioText,
                topLeft = Offset(x(25f), y(54f)),
                size = Size(58f * unit, 30f * unit),
                cornerRadius = CornerRadius(7f * unit)
            )
            // Reixeta de l'altaveu
            listOf(31.5f, 37.5f, 43.5f, 49.5f).forEach { slotX ->
                drawLine(
                    color = RadioNight,
                    start = point(slotX, 62f),
                    end = point(slotX, 76f),
                    strokeWidth = 3f * unit,
                    cap = StrokeCap.Round
                )
            }
            // Escala de sintonia
            val displayTopLeft = Offset(x(58f), y(59f))
            val displaySize = Size(21f * unit, 10f * unit)
            drawRoundRect(
                color = RadioNight,
                topLeft = displayTopLeft,
                size = displaySize,
                cornerRadius = CornerRadius(2.5f * unit)
            )
            drawRoundRect(
                color = RadioYellow,
                topLeft = displayTopLeft,
                size = displaySize,
                cornerRadius = CornerRadius(2.5f * unit),
                style = Stroke(width = 1.2f * unit)
            )
            listOf(61f, 65f, 69f, 76f).forEach { tickX ->
                drawLine(
                    color = RadioYellow,
                    start = point(tickX, 65f),
                    end = point(tickX, 67f),
                    strokeWidth = 0.9f * unit,
                    cap = StrokeCap.Round
                )
            }
            drawLine(
                color = RadioText,
                start = point(73f, 60.5f),
                end = point(73f, 67f),
                strokeWidth = 1.4f * unit,
                cap = StrokeCap.Round
            )
            // Botons
            listOf(63f, 72f).forEach { knobX ->
                drawCircle(
                    color = RadioYellow,
                    radius = 2.5f * unit,
                    center = point(knobX, 77f)
                )
                drawCircle(
                    color = RadioNight,
                    radius = 1.1f * unit,
                    center = point(knobX, 77f)
                )
            }
        }
    }
}

@Composable
fun RadioCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        content = { Column(Modifier.padding(16.dp), content = content) }
    )
}

/**
 * Títol de pàgina unificat: un petit kicker daurat en majúscules, el títol
 * elegant i un subtítol opcional. Dona coherència visual a les quatre
 * pestanyes (Hora, Ràdio, Alarmes i Ajustaments).
 */
@Composable
fun ScreenTitle(
    kicker: String,
    title: String,
    subtitle: String? = null
) {
    Column {
        // Conservem l'alçada de l'antiga etiqueta perquè els títols no saltin.
        Spacer(Modifier.height(with(androidx.compose.ui.platform.LocalDensity.current) {
            MaterialTheme.typography.labelSmall.lineHeight.toDp()
        } + 2.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = (-0.5).sp),
            color = RadioText,
            fontWeight = FontWeight.Medium
        )
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = RadioMuted
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
            }
        }
        trailing?.invoke()
    }
}

private val StationLogoDrawables: Map<String, Int> = mapOf(
    "logo_catalunya_radio" to R.drawable.logo_catalunya_radio,
    "logo_catalunya_informacio" to R.drawable.logo_catalunya_informacio,
    "logo_catalunya_musica" to R.drawable.logo_catalunya_musica,
    "logo_icat" to R.drawable.logo_icat,
    "logo_rac1" to R.drawable.logo_rac1,
    "logo_rac105" to R.drawable.logo_rac105,
    "logo_flaixbac" to R.drawable.logo_flaixbac,
    "logo_flaixfm" to R.drawable.logo_flaixfm,
    "logo_radio_estel" to R.drawable.logo_radio_estel,
    "logo_apunt_radio" to R.drawable.logo_apunt_radio,
    "logo_radio_klara" to R.drawable.logo_radio_klara,
    "logo_ib3_radio" to R.drawable.logo_ib3_radio,
    "logo_ona_mediterrania" to R.drawable.logo_ona_mediterrania,
    "logo_radio_fraga" to R.drawable.logo_radio_fraga,
    "logo_radio_arrels" to R.drawable.logo_radio_arrels
)

/** Logotip quadrat, per a files compactes i el mini-reproductor. */
@Composable
fun StationLogo(station: Station, size: Dp = 56.dp) {
    val drawable = StationLogoDrawables[station.logoRes]
    if (drawable == null) {
        InitialsLogo(
            station = station,
            modifier = Modifier.size(size),
            fontSize = (size.value * 0.27f).sp
        )
    } else {
        LogoPlate(
            drawable = drawable,
            name = station.name,
            modifier = Modifier.size(size),
            contentPadding = size * 0.10f,
            cornerRadius = size * 0.24f
        )
    }
}

/**
 * Logotip ample: ocupa l'amplada que li dona el contenidor (per a la llista i
 * el mosaic d'emissores), amb una placa clara perquè els logotips blancs
 * segueixin llegint-se sobre el blau nit.
 */
@Composable
fun StationLogoBanner(station: Station, height: Dp, modifier: Modifier = Modifier) {
    val drawable = StationLogoDrawables[station.logoRes]
    if (drawable == null) {
        InitialsLogo(
            station = station,
            modifier = modifier.height(height),
            fontSize = (height.value * 0.26f).sp,
            cornerRadius = height * 0.26f
        )
    } else {
        LogoPlate(
            drawable = drawable,
            name = station.name,
            modifier = modifier.height(height),
            contentPadding = height * 0.13f,
            cornerRadius = height * 0.26f
        )
    }
}

@Composable
private fun LogoPlate(
    drawable: Int,
    name: String,
    modifier: Modifier,
    contentPadding: Dp,
    cornerRadius: Dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(EgeuMarble),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        )
    }
}

@Composable
private fun InitialsLogo(
    station: Station,
    modifier: Modifier,
    fontSize: TextUnit,
    cornerRadius: Dp? = null
) {
    Box(
        modifier = modifier
            .clip(cornerRadius?.let { RoundedCornerShape(it) } ?: CircleShape)
            .background(Color(station.colorArgb())),
        contentAlignment = Alignment.Center
    ) {
        Text(
            station.displayLogo(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

/**
 * Barra de progrés del reproductor: fina, plana i a l'estil de Spotify. La
 * pista ocupa tot l'ample, el farciment és blau Egeu i el polze de marbre duu
 * un anell que es torna daurat quan ets al directe. Quan vas endarrerit, un
 * punt daurat marca el final de la finestra (el directe).
 */
private val SeekThumbRadius = 6.dp
private val SeekTrackHeight = 4.dp

@Composable
fun CronosSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val thumbScale by animateFloatAsState(
        targetValue = if (dragging) 1.25f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "seekThumb"
    )
    val clamped = value.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics {
                contentDescription = "Posició de reproducció"
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
                setProgress { target ->
                    currentOnValueChange(target.coerceIn(0f, 1f))
                    currentOnFinished()
                    true
                }
            }
            .pointerInput(Unit) {
                // El centre del polze no pot arribar a les vores: es descompta
                // el seu radi perquè el dit i el polze coincideixin exactament.
                val inset = SeekThumbRadius.toPx()
                fun seekAt(x: Float) {
                    val usable = (size.width - inset * 2f).coerceAtLeast(1f)
                    currentOnValueChange(((x - inset) / usable).coerceIn(0f, 1f))
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dragging = true
                    seekAt(down.position.x)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        seekAt(change.position.x)
                        change.consume()
                    }
                    dragging = false
                    currentOnFinished()
                }
            }
    ) {
        val trackHeight = SeekTrackHeight.toPx()
        val inset = SeekThumbRadius.toPx()
        val centerY = size.height / 2f
        val endX = size.width - inset
        val usable = (size.width - inset * 2f).coerceAtLeast(1f)
        val thumbX = inset + usable * clamped
        val accent = if (isLive) RadioYellow else RadioBlue
        val corner = CornerRadius(trackHeight / 2f)

        drawRoundRect(
            color = RadioText.copy(alpha = 0.14f),
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = corner
        )

        if (!isLive) {
            drawCircle(
                color = RadioYellow.copy(alpha = 0.18f),
                radius = 6.dp.toPx(),
                center = Offset(endX, centerY)
            )
            drawCircle(
                color = RadioYellow,
                radius = 2.5.dp.toPx(),
                center = Offset(endX, centerY)
            )
        }

        if (thumbX > 0f) {
            drawRoundRect(
                color = RadioBlue,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(thumbX.coerceAtLeast(trackHeight), trackHeight),
                cornerRadius = corner
            )
        }

        val thumbRadius = SeekThumbRadius.toPx() * thumbScale
        drawCircle(
            color = accent.copy(alpha = 0.16f),
            radius = thumbRadius + 5.dp.toPx(),
            center = Offset(thumbX, centerY)
        )
        drawCircle(
            color = accent,
            radius = thumbRadius + 2.dp.toPx(),
            center = Offset(thumbX, centerY)
        )
        drawCircle(
            color = RadioText,
            radius = thumbRadius,
            center = Offset(thumbX, centerY)
        )
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

@Composable
fun ReliabilityPill(level: ReliabilityLevel) {
    when (level) {
        ReliabilityLevel.GREEN -> StatusPill("FIABLE", RadioGreen)
        ReliabilityLevel.YELLOW -> StatusPill("A REVISAR", RadioYellow)
        ReliabilityLevel.RED -> StatusPill("NO GARANTIDA", RadioRed)
    }
}

@Composable
fun OutcomeBadge(outcome: AlarmOutcome) {
    val (label, color) = when (outcome) {
        AlarmOutcome.SUCCESS -> "CORRECTE" to RadioGreen
        AlarmOutcome.FALLBACK_STATION -> "RESERVA" to RadioYellow
        AlarmOutcome.LOCAL_ALARM -> "TO LOCAL" to RadioOrange
        AlarmOutcome.ERROR -> "ERROR" to RadioRed
    }
    StatusPill(label, color)
}

fun reliabilityColor(level: ReliabilityLevel): Color = when (level) {
    ReliabilityLevel.GREEN -> RadioGreen
    ReliabilityLevel.YELLOW -> RadioYellow
    ReliabilityLevel.RED -> RadioRed
}

fun formatDurationUs(durationUs: Long): String {
    val totalSeconds = (durationUs.coerceAtLeast(0L) / 1_000_000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatClock(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm:ss"))

fun stopAfterLabel(minutes: Int?): String =
    when (minutes) {
        null -> "Sense aturada automàtica"
        60 -> "1 h"
        else -> "$minutes min"
    }

@Composable
fun OemGuidanceDialog(
    manufacturer: String,
    acknowledged: Boolean,
    onDismiss: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onOpenGuide: () -> Unit,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inici automàtic a $manufacturer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Perquè una alarma pugui engegar l'app amb la pantalla bloquejada, " +
                        "alguns fabricants requereixen passos addicionals:",
                    style = MaterialTheme.typography.bodySmall
                )
                OemBullet("Xiaomi / Redmi", "Activa l'inici automàtic i posa l'app a \"Sense restriccions\".")
                OemBullet("Samsung", "Treu Cronos de \"Aplicacions en repòs profund\".")
                OemBullet("Huawei / Honor", "Permet l'inici automàtic i l'activitat en segon pla.")
                OemBullet("OnePlus / Oppo / Realme", "Desactiva l'optimització agressiva i permet l'inici automàtic.")
                OemBullet("Vivo / iQOO", "Afegeix l'app a la llista blanca d'inici automàtic.")
                Text(
                    "La guia actualitzada per al teu model és a dontkillmyapp.com.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenAutoStart) { Text("Inici automàtic") }
                    TextButton(onClick = onOpenGuide) { Text("Guia OEM") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!acknowledged) onAcknowledge()
                    onDismiss()
                }
            ) {
                Text(if (acknowledged) "Tanca" else "Ho he revisat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Més tard") }
        }
    )
}

@Composable
private fun OemBullet(manufacturer: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .background(RadioOrange, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(manufacturer, style = MaterialTheme.typography.labelLarge)
            Text(text, style = MaterialTheme.typography.bodySmall, color = RadioMuted)
        }
    }
}

@Composable
fun MiniLabel(text: String, tabular: Boolean = false) {
    Text(
        text,
        style = if (tabular) {
            MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
        } else {
            MaterialTheme.typography.labelSmall
        },
        color = RadioMuted,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun SurfaceRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = RadioMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

val CardTint = RadioSurfaceHigh
