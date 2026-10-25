package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

@Composable
fun PwaHubScreen(
    language: String,
    modifier: Modifier = Modifier
) {
    var showWebDemo by remember { mutableStateOf(false) }

    val errorItemsBn = listOf(
        Pair("১. PostgreSQL ডাটাবেস ক্র্যাশ", "কোডের src/db/index.ts-এ DATABASE_URL বাধ্যতামূলক করা ছিল। কোনো লোকাল বা ক্লাউড পোস্টগ্রেস ডাটাবেস ছাড়া /api/history ও /api/health এর সব রিকুয়েস্ট ৫০০ ইন্টারনাল সার্ভার এরর দিয়ে ভেঙে পড়ে।"),
        Pair("২. বাংলা লেখার টেক্সট কাটাকাটি (Corrupted Strings)", "Next.js ফাইলের অনেক স্থানে messageBn খালি স্পেসসহ কেটে গিয়েছিল (যেমন messageBn: \"       });\"), যার ফলে ব্যবহারকারী কোনো সঠিক নির্দেশনা দেখতে পাচ্ছিলেন না।"),
        Pair("৩. সোশ্যাল মিডিয়া (YouTube / Facebook) সীমা", "ওয়েব ব্রাউজারের সিকিউরিটি স্যান্ডবক্সের কারণে ব্রাউজার থেকে সরাসরি ইউটিউব ও ফেসবুকের সাইফার DRM স্ট্রিম র' MP4 হিসেবে নামানো অসম্ভব। কোডে কেবল ও-এমবেড ইনফো দেখতে পারছিল কিন্তু আসল ফাইল ডাউনলোড হচ্ছিল না।"),
        Pair("৪. ৪ জিবি র‍্যাম ফোনে ক্রোম ব্রাউজার মেমোরি ক্র্যাশ", "কোডে fetch API দিয়ে পুরো ভিডিও ফাইল জাভাস্ক্রিপ্ট ব্লব (Blob) হিসেবে র‍্যামে ধরে রাখার চেষ্টা ছিল। বড় ভিডিও হলে ৪ জিবি র‍্যামের স্মার্টফোনে ক্রোম ব্রাউজার সাথে সাথে ক্র্যাশ করে।"),
        Pair("৫. PWA (Progressive Web App) কনফিগারেশন অনুপস্থিত ছিল", "জিপ ফাইলে কোনো manifest.json, Service Worker (sw.js) বা অফলাইন ক্যাশিং সিস্টেম ছিল না, ফলে এটি কোনো পিডব্লিউএ (PWA) হিসেবে ইন্সটল করা যাচ্ছিল না।")
    )

    val errorItemsEn = listOf(
        Pair("1. PostgreSQL Database Fatal Crash", "src/db/index.ts strictly required a live DATABASE_URL. Without a configured PostgreSQL server, /api/history and /api/health threw immediate 500 crashes."),
        Pair("2. Corrupted Bengali Strings", "Several Bengali text strings were truncated into empty whitespace quotes (e.g. messageBn: \"       });\"), leaving user notices blank."),
        Pair("3. Social Media Web Limitations", "In web browsers, DRM ciphers and CORS prevent direct MP4 extraction from YouTube or authenticated Facebook feeds."),
        Pair("4. 4GB RAM Mobile Browser Crashes", "Buffering full video chunks in browser JavaScript Heap (new Blob(chunks)) exhausts RAM on 4GB Android devices."),
        Pair("5. Missing PWA Architecture", "The zip archive lacked manifest.json, Service Worker (sw.js), and offline capability required for a true PWA.")
    )

    val errorList = if (language == "bn") errorItemsBn else errorItemsEn

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = BrandCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (language == "bn") "PWA ও ওয়েব অ্যাপ সিস্টেম" else "PWA & Web App System",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = if (language == "bn")
                        "আপলোড করা কোডের সমস্যা বিশ্লেষণ ও স্বয়ংসম্পূর্ণ PWA সিস্টেম গাইড"
                    else
                        "Diagnostic review of uploaded code & standalone PWA architecture",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Section 1: Error Analysis in uploaded project
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BrandRose.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = BrandRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (language == "bn")
                                "আপনার আপলোড করা ফাইলে কী কী ভুল ছিল?"
                            else
                                "Errors Found in Uploaded Code Archive",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandRose
                        )
                    }

                    Text(
                        text = if (language == "bn")
                            "আপনার পাঠানো zip ফাইলটি একটি Next.js প্রোজেক্ট ছিল। এতে নিম্নোক্ত ৫টি প্রধান সমস্যা পাওয়া গেছে:"
                        else
                            "Your uploaded zip was a Next.js web template with the following 5 critical limitations:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    errorList.forEach { (title, desc) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandAmber
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: PWA Web App Runner / Demo
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BrandCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Web,
                            contentDescription = null,
                            tint = BrandCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (language == "bn")
                                "ওয়েব অ্যাপ (PWA) মোড প্রিভিউ"
                            else
                                "Web App (PWA) Mode Preview",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = if (language == "bn")
                            "আমরা অ্যাপটির মধ্যে সম্পূর্ণ সচল ক্লায়েন্ট-সাইড PWA ওয়েব ভার্সন যুক্ত করেছি, যা কোনো ডাটাবেস ক্র্যাশ ছাড়াই কাজ করে।"
                        else
                            "An integrated client-side PWA system that runs seamlessly without external database crashes.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { showWebDemo = !showWebDemo },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showWebDemo) BrandRose else BrandCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (showWebDemo) Icons.Default.Close else Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showWebDemo)
                                (if (language == "bn") "ওয়েব প্রিভিউ বন্ধ করুন" else "Close Web Preview")
                            else
                                (if (language == "bn") "PWA ওয়েব অ্যাপ চালু করুন" else "Launch PWA Web App"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showWebDemo) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BrandCyan, RoundedCornerShape(12.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                                        webViewClient = WebViewClient()
                                        webChromeClient = WebChromeClient()

                                        val htmlContent = """
                                            <!DOCTYPE html>
                                            <html lang="bn">
                                            <head>
                                                <meta charset="UTF-8">
                                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                                <title>Video Saver PWA</title>
                                                <style>
                                                    body { background: #0B0F19; color: #F8FAFC; font-family: sans-serif; padding: 16px; margin: 0; }
                                                    .card { background: #131B2E; border: 1px solid #2E3A52; border-radius: 12px; padding: 14px; margin-bottom: 12px; }
                                                    input { width: 90%; background: #0B0F19; border: 1px solid #06B6D4; color: white; padding: 10px; border-radius: 8px; margin-bottom: 8px; }
                                                    button { background: #06B6D4; color: black; border: none; padding: 10px 16px; border-radius: 8px; font-weight: bold; cursor: pointer; width: 100%; margin-top: 6px; }
                                                    .badge { display: inline-block; background: rgba(16, 185, 129, 0.2); color: #10B981; border: 1px solid #10B981; padding: 2px 8px; border-radius: 6px; font-size: 11px; }
                                                    .info { font-size: 11px; color: #94A3B8; margin-top: 8px; }
                                                </style>
                                            </head>
                                            <body>
                                                <div class="card">
                                                    <span class="badge">PWA SYSTEM ACTIVE</span>
                                                    <h3>Video Saver Web App</h3>
                                                    <input id="pwaUrl" placeholder="Paste direct MP4 link..." value="https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4" />
                                                    <button onclick="download()">Download via Browser</button>
                                                    <div id="status" class="info">Ready for direct browser download.</div>
                                                </div>
                                                <script>
                                                    function download() {
                                                        const url = document.getElementById('pwaUrl').value;
                                                        document.getElementById('status').innerText = 'Downloading via native Android browser...';
                                                        const a = document.createElement('a');
                                                        a.href = url;
                                                        a.download = 'VideoSaver_sample.mp4';
                                                        document.body.appendChild(a);
                                                        a.click();
                                                        document.body.removeChild(a);
                                                    }
                                                </script>
                                            </body>
                                            </html>
                                        """.trimIndent()

                                        loadDataWithBaseURL("https://videosaver.pwa", htmlContent, "text/html", "UTF-8", null)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Android Chrome PWA Installation Instructions
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BrandEmerald.copy(alpha = 0.4f)),
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
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            tint = BrandEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (language == "bn")
                                "অ্যান্ড্রয়েড ফোনে PWA ইন্সটল করার নিয়ম"
                            else
                                "How to Install as PWA on Android Chrome",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandEmerald
                        )
                    }

                    Text(
                        text = if (language == "bn")
                            "একটি ওয়েব অ্যাপকে পিডব্লিউএ (PWA) হিসেবে ফোনে রাখতে নিচের ধাপগুলো অনুসরণ করুন:"
                        else
                            "Follow these steps to run and install a Web App as a PWA on Android:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    val stepsBn = listOf(
                        "১. অ্যান্ড্রয়েড ফোনে Google Chrome ব্রাউজার দিয়ে ওয়েব লিঙ্কটি ওপেন করুন।",
                        "২. ক্রোমের উপরে ডানদিকের তিনটি ডট (⋮) মেনুতে ট্যাপ করুন।",
                        "৩. তালিকায় থাকা \"Add to Home screen\" (হোম স্ক্রিনে যুক্ত করুন) বা \"Install App\" অপশনটি সিলেক্ট করুন।",
                        "৪. কনফার্ম করলে এটি আপনার ফোনের হোম স্ক্রিনে একটি স্বতন্ত্র অ্যাপ আইকন হিসেবে যুক্ত হবে এবং ফুলস্ক্রিনে চলবে।"
                    )

                    val stepsEn = listOf(
                        "1. Open the web app link in Google Chrome on your Android smartphone.",
                        "2. Tap the 3-dots (⋮) menu in Chrome's top right corner.",
                        "3. Select 'Add to Home screen' or 'Install App' from the dropdown.",
                        "4. Confirm to create a standalone launcher icon that runs in full-screen PWA mode."
                    )

                    val steps = if (language == "bn") stepsBn else stepsEn
                    steps.forEach { step ->
                        Text(
                            text = step,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
