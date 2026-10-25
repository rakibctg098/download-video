package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.HistoryEntity
import com.example.network.NetworkProbe
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    items: List<HistoryEntity>,
    onReload: (String) -> Unit,
    onPlay: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    language: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = if (language == "bn") "হিস্ট্রি মুছবেন?" else "Clear All History?",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (language == "bn")
                        "আপনি কি নিশ্চিত যে সমস্ত ডাউনলোডের হিস্ট্রি রেকর্ড মুছে ফেলতে চান?"
                    else
                        "Are you sure you want to permanently clear all download history records?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose)
                ) {
                    Text(if (language == "bn") "সব মুছুন" else "Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel", color = TextPrimary)
                }
            },
            containerColor = DarkSurface
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == "bn") "ডাউনলোড ও লিংক হিস্ট্রি" else "Download & Link History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == "bn")
                            "${items.size} টি আইটেম সংরক্ষিত"
                        else
                            "${items.size} saved records",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                if (items.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRose),
                        border = BorderStroke(1.dp, BrandRose.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "মুছুন" else "Clear",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (language == "bn") "কোনো হিস্ট্রি পাওয়া যায়নি" else "No Download History Yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = if (language == "bn")
                                "আপনি যখন কোনো লিঙ্ক যাচাই বা ডাউনলোড করবেন, তা এখানে জমা থাকবে।"
                            else
                                "Validated or downloaded videos will automatically be recorded here.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title + platform badge + date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BrandCyan.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, BrandCyan.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = item.platform.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (item.fileSizeBytes != null && item.fileSizeBytes > 0) {
                                        Text(
                                            text = NetworkProbe.formatBytes(item.fileSizeBytes),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = dateFormat.format(Date(item.timestamp)),
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }

                        // URL box
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Text(
                                text = item.url,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Reload in Downloader
                                FilledTonalButton(
                                    onClick = { onReload(item.url) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = BrandEmerald.copy(alpha = 0.15f),
                                        contentColor = BrandEmerald
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (language == "bn") "রিলোড" else "Reload", fontSize = 10.sp)
                                }

                                // In-app Play
                                OutlinedButton(
                                    onClick = { onPlay(item.localUri ?: item.url) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp), tint = BrandCyan)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (language == "bn") "প্লে" else "Play", fontSize = 10.sp, color = TextPrimary)
                                }

                                // External browser
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", tint = TextSecondary, modifier = Modifier.size(15.dp))
                                }

                                // Copy
                                IconButton(
                                    onClick = { onCopy(item.url) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }

                            // Delete button
                            IconButton(
                                onClick = { onDelete(item.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BrandRose, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
