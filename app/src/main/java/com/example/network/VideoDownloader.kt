package com.example.network

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.DownloadProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

object VideoDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Real-time streaming download with chunk-based progress and speed calculation.
     * Prevents Android 4GB RAM out-of-memory errors by streaming directly to disk without heap caching.
     */
    fun downloadStream(
        context: Context,
        url: String,
        filename: String,
        mimeType: String = "video/mp4"
    ): Flow<DownloadProgress> = flow {
        emit(
            DownloadProgress(
                isDownloading = true,
                downloadedBytes = 0,
                totalBytes = null,
                percentage = 0f,
                speedBytesPerSec = 0,
                statusTextBn = "মিডিয়া সার্ভারের সাথে সংযোগ করা হচ্ছে...",
                statusTextEn = "Connecting to media stream..."
            )
        )

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) VideoSaver/2.0")
            .header("Accept", "*/*")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            emit(
                DownloadProgress(
                    isDownloading = false,
                    error = "HTTP error: ${response.code} (${response.message})"
                )
            )
            return@flow
        }

        val body = response.body
        if (body == null) {
            emit(
                DownloadProgress(
                    isDownloading = false,
                    error = "Empty response body from media server."
                )
            )
            return@flow
        }

        val totalBytes = body.contentLength().takeIf { it > 0 }
        val safeFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        // Prepare target file: write to external app cache first, then copy to MediaStore / Public Downloads
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val tempFile = File(targetDir, safeFilename)

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(32 * 1024) // 32 KB chunk size (optimal for Android 4GB RAM)
            var bytesRead: Int
            var totalRead: Long = 0
            var lastTime = System.currentTimeMillis()
            var lastBytes: Long = 0
            var speed: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = (now - lastTime) / 1000.0
                if (elapsed >= 0.3) {
                    val delta = totalRead - lastBytes
                    speed = (delta / elapsed).toLong()
                    lastTime = now
                    lastBytes = totalRead

                    val pct = if (totalBytes != null && totalBytes > 0) {
                        (totalRead.toFloat() / totalBytes.toFloat()) * 100f
                    } else null

                    emit(
                        DownloadProgress(
                            isDownloading = true,
                            downloadedBytes = totalRead,
                            totalBytes = totalBytes,
                            percentage = pct,
                            speedBytesPerSec = speed,
                            statusTextBn = "ভিডিও ডাউনলোড হচ্ছে...",
                            statusTextEn = "Downloading video chunks..."
                        )
                    )
                }
            }
            outputStream.flush()

            // Save to MediaStore so it appears in device Gallery and Google Files
            val savedUri = saveToPublicDownloads(context, tempFile, safeFilename, mimeType)

            emit(
                DownloadProgress(
                    isDownloading = false,
                    downloadedBytes = totalRead,
                    totalBytes = totalRead,
                    percentage = 100f,
                    speedBytesPerSec = 0,
                    statusTextBn = "ডাউনলোড সফল হয়েছে! 'Downloads' ফোল্ডারে সেভ করা হয়েছে।",
                    statusTextEn = "Download complete! Saved to Android Downloads folder.",
                    savedFilePath = savedUri ?: tempFile.absolutePath
                )
            )
        } catch (e: CancellationException) {
            tempFile.delete()
            emit(
                DownloadProgress(
                    isDownloading = false,
                    statusTextBn = "ব্যবহারকারী ডাউনলোড বাতিল করেছেন।",
                    statusTextEn = "Download canceled by user.",
                    error = "Download canceled."
                )
            )
        } catch (e: Exception) {
            tempFile.delete()
            emit(
                DownloadProgress(
                    isDownloading = false,
                    error = e.localizedMessage ?: "Unknown download failure"
                )
            )
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Copy file into public Android Downloads directory via MediaStore or File API.
     */
    private fun saveToPublicDownloads(
        context: Context,
        sourceFile: File,
        filename: String,
        mimeType: String
    ): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VideoSaver")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { inp ->
                            inp.copyTo(out)
                        }
                    }
                    uri.toString()
                } else {
                    sourceFile.absolutePath
                }
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(publicDir, filename)
                sourceFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        } catch (e: Exception) {
            sourceFile.absolutePath
        }
    }

    /**
     * Triggers Android system DownloadManager
     */
    fun enqueueWithDownloadManager(
        context: Context,
        url: String,
        filename: String,
        title: String
    ): Long {
        val safeFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(title)
            setDescription("Video Saver Downloading...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFilename)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return manager.enqueue(request)
    }
}
