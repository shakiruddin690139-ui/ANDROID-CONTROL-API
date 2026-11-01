package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

data class ProjectFile(
    val path: String = "",
    val name: String = "",
    val content: String = "",
    val language: String = "text",
    val isFolder: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("path", path)
            put("name", name)
            put("content", content)
            put("language", language)
            put("isFolder", isFolder)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ProjectFile {
            return ProjectFile(
                path = json.optString("path", ""),
                name = json.optString("name", ""),
                content = json.optString("content", ""),
                language = json.optString("language", "text"),
                isFolder = json.optBoolean("isFolder", false)
            )
        }
    }
}

@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val platform: String = "android", // android, react, web, python, kotlin, ai, video
    val prompt: String = "",
    val status: String = "ready", // ready, generating, draft
    val filesJson: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun getFiles(): List<ProjectFile> {
        return try {
            val list = mutableListOf<ProjectFile>()
            val array = JSONArray(filesJson)
            for (i in 0 until array.length()) {
                list.add(ProjectFile.fromJson(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun withUpdatedFiles(newFiles: List<ProjectFile>): Project {
        val array = JSONArray()
        newFiles.forEach { array.put(it.toJson()) }
        return this.copy(filesJson = array.toString())
    }
}
