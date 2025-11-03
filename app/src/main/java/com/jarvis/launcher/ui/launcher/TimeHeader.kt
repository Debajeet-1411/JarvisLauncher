package com.jarvis.launcher.ui.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Time Header - displays current time and date at top of launcher
 *
 * @param time Current time string (HH:mm format)
 * @param date Current date string (Day, Month Date format)
 */
@Composable
fun TimeHeader(time: String, date: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = time,
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            letterSpacing = (-2).sp
        )
        Text(
            text = date,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 0.5.sp
        )
    }
}
