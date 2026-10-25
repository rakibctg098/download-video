package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlatformType
import com.example.ui.theme.*

data class SampleLink(
    val title: String,
    val url: String,
    val tag: String,
    val color: Color
)

@Composable
fun UrlInputSection(
    url: String,
    onUrlChange: (String) -> Unit,
    selectedPlatform: PlatformType,
    onPlatformSelect: (PlatformType) -> Unit,
    isValidating: Boolean,
    onValidate: () -> Unit,
    onPaste: () -> Unit,
    language: String,
    modifier: Modifier = Modifier
) {
    val sampleLinks = listOf(
        SampleLink("Flower Sample MP4", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Direct MP4", BrandEmerald),
        SampleLink("Big Buck Bunny WebM", "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c0/Big_Buck_Bunny_4K.webm/Big_Buck_Bunny_4K.webm.360p.vp9.webm", "Direct WebM", BrandCyan),
        SampleLink("YouTube Link", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "YouTube", Color(0xFFEF4444)),
        SampleLink("Facebook Watch", "https://fb.watch/sample-video/", "Facebook", Color(0xFF3B82F6)),
        SampleLink("Pinterest Pin", "https://pin.it/sample-pin-id", "Pinterest", Color(0xFFE11D48))
    )

    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Platform selector pills
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (language == "bn") "প্ল্যাটফর্ম ফিল্টার" else "Select Platform",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlatformType.values().filter { it != PlatformType.OTHER }.forEach { platform ->
                        val isSelected = selectedPlatform == platform
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPlatformSelect(platform) },
                            label = {
                                Text(
                                    text = platform.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary,
                                selectedContainerColor = BrandCyan.copy(alpha = 0.2f),
                                selectedLabelColor = BrandCyan
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) BrandCyan.copy(alpha = 0.6f) else DarkBorder
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Input field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "ভিডিও লিংক (URL)" else "Video Link (URL)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    TextButton(
                        onClick = onPaste,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = BrandCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "পেস্ট করুন" else "Paste",
                            fontSize = 11.sp,
                            color = BrandCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    placeholder = {
                        Text(
                            text = if (language == "bn")
                                "এখানে MP4, YouTube বা যেকোনো ভিডিও লিংক পেস্ট করুন..."
                            else
                                "Paste direct MP4, YouTube or video URL here...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    trailingIcon = {
                        if (url.isNotEmpty()) {
                            IconButton(
                                onClick = { onUrlChange("") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (url.isNotBlank()) onValidate() }
                    )
                )
            }

            // Validate Link Action Button (Min height 48dp)
            Button(
                onClick = onValidate,
                enabled = url.isNotBlank() && !isValidating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("validate_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandCyan,
                    contentColor = Color.Black,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                )
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == "bn") "সার্ভারে যাচাই করা হচ্ছে..." else "Validating Link...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Validate",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == "bn") "লিংক যাচাই করুন (Validate)" else "Validate Link",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sample quick links
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BrandAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (language == "bn") "টেস্ট করার জন্য স্যাম্পল লিঙ্ক:" else "Quick test sample links:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleLinks.forEach { sample ->
                        Surface(
                            onClick = { onUrlChange(sample.url) },
                            shape = RoundedCornerShape(10.dp),
                            color = sample.color.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, sample.color.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = sample.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = sample.color,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
