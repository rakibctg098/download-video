package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadProgress
import com.example.data.model.PlatformType
import com.example.data.model.ProbeResult
import com.example.ui.ScreenTab
import com.example.ui.components.ProgressBarCard
import com.example.ui.components.ProbeResultCard
import com.example.ui.components.UrlInputSection
import com.example.ui.theme.*

@Composable
fun SaverScreen(
    url: String,
    onUrlChange: (String) -> Unit,
    selectedPlatform: PlatformType,
    onPlatformSelect: (PlatformType) -> Unit,
    isValidating: Boolean,
    onValidate: () -> Unit,
    onPaste: () -> Unit,
    probeResult: ProbeResult?,
    downloadProgress: DownloadProgress,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onPlayPreview: (String) -> Unit,
    onCopy: (String) -> Unit,
    onNavigateTab: (ScreenTab) -> Unit,
    toastMessage: Pair<String, Boolean>?,
    onDismissToast: () -> Unit,
    language: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Toast message banner if any
        if (toastMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (toastMessage.second) BrandRose.copy(alpha = 0.15f) else BrandEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (toastMessage.second) BrandRose.copy(alpha = 0.4f) else BrandEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = toastMessage.first,
                            fontSize = 12.sp,
                            color = if (toastMessage.second) BrandRose else BrandEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissToast) {
                            Text("OK", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // URL input card
        item {
            UrlInputSection(
                url = url,
                onUrlChange = onUrlChange,
                selectedPlatform = selectedPlatform,
                onPlatformSelect = onPlatformSelect,
                isValidating = isValidating,
                onValidate = onValidate,
                onPaste = onPaste,
                language = language
            )
        }

        // Progress card
        item {
            ProgressBarCard(
                progress = downloadProgress,
                onCancel = onCancelDownload,
                language = language
            )
        }

        // Probe result card
        if (probeResult != null) {
            item {
                ProbeResultCard(
                    result = probeResult,
                    onDownload = onDownload,
                    onPlayPreview = onPlayPreview,
                    onCopy = onCopy,
                    isDownloading = downloadProgress.isDownloading,
                    language = language
                )
            }
        }

        // Beginner storage banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrandIndigo.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, BrandIndigo.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateTab(ScreenTab.GUIDE) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = BrandIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (language == "bn")
                                    "ডাউনলোড করা ফাইল কোথায় পাবেন?"
                                else
                                    "Where to find downloaded videos?",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (language == "bn")
                                    "Internal Storage > Downloads এবং গ্যালারি গাইড দেখুন"
                                else
                                    "Guide: Internal Storage > Downloads & Gallery sync",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Guide",
                        tint = BrandIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
