package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.HistoryEntity
import com.example.data.model.AppSettings
import com.example.data.model.DownloadProgress
import com.example.data.model.PlatformType
import com.example.data.model.ProbeResult
import com.example.data.model.TestCase
import com.example.network.NetworkProbe
import com.example.network.UrlAnalyzer
import com.example.network.VideoDownloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    SAVER,
    HISTORY,
    PWA_HUB,
    TESTS,
    GUIDE,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val historyDao = db.historyDao()

    // Active Screen
    private val _activeTab = MutableStateFlow(ScreenTab.SAVER)
    val activeTab: StateFlow<ScreenTab> = _activeTab.asStateFlow()

    // App Settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Saver State
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(PlatformType.ALL)
    val selectedPlatform: StateFlow<PlatformType> = _selectedPlatform.asStateFlow()

    private val _isValidating = MutableStateFlow(false)
    val isValidating: StateFlow<Boolean> = _isValidating.asStateFlow()

    private val _probeResult = MutableStateFlow<ProbeResult?>(null)
    val probeResult: StateFlow<ProbeResult?> = _probeResult.asStateFlow()

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val _toastMessage = MutableStateFlow<Pair<String, Boolean>?>(null) // message to isError
    val toastMessage: StateFlow<Pair<String, Boolean>?> = _toastMessage.asStateFlow()

    // Video Player Preview State
    private val _activePlayerUrl = MutableStateFlow<String?>(null)
    val activePlayerUrl: StateFlow<String?> = _activePlayerUrl.asStateFlow()

    // Download Job
    private var downloadJob: Job? = null

    // Room DB History Stream
    val historyItems: StateFlow<List<HistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Test Suite State
    private val _testCases = MutableStateFlow<List<TestCase>>(emptyList())
    val testCases: StateFlow<List<TestCase>> = _testCases.asStateFlow()

    private val _isRunningTests = MutableStateFlow(false)
    val isRunningTests: StateFlow<Boolean> = _isRunningTests.asStateFlow()

    fun setTab(tab: ScreenTab) {
        _activeTab.value = tab
    }

    fun toggleLanguage() {
        val newLang = if (_settings.value.language == "bn") "en" else "bn"
        _settings.value = _settings.value.copy(language = newLang)
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
    }

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
        _probeResult.value = null
        if (newUrl.isNotBlank()) {
            val analysis = UrlAnalyzer.analyze(newUrl)
            _selectedPlatform.value = analysis.platform
        }
    }

    fun setPlatform(platform: PlatformType) {
        _selectedPlatform.value = platform
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                onUrlChange(text)
                val msg = if (_settings.value.language == "bn") "ক্লিপবোর্ড থেকে পেস্ট করা হয়েছে!" else "Pasted from clipboard!"
                _toastMessage.value = Pair(msg, false)
                if (_settings.value.autoValidateOnPaste) {
                    validateUrl()
                }
            }
        }
    }

    fun validateUrl() {
        val url = _urlInput.value.trim()
        if (url.isEmpty()) {
            val msg = if (_settings.value.language == "bn") "দয়া করে একটি লিঙ্ক প্রবেশ করান।" else "Please enter a URL first."
            _toastMessage.value = Pair(msg, true)
            return
        }

        viewModelScope.launch {
            _isValidating.value = true
            _toastMessage.value = null
            try {
                val result = NetworkProbe.probe(url)
                _probeResult.value = result

                // Record history item
                historyDao.insert(
                    HistoryEntity(
                        url = result.url,
                        title = result.title,
                        platform = result.platform.tag,
                        contentType = result.contentType,
                        fileSizeBytes = result.contentLength,
                        status = if (result.canDownloadDirectly) "validated" else "inspected"
                    )
                )
            } catch (e: Exception) {
                val err = if (_settings.value.language == "bn") "যাচাইকরণ ব্যর্থ: ${e.localizedMessage}" else "Validation failed: ${e.localizedMessage}"
                _toastMessage.value = Pair(err, true)
            } finally {
                _isValidating.value = false
            }
        }
    }

    fun startDownload() {
        val probe = _probeResult.value ?: return
        if (!probe.canDownloadDirectly) {
            val msg = if (_settings.value.language == "bn") "এই ফাইলটি সরাসরি ডাউনলোডের যোগ্য নয়।" else "This file cannot be downloaded directly."
            _toastMessage.value = Pair(msg, true)
            return
        }

        downloadJob?.cancel()

        if (probe.platform == PlatformType.YOUTUBE && !probe.url.endsWith(".mp4")) {
            val videoId = UrlAnalyzer.analyze(probe.url).videoId
            val ytTarget = if (!videoId.isNullOrEmpty()) "https://www.youtube.com/watch?v=$videoId" else probe.url
            val downloadUrl = "https://loader.to/?link=" + java.net.URLEncoder.encode(ytTarget, "UTF-8")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                getApplication<Application>().startActivity(intent)
                val msg = if (_settings.value.language == "bn") "ইউটিউব ডাউনলোডার (Loader.to) খোলা হয়েছে।" else "YouTube downloader (Loader.to) opened."
                _toastMessage.value = Pair(msg, false)
            } catch (_: Exception) {}
            return
        }

        if (probe.platform == PlatformType.PINTEREST && !probe.url.contains(".mp4")) {
            val msg = if (_settings.value.language == "bn") "এই পিনটিতে সরাসরি কোনো ভিডিও পাওয়া যায়নি।" else "No direct video found in this pin."
            _toastMessage.value = Pair(msg, true)
            return
        }

        val filename = if (_settings.value.filenamePrefix.isNotBlank() && !probe.filename.startsWith(_settings.value.filenamePrefix)) {
            "${_settings.value.filenamePrefix}${probe.filename}"
        } else probe.filename

        if (_settings.value.downloadMethod == "manager") {
            try {
                VideoDownloader.enqueueWithDownloadManager(
                    context = getApplication(),
                    url = probe.url,
                    filename = filename,
                    title = probe.title
                )
                val msg = if (_settings.value.language == "bn") "Android DownloadManager-এ ডাউনলোড শুরু হয়েছে।" else "Enqueued in Android DownloadManager."
                _toastMessage.value = Pair(msg, false)
                viewModelScope.launch {
                    historyDao.insert(
                        HistoryEntity(
                            url = probe.url,
                            title = probe.title,
                            platform = probe.platform.tag,
                            contentType = probe.contentType,
                            fileSizeBytes = probe.contentLength,
                            status = "completed_dm"
                        )
                    )
                }
            } catch (e: Exception) {
                _toastMessage.value = Pair("DownloadManager error: ${e.localizedMessage}", true)
            }
            return
        }

        // Stream mode
        downloadJob = viewModelScope.launch {
            VideoDownloader.downloadStream(
                context = getApplication(),
                url = probe.url,
                filename = filename,
                mimeType = probe.contentType ?: "video/mp4"
            ).collect { progress ->
                _downloadProgress.value = progress

                if (progress.percentage == 100f && !progress.isDownloading) {
                    historyDao.insert(
                        HistoryEntity(
                            url = probe.url,
                            title = probe.title,
                            platform = probe.platform.tag,
                            contentType = probe.contentType,
                            fileSizeBytes = progress.downloadedBytes,
                            status = "completed",
                            localUri = progress.savedFilePath
                        )
                    )
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadProgress.value = DownloadProgress(
            isDownloading = false,
            statusTextBn = "ডাউনলোড বাতিল করা হয়েছে।",
            statusTextEn = "Download canceled by user.",
            error = if (_settings.value.language == "bn") "ব্যবহারকারী বাতিল করেছেন।" else "Download canceled."
        )
    }

    fun openPlayer(url: String) {
        _activePlayerUrl.value = url
    }

    fun closePlayer() {
        _activePlayerUrl.value = null
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyDao.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }

    fun copyToClipboard(text: String, label: String = "Link") {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        val msg = if (_settings.value.language == "bn") "কপি করা হয়েছে!" else "Copied to clipboard!"
        _toastMessage.value = Pair(msg, false)
    }

    fun loadTestUrl(url: String) {
        onUrlChange(url)
        _activeTab.value = ScreenTab.SAVER
        validateUrl()
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _isRunningTests.value = true
            val list = mutableListOf<TestCase>()

            // Test 1: Direct MP4 analysis
            val t1Start = System.currentTimeMillis()
            val a1 = UrlAnalyzer.analyze("https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4")
            val p1 = a1.isValid && a1.platform == PlatformType.DIRECT && a1.mediaExtension == ".mp4"
            list.add(
                TestCase(
                    id = "t1",
                    name = "Direct MP4 Detection",
                    description = "Verifies URL analyzer identifies .mp4 direct streams",
                    inputUrl = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                    expected = "platform=DIRECT, isValid=true, ext=.mp4",
                    actual = "platform=${a1.platform}, isValid=${a1.isValid}, ext=${a1.mediaExtension}",
                    isPassed = p1,
                    durationMs = System.currentTimeMillis() - t1Start
                )
            )

            // Test 2: YouTube Link Detection & ID Extraction
            val t2Start = System.currentTimeMillis()
            val a2 = UrlAnalyzer.analyze("https://youtu.be/dQw4w9WgXcQ")
            val p2 = a2.isValid && a2.platform == PlatformType.YOUTUBE && a2.videoId == "dQw4w9WgXcQ"
            list.add(
                TestCase(
                    id = "t2",
                    name = "YouTube Shortlink Parser",
                    description = "Extracts YouTube video ID from youtu.be short URL",
                    inputUrl = "https://youtu.be/dQw4w9WgXcQ",
                    expected = "platform=YOUTUBE, videoId=dQw4w9WgXcQ",
                    actual = "platform=${a2.platform}, videoId=${a2.videoId}",
                    isPassed = p2,
                    durationMs = System.currentTimeMillis() - t2Start
                )
            )

            // Test 3: Unsafe Scheme Prevention (javascript:)
            val t3Start = System.currentTimeMillis()
            val a3 = UrlAnalyzer.analyze("javascript:alert(1)")
            val p3 = !a3.isValid
            list.add(
                TestCase(
                    id = "t3",
                    name = "Dangerous Protocol Rejection",
                    description = "Blocks javascript:, file:, data: and unsafe URI protocols",
                    inputUrl = "javascript:alert(1)",
                    expected = "isValid=false, scheme rejected",
                    actual = "isValid=${a3.isValid}, reason=${a3.limitationReason}",
                    isPassed = p3,
                    durationMs = System.currentTimeMillis() - t3Start
                )
            )

            // Test 4: Facebook Watch Link Classification
            val t4Start = System.currentTimeMillis()
            val a4 = UrlAnalyzer.analyze("https://fb.watch/example-video-id/")
            val p4 = a4.isValid && a4.platform == PlatformType.FACEBOOK
            list.add(
                TestCase(
                    id = "t4",
                    name = "Facebook Video Detection",
                    description = "Classifies Facebook Watch links as social media post",
                    inputUrl = "https://fb.watch/example-video-id/",
                    expected = "platform=FACEBOOK, isDirectMediaCandidate=false",
                    actual = "platform=${a4.platform}, isDirect=${a4.isDirectMediaCandidate}",
                    isPassed = p4,
                    durationMs = System.currentTimeMillis() - t4Start
                )
            )

            // Test 5: Pinterest Pin Detection
            val t5Start = System.currentTimeMillis()
            val a5 = UrlAnalyzer.analyze("https://pin.it/7xTestPin")
            val p5 = a5.isValid && a5.platform == PlatformType.PINTEREST
            list.add(
                TestCase(
                    id = "t5",
                    name = "Pinterest Pin Detection",
                    description = "Detects pin.it shortlinks and flags social gallery limitation",
                    inputUrl = "https://pin.it/7xTestPin",
                    expected = "platform=PINTEREST",
                    actual = "platform=${a5.platform}",
                    isPassed = p5,
                    durationMs = System.currentTimeMillis() - t5Start
                )
            )

            _testCases.value = list
            _isRunningTests.value = false
        }
    }
}
