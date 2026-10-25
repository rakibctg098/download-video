package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GuideScreen(
    language: String,
    modifier: Modifier = Modifier
) {
    val sectionsBn = listOf(
        Triple(
            "১. ফাইল সেভ হওয়ার লোকেশন",
            Icons.Default.Folder,
            "অ্যান্ড্রয়েড ১২ এবং ১৩ এর সিকিউরিটি পলিসি অনুযায়ী, আপনার ডাউনলোড করা ভিডিও স্বয়ংক্রিয়ভাবে ফোনের প্রধান \"Downloads\" (অথবা Downloads/VideoSaver) ফোল্ডারে সেভ হয়। এটি Google Files বা যেকোনো ফাইল ম্যানেজারে সরাসরি পাওয়া যায়।"
        ),
        Triple(
            "২. Movies/VideoSaver ফোল্ডারে মুভ করার নিয়ম",
            Icons.Default.DriveFileMove,
            "যদি আপনি ভিডিওগুলোকে একটি বিশেষ ফোল্ডারে গুছিয়ে রাখতে চান:\n• 'Files by Google' অথবা স্যামসাং 'My Files' অ্যাপ খুলুন।\n• 'Downloads' ফোল্ডারে গিয়ে ভিডিওটির পাশে তিনটি ডট (...) চাপুন।\n• 'Move to' > 'Internal storage' > 'Movies'-এ যান।\n• 'Create folder' দিয়ে 'VideoSaver' নাম দিন এবং 'Move here' করুন।"
        ),
        Triple(
            "৩. গ্যালারি ও গুগল ফটোসে দৃশ্যমান হওয়া",
            Icons.Default.PhotoLibrary,
            "অ্যান্ড্রয়েডের ব্যাকগ্রাউন্ড 'MediaScanner' সার্ভিস নতুন সেভ হওয়া MP4 ভিডিওকে কয়েক সেকেন্ডের মধ্যে ইনডেক্স করে নেয়। ফলে ভিডিওটি আপনার ফোনের Gallery এবং Google Photos-এ সাথে সাথেই দেখা যায়।"
        ),
        Triple(
            "৪. ৪ জিবি র‍্যাম ডিভাইসের অপ্টিমাইজেশন",
            Icons.Default.Memory,
            "আমাদের অ্যাপটি পুরো ভিডিও মেমোরিতে না রেখে সরাসরি ৩২ কিলোবাইট (32 KB) চাঙ্ক আকারে মেমোরি থেকে স্টোরেজে লিখে। এতে ৪ জিবি র‍্যামের স্মার্টফোন কোনো রকম হ্যাং বা ক্র্যাশ ছাড়াই হাই-স্পিডে কাজ করে।"
        ),
        Triple(
            "৫. সোশ্যাল মিডিয়া ডাউনলোডের বাস্তব নিয়ম",
            Icons.Default.Security,
            "ইউটিউব এবং ফেসবুকের ভিডিও তাদের টার্মস ও কপিরাইট নীতির কারণে এনক্রিপ্ট থাকে। এই অ্যাপটি স্বচ্ছভাবে ও-এমবেড প্রিভিউ এবং ডিরেক্ট মিডিয়া সিডিএন লিঙ্ক সাপোর্ট করে যাতে কোনো পলিসি লংঘন না হয়।"
        )
    )

    val sectionsEn = listOf(
        Triple(
            "1. Default File Storage Location",
            Icons.Default.Folder,
            "Under Android 12 & 13 security guidelines, downloaded videos are automatically saved into your device's primary 'Downloads' (or Downloads/VideoSaver) folder, readily accessible via Files by Google or Samsung My Files."
        ),
        Triple(
            "2. Moving Files to Movies/VideoSaver",
            Icons.Default.DriveFileMove,
            "To organize your downloaded videos into a dedicated folder:\n• Open 'Files by Google' or Samsung 'My Files'.\n• In 'Downloads', tap the 3-dots (...) next to the video.\n• Tap 'Move to' > Internal storage > 'Movies'.\n• Create a folder named 'VideoSaver' and tap 'Move here'."
        ),
        Triple(
            "3. Gallery & Google Photos Visibility",
            Icons.Default.PhotoLibrary,
            "Android's background MediaScanner service automatically indexes downloaded MP4 files within seconds, making them instantly visible in your default Gallery and Google Photos."
        ),
        Triple(
            "4. 4 GB RAM Smartphone Optimization",
            Icons.Default.Memory,
            "Unlike web browsers that buffer massive blobs into RAM, Video Saver streams media in 32 KB disk-direct chunks, ensuring zero memory lag or phone slowdowns on 4 GB RAM devices."
        ),
        Triple(
            "5. Honest Social Media Policies",
            Icons.Default.Security,
            "YouTube and authenticated Facebook streams utilize dynamic encryption and login credentials. Video Saver truthfully presents verified oEmbed previews and direct media downloads."
        )
    )

    val sections = if (language == "bn") sectionsBn else sectionsEn

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
                    text = if (language == "bn") "ইউজার গাইড ও স্টোরেজ সহায়িকা" else "User Guide & Storage Guide",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (language == "bn")
                        "অ্যান্ড্রয়েড ১২/১৩ স্মার্টফোন ও নতুন ব্যবহারকারীদের জন্য সহজ নির্দেশিকা"
                    else
                        "Complete instructions for Android 12/13 smartphones & beginners",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        sections.forEach { (title, icon, content) ->
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, BrandCyan.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = BrandCyan,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(18.dp)
                                )
                            }
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = content,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
