package com.example.cronos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HoracatApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoracatApp() {
    var currentTime by remember { mutableStateOf(CatalanTimeFormatter.getCurrentTimeInCatalan()) }
    var digitalTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // Actualitzar l'hora cada segon
    LaunchedEffect(true) {
        while (true) {
            currentTime = CatalanTimeFormatter.getCurrentTimeInCatalan()
            val now = Date()
            digitalTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ca", "ES")).format(now)
            delay(1000)
        }
    }

    // Gradient de fons que canvia segons l'hora del dia
    val backgroundGradient = getTimeBasedGradient()
    // Colors per a les cards que també canvien
    val cardColors = getTimeBasedCardColors()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundGradient)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Data actual
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardColors.dateCard
                    )
                ) {
                    Text(
                        text = currentDate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = cardColors.textColor
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Hora en català
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardColors.mainCard
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentTime,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = cardColors.textColor,
                            lineHeight = 32.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Hora digital
                Card(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardColors.digitalCard
                    )
                ) {
                    Text(
                        text = digitalTime,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardColors.textColor
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

data class TimeBasedColors(
    val dateCard: Color,
    val mainCard: Color,
    val digitalCard: Color,
    val textColor: Color
)

@Composable
fun getTimeBasedCardColors(): TimeBasedColors {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    return when (hour) {
        in 5..7 -> { // Alba - tonalitats clares i càlides
            TimeBasedColors(
                dateCard = Color(0xFFFFF3E0),     // Taronja molt clar
                mainCard = Color(0xFFFFE0B2),     // Taronja clar
                digitalCard = Color(0xFFFFCC80),  // Taronja suau
                textColor = Color(0xFF5D4037)     // Marró fosc per contrast
            )
        }
        in 8..11 -> { // Matí - tonalitats fresques i blaves
            TimeBasedColors(
                dateCard = Color(0xFFE3F2FD),     // Blau molt clar
                mainCard = Color(0xFFBBDEFB),     // Blau clar
                digitalCard = Color(0xFF90CAF9),  // Blau suau
                textColor = Color(0xFF01579B)     // Blau fosc per contrast
            )
        }
        in 12..16 -> { // Migdia/Tarda - tonalitats brillants
            TimeBasedColors(
                dateCard = Color(0xFFFFF9C4),     // Groc clar
                mainCard = Color(0xFFFFF59D),     // Groc
                digitalCard = Color(0xFFFFF176),  // Groc més fort
                textColor = Color(0xFF4E342E)     // Marró fosc per contrast
            )
        }
        in 17..19 -> { // Vespre - tonalitats càlides (taronja/rosa)
            TimeBasedColors(
                dateCard = Color(0xFFFFEBEE),     // Rosa molt clar
                mainCard = Color(0xFFFFCDD2),     // Rosa clar
                digitalCard = Color(0xFFEF9A9A),  // Rosa suau
                textColor = Color(0xFFB71C1C)     // Vermell fosc per contrast
            )
        }
        else -> { // Nit - tonalitats fosques però amb text blanc
            TimeBasedColors(
                dateCard = Color(0xFF3F51B5),     // Blau nit
                mainCard = Color(0xFF303F9F),     // Blau nit fosc
                digitalCard = Color(0xFF1A237E),  // Blau molt fosc
                textColor = Color.White           // Blanc per contrast
            )
        }
    }
}

@Composable
fun getTimeBasedGradient(): Brush {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    return when (hour) {
        in 5..7 -> { // Alba
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFE5B4),
                    Color(0xFFFFB6C1),
                    Color(0xFF87CEEB)
                )
            )
        }
        in 8..11 -> { // Matí
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF87CEEB),
                    Color(0xFF87CEEB),
                    Color(0xFF98D8E8)
                )
            )
        }
        in 12..16 -> { // Migdia/Tarda
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6BA6CD),
                    Color(0xFF87CEEB),
                    Color(0xFF98D8E8)
                )
            )
        }
        in 17..19 -> { // Vespre
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFA500),
                    Color(0xFFFF6347),
                    Color(0xFFFF69B4)
                )
            )
        }
        else -> { // Nit
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF191970),
                    Color(0xFF2F4F8F),
                    Color(0xFF483D8B)
                )
            )
        }
    }
}
