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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ConnectionStatus
import com.example.data.repository.AiProviderManager
import com.example.data.repository.AiStudioRepository
import com.example.data.repository.AuthRepository
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

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    aiProviderManager: AiProviderManager,
    studioRepository: AiStudioRepository,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by authRepository.currentUser.collectAsState()
    val connections by aiProviderManager.connections.collectAsState()
    val projectsFlow = remember { studioRepository.observeProjects() }
    val projects by projectsFlow.collectAsState(initial = emptyList())

    val connectedProviders = connections.values.count { it.status == ConnectionStatus.CONNECTED }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PROFILE & SETTINGS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan,
            letterSpacing = 1.2.sp
        )

        Text(
            text = "Studio Account",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Account Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!currentUser?.photoUrl.toString().isNullOrBlank()) {
                    AsyncImage(
                        model = currentUser?.photoUrl,
                        contentDescription = "User profile picture",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, ElectricCyan, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f))
                            .border(2.dp, ElectricCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = currentUser?.displayName ?: "AI Creator",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = currentUser?.email ?: "Google Authenticated",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SECURE GOOGLE SESSION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.Folder,
                value = "${projects.size}",
                label = "Saved Projects",
                color = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Hub,
                value = "$connectedProviders / 7",
                label = "AI Engines Connected",
                color = NeonPurple,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Section
        Text(
            text = "Platform Settings",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dark Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brightness4, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Dark Theme", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                            Text("Default high-contrast obsidian mode", fontSize = 11.sp, color = TextSecondaryDark)
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = ElectricCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security & Zero-Trust Notice
        Text(
            text = "Security & Cloud Architecture",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SecurityItem(
                    icon = Icons.Default.Security,
                    title = "Firebase App Check",
                    subtitle = "Attestation token verified; backend protects against unauthorized clients."
                )
                Spacer(modifier = Modifier.height(12.dp))
                SecurityItem(
                    icon = Icons.Default.Lock,
                    title = "Firestore Zero-Trust Rules",
                    subtitle = "User data isolated per authenticated UID with granular security rules."
                )
                Spacer(modifier = Modifier.height(12.dp))
                SecurityItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Client-Side Secret Isolation",
                    subtitle = "No password collection or reverse proxy credential harvesting."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        Button(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = AccentPink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_profile_logout")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = AccentPink, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out of Studio", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DarkSurface,
            title = { Text("Sign Out", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = { Text("Are you sure you want to sign out? Your cloud projects will remain securely saved in Firestore.", color = TextSecondaryDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authRepository.signOut {
                            Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPink, contentColor = Color.White)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Text(text = label, fontSize = 11.sp, color = TextSecondaryDark)
        }
    }
}

@Composable
private fun SecurityItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(AccentGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondaryDark, lineHeight = 15.sp)
        }
    }
}
