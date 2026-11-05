package com.example.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var symbolIndex by remember { mutableIntStateOf(0) }
    val symbols = listOf("</>", "{ }", "fn()", "async/await", "lambda x:", "class DevHub")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    LaunchedEffect(Unit) {
        for (i in 0 until 5) {
            delay(350)
            symbolIndex = (symbolIndex + 1) % symbols.size
        }
        delay(400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF0F172A),
                        Color(0xFF070B14)
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.devhub_logo_1793878835008),
                    contentDescription = "DevHub IDE Logo",
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .testTag("splash_logo")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DevHub",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "Mobile Developer IDE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = CyberCyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated coding symbol ticker
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = symbols[symbolIndex],
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .testTag("splash_progress"),
                color = CyberCyan,
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Initializing runtime...",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }

        // Bottom version footer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Version 1.0.0 • ARM64 Android",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }
    }
}
