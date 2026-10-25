package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TestCase
import com.example.ui.theme.*

@Composable
fun TestsScreen(
    testCases: List<TestCase>,
    isRunning: Boolean,
    onRunTests: () -> Unit,
    onLoadTestUrl: (String) -> Unit,
    language: String,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (testCases.isEmpty()) {
            onRunTests()
        }
    }

    val passedCount = testCases.count { it.isPassed }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == "bn") "স্বয়ংক্রিয় টেস্ট স্যুইট" else "Automated Test Suite",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == "bn") "লিংক ভ্যালিডেশন ও সিকিউরিটি পরীক্ষা" else "Link validation & security diagnostics",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onRunTests,
                    enabled = !isRunning,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.Black)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "bn") "রান করুন" else "Run Tests", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Summary Scorecard
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (language == "bn") "মোট টেস্ট" else "Total Tests", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "${testCases.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (language == "bn") "উত্তীর্ণ (Passed)" else "Passed", fontSize = 11.sp, color = BrandEmerald)
                        Text(text = "$passedCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandEmerald)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (language == "bn") "সফলতার হার" else "Success Rate", fontSize = 11.sp, color = BrandCyan)
                        val rate = if (testCases.isNotEmpty()) (passedCount * 100 / testCases.size) else 100
                        Text(text = "$rate%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandCyan)
                    }
                }
            }
        }

        // Test items
        items(testCases, key = { it.id }) { test ->
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (test.isPassed) BrandEmerald.copy(alpha = 0.35f) else BrandRose.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (test.isPassed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (test.isPassed) BrandEmerald else BrandRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = test.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "${test.durationMs}ms",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }

                    Text(
                        text = test.description,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Input: ${test.inputUrl}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                maxLines = 1
                            )
                            Text(
                                text = "Result: ${test.actual}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (test.isPassed) BrandEmerald else BrandRose
                            )
                        }
                    }

                    if (test.inputUrl.startsWith("http")) {
                        TextButton(
                            onClick = { onLoadTestUrl(test.inputUrl) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == "bn") "এই লিংকটি ডাউনলোডারে লোড করুন" else "Load in Downloader",
                                fontSize = 11.sp,
                                color = BrandCyan
                            )
                        }
                    }
                }
            }
        }
    }
}
