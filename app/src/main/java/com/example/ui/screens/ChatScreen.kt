package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiProvidersRegistry
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.repository.AiProviderManager
import com.example.data.repository.AiStudioRepository
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    studioRepository: AiStudioRepository,
    aiProviderManager: AiProviderManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val conversationsFlow = remember { studioRepository.observeConversations() }
    val conversations by conversationsFlow.collectAsState(initial = emptyList())

    var currentConversation by remember { mutableStateOf<Conversation?>(null) }
    var selectedProviderId by remember { mutableStateOf("gemini") }
    var selectedModelId by remember { mutableStateOf("gemini-3.8-flash") }
    var messageInput by remember { mutableStateOf("") }

    val messagesFlow = remember(currentConversation?.id) {
        if (currentConversation != null && currentConversation!!.id.isNotBlank()) {
            studioRepository.observeMessages(currentConversation!!.id)
        } else null
    }
    val messages by (messagesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    var isStreaming by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }
    var generationJob by remember { mutableStateOf<Job?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Conversations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricCyan)
                            .clickable {
                                scope.launch {
                                    val newConvo = Conversation(
                                        title = "New Conversation",
                                        providerId = selectedProviderId,
                                        modelId = selectedModelId
                                    )
                                    val id = studioRepository.createConversation(newConvo)
                                    currentConversation = newConvo.copy(id = id)
                                    drawerState.close()
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Chat", fontWeight = FontWeight.Bold, color = DarkBackground)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(conversations) { convo ->
                            val isSelected = currentConversation?.id == convo.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                                    .clickable {
                                        currentConversation = convo
                                        selectedProviderId = convo.providerId
                                        selectedModelId = convo.modelId
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = convo.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = TextPrimaryDark,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = convo.providerId.uppercase(),
                                        fontSize = 10.sp,
                                        color = ElectricCyan
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            studioRepository.deleteConversation(convo.id)
                                            if (currentConversation?.id == convo.id) {
                                                currentConversation = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.testTag("btn_open_chat_drawer")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = TextPrimaryDark)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = currentConversation?.title ?: "AI Chat",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1
                        )
                        Text(
                            text = "${selectedProviderId.uppercase()} - $selectedModelId",
                            fontSize = 11.sp,
                            color = ElectricCyan
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            val newConvo = Conversation(
                                title = "New Conversation",
                                providerId = selectedProviderId,
                                modelId = selectedModelId
                            )
                            val id = studioRepository.createConversation(newConvo)
                            currentConversation = newConvo.copy(id = id)
                        }
                    },
                    modifier = Modifier.testTag("btn_new_chat")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat", tint = ElectricCyan)
                }
            }

            // Provider Switcher Row
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AiProvidersRegistry.ALL_PROVIDERS) { provider ->
                    val isSelected = selectedProviderId == provider.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedProviderId = provider.id
                            selectedModelId = provider.defaultModelId
                        },
                        label = { Text(provider.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextPrimaryDark
                        )
                    )
                }
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty() && streamingContent.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DarkSurfaceBorder, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Ask anything or generate code", fontSize = 14.sp, color = TextSecondaryDark)
                                Text("Selected provider: ${selectedProviderId.uppercase()}", fontSize = 12.sp, color = ElectricCyan)
                            }
                        }
                    }
                }

                items(messages) { msg ->
                    ChatBubble(
                        message = msg,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.content))
                            Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isStreaming && streamingContent.isNotEmpty()) {
                    item {
                        ChatBubble(
                            message = ChatMessage(
                                role = "model",
                                content = streamingContent,
                                provider = selectedProviderId,
                                model = selectedModelId
                            ),
                            isStreaming = true,
                            onCopy = {}
                        )
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Message ${selectedProviderId.uppercase()}...", fontSize = 13.sp, color = TextSecondaryDark) },
                    modifier = Modifier.weight(1f).testTag("input_chat_message"),
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = DarkSurfaceBorder
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (isStreaming) {
                    IconButton(
                        onClick = {
                            generationJob?.cancel()
                            isStreaming = false
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val textToSend = messageInput
                                messageInput = ""

                                scope.launch {
                                    // Ensure conversation exists
                                    var convo = currentConversation
                                    if (convo == null) {
                                        val newConvo = Conversation(
                                            title = textToSend.take(30),
                                            providerId = selectedProviderId,
                                            modelId = selectedModelId
                                        )
                                        val id = studioRepository.createConversation(newConvo)
                                        convo = newConvo.copy(id = id)
                                        currentConversation = convo
                                    }

                                    // Add User message
                                    studioRepository.addMessage(
                                        ChatMessage(
                                            conversationId = convo.id,
                                            role = "user",
                                            content = textToSend,
                                            provider = selectedProviderId,
                                            model = selectedModelId
                                        )
                                    )

                                    // Trigger AI streaming response
                                    isStreaming = true
                                    streamingContent = ""
                                    val accumulated = StringBuilder()

                                    generationJob = launch {
                                        try {
                                            aiProviderManager.generateCompletionStream(
                                                providerId = selectedProviderId,
                                                modelId = selectedModelId,
                                                prompt = textToSend
                                            ).collect { chunk ->
                                                accumulated.append(chunk)
                                                streamingContent = accumulated.toString()
                                            }

                                            // Save final model message
                                            if (accumulated.isNotBlank()) {
                                                studioRepository.addMessage(
                                                    ChatMessage(
                                                        conversationId = convo.id,
                                                        role = "model",
                                                        content = accumulated.toString(),
                                                        provider = selectedProviderId,
                                                        model = selectedModelId
                                                    )
                                                )
                                            }
                                        } catch (e: Exception) {
                                            studioRepository.addMessage(
                                                ChatMessage(
                                                    conversationId = convo.id,
                                                    role = "model",
                                                    content = "Generation failed: ${e.localizedMessage ?: "Unknown error"}",
                                                    provider = selectedProviderId,
                                                    model = selectedModelId
                                                )
                                            )
                                        } finally {
                                            isStreaming = false
                                            streamingContent = ""
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                            .testTag("btn_send_chat")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = DarkBackground)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isStreaming: Boolean = false,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) ElectricCyan else DarkSurface)
                .border(
                    1.dp,
                    if (isUser) Color.Transparent else DarkSurfaceBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            if (!isUser) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${message.provider.uppercase()} [${message.model}]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                    IconButton(onClick = onCopy, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = message.content,
                fontSize = 14.sp,
                color = if (isUser) DarkBackground else TextPrimaryDark,
                lineHeight = 20.sp,
                fontFamily = if (message.content.contains("```") || message.content.startsWith("fun ") || message.content.startsWith("import ")) FontFamily.Monospace else FontFamily.Default
            )

            if (isStreaming) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = ElectricCyan, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Streaming response...", fontSize = 11.sp, color = ElectricCyan)
                }
            }
        }
    }
}
