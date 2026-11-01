package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AiProvidersRegistry
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("AI Creator Studio", appName)
  }

  @Test
  fun `test ai providers registry integrity`() {
    val providers = AiProvidersRegistry.ALL_PROVIDERS
    assertTrue(providers.size >= 7)
    val gemini = AiProvidersRegistry.getProvider("gemini")
    assertEquals("Google Gemini", gemini.name)
    val claude = AiProvidersRegistry.getProvider("claude")
    assertEquals("Anthropic Claude", claude.name)
    val openai = AiProvidersRegistry.getProvider("openai")
    assertEquals("OpenAI", openai.name)
  }

  @Test
  fun `test project serialization`() {
    val file1 = ProjectFile(path = "src/Main.kt", name = "Main.kt", content = "fun main() {}", language = "kotlin")
    val project = Project(name = "Test App", platform = "android").withUpdatedFiles(listOf(file1))
    val files = project.getFiles()
    assertEquals(1, files.size)
    assertEquals("Main.kt", files[0].name)
  }
}
