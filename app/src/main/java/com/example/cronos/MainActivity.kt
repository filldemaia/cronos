package com.example.cronos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cronos.ui.theme.CronosTheme
import com.example.cronos.ui.theme.LocalTimePalette
import com.example.cronos.ui.theme.paletteForHour
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsRepository(this)
        setContent {
            CronosTheme {
                CronosApp(settings)
            }
        }
    }
}

/** Destinacions de l'app. Sense navegar a llocs externs. */
private enum class Screen { Clock, Settings }

/** Punt d'entrada de la UI. Decideix quina pantalla mostrar. */
@Composable
private fun CronosApp(settings: SettingsRepository) {
    var screen by remember { mutableStateOf(Screen.Clock) }
    when (screen) {
        Screen.Clock -> HoracatApp(
            settings = settings,
            onOpenSettings = { screen = Screen.Settings },
        )
        Screen.Settings -> SettingsScreen(
            settings = settings,
            onBack = { screen = Screen.Clock },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoracatApp(
    settings: SettingsRepository,
    onOpenSettings: () -> Unit,
) {
    var currentTime by remember { mutableStateOf(CatalanTimeFormatter.getCurrentTimeInCatalan()) }
    var digitalTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // Paleta del moment del dia com a estat: quan es creua una franja
    // horària es canvia sola i el gradient ho anima amb transició suau
    // (abans es calculava una sola vegada i no seguia l'hora en temps real).
    val initialPalette = LocalTimePalette.current
    var timePalette by remember { mutableStateOf(initialPalette) }

    // Loop de segon a segon. Només es refà la feina que ha canviat:
    // hora catalana quan canvia el minut, data quan canvia el dia,
    // paleta quan canvia l'hora, i formatter digital quan canvia el
    // patró (showSeconds). Així el cost per segon és mínim.
    LaunchedEffect(Unit) {
        val dateFormatter = SimpleDateFormat("EEEE, d MMMM 'del 'yyyy", Locale("ca", "ES"))
        var lastPattern: String? = null
        var digitalFormatter: SimpleDateFormat? = null
        var lastMinute = -1L
        var lastDayKey = -1
        var lastHour = -1
        while (true) {
            val now = Date()
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)
            val minuteKey = now.time / 60_000
            val dayKey = calendar.get(Calendar.DAY_OF_YEAR) +
                calendar.get(Calendar.YEAR) * 1000

            if (hour != lastHour) {
                timePalette = paletteForHour(hour)
                lastHour = hour
            }

            // Els segons per escrit a l'hora tradicional i els de l'hora
            // digital són opcions independents. Amb els escrits, la frase
            // tradicional canvia cada segon; sense, només al minut.
            val writtenSeconds = settings.showSecondsWritten
            if (writtenSeconds || minuteKey != lastMinute) {
                currentTime = CatalanTimeFormatter.formatTime(
                    hour, minute, if (writtenSeconds) seconds else 0
                )
                lastMinute = minuteKey
            }
            if (dayKey != lastDayKey) {
                currentDate = dateFormatter.format(now)
                lastDayKey = dayKey
            }

            // L'hora digital només es formateja quan és visible.
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

    // Gradient de fons que canvia segons l'hora del dia, amb
    // transició suau quan es creua una franja horària.
    val animatedTop by animateColorAsState(
        targetValue = timePalette.gradientTop,
        animationSpec = tween(durationMillis = 1500),
        label = "gradientTop",
    )
    val animatedMiddle by animateColorAsState(
        targetValue = timePalette.gradientMiddle,
        animationSpec = tween(durationMillis = 1500),
        label = "gradientMiddle",
    )
    val animatedBottom by animateColorAsState(
        targetValue = timePalette.gradientBottom,
        animationSpec = tween(durationMillis = 1500),
        label = "gradientBottom",
    )
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(animatedTop, animatedMiddle, animatedBottom)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundGradient)
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Bloc superior: salutació i data, alineades a l'esquerra i ben amunt
                // de la pantalla (primer terç). El text ocupa tota l'amplada
                // (fillMaxWidth) perquè l'alineació tingui efecte.
                Spacer(modifier = Modifier.weight(0.15f))
                Text(
                    text = getGreeting(),
                    style = GreetingStyle,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.weight(0.1f))
                Text(
                    text = buildAnnotatedString {
                        withStyle(GreetingStyle.toSpanStyle()) {
                            // L'espai es posa explícitament aquí: Android retalla
                            // els espais finals de les strings en compilar.
                            append(stringResource(R.string.before_date).trim())
                            append(" ")
                        }

                        withStyle(DateStyle.toSpanStyle()) {
                            append(currentDate)
                        }
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )


                // Hero: l'hora catalana, mida regulable des de Configuració
                Spacer(modifier = Modifier.weight(1f))
                val hourFontSize = settings.hourSize.sp
                Text(
                    text = currentTime,
                    style = TextStyle(
                        fontSize = hourFontSize,
                        lineHeight = hourFontSize * 1.18f,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )

                // Hora digital, complementària (opcional a Configuració)
                if (settings.showDigital) {
                    Spacer(modifier = Modifier.weight(0.7f))
                    Text(
                        text = digitalTime,
                        style = DigitalStyle,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center,
                    )
                }

                // Peu de pàgina
                Spacer(modifier = Modifier.weight(1.4f))
                Text(
                    text = stringResource(R.string.about_text),
                    style = FooterStyle,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

/** Data: petita i clara, per sobre de tot. */
private val DateStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.Medium,
)

/** Salutació: suau, just a sota de la data. */
private val GreetingStyle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Medium,
)

/** Hora digital: prima, per no robar protagonisme a l'hora catalana. */
private val DigitalStyle = TextStyle(
    fontSize = 26.sp,
    fontWeight = FontWeight.Thin,
    letterSpacing = 1.sp,
)

/** Peu de pàgina: discretíssim. */
private val FooterStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
)

/**
 * Salutació segons el moment del dia.
 */
fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Bon dia, Catalunya!"
        in 12..14 -> "Bon migdia!"
        in 15..18 -> "Bona tarda!"
        in 19..20 -> "Bon vespre!"
        else -> "Bona nit!"
    }
}