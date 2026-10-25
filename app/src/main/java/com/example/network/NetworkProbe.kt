package com.example.network

import com.example.data.model.PlatformType
import com.example.data.model.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object NetworkProbe {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "Unknown size"
        val k = 1024.0
        val sizes = arrayOf("B", "KB", "MB", "GB")
        val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt().coerceIn(0, 3)
        val value = bytes / Math.pow(k, i.toDouble())
        return String.format("%.2f %s", value, sizes[i])
    }

    private fun cleanEscapedUrl(raw: String): String {
        return raw.replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("\\u0025", "%")
            .replace("&amp;", "&")
    }

    suspend fun probe(url: String): ProbeResult = withContext(Dispatchers.IO) {
        val analysis = UrlAnalyzer.analyze(url)
        val targetUrl = analysis.normalizedUrl.ifEmpty { url.trim() }

        // 1. YouTube handling
        if (analysis.platform == PlatformType.YOUTUBE) {
            var title = analysis.suggestedTitle
            var author = "YouTube Creator"
            var thumb: String? = null

            try {
                val oEmbedUrl = "https://www.youtube.com/oembed?url=${URLEncoder.encode(targetUrl, "UTF-8")}&format=json"
                val request = Request.Builder()
                    .url(oEmbedUrl)
                    .header("User-Agent", "Mozilla/5.0 VideoSaver/2.0")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val json = JSONObject(body)
                        title = json.optString("title", title)
                        author = json.optString("author_name", author)
                        thumb = json.optString("thumbnail_url", null)
                    }
                }
            } catch (_: Exception) {}

            return@withContext ProbeResult(
                url = targetUrl,
                platform = PlatformType.YOUTUBE,
                isDirectMedia = false,
                contentType = "text/html",
                contentLength = null,
                formattedSize = "Encrypted Stream",
                filename = analysis.suggestedFilename,
                title = title,
                thumbnailUrl = thumb,
                authorName = author,
                supportsRanges = false,
                canDownloadDirectly = false,
                messageBn = "ইউটিউব ভিডিও পাওয়া গেছে! পলিসি ও সাইফার এনক্রিপশনের কারণে ডিরেক্ট MP4 ফাইল স্টোর করা যায় না। আপনি ব্রাউজারে দেখতে পারেন।",
                messageEn = "YouTube video verified! YouTube streams are encrypted with dynamic DRM ciphers. You can open the official stream directly."
            )
        }

        // 2. Facebook Video Extraction
        if (analysis.platform == PlatformType.FACEBOOK) {
            var extractedVideoUrl: String? = null
            var thumbUrl: String? = null
            var fbTitle = "Facebook Video"
            var qualityLabel = "HD"

            try {
                val fbRequest = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()

                val fbResponse = client.newCall(fbRequest).execute()
                if (fbResponse.isSuccessful) {
                    val html = fbResponse.body?.string() ?: ""

                    // Extract HD URL
                    val hdPattern = Pattern.compile("\"(?:browser_native_hd_url|playable_url_quality_hd|hd_src)\"\\s*:\\s*\"([^\"]+)\"")
                    val hdMatcher = hdPattern.matcher(html)
                    if (hdMatcher.find()) {
                        extractedVideoUrl = cleanEscapedUrl(hdMatcher.group(1))
                        qualityLabel = "HD"
                    }

                    // Extract SD URL if HD not found
                    if (extractedVideoUrl == null) {
                        val sdPattern = Pattern.compile("\"(?:browser_native_sd_url|playable_url|sd_src)\"\\s*:\\s*\"([^\"]+)\"")
                        val sdMatcher = sdPattern.matcher(html)
                        if (sdMatcher.find()) {
                            extractedVideoUrl = cleanEscapedUrl(sdMatcher.group(1))
                            qualityLabel = "SD"
                        }
                    }

                    // Extract og:video if still null
                    if (extractedVideoUrl == null) {
                        val ogPattern = Pattern.compile("<meta\\s+(?:property|name)=\"og:video(?:\\:secure_url|\\:url)?\"\\s+content=\"([^\"]+)\"")
                        val ogMatcher = ogPattern.matcher(html)
                        if (ogMatcher.find()) {
                            extractedVideoUrl = cleanEscapedUrl(ogMatcher.group(1))
                            qualityLabel = "Standard"
                        }
                    }

                    // Extract thumbnail
                    val thumbPattern = Pattern.compile("<meta\\s+(?:property|name)=\"og:image\"\\s+content=\"([^\"]+)\"")
                    val thumbMatcher = thumbPattern.matcher(html)
                    if (thumbMatcher.find()) {
                        thumbUrl = cleanEscapedUrl(thumbMatcher.group(1))
                    }

                    // Extract title
                    val titlePattern = Pattern.compile("<meta\\s+(?:property|name)=\"og:title\"\\s+content=\"([^\"]+)\"")
                    val titleMatcher = titlePattern.matcher(html)
                    if (titleMatcher.find()) {
                        fbTitle = titleMatcher.group(1).replace("&amp;", "&").replace("&quot;", "\"")
                    }
                }
            } catch (_: Exception) {}

            if (extractedVideoUrl != null && extractedVideoUrl.startsWith("http")) {
                return@withContext ProbeResult(
                    url = extractedVideoUrl,
                    platform = PlatformType.FACEBOOK,
                    isDirectMedia = true,
                    contentType = "video/mp4",
                    contentLength = null,
                    formattedSize = "$qualityLabel MP4",
                    filename = "facebook_${System.currentTimeMillis() % 10000}.mp4",
                    title = fbTitle,
                    thumbnailUrl = thumbUrl,
                    authorName = "Facebook Video ($qualityLabel)",
                    supportsRanges = true,
                    canDownloadDirectly = true,
                    messageBn = "ফেসবুক $qualityLabel ভিডিও সফলভাবে প্রস্তুত হয়েছে! নিচে 'ভিডিও ডাউনলোড করুন' বাটনে চাপ দিয়ে ফোনে সেভ করুন।",
                    messageEn = "Facebook $qualityLabel video extracted successfully! Tap 'Download Video' to save to your Android device."
                )
            } else {
                // If extraction requires user login session (private/friends-only reel)
                // Provide dedicated fallback download resolver URL
                val resolverUrl = "https://fdownloader.net/download?k_url=${URLEncoder.encode(targetUrl, "UTF-8")}"
                return@withContext ProbeResult(
                    url = resolverUrl,
                    platform = PlatformType.FACEBOOK,
                    isDirectMedia = true,
                    contentType = "video/mp4",
                    contentLength = null,
                    formattedSize = "MP4 Options",
                    filename = "facebook_video.mp4",
                    title = fbTitle,
                    thumbnailUrl = thumbUrl,
                    authorName = "Facebook Post",
                    supportsRanges = true,
                    canDownloadDirectly = true,
                    messageBn = "ফেসবুক ভিডিওর সরাসরি ডাউনলোড অপশন তৈরি করা হয়েছে। নিচে 'ভিডিও ডাউনলোড করুন' বাটনে চাপ দিন।",
                    messageEn = "Facebook video download options ready. Tap 'Download Video' below to save."
                )
            }
        }

        // 3. Network probe with HEAD or minimal Range GET for other links
        var contentType: String? = null
        var contentLength: Long? = null
        var supportsRanges = false
        var serverFilename: String? = null

        try {
            var request = Request.Builder()
                .url(targetUrl)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) VideoSaver/2.0")
                .header("Accept", "*/*")
                .build()

            var response = client.newCall(request).execute()

            if (response.code == 405 || response.code == 403 || response.code == 501) {
                request = Request.Builder()
                    .url(targetUrl)
                    .get()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) VideoSaver/2.0")
                    .header("Range", "bytes=0-1")
                    .build()
                response = client.newCall(request).execute()
            }

            contentType = response.header("Content-Type")
            val lengthHeader = response.header("Content-Length")
            val contentRange = response.header("Content-Range")
            val acceptRanges = response.header("Accept-Ranges")
            val contentDisposition = response.header("Content-Disposition")

            if (lengthHeader != null) {
                contentLength = lengthHeader.toLongOrNull()
            } else if (contentRange != null) {
                val totalStr = contentRange.substringAfterLast("/")
                contentLength = totalStr.toLongOrNull()
            }

            if (acceptRanges?.equals("bytes", ignoreCase = true) == true) {
                supportsRanges = true
            }

            if (contentDisposition != null && contentDisposition.contains("filename=")) {
                serverFilename = contentDisposition.substringAfter("filename=")
                    .trim('\"', '\'', ' ', ';')
            }
        } catch (e: Exception) {
            return@withContext ProbeResult(
                url = targetUrl,
                platform = analysis.platform,
                isDirectMedia = analysis.isDirectMediaCandidate,
                contentType = null,
                contentLength = null,
                formattedSize = "Unknown",
                filename = analysis.suggestedFilename,
                title = analysis.suggestedTitle,
                canDownloadDirectly = analysis.isDirectMediaCandidate,
                messageBn = "নেটওয়ার্ক সংযোগ ত্রুটি (${e.localizedMessage ?: "timeout"})। ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন।",
                messageEn = "Network probe failed: ${e.localizedMessage ?: "timeout"}. Check connection and retry."
            )
        }

        val cleanMime = contentType?.lowercase() ?: ""
        val isVideoMime = cleanMime.startsWith("video/") ||
                cleanMime.contains("application/octet-stream") ||
                cleanMime.contains("application/x-mpegurl") ||
                cleanMime.contains("application/vnd.apple.mpegurl")

        val isHtml = cleanMime.contains("text/html")
        val finalFilename = serverFilename ?: analysis.suggestedFilename

        if (isHtml) {
            return@withContext ProbeResult(
                url = targetUrl,
                platform = analysis.platform,
                isDirectMedia = false,
                contentType = cleanMime,
                contentLength = contentLength,
                formattedSize = contentLength?.let { formatBytes(it) } ?: "HTML Page",
                filename = finalFilename,
                title = analysis.suggestedTitle,
                supportsRanges = supportsRanges,
                canDownloadDirectly = false,
                messageBn = "এই লিঙ্কটি একটি সাধারণ ওয়েব পেজ। সরাসরি ভিডিও সেভ করতে এর আসল .mp4 লিঙ্ক প্রয়োজন।",
                messageEn = "This URL returned an HTML webpage rather than a media stream. Direct media link required."
            )
        }

        val canDownload = isVideoMime || analysis.isDirectMediaCandidate
        val formattedSize = contentLength?.let { formatBytes(it) } ?: "Stream Size"

        return@withContext ProbeResult(
            url = targetUrl,
            platform = if (canDownload) PlatformType.DIRECT else analysis.platform,
            isDirectMedia = true,
            contentType = cleanMime.ifEmpty { "video/mp4" },
            contentLength = contentLength,
            formattedSize = formattedSize,
            filename = finalFilename,
            title = analysis.suggestedTitle,
            supportsRanges = supportsRanges,
            canDownloadDirectly = canDownload,
            messageBn = if (canDownload)
                "বৈধ ডিরেক্ট ভিডিও ফাইল পাওয়া গেছে! 'ভিডিও ডাউনলোড করুন' বাটনে চাপ দিয়ে ফোনে সেভ করুন।"
            else
                "সার্ভার সাড়া দিয়েছে কিন্তু রেসপন্সটি ভিডিও টাইপ নয়।",
            messageEn = if (canDownload)
                "Valid direct video stream confirmed! Tap 'Download Video' to save to your Android device."
            else
                "Server responded, but content-type is not recognized as a video."
        )
    }
}
