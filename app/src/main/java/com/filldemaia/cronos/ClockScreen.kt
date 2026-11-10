package com.filldemaia.cronos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filldemaia.cronos.ui.theme.DayPhase
import com.filldemaia.cronos.ui.theme.MiddayPalette
import com.filldemaia.cronos.ui.theme.paletteForPhase
import com.filldemaia.cronos.ui.theme.phaseFor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * El rellotge de Cronos, amb el mateix funcionament de sempre: hora
 * tradicional catalana, salutació, data i gradient de fons que canvia amb
 * la llum solar segons la ubicació triada. Només s'ha adaptat la tipografia
 * i l'ordre dels elements per integrar-lo amb la resta de l'app.
 */
@Composable
fun ClockScreen(settings: SettingsRepository) {
    var currentTime by remember { mutableStateOf(CatalanTimeFormatter.getCurrentTimeInCatalan()) }
    var digitalTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf(CatalanDateFormatter.todayWithYear()) }

    // Paleta del moment del dia com a estat: quan es creua una franja de
    // llum solar es canvia sola i el gradient ho anima amb transició suau.

    // Loop de segon a segon. Només es refà la feina que ha canviat.
    LaunchedEffect(settings) {
        var lastPattern: String? = null
        var digitalFormatter: SimpleDateFormat? = null
        var lastMinute = -1L
        var lastDayKey = -1
        while (true) {
            val now = Date()
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)
            val minuteKey = now.time / 60_000
            val dayKey = calendar.get(Calendar.DAY_OF_YEAR) +
                calendar.get(Calendar.YEAR) * 1000

            if (dayKey != lastDayKey) {
                currentDate = CatalanDateFormatter.todayWithYear(now)
                lastDayKey = dayKey
            }

            val writtenSeconds = settings.showSecondsWritten
            if (writtenSeconds || minuteKey != lastMinute) {
                currentTime = CatalanTimeFormatter.formatTime(
                    hour, minute, if (writtenSeconds) seconds else 0
                )
                lastMinute = minuteKey
            }

            if (settings.showDigital) {
                val pattern = if (settings.showSeconds) "HH:mm:ss" else "HH:mm"
                if (pattern != lastPattern) {
                    digitalFormatter = SimpleDateFormat(pattern, Locale.getDefault())
                    lastPattern = pattern
                }
                digitalTime = digitalFormatter!!.format(now)
            }
            delay(1000)
        }
    }

    val currentTextShadow = NoShadow

    val fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
    val greeting = greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Títol de pàgina, a dalt de tot, amb la mateixa veu tipogràfica.
            Box(Modifier.fillMaxWidth()) {
                com.filldemaia.cronos.radio.ui.components.ScreenTitle(
                    kicker = "", title = greeting
                )
            }

            Spacer(modifier = Modifier.weight(0.54f))

            Text(
                text = buildAnnotatedString {
                    withStyle(DateStyle.copy(shadow = currentTextShadow, fontFamily = fontFamily).toSpanStyle()) {
                        append(stringResource(R.string.before_date).trim())
                        append(" ")
                        append(currentDate.replaceFirst(", ", ",\n"))
                    }
                },
                color = Color.White,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )

            // Hero: l'hora catalana, mida regulable des de Configuració.
            Spacer(modifier = Modifier.weight(0.54f))
            val hourFontSize = settings.hourSize.sp
            Text(
                text = currentTime,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = hourFontSize,
                    lineHeight = hourFontSize * 1.18f,
                    fontWeight = FontWeight.SemiBold,
                    shadow = currentTextShadow,
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )

            if (settings.showDigital) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = digitalTime,
                    style = DigitalStyle.copy(shadow = currentTextShadow, fontFamily = fontFamily),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.donec_perficiam),
                    contentDescription = "Donec perficiam",
                    modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1045f / 735f)
                )
            }
        }
    }
}

private val TextShadow = Shadow(
    color = Color(0xCC000000),
    offset = Offset(0f, 1f),
    blurRadius = 5f,
)

private val NoShadow = Shadow(Color.Transparent, Offset.Zero, 0f)

private val DateStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.Medium,
)

private val GreetingStyle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Medium,
)

private val DigitalStyle = TextStyle(
    fontSize = 40.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 2.sp,
)

private const val MIDDAY_SCRIM_ALPHA = 0.12f

private fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Bon dia, Catalunya!"
    in 12..14 -> "Bon migdia!"
    in 15..18 -> "Bona tarda!"
    in 19..20 -> "Bon vespre!"
    else -> "Bona nit!"
}
