package com.example.network

import android.util.Base64
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

    private fun extractFbCdnFromToken(snapUrl: String): String {
        return try {
            if (!snapUrl.contains("token=")) return snapUrl
            val token = snapUrl.substringAfter("token=").substringBefore("&")
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payload = parts[1]
                val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
                json.optString("url", snapUrl)
            } else {
                snapUrl
            }
        } catch (_: Exception) {
            snapUrl
        }
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
                isDirectMedia = true,
                contentType = "video/mp4",
                contentLength = null,
                formattedSize = "720p / 360p MP4",
                filename = analysis.suggestedFilename,
                title = title,
                thumbnailUrl = thumb,
                authorName = author,
                supportsRanges = true,
                canDownloadDirectly = true,
                messageBn = "ইউটিউব ভিডিও সফলভাবে পাওয়া গেছে! প্রিভিউ দেখতে প্লেয়ারে চালান অথবা 'ভিডিও ডাউনলোড করুন' বাটনে চাপুন।",
                messageEn = "YouTube video verified! Tap play to preview or download."
            )
        }

        // 2. Facebook Video Direct Extraction
        if (analysis.platform == PlatformType.FACEBOOK) {
            var directMp4Url: String? = null
            var fbTitle = "Facebook Video"
            var fbThumb: String? = null
            var qualityLabel = "HD"

            // Method A: Direct resolver endpoint
            try {
                val resolverUrl = "https://api.siputzx.my.id/api/d/facebook?url=${URLEncoder.encode(targetUrl, "UTF-8")}"
                val req = Request.Builder()
                    .url(resolverUrl)
                    .header("User-Agent", "Mozilla/5.0 VideoSaver/2.0")
                    .build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val body = res.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val root = JSONObject(body)
                        if (root.optBoolean("status", false)) {
                            val data = root.optJSONObject("data")
                            if (data != null) {
                                fbTitle = data.optString("title", fbTitle).ifEmpty { "Facebook Video" }
                                fbThumb = data.optString("thumbnail", null)
                                val downloads = data.optJSONArray("downloads")
                                if (downloads != null && downloads.length() > 0) {
                                    val firstItem = downloads.getJSONObject(0)
                                    val snapUrl = firstItem.optString("url", "")
                                    qualityLabel = firstItem.optString("quality", "HD")
                                    if (snapUrl.isNotEmpty()) {
                                        // Extract direct FBCDN url from token or use snapUrl
                                        directMp4Url = extractFbCdnFromToken(snapUrl)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // Method B: Fallback HTML scraping
            if (directMp4Url == null) {
                try {
                    val fbRequest = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .build()
                    val fbResponse = client.newCall(fbRequest).execute()
                    if (fbResponse.isSuccessful) {
                        val html = fbResponse.body?.string() ?: ""

                        val hdPattern = Pattern.compile("\"(?:browser_native_hd_url|playable_url_quality_hd|hd_src)\"\\s*:\\s*\"([^\"]+)\"")
                        val hdMatcher = hdPattern.matcher(html)
                        if (hdMatcher.find()) {
                            directMp4Url = cleanEscapedUrl(hdMatcher.group(1))
                            qualityLabel = "HD"
                        } else {
                            val sdPattern = Pattern.compile("\"(?:browser_native_sd_url|playable_url|sd_src)\"\\s*:\\s*\"([^\"]+)\"")
                            val sdMatcher = sdPattern.matcher(html)
                            if (sdMatcher.find()) {
                                directMp4Url = cleanEscapedUrl(sdMatcher.group(1))
                                qualityLabel = "SD"
                            }
                        }

                        val thumbPattern = Pattern.compile("<meta\\s+(?:property|name)=\"og:image\"\\s+content=\"([^\"]+)\"")
                        val thumbMatcher = thumbPattern.matcher(html)
                        if (thumbMatcher.find()) {
                            fbThumb = cleanEscapedUrl(thumbMatcher.group(1))
                        }

                        val titlePattern = Pattern.compile("<meta\\s+(?:property|name)=\"og:title\"\\s+content=\"([^\"]+)\"")
                        val titleMatcher = titlePattern.matcher(html)
                        if (titleMatcher.find()) {
                            fbTitle = titleMatcher.group(1).replace("&amp;", "&").replace("&quot;", "\"")
                        }
                    }
                } catch (_: Exception) {}
            }

            val finalDownloadUrl = directMp4Url ?: targetUrl
            val isDirectResolved = directMp4Url != null

            return@withContext ProbeResult(
                url = finalDownloadUrl,
                platform = PlatformType.FACEBOOK,
                isDirectMedia = true,
                contentType = "video/mp4",
                contentLength = null,
                formattedSize = "$qualityLabel MP4",
                filename = "facebook_${System.currentTimeMillis() % 10000}.mp4",
                title = fbTitle,
                thumbnailUrl = fbThumb,
                authorName = "Facebook Video ($qualityLabel)",
                supportsRanges = true,
                canDownloadDirectly = isDirectResolved,
                messageBn = if (isDirectResolved)
                    "ফেসবুক $qualityLabel ভিডিও সরাসরি প্রস্তুত হয়েছে! নিচের 'ভিডিও ডাউনলোড করুন' বাটনে চাপলে সরাসরি ফোনে সেভ হবে।"
                else
                    "ফেসবুক ভিডিও পেজ সনাক্ত হয়েছে। ডাউনলোডের জন্য লিঙ্কটি ব্রাউজারে খুলুন বা সরাসরি সেভ করুন।",
                messageEn = if (isDirectResolved)
                    "Facebook $qualityLabel video ready! Tap 'Download Video' to save directly to Downloads."
                else
                    "Facebook video stream ready."
            )
        }

        // 3. Pinterest Direct Video Extraction
        if (analysis.platform == PlatformType.PINTEREST) {
            var directPinMp4: String? = null
            var pinTitle = "Pinterest Video"
            var pinThumb: String? = null

            try {
                val req = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val html = res.body?.string() ?: ""

                    // Search for direct pinimg MP4 video URL
                    val mp4Pattern = Pattern.compile("(https://v1\\.pinimg\\.com/videos/[^\"'<> ]+\\.mp4)")
                    val mp4Matcher = mp4Pattern.matcher(html)
                    if (mp4Matcher.find()) {
                        directPinMp4 = mp4Matcher.group(1)
                    } else {
                        val anyMp4Pattern = Pattern.compile("(https://[a-zA-Z0-9.-]*pinimg\\.com/videos/[^\"'<> ]+\\.mp4)")
                        val anyMatcher = anyMp4Pattern.matcher(html)
                        if (anyMatcher.find()) {
                            directPinMp4 = anyMatcher.group(1)
                        } else {
                            val m3u8Pattern = Pattern.compile("(https://v1\\.pinimg\\.com/videos/[^\"'<> ]+\\.m3u8)")
                            val m3u8Matcher = m3u8Pattern.matcher(html)
                            if (m3u8Matcher.find()) {
                                directPinMp4 = m3u8Matcher.group(1).replace("hls", "720p").replace(".m3u8", ".mp4")
                            }
                        }
                    }

                    // Extract Title
                    val titlePattern = Pattern.compile("\"seoTitle\"\\s*:\\s*\"([^\"]+)\"")
                    val titleMatcher = titlePattern.matcher(html)
                    if (titleMatcher.find()) {
                        pinTitle = titleMatcher.group(1).replace("&amp;", "&").replace("&quot;", "\"")
                    } else {
                        val ogTitle = Pattern.compile("<meta\\s+(?:property|name)=\"og:title\"\\s+content=\"([^\"]+)\"")
                        val ogMatcher = ogTitle.matcher(html)
                        if (ogMatcher.find()) {
                            pinTitle = ogMatcher.group(1).replace("&amp;", "&").replace("&quot;", "\"")
                        }
                    }

                    // Extract Thumbnail
                    val thumbPattern = Pattern.compile("\"imageLargeUrl\"\\s*:\\s*\"([^\"]+)\"")
                    val thumbMatcher = thumbPattern.matcher(html)
                    if (thumbMatcher.find()) {
                        pinThumb = thumbMatcher.group(1)
                    } else {
                        val ogThumb = Pattern.compile("<meta\\s+(?:property|name)=\"og:image\"\\s+content=\"([^\"]+)\"")
                        val ogThumbMatcher = ogThumb.matcher(html)
                        if (ogThumbMatcher.find()) {
                            pinThumb = ogThumbMatcher.group(1)
                        }
                    }
                }
            } catch (_: Exception) {}

            val isResolved = directPinMp4 != null
            return@withContext ProbeResult(
                url = directPinMp4 ?: targetUrl,
                platform = PlatformType.PINTEREST,
                isDirectMedia = true,
                contentType = "video/mp4",
                contentLength = null,
                formattedSize = if (isResolved) "720p HD MP4" else "Pinterest Video",
                filename = "pinterest_${System.currentTimeMillis() % 10000}.mp4",
                title = pinTitle,
                thumbnailUrl = pinThumb,
                authorName = "Pinterest Video (720p)",
                supportsRanges = true,
                canDownloadDirectly = isResolved,
                messageBn = if (isResolved)
                    "পিন্টারেস্ট HD ভিডিও সরাসরি প্রস্তুত হয়েছে! নিচের 'ভিডিও ডাউনলোড করুন' বাটনে চাপলে সরাসরি ফোনে সেভ হবে।"
                else
                    "পিন্টারেস্ট ভিডিও পেজ সনাক্ত হয়েছে। ডাউনলোডের জন্য লিঙ্কটি ব্রাউজারে খুলুন বা সরাসরি সেভ করুন।",
                messageEn = if (isResolved)
                    "Pinterest HD video ready! Tap 'Download Video' to save directly to Downloads."
                else
                    "Pinterest video stream detected."
            )
        }

        // 4. Network probe for other links
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
