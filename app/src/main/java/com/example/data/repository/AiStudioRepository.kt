package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.Project
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "AiStudioRepository"

class AiStudioRepository(private val context: Context) {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    }

    private fun requireUserId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated with Google to access projects.")
    }

    // Projects CRUD
    fun observeProjects(): Flow<List<Project>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/projects"
        val query = firestore.collection("users").document(uid).collection("projects")
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(Project::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createProject(project: Project): String {
        val uid = requireUserId()
        val collectionRef = firestore.collection("users").document(uid).collection("projects")
        val docRef = if (project.id.isNotBlank()) collectionRef.document(project.id) else collectionRef.document()
        val finalId = docRef.id

        val payload = hashMapOf<String, Any>(
            "id" to finalId,
            "userId" to uid,
            "name" to project.name,
            "description" to project.description,
            "platform" to project.platform,
            "status" to project.status,
            "filesJson" to project.filesJson,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return finalId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProject(project: Project) {
        val uid = requireUserId()
        require(project.id.isNotBlank()) { "Project ID cannot be blank for update" }
        val docRef = firestore.collection("users").document(uid).collection("projects").document(project.id)

        val updates = hashMapOf<String, Any>(
            "name" to project.name,
            "description" to project.description,
            "platform" to project.platform,
            "status" to project.status,
            "filesJson" to project.filesJson,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProject(projectId: String) {
        val uid = requireUserId()
        val docRef = firestore.collection("users").document(uid).collection("projects").document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Conversations CRUD
    fun observeConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/conversations"
        val query = firestore.collection("users").document(uid).collection("conversations")
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(Conversation::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createConversation(conversation: Conversation): String {
        val uid = requireUserId()
        val collectionRef = firestore.collection("users").document(uid).collection("conversations")
        val docRef = if (conversation.id.isNotBlank()) collectionRef.document(conversation.id) else collectionRef.document()
        val finalId = docRef.id

        val payload = hashMapOf<String, Any>(
            "id" to finalId,
            "userId" to uid,
            "title" to conversation.title,
            "providerId" to conversation.providerId,
            "modelId" to conversation.modelId,
            "lastMessage" to conversation.lastMessage,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return finalId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateConversation(conversationId: String, title: String, lastMessage: String) {
        val uid = requireUserId()
        val docRef = firestore.collection("users").document(uid).collection("conversations").document(conversationId)
        val updates = hashMapOf<String, Any>(
            "title" to title,
            "lastMessage" to lastMessage,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteConversation(conversationId: String) {
        val uid = requireUserId()
        val docRef = firestore.collection("users").document(uid).collection("conversations").document(conversationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Messages CRUD
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/conversations/$conversationId/messages"
        val query = firestore.collection("users").document(uid)
            .collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(ChatMessage::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addMessage(message: ChatMessage): String {
        val uid = requireUserId()
        val collectionRef = firestore.collection("users").document(uid)
            .collection("conversations").document(message.conversationId)
            .collection("messages")

        val docRef = if (message.id.isNotBlank()) collectionRef.document(message.id) else collectionRef.document()
        val finalId = docRef.id

        val payload = hashMapOf<String, Any>(
            "id" to finalId,
            "conversationId" to message.conversationId,
            "userId" to uid,
            "role" to message.role,
            "content" to message.content,
            "provider" to message.provider,
            "model" to message.model,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            // Also update parent conversation's lastMessage and updatedAt
            firestore.collection("users").document(uid)
                .collection("conversations").document(message.conversationId)
                .update(
                    mapOf(
                        "lastMessage" to message.content.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            return finalId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
