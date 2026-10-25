package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadProgress
import com.example.network.NetworkProbe
import com.example.ui.theme.*

@Composable
fun ProgressBarCard(
    progress: DownloadProgress,
    onCancel: () -> Unit,
    language: String,
    modifier: Modifier = Modifier
) {
    if (!progress.isDownloading && progress.error == null && progress.percentage != 100f) {
        return
    }

    val isCompleted = progress.percentage == 100f && !progress.isDownloading && progress.error == null
    val isError = progress.error != null

    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            when {
                isCompleted -> BrandEmerald.copy(alpha = 0.5f)
                isError -> BrandRose.copy(alpha = 0.5f)
                else -> BrandCyan.copy(alpha = 0.4f)
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Status icon + title + speed / cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    when {
                        isCompleted -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(20.dp))
                        isError -> Icon(Icons.Default.Error, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                        else -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BrandCyan)
                    }

                    Column {
                        Text(
                            text = when {
                                isCompleted -> if (language == "bn") "ডাউনলোড সম্পন্ন হয়েছে!" else "Download Complete!"
                                isError -> if (language == "bn") "ডাউনলোড ত্রুটি" else "Download Error"
                                else -> if (language == "bn") progress.statusTextBn.ifEmpty { "ডাউনলোড হচ্ছে..." } else progress.statusTextEn.ifEmpty { "Downloading..." }
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (progress.isDownloading) {
                            Text(
                                text = if (language == "bn") "আসল বাইট স্ট্রিম প্রসেস হচ্ছে" else "Real byte stream in progress",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Speed or cancel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (progress.isDownloading && progress.speedBytesPerSec > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandEmerald.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BrandEmerald.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${NetworkProbe.formatBytes(progress.speedBytesPerSec)}/s",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = BrandEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (progress.isDownloading) {
                        OutlinedButton(
                            onClick = onCancel,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BrandRose
                            ),
                            border = BorderStroke(1.dp, BrandRose.copy(alpha = 0.4f)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == "bn") "বাতিল" else "Cancel",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Progress Bar
            val pct = progress.percentage
            if (pct != null) {
                LinearProgressIndicator(
                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (isCompleted) BrandEmerald else BrandCyan,
                    trackColor = DarkBackground,
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = BrandCyan,
                    trackColor = DarkBackground,
                )
            }

            // Stats row: Downloaded / Total and Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${NetworkProbe.formatBytes(progress.downloadedBytes)}" +
                            (progress.totalBytes?.let { " / ${NetworkProbe.formatBytes(it)}" } ?: ""),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                Text(
                    text = pct?.let { String.format("%.1f%%", it) } ?: "Streaming...",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) BrandEmerald else BrandCyan
                )
            }

            if (progress.savedFilePath != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, BrandEmerald.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (language == "bn")
                            "ফাইল সেভ হয়েছে: ${progress.savedFilePath}"
                        else
                            "File saved: ${progress.savedFilePath}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BrandEmerald,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (progress.error != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandRose.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, BrandRose.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = progress.error,
                        fontSize = 11.sp,
                        color = BrandRose,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
