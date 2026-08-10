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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cronos.ui.theme.CronosTheme
import com.example.cronos.ui.theme.LocalTimePalette
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

/**
 * Estil únic per a tots els textos de la pantalla principal.
 * Mateixa mida, mateix pes, mateix `lineHeight` — l'únic que
 * varia entre textos és el color (alpha) i el contingut.
 */
private val UnifiedTextStyle = TextStyle(
    fontSize = 22.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.SemiBold,
)

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

    // Patró que reconnecta quan canvia la preferència showSeconds:
    // llegim el valor actual cada segon dins del loop, de manera
    // que un canvi a Configuració es reflecteix a l'instant.
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = CatalanTimeFormatter.getCurrentTimeInCatalan()
            val now = Date()
            val pattern = if (settings.showSeconds) "HH:mm:ss" else "HH:mm"
            digitalTime = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ca", "ES")).format(now)
            delay(1000)
        }
    }

    // Gradient de fons que canvia segons l'hora del dia, amb
    // transició suau quan es creua una franja horària.
    val timePalette = LocalTimePalette.current
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
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Capçalera "HORA CATALANA" — text a la part superior
                Text(
                    text = stringResource(R.string.hour_label).uppercase(),
                    style = UnifiedTextStyle,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )

                // Salutació segons l'hora del dia
                Text(
                    text = getGreeting(),
                    style = UnifiedTextStyle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                // Data — text directe sobre el gradient, sense bombolla
                Text(
                    text = currentDate.replaceFirstChar { it.uppercase() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    style = UnifiedTextStyle,
                    color = Color.White.copy(alpha = 0.92f),
                )

                // Hora catalana — text directe sobre el gradient, sense bombolla
                Text(
                    text = currentTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    style = UnifiedTextStyle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                // Hora digital — text directe sobre el gradient, sense bombolla
                Text(
                    text = digitalTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    style = UnifiedTextStyle,
                    color = Color.White.copy(alpha = 0.92f),
                )

                Spacer(modifier = Modifier.weight(1.2f))

                // Peu de pàgina — text directe, sense bombolla
                Text(
                    text = stringResource(R.string.about_text),
                    style = UnifiedTextStyle,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

/**
 * Salutació segons el moment del dia — manté el format original
 * (inclòs "Bon dia, Catalunya!").
 */
fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return "Benvolgut, benvolguda..."
}
