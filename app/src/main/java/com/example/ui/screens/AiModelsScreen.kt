package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiProviderInfo
import com.example.data.model.AiProvidersRegistry
import com.example.data.model.ConnectionStatus
import com.example.data.model.ConnectionType
import com.example.data.model.ProviderConnection
import com.example.data.repository.AiProviderManager
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPink
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

@Composable
fun AiModelsScreen(
    aiProviderManager: AiProviderManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connections by aiProviderManager.connections.collectAsState()

    var selectedProviderForDialog by remember { mutableStateOf<AiProviderInfo?>(null) }
    var testingProviderId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AI PROVIDER CENTER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan,
            letterSpacing = 1.2.sp
        )

        Text(
            text = "Connected Intelligence",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )

        Text(
            text = "Manage authentication, API keys, and models for all supported AI engines.",
            fontSize = 13.sp,
            color = TextSecondaryDark,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(AiProvidersRegistry.ALL_PROVIDERS) { provider ->
                val connection = connections[provider.id] ?: ProviderConnection(providerId = provider.id)
                val isTesting = testingProviderId == provider.id

                ProviderCard(
                    provider = provider,
                    connection = connection,
                    isTesting = isTesting,
                    onConfigure = { selectedProviderForDialog = provider },
                    onTestConnection = {
                        testingProviderId = provider.id
                        scope.launch {
                            val res = aiProviderManager.testProviderConnection(provider.id)
                            testingProviderId = null
                            if (res.isSuccess) {
                                Toast.makeText(context, "${provider.name} connected (${res.getOrNull()}ms)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Connection failed: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onDisconnect = {
                        aiProviderManager.disconnectProvider(provider.id)
                        Toast.makeText(context, "${provider.name} disconnected", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Configure Provider Dialog (API Key / OAuth)
    selectedProviderForDialog?.let { provider ->
        val currentConn = connections[provider.id]
        var inputKey by remember { mutableStateOf(currentConn?.apiKey.orEmpty()) }
        var selectedModel by remember { mutableStateOf(currentConn?.activeModelId ?: provider.defaultModelId) }

        AlertDialog(
            onDismissRequest = { selectedProviderForDialog = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Configure ${provider.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column {
                    Text(
                        text = provider.description,
                        fontSize = 13.sp,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (provider.connectionType == ConnectionType.NATIVE_APP_CHECK) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentGreen.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Connected via Firebase AI Logic & App Check. Zero client key exposure. Protected by server attestation.",
                                fontSize = 12.sp,
                                color = AccentGreen
                            )
                        }
                    } else {
                        Text(
                            text = "API Key",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = { inputKey = it },
                            placeholder = { Text("Enter your ${provider.name} secret key", fontSize = 12.sp, color = TextSecondaryDark) },
                            modifier = Modifier.fillMaxWidth().testTag("input_provider_key"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedIndicatorColor = ElectricCyan,
                                unfocusedIndicatorColor = DarkSurfaceBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(provider.officialAuthUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Get official key from ${provider.name}",
                                fontSize = 12.sp,
                                color = ElectricCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Active Model",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    provider.supportedModels.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedModel = m.id }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (selectedModel == m.id) ElectricCyan else DarkSurfaceBorder,
                                        CircleShape
                                    )
                                    .background(if (selectedModel == m.id) ElectricCyan else Color.Transparent)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = m.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedModel == m.id) FontWeight.Bold else FontWeight.Normal,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "${m.contextWindow} - ${m.description}",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (provider.connectionType != ConnectionType.NATIVE_APP_CHECK) {
                            aiProviderManager.saveApiKey(provider.id, inputKey)
                        }
                        aiProviderManager.setModel(provider.id, selectedModel)
                        selectedProviderForDialog = null
                        Toast.makeText(context, "${provider.name} settings saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_save_provider_config")
                ) {
                    Text("Save & Connect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProviderForDialog = null }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun ProviderCard(
    provider: AiProviderInfo,
    connection: ProviderConnection,
    isTesting: Boolean,
    onConfigure: () -> Unit,
    onTestConnection: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (connection.status == ConnectionStatus.CONNECTED) ElectricCyan.copy(alpha = 0.3f) else DarkSurfaceBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = provider.tagline,
                        fontSize = 12.sp,
                        color = ElectricCyan
                    )
                }

                // Status Pill
                StatusBadge(status = connection.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = provider.description,
                fontSize = 12.sp,
                color = TextSecondaryDark,
                lineHeight = 16.sp
            )

            if (connection.latencyMs > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Latency: ${connection.latencyMs}ms",
                        fontSize = 11.sp,
                        color = AccentGreen
                    )
                    if (connection.activeModelId.isNotBlank()) {
                        Text(
                            text = " | Active: ${connection.activeModelId}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            if (connection.errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Error: ${connection.errorMessage}",
                    fontSize = 11.sp,
                    color = AccentPink
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (connection.status == ConnectionStatus.CONNECTED) {
                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !isTesting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_test_conn_${provider.id}")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = ElectricCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Test Ping", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = onDisconnect,
                        modifier = Modifier.testTag("btn_disconnect_${provider.id}")
                    ) {
                        Text("Disconnect", color = AccentPink, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = onConfigure,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connection.status == ConnectionStatus.CONNECTED) DarkSurfaceElevated else ElectricCyan,
                        contentColor = if (connection.status == ConnectionStatus.CONNECTED) TextPrimaryDark else DarkBackground
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_configure_${provider.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (connection.status == ConnectionStatus.CONNECTED) "Manage" else "Connect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ConnectionStatus) {
    val (bgColor, textColor, text) = when (status) {
        ConnectionStatus.CONNECTED -> Triple(AccentGreen.copy(alpha = 0.15f), AccentGreen, "CONNECTED")
        ConnectionStatus.DISCONNECTED -> Triple(Color.Gray.copy(alpha = 0.2f), Color.LightGray, "NOT CONNECTED")
        ConnectionStatus.CONFIGURING -> Triple(ElectricCyan.copy(alpha = 0.15f), ElectricCyan, "CONFIGURING")
        ConnectionStatus.ERROR -> Triple(AccentPink.copy(alpha = 0.15f), AccentPink, "ERROR")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.8.sp
        )
    }
}
