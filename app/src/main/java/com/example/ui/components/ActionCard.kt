package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SiriAmber
import com.example.ui.theme.SiriBorder
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriGreen
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriSurfaceElevated
import com.example.ui.theme.SiriSurfaceVariant
import com.example.ui.theme.SiriTextPrimary
import com.example.ui.theme.SiriTextSecondary
import com.example.ui.theme.SiriViolet

@Composable
fun ActionCard(
    actionType: String,
    actionData: String?,
    onActionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("action_card_${actionType.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SiriSurfaceElevated),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(SiriBorder, SiriCyan.copy(alpha = 0.35f), SiriBorder))
        )
    ) {
        when (actionType) {
            "TORCH" -> TorchCard(isOn = actionData == "ON", onToggle = { onActionClick("TOGGLE_TORCH") })
            "BATTERY" -> BatteryCard(level = actionData?.replace("%", "")?.toIntOrNull() ?: 80)
            "VIVO_INFO" -> VivoInfoCard(onActionClick)
            "CALCULATOR" -> CalculatorCard(result = actionData ?: "")
            "APP" -> AppCard(appName = actionData ?: "App")
            "NOTE" -> NoteCard(note = actionData ?: "")
            "ALARM" -> AlarmCard(time = actionData ?: "07:00 AM")
            "TIMER" -> TimerCard(duration = actionData ?: "5 min")
            else -> DefaultActionCard(title = actionType, detail = actionData ?: "")
        }
    }
}

@Composable
private fun TorchCard(isOn: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isOn) SiriAmber.copy(alpha = 0.2f) else SiriSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Torch",
                    tint = if (isOn) SiriAmber else SiriTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ફ્લેશલાઇટ (Flashlight)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SiriTextPrimary
                )
                Text(
                    text = if (isOn) "ચાલુ છે (Active ON)" else "બંધ છે (OFF)",
                    fontSize = 13.sp,
                    color = if (isOn) SiriAmber else SiriTextSecondary
                )
            }
        }
        OutlinedButton(
            onClick = onToggle,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("torch_toggle_btn")
        ) {
            Text(if (isOn) "બંધ કરો" else "ચાલુ કરો", color = SiriCyan, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BatteryCard(level: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (level > 20) SiriGreen else SiriMagenta,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vivo T3 5000 mAh Battery",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SiriTextPrimary
                )
            }
            Text(
                text = "$level%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (level > 20) SiriGreen else SiriMagenta
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (level / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (level > 20) SiriGreen else SiriMagenta,
            trackColor = SiriSurfaceVariant
        )
    }
}

@Composable
private fun VivoInfoCard(onActionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = SiriCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Vivo T3 5G હાર્ડવેર અને સિસ્ટમ",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SiriCyan
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "⚡ MediaTek Dimensity 7200 (4nm)\n📺 6.67\" 120Hz Crystal AMOLED\n📸 50MP Sony IMX882 OIS Camera\n🔋 5000 mAh + 44W FlashCharge",
            fontSize = 13.sp,
            color = SiriTextSecondary,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun CalculatorCard(result: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SiriViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                tint = SiriViolet,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "ઓફલાઇન ગણતરી (Calculation)",
                fontSize = 12.sp,
                color = SiriTextSecondary
            )
            Text(
                text = result,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SiriCyan
            )
        }
    }
}

@Composable
private fun AppCard(appName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SiriCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = SiriCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "$appName ખોલ્યું",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SiriTextPrimary
            )
            Text(
                text = "એપ્લિકેશન સફળતાપૂર્વક શરૂ થઈ",
                fontSize = 12.sp,
                color = SiriTextSecondary
            )
        }
    }
}

@Composable
private fun NoteCard(note: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SiriGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Note,
                contentDescription = null,
                tint = SiriGreen,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "ઓફલાઇન નોંધ સાચવી લીધી",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SiriGreen
            )
            Text(
                text = note,
                fontSize = 13.sp,
                color = SiriTextPrimary
            )
        }
    }
}

@Composable
private fun AlarmCard(time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SiriAmber.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = SiriAmber,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "એલાર્મ સેટ કર્યો છે",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SiriAmber
            )
            Text(
                text = time,
                fontSize = 13.sp,
                color = SiriTextPrimary
            )
        }
    }
}

@Composable
private fun TimerCard(duration: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SiriCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = SiriCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "ટાઈમર શરૂ કર્યો છે",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SiriCyan
            )
            Text(
                text = duration,
                fontSize = 13.sp,
                color = SiriTextPrimary
            )
        }
    }
}

@Composable
private fun DefaultActionCard(title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SiriCyan,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = SiriTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (detail.isNotBlank()) {
                Text(detail, color = SiriTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
