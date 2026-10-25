package com.example.data.model

enum class PlatformType(val displayName: String, val tag: String) {
    ALL("All", "all"),
    DIRECT("Direct MP4", "direct"),
    YOUTUBE("YouTube", "youtube"),
    FACEBOOK("Facebook", "facebook"),
    PINTEREST("Pinterest", "pinterest"),
    OTHER("Other", "other")
}

data class UrlAnalysis(
    val isValid: Boolean = false,
    val rawUrl: String = "",
    val normalizedUrl: String = "",
    val platform: PlatformType = PlatformType.OTHER,
    val isDirectMediaCandidate: Boolean = false,
    val mediaExtension: String? = null,
    val videoId: String? = null,
    val suggestedTitle: String = "",
    val suggestedFilename: String = "video.mp4",
    val limitationReason: String? = null,
    val guidanceBn: String = "",
    val guidanceEn: String = ""
)

data class ProbeResult(
    val url: String,
    val platform: PlatformType,
    val isDirectMedia: Boolean,
    val contentType: String?,
    val contentLength: Long?,
    val formattedSize: String,
    val filename: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val authorName: String? = null,
    val supportsRanges: Boolean = false,
    val canDownloadDirectly: Boolean = false,
    val messageBn: String,
    val messageEn: String
)

data class DownloadProgress(
    val isDownloading: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val percentage: Float? = null,
    val speedBytesPerSec: Long = 0,
    val statusTextBn: String = "",
    val statusTextEn: String = "",
    val error: String? = null,
    val savedFilePath: String? = null
)

data class AppSettings(
    val language: String = "bn", // "bn" or "en"
    val filenamePrefix: String = "VideoSaver_",
    val autoValidateOnPaste: Boolean = true,
    val downloadMethod: String = "stream" // "stream" (OkHttp chunk stream) or "manager" (Android DownloadManager)
)

data class TestCase(
    val id: String,
    val name: String,
    val description: String,
    val inputUrl: String,
    val expected: String,
    val actual: String,
    val isPassed: Boolean,
    val durationMs: Long
)
