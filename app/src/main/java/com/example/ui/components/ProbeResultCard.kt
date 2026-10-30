package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PlatformType
import com.example.data.model.ProbeResult
import com.example.ui.theme.*

@Composable
fun ProbeResultCard(
    result: ProbeResult,
    onDownload: () -> Unit,
    onPlayPreview: (String) -> Unit,
    onCopy: (String) -> Unit,
    isDownloading: Boolean,
    language: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDirect = result.isDirectMedia && result.canDownloadDirectly

    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isDirect) BrandEmerald.copy(alpha = 0.5f) else DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Platform badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (result.platform) {
                            PlatformType.DIRECT -> BrandEmerald.copy(alpha = 0.15f)
                            PlatformType.YOUTUBE -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            PlatformType.FACEBOOK -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                            PlatformType.PINTEREST -> Color(0xFFE11D48).copy(alpha = 0.15f)
                            else -> DarkSurfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            when (result.platform) {
                                PlatformType.DIRECT -> BrandEmerald.copy(alpha = 0.4f)
                                PlatformType.YOUTUBE -> Color(0xFFEF4444).copy(alpha = 0.4f)
                                PlatformType.FACEBOOK -> Color(0xFF3B82F6).copy(alpha = 0.4f)
                                PlatformType.PINTEREST -> Color(0xFFE11D48).copy(alpha = 0.4f)
                                else -> DarkBorder
                            }
                        )
                    ) {
                        Text(
                            text = result.platform.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (result.platform) {
                                PlatformType.DIRECT -> BrandEmerald
                                PlatformType.YOUTUBE -> Color(0xFFF87171)
                                PlatformType.FACEBOOK -> Color(0xFF60A5FA)
                                PlatformType.PINTEREST -> Color(0xFFFB7185)
                                else -> TextSecondary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Direct/Web badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDirect) BrandEmerald.copy(alpha = 0.15f) else BrandAmber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isDirect) BrandEmerald.copy(alpha = 0.4f) else BrandAmber.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (isDirect)
                                (if (language == "bn") "ডিরেক্ট মিডিয়া" else "Direct Media")
                            else
                                (if (language == "bn") "ওয়েব স্ট্রিম" else "Web Stream"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDirect) BrandEmerald else BrandAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Size
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = result.formattedSize,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onCopy(result.url) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Link",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Thumbnail if YouTube or available
            if (result.thumbnailUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = result.thumbnailUrl,
                        contentDescription = result.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = result.authorName ?: "Verified Video",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Video Title & Filename
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = result.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2
                )
                Text(
                    text = result.filename,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = BrandCyan
                )
            }

            // Guidance & Limitation explanation box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDirect) BrandEmerald.copy(alpha = 0.1f) else BrandIndigo.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (isDirect) BrandEmerald.copy(alpha = 0.3f) else BrandIndigo.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isDirect) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isDirect) BrandEmerald else BrandIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (language == "bn") result.messageBn else result.messageEn,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = if (isDirect) BrandEmerald else TextSecondary
                    )
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.canDownloadDirectly) {
                    Button(
                        onClick = onDownload,
                        enabled = !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("download_video_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDownloading)
                                (if (language == "bn") "ডাউনলোড হচ্ছে..." else "Downloading...")
                            else
                                (if (language == "bn") "ভিডিও ডাউনলোড করুন (সরাসরি MP4)" else "Download Video (Direct MP4)"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preview button
                    OutlinedButton(
                        onClick = { onPlayPreview(result.url) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrandCyan.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandCyan
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "প্লেয়ারে চালান" else "In-App Play",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Open in Browser button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "ব্রাউজারে খুলুন" else "Open in Browser",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
