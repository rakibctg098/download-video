package com.example.network

import com.example.data.model.PlatformType
import com.example.data.model.UrlAnalysis
import java.net.URI

object UrlAnalyzer {
    private val VIDEO_EXTENSIONS = listOf(".mp4", ".webm", ".mkv", ".mov", ".m4v", ".ogv", ".3gp")
    private val UNSAFE_SCHEMES = listOf("javascript", "data", "file", "vbscript", "about", "content", "blob")

    fun analyze(rawInput: String): UrlAnalysis {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return UrlAnalysis(
                isValid = false,
                rawUrl = "",
                platform = PlatformType.OTHER,
                limitationReason = "URL cannot be empty.",
                guidanceBn = "অনুগ্রহ করে একটি সঠিক ভিডিও লিংক (https:// সহ) দিন।",
                guidanceEn = "Please enter a valid video URL starting with https://"
            )
        }

        // Scheme validation
        val scheme = try {
            val uri = URI(trimmed)
            uri.scheme?.lowercase() ?: ""
        } catch (_: Exception) {
            val idx = trimmed.indexOf(":")
            if (idx != -1) trimmed.substring(0, idx).lowercase() else ""
        }

        if (UNSAFE_SCHEMES.contains(scheme)) {
            return UrlAnalysis(
                isValid = false,
                rawUrl = trimmed,
                platform = PlatformType.OTHER,
                limitationReason = "Unsafe scheme '$scheme:' is blocked for security.",
                guidanceBn = "নিরাপত্তার স্বার্থে '$scheme:' প্রোটোকল ব্লক করা হয়েছে। শুধুমাত্র http/https ব্যবহার করুন।",
                guidanceEn = "Security policy: '$scheme:' protocol is blocked. Only http and https are permitted."
            )
        }

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return UrlAnalysis(
                isValid = false,
                rawUrl = trimmed,
                platform = PlatformType.OTHER,
                limitationReason = "Malformed URL format.",
                guidanceBn = "লিংকের ফরম্যাট সঠিক নয়। ডোমেইন নাম ও https:// যাচাই করুন।",
                guidanceEn = "Malformed URL format. Make sure it includes https:// and a valid domain."
            )
        }

        val protocol = uri.scheme?.lowercase()
        if (protocol != "http" && protocol != "https") {
            return UrlAnalysis(
                isValid = false,
                rawUrl = trimmed,
                platform = PlatformType.OTHER,
                limitationReason = "Unsupported protocol '$protocol'.",
                guidanceBn = "শুধুমাত্র HTTP এবং HTTPS প্রোটোকল সমর্থিত।",
                guidanceEn = "Only standard HTTP and HTTPS protocols are supported."
            )
        }

        val host = uri.host?.lowercase() ?: ""
        val path = uri.path ?: ""

        // Check if direct video file
        var detectedExt: String? = null
        val lowerPath = path.lowercase()
        for (ext in VIDEO_EXTENSIONS) {
            if (lowerPath.endsWith(ext) || lowerPath.contains("$ext?")) {
                detectedExt = ext
                break
            }
        }

        // Extract base filename
        val lastSegment = path.substringAfterLast("/").substringBefore("?")
        var baseFilename = if (lastSegment.isNotBlank()) lastSegment else "video"
        for (ext in VIDEO_EXTENSIONS) {
            if (baseFilename.lowercase().endsWith(ext)) {
                baseFilename = baseFilename.substring(0, baseFilename.length - ext.length)
                break
            }
        }
        val safeBase = baseFilename.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifEmpty { "video" }
        val extToUse = detectedExt ?: ".mp4"
        val suggestedFilename = "$safeBase$extToUse"

        // 1. YouTube Detection
        if (host.contains("youtube.com") || host.contains("youtu.be")) {
            val videoId = when {
                host.contains("youtu.be") -> path.trimStart('/').substringBefore('?').substringBefore('&')
                path.contains("/watch") -> uri.query?.split("&")?.firstOrNull { it.startsWith("v=") }?.substringAfter("v=")
                path.contains("/shorts/") -> path.substringAfter("/shorts/").substringBefore('?').substringBefore('/')
                path.contains("/embed/") -> path.substringAfter("/embed/").substringBefore('?').substringBefore('/')
                else -> null
            }
            return UrlAnalysis(
                isValid = true,
                rawUrl = trimmed,
                normalizedUrl = trimmed,
                platform = PlatformType.YOUTUBE,
                isDirectMediaCandidate = false,
                videoId = videoId,
                suggestedTitle = if (videoId != null) "YouTube Video ($videoId)" else "YouTube Link",
                suggestedFilename = "youtube_${videoId ?: "video"}.mp4",
                limitationReason = "YouTube uses encrypted streams/DRM. Direct raw MP4 scraping violates ToS.",
                guidanceBn = "ইউটিউব ভিডিও সনাক্ত হয়েছে। অফিসিয়াল ও-এমবেড প্রিভিউ দেখতে 'যাচাই করুন' ট্যাপ করুন।",
                guidanceEn = "YouTube video detected. Tap 'Validate Link' to view official preview and information."
            )
        }

        // 2. Facebook Detection
        if (host.contains("facebook.com") || host.contains("fb.watch")) {
            return UrlAnalysis(
                isValid = true,
                rawUrl = trimmed,
                normalizedUrl = trimmed,
                platform = PlatformType.FACEBOOK,
                isDirectMediaCandidate = false,
                suggestedTitle = "Facebook Video",
                suggestedFilename = "facebook_video.mp4",
                limitationReason = "Facebook video posts require user authentication and cookie sessions.",
                guidanceBn = "ফেসবুক ভিডিও পেজ সনাক্ত হয়েছে। সরাসরি MP4 ফাইলের জন্য ডিরেক্ট মিডিয়া লিংক ব্যবহার করুন।",
                guidanceEn = "Facebook post detected. Facebook posts deliver HTML pages rather than raw MP4 files."
            )
        }

        // 3. Pinterest Detection
        if (host.contains("pinterest.com") || host.contains("pin.it")) {
            return UrlAnalysis(
                isValid = true,
                rawUrl = trimmed,
                normalizedUrl = trimmed,
                platform = PlatformType.PINTEREST,
                isDirectMediaCandidate = false,
                suggestedTitle = "Pinterest Pin",
                suggestedFilename = "pinterest_video.mp4",
                limitationReason = "Pinterest pin URLs are gallery pages.",
                guidanceBn = "পিন্টারেস্ট পিন সনাক্ত হয়েছে। সরাসরি ডাউনলোডের জন্য এর ডিরেক্ট মিডিয়া লিংক দিন।",
                guidanceEn = "Pinterest pin detected. Direct media link (e.g. .mp4) is required for raw file saving."
            )
        }

        // 4. Direct Media File
        if (detectedExt != null) {
            return UrlAnalysis(
                isValid = true,
                rawUrl = trimmed,
                normalizedUrl = trimmed,
                platform = PlatformType.DIRECT,
                isDirectMediaCandidate = true,
                mediaExtension = detectedExt,
                suggestedTitle = "$safeBase$detectedExt",
                suggestedFilename = suggestedFilename,
                guidanceBn = "ডিরেক্ট ভিডিও ফাইল সনাক্ত হয়েছে! 'যাচাই করুন' চাপলে ফাইলের সাইজ ও ডিটেইলস জানা যাবে।",
                guidanceEn = "Direct media file confirmed! Tap 'Validate Link' to inspect file headers and download."
            )
        }

        // 5. Other general web URL
        return UrlAnalysis(
            isValid = true,
            rawUrl = trimmed,
            normalizedUrl = trimmed,
            platform = PlatformType.OTHER,
            isDirectMediaCandidate = true,
            suggestedTitle = "${host.take(20)} Video",
            suggestedFilename = suggestedFilename,
            guidanceBn = "সার্ভারের সাথে সংযোগ করে ভিডিও স্ট্রীম পরীক্ষা করতে 'যাচাই করুন' চাপুন।",
            guidanceEn = "Tap 'Validate Link' to probe server headers and test if this is a downloadable stream."
        )
    }
}
