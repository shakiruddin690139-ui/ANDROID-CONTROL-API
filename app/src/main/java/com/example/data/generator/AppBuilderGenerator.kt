package com.example.data.generator

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import com.example.data.repository.AiProviderManager
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val TAG = "AppBuilderGenerator"

class AppBuilderGenerator(
    private val context: Context,
    private val aiProviderManager: AiProviderManager
) {

    suspend fun generateProject(
        name: String,
        description: String,
        platform: String,
        preferredProvider: String = "gemini",
        preferredModel: String = "gemini-3.8-flash"
    ): Project = withContext(Dispatchers.IO) {
        val sanitizedPlatform = platform.lowercase()
        val prompt = """
            You are a master software architect and code generation engine.
            The user wants to generate a complete, working, high-quality application.
            
            PROJECT NAME: $name
            PLATFORM/FRAMEWORK: $sanitizedPlatform
            USER REQUIREMENTS: $description
            
            Generate a comprehensive project with clean architecture and working code.
            Return a JSON object with:
            {
              "description": "Short summary of the generated architecture",
              "files": [
                {
                  "path": "relative/path/to/File.ext",
                  "name": "File.ext",
                  "language": "kotlin|javascript|typescript|python|html|css|json",
                  "content": "Full source code here without markdown fences"
                }
              ]
            }
            Include at least 4-6 essential files that make up a complete foundation.
        """.trimIndent()

        var files = mutableListOf<ProjectFile>()
        var generatedDesc = description

        try {
            // Attempt AI generation with Gemini or connected provider
            if (preferredProvider == "gemini") {
                val model = Firebase.ai.generativeModel(
                    modelName = if (preferredModel.isNotBlank()) preferredModel else "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    }
                )

                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }

                val jsonText = response.text.orEmpty()
                if (jsonText.isNotBlank()) {
                    val root = JSONObject(jsonText)
                    generatedDesc = root.optString("description", description)
                    val filesArray = root.optJSONArray("files")
                    if (filesArray != null) {
                        for (i in 0 until filesArray.length()) {
                            val f = filesArray.getJSONObject(i)
                            files.add(
                                ProjectFile(
                                    path = f.optString("path", "file_$i.txt"),
                                    name = f.optString("name", "file_$i.txt"),
                                    content = f.optString("content", ""),
                                    language = f.optString("language", "text")
                                )
                            )
                        }
                    }
                }
            } else {
                // Try streaming/unary from provider manager
                val text = aiProviderManager.generateCompletionStream(
                    providerId = preferredProvider,
                    modelId = preferredModel,
                    prompt = prompt,
                    systemInstruction = "You must return valid JSON only adhering to {description, files: [{path, name, language, content}]} without markdown wrappers."
                ).first()

                val root = JSONObject(text.trim())
                val filesArray = root.optJSONArray("files")
                if (filesArray != null) {
                    for (i in 0 until filesArray.length()) {
                        val f = filesArray.getJSONObject(i)
                        files.add(
                            ProjectFile(
                                path = f.optString("path", "file_$i.txt"),
                                name = f.optString("name", "file_$i.txt"),
                                content = f.optString("content", ""),
                                language = f.optString("language", "text")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AI model generation threw exception or returned empty, generating specialized template: ${e.message}")
            // Fallback to high-quality curated template tailored to the requested platform
            files = generateCuratedTemplateFiles(name, description, sanitizedPlatform).toMutableList()
        }

        if (files.isEmpty()) {
            files = generateCuratedTemplateFiles(name, description, sanitizedPlatform).toMutableList()
        }

        Project(
            name = name,
            description = generatedDesc,
            platform = sanitizedPlatform,
            prompt = description,
            status = "ready",
            filesJson = JSONArray().apply { files.forEach { put(it.toJson()) } }.toString()
        )
    }

    suspend fun explainCode(file: ProjectFile, userPrompt: String = ""): String = withContext(Dispatchers.IO) {
        val prompt = """
            Analyze the following source code file and provide a clear, professional technical explanation.
            File: ${file.path} (${file.language})
            Code:
            ${file.content}
            
            Specific User Question: ${userPrompt.ifBlank { "Provide architectural summary, key functions, and design patterns used." }}
        """.trimIndent()

        try {
            val model = Firebase.ai.generativeModel("gemini-3.8-flash")
            val resp = withTimeout(45_000L) { model.generateContent(prompt) }
            resp.text ?: "No explanation generated."
        } catch (e: Exception) {
            "Analysis unavailable: ${e.localizedMessage ?: "Network or quota limit reached."}"
        }
    }

    suspend fun debugCode(file: ProjectFile): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are an expert static analyzer and security auditor.
            Review this code for bugs, logic defects, memory leaks, security vulnerabilities, or bad practices.
            File: ${file.path}
            Code:
            ${file.content}
            
            Format your response clearly:
            1. Identified Issues (if any)
            2. Severity (High, Medium, Low)
            3. Recommended Fix
            4. Corrected Code Snippet
        """.trimIndent()

        try {
            val model = Firebase.ai.generativeModel("gemini-3.8-flash")
            val resp = withTimeout(45_000L) { model.generateContent(prompt) }
            resp.text ?: "No bugs detected."
        } catch (e: Exception) {
            "Debugger unavailable: ${e.localizedMessage ?: "Network or quota limit reached."}"
        }
    }

    suspend fun improveCode(file: ProjectFile): String = withContext(Dispatchers.IO) {
        val prompt = """
            Refactor and optimize the following source code for performance, readability, and modern industry standards:
            File: ${file.path}
            Code:
            ${file.content}
            
            Provide the refactored, upgraded source code along with a brief explanation of improvements made.
        """.trimIndent()

        try {
            val model = Firebase.ai.generativeModel("gemini-3.8-flash")
            val resp = withTimeout(45_000L) { model.generateContent(prompt) }
            resp.text ?: file.content
        } catch (e: Exception) {
            "Improvement suggestions unavailable: ${e.localizedMessage}"
        }
    }

    // ZIP Export
    fun exportProjectAsZip(project: Project): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            project.getFiles().forEach { file ->
                if (!file.isFolder) {
                    val entry = ZipEntry(file.path.ifBlank { file.name })
                    zos.putNextEntry(entry)
                    zos.write(file.content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
        }
        return baos.toByteArray()
    }

    // ZIP Import from InputStream
    fun importProjectFromZip(inputStream: InputStream, projectName: String): Project {
        val files = mutableListOf<ProjectFile>()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val content = zis.readBytes().toString(Charsets.UTF_8)
                    val filename = entry.name.substringAfterLast('/')
                    val ext = filename.substringAfterLast('.', "txt")
                    val lang = when (ext) {
                        "kt" -> "kotlin"
                        "java" -> "java"
                        "js" -> "javascript"
                        "jsx", "tsx" -> "react"
                        "ts" -> "typescript"
                        "py" -> "python"
                        "html" -> "html"
                        "css" -> "css"
                        "json" -> "json"
                        else -> "text"
                    }
                    files.add(
                        ProjectFile(
                            path = entry.name,
                            name = filename,
                            content = content,
                            language = lang
                        )
                    )
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return Project(
            name = projectName,
            description = "Imported ZIP archive with ${files.size} files",
            platform = detectPlatformFromFiles(files),
            status = "ready",
            filesJson = JSONArray().apply { files.forEach { put(it.toJson()) } }.toString()
        )
    }

    private fun detectPlatformFromFiles(files: List<ProjectFile>): String {
        return when {
            files.any { it.name.endsWith(".kt") || it.name.contains("AndroidManifest.xml") } -> "android"
            files.any { it.name.endsWith(".jsx") || it.name.endsWith(".tsx") } -> "react"
            files.any { it.name.endsWith(".py") } -> "python"
            files.any { it.name.endsWith(".html") } -> "web"
            else -> "custom"
        }
    }

    private fun generateCuratedTemplateFiles(
        name: String,
        description: String,
        platform: String
    ): List<ProjectFile> {
        val sanitized = name.replace(" ", "")
        return when (platform) {
            "android" -> listOf(
                ProjectFile(
                    path = "app/src/main/java/com/app/MainActivity.kt",
                    name = "MainActivity.kt",
                    language = "kotlin",
                    content = """
                        package com.app

                        import android.os.Bundle
                        import androidx.activity.ComponentActivity
                        import androidx.activity.compose.setContent
                        import androidx.compose.foundation.layout.*
                        import androidx.compose.material3.*
                        import androidx.compose.runtime.*
                        import androidx.compose.ui.Alignment
                        import androidx.compose.ui.Modifier
                        import androidx.compose.ui.unit.dp

                        class MainActivity : ComponentActivity() {
                            override fun onCreate(savedInstanceState: Bundle?) {
                                super.onCreate(savedInstanceState)
                                setContent {
                                    MaterialTheme {
                                        Surface(modifier = Modifier.fillMaxSize()) {
                                            AppContent()
                                        }
                                    }
                                }
                            }
                        }

                        @Composable
                        fun AppContent() {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$name",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "$description",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { /* Primary Action */ }) {
                                    Text("Explore Studio")
                                }
                            }
                        }
                    """.trimIndent()
                ),
                ProjectFile(
                    path = "app/build.gradle.kts",
                    name = "build.gradle.kts",
                    language = "kotlin",
                    content = """
                        plugins {
                            id("com.android.application")
                            id("org.jetbrains.kotlin.android")
                        }

                        android {
                            namespace = "com.app.$sanitized"
                            compileSdk = 35

                            defaultConfig {
                                applicationId = "com.app.$sanitized"
                                minSdk = 24
                                targetSdk = 35
                                versionCode = 1
                                versionName = "1.0"
                            }
                        }
                    """.trimIndent()
                ),
                ProjectFile(
                    path = "app/src/main/AndroidManifest.xml",
                    name = "AndroidManifest.xml",
                    language = "xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                            <uses-permission android:name="android.permission.INTERNET" />
                            <application
                                android:allowBackup="true"
                                android:label="$name"
                                android:theme="@android:style/Theme.Material.NoActionBar">
                                <activity
                                    android:name=".MainActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="android.intent.action.MAIN" />
                                        <category android:name="android.intent.category.LAUNCHER" />
                                    </intent-filter>
                                </activity>
                            </application>
                        </manifest>
                    """.trimIndent()
                )
            )

            "react" -> listOf(
                ProjectFile(
                    path = "src/App.jsx",
                    name = "App.jsx",
                    language = "react",
                    content = """
                        import React, { useState } from 'react';
                        import './App.css';

                        export default function App() {
                          const [count, setCount] = useState(0);

                          return (
                            <div className="container">
                              <header className="hero">
                                <h1>$name</h1>
                                <p>$description</p>
                                <button className="btn-primary" onClick={() => setCount(c => c + 1)}>
                                  Clicked: {count} times
                                </button>
                              </header>
                            </div>
                          );
                        }
                    """.trimIndent()
                ),
                ProjectFile(
                    path = "src/App.css",
                    name = "App.css",
                    language = "css",
                    content = """
                        .container {
                          min-height: 100vh;
                          background: #0b0f19;
                          color: #f8fafc;
                          display: flex;
                          align-items: center;
                          justify-content: center;
                          font-family: system-ui, sans-serif;
                        }
                        .hero {
                          text-align: center;
                          max-width: 600px;
                          padding: 2rem;
                        }
                        .btn-primary {
                          background: #00e5ff;
                          color: #0b0f19;
                          font-weight: bold;
                          border: none;
                          padding: 0.75rem 1.5rem;
                          border-radius: 8px;
                          cursor: pointer;
                        }
                    """.trimIndent()
                ),
                ProjectFile(
                    path = "package.json",
                    name = "package.json",
                    language = "json",
                    content = """
                        {
                          "name": "${sanitized.lowercase()}",
                          "private": true,
                          "version": "1.0.0",
                          "scripts": {
                            "dev": "vite",
                            "build": "vite build"
                          },
                          "dependencies": {
                            "react": "^18.3.1",
                            "react-dom": "^18.3.1"
                          }
                        }
                    """.trimIndent()
                )
            )

            "python" -> listOf(
                ProjectFile(
                    path = "main.py",
                    name = "main.py",
                    language = "python",
                    content = """
                        from fastapi import FastAPI
                        from pydantic import BaseModel

                        app = FastAPI(title="$name", description="$description")

                        class TaskRequest(BaseModel):
                            prompt: str

                        @app.get("/")
                        def root():
                            return {
                                "status": "online",
                                "service": "$name",
                                "version": "1.0.0"
                            }

                        @app.post("/api/execute")
                        def execute_task(req: TaskRequest):
                            return {
                                "input": req.prompt,
                                "result": f"Executed processing for {req.prompt}"
                            }
                    """.trimIndent()
                ),
                ProjectFile(
                    path = "requirements.txt",
                    name = "requirements.txt",
                    language = "text",
                    content = "fastapi>=0.111.0\nuvicorn>=0.30.0\npydantic>=2.7.0\nrequests>=2.32.0"
                )
            )

            else -> listOf(
                ProjectFile(
                    path = "index.html",
                    name = "index.html",
                    language = "html",
                    content = """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                          <title>$name</title>
                          <link rel="stylesheet" href="style.css">
                        </head>
                        <body>
                          <main class="card">
                            <h1>$name</h1>
                            <p>$description</p>
                            <button id="actionBtn">Launch Studio</button>
                          </main>
                          <script src="script.js"></script>
                        </body>
                        </html>
                    """.trimIndent()
                ),
                ProjectFile(
                    path = "style.css",
                    name = "style.css",
                    language = "css",
                    content = """
                        body {
                          margin: 0;
                          background: #0b0f19;
                          color: #f8fafc;
                          font-family: sans-serif;
                          display: grid;
                          place-content: center;
                          height: 100vh;
                        }
                        .card {
                          background: #131b2e;
                          padding: 2rem;
                          border-radius: 16px;
                          border: 1px solid #263352;
                          text-align: center;
                        }
                        button {
                          background: #00e5ff;
                          color: #0b0f19;
                          border: none;
                          padding: 10px 20px;
                          font-weight: bold;
                          border-radius: 8px;
                          cursor: pointer;
                        }
                    """.trimIndent()
                ),
                ProjectFile(
                    path = "script.js",
                    name = "script.js",
                    language = "javascript",
                    content = """
                        document.getElementById('actionBtn').addEventListener('click', () => {
                          alert('Welcome to $name!');
                        });
                    """.trimIndent()
                )
            )
        }
    }
}
