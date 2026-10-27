package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AppSettings
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = settings.language == "bn"
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isBn) "অ্যাপ্লিকেশন সেটিংস" else "Application Settings",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isBn)
                        "ডাউনলোড ইঞ্জিন ও ভাষা পছন্দ কনফিগার করুন"
                    else
                        "Configure download engine preferences & language",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Memory badge
        item {
            Surface(
                color = BrandEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrandEmerald.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = BrandEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (isBn) "অ্যান্ড্রয়েড ৪ জিবি র‍্যাম অপ্টিমাইজেশন সক্রিয়" else "Android 4GB RAM Optimization Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandEmerald
                        )
                        Text(
                            text = if (isBn)
                                "চাঙ্ক-ভিত্তিক স্ট্রিমিং মেমোরি খরচ শূন্যে নামিয়ে আনে এবং ব্রাউজার ক্র্যাশ হওয়া প্রতিরোধ করে।"
                            else
                                "Chunk-based streaming eliminates heap bloat and prevents mobile crashes.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Language toggle
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isBn) "ভাষা নির্বাচন (Language)" else "App Language",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { onUpdateSettings(settings.copy(language = "bn")) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isBn) BrandCyan.copy(alpha = 0.2f) else DarkBackground,
                            border = BorderStroke(1.dp, if (isBn) BrandCyan else DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "বাংলা (Bengali)",
                                    fontSize = 13.sp,
                                    fontWeight = if (isBn) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isBn) BrandCyan else TextSecondary
                                )
                            }
                        }

                        Surface(
                            onClick = { onUpdateSettings(settings.copy(language = "en")) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isBn) BrandCyan.copy(alpha = 0.2f) else DarkBackground,
                            border = BorderStroke(1.dp, if (!isBn) BrandCyan else DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "English",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isBn) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isBn) BrandCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Download Engine Mode
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isBn) "ডাউনলোড ইঞ্জিন মোড" else "Download Engine Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Option 1: OkHttp Real Stream
                    Surface(
                        onClick = { onUpdateSettings(settings.copy(downloadMethod = "stream")) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (settings.downloadMethod == "stream") BrandCyan.copy(alpha = 0.15f) else DarkBackground,
                        border = BorderStroke(1.dp, if (settings.downloadMethod == "stream") BrandCyan else DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "স্মার্ট রিয়েল-টাইম স্ট্রিম (সুপারিশকৃত)" else "Smart Real-Time Stream (Recommended)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (settings.downloadMethod == "stream") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = if (isBn)
                                    "আসল বাইট স্পিড (MB/s) ও রিয়েল-টাইম প্রোগ্রেস বার দেখায় এবং সরাসরি ডিভাইসে সেভ করে।"
                                else
                                    "Shows real-time byte count, speed (MB/s), and writes directly to disk safely.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Option 2: Android System DownloadManager
                    Surface(
                        onClick = { onUpdateSettings(settings.copy(downloadMethod = "manager")) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (settings.downloadMethod == "manager") BrandCyan.copy(alpha = 0.15f) else DarkBackground,
                        border = BorderStroke(1.dp, if (settings.downloadMethod == "manager") BrandCyan else DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "অ্যান্ড্রয়েড সিস্টেম ডাউনলোড ম্যানেজার" else "Android System DownloadManager",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (settings.downloadMethod == "manager") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = if (isBn)
                                    "অ্যান্ড্রয়েডের নেটিভ নোটিফিকেশন শেড ব্যবহার করে ব্যাকগ্রাউন্ডে বড় ফাইল ডাউনলোড করে।"
                                else
                                    "Queues download via Android OS notification service; ideal for large background files.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Custom filename prefix
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isBn) "ফাইলের প্রিফিক্স (Filename Prefix)" else "Filename Prefix",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = settings.filenamePrefix,
                        onValueChange = { onUpdateSettings(settings.copy(filenamePrefix = it)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedBorderColor = BrandCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )

                    Text(
                        text = if (isBn)
                            "উদাহরণ: VideoSaver_clip.mp4 (ফাইল সার্চ করা সহজ করার জন্য)"
                        else
                            "Saved files will begin with this prefix for easy filtering.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Developer Info Card (Made by Mohammad Rakib)
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrandCyan.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BrandCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isBn) "ডেভেলপার পরিচিতি" else "Developer Info",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Made by Mohammad Rakib",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandCyan
                    )

                    Text(
                        text = "Telegram channel :https://t.me/appdevbyrakib",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/appdevbyrakib")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "টেলিগ্রাম চ্যানেলে যুক্ত হোন" else "Join Telegram Channel",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
