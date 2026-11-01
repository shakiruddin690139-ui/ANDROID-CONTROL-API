package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiProvidersRegistry
import com.example.data.model.ConnectionStatus
import com.example.data.model.Project
import com.example.data.repository.AiProviderManager
import com.example.data.repository.AiStudioRepository
import com.example.data.repository.AuthRepository
import com.example.ui.components.StudioScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun HomeScreen(
    authRepository: AuthRepository,
    aiProviderManager: AiProviderManager,
    studioRepository: AiStudioRepository,
    onNavigate: (StudioScreen) -> Unit,
    onOpenCreateWithPrompt: (String, String) -> Unit,
    onOpenProject: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val connections by aiProviderManager.connections.collectAsState()
    val projectsFlow = remember { studioRepository.observeProjects() }
    val projects by projectsFlow.collectAsState(initial = emptyList())

    var quickPrompt by remember { mutableStateOf("") }

    val connectedCount = connections.values.count { it.status == ConnectionStatus.CONNECTED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
                Text(
                    text = currentUser?.displayName ?: "Creator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            // Connection Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                    .clickable { onNavigate(StudioScreen.MODELS) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("btn_models_status_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (connectedCount > 0) AccentGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$connectedCount Connected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Banner: AI App Builder Launchpad
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(ElectricCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.5f)))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricCyan.copy(alpha = 0.08f), NeonPurple.copy(alpha = 0.08f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI APP BUILDER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Transform natural language into production code.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text(
                    text = "Generate Android, React, Python, or Kotlin projects with automated file structuring.",
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onNavigate(StudioScreen.CREATE) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_hero_launch_create")
                ) {
                    Text(
                        text = "Build New App",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick AI Prompt Bar
        Text(
            text = "Quick Creation",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = quickPrompt,
                onValueChange = { quickPrompt = it },
                placeholder = { Text("e.g. Create a sleek video editor in Compose", fontSize = 13.sp, color = TextSecondaryDark) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_quick_prompt"),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurface,
                    focusedIndicatorColor = ElectricCyan,
                    unfocusedIndicatorColor = DarkSurfaceBorder
                ),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (quickPrompt.isNotBlank()) {
                        onOpenCreateWithPrompt(quickPrompt, "android")
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricCyan)
                    .testTag("btn_submit_quick_prompt")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Submit prompt",
                    tint = DarkBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Platform Launchers
        Text(
            text = "Project Templates",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickTemplateCard(
                icon = Icons.Default.Android,
                title = "Android",
                subtitle = "Compose",
                tint = Color(0xFF3DDC84),
                modifier = Modifier.weight(1f),
                onClick = { onOpenCreateWithPrompt("Create a modern Android application with Jetpack Compose", "android") }
            )
            QuickTemplateCard(
                icon = Icons.Default.Language,
                title = "Web / React",
                subtitle = "Vite App",
                tint = Color(0xFF61DAFB),
                modifier = Modifier.weight(1f),
                onClick = { onOpenCreateWithPrompt("Build a responsive web application dashboard in React", "react") }
            )
            QuickTemplateCard(
                icon = Icons.Default.Terminal,
                title = "Python",
                subtitle = "FastAPI / Script",
                tint = Color(0xFFFFD43B),
                modifier = Modifier.weight(1f),
                onClick = { onOpenCreateWithPrompt("Create a high performance Python FastAPI backend service", "python") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Projects Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Projects (${projects.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
            if (projects.isNotEmpty()) {
                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    color = ElectricCyan,
                    modifier = Modifier.clickable { onNavigate(StudioScreen.PROJECTS) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (projects.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No projects yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Use the AI App Builder above to generate your first software project.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(projects.take(5)) { proj ->
                    ProjectSnippetCard(
                        project = proj,
                        onClick = { onOpenProject(proj) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickTemplateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
private fun ProjectSnippetCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElectricCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = project.platform.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = project.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 1
            )
            Text(
                text = project.description.ifBlank { "Generated project" },
                fontSize = 12.sp,
                color = TextSecondaryDark,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${project.getFiles().size} files",
                fontSize = 11.sp,
                color = NeonPurple
            )
        }
    }
}
