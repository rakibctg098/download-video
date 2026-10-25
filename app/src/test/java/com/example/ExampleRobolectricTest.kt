package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PlatformType
import com.example.network.UrlAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Video Saver", appName)
  }

  @Test
  fun `test direct MP4 link analyzer`() {
    val result = UrlAnalyzer.analyze("https://example.com/video/flower.mp4")
    assertTrue(result.isValid)
    assertEquals(PlatformType.DIRECT, result.platform)
    assertTrue(result.isDirectMediaCandidate)
    assertEquals(".mp4", result.mediaExtension)
  }
}
