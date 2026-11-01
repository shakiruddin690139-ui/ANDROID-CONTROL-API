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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.generator.AppBuilderGenerator
import com.example.data.model.AiProvidersRegistry
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import com.example.data.repository.AiProviderManager
import com.example.data.repository.AiStudioRepository
import com.example.ui.components.CodeViewer
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
fun CreateAppScreen(
    appBuilderGenerator: AppBuilderGenerator,
    studioRepository: AiStudioRepository,
    aiProviderManager: AiProviderManager,
    initialPrompt: String = "",
    initialPlatform: String = "android",
    onProjectSaved: (Project) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connections by aiProviderManager.connections.collectAsState()

    var projectName by remember { mutableStateOf("New Project") }
    var prompt by remember(initialPrompt) { mutableStateOf(initialPrompt.ifBlank { "Create a modern, feature-rich application with intuitive UI and clean architecture." }) }
    var selectedPlatform by remember(initialPlatform) { mutableStateOf(initialPlatform) }
    var selectedProviderId by remember { mutableStateOf("gemini") }

    val platforms = listOf(
        "android" to "Android (Compose)",
        "react" to "React Web",
        "web" to "HTML/CSS/JS",
        "python" to "Python (FastAPI)",
        "kotlin" to "Kotlin Service",
        "video" to "Video Editor",
        "ai" to "AI Agent Pipeline"
    )

    var isGenerating by remember { mutableStateOf(false) }
    var generatedProject by remember { mutableStateOf<Project?>(null) }
    var selectedFileIndex by remember { mutableIntStateOf(0) }

    // Analysis Tool Tabs (0: Code, 1: Explainer, 2: Bug Detector, 3: Improver)
    var selectedToolTab by remember { mutableIntStateOf(0) }
    var analysisResult by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "AI APP BUILDER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan,
            letterSpacing = 1.2.sp
        )

        Text(
            text = "Generate Any Application",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Project Name
        Text(
            text = "Project Name",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = projectName,
            onValueChange = { projectName = it },
            modifier = Modifier.fillMaxWidth().testTag("input_project_name"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedIndicatorColor = ElectricCyan,
                unfocusedIndicatorColor = DarkSurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target Platform Selection
        Text(
            text = "Target Platform / Architecture",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(platforms) { (key, label) ->
                val isSelected = selectedPlatform == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPlatform = key },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurface,
                        labelColor = TextPrimaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) ElectricCyan else DarkSurfaceBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Provider Switcher
        Text(
            text = "Synthesis Engine (AI Provider)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AiProvidersRegistry.ALL_PROVIDERS) { provider ->
                val isSelected = selectedProviderId == provider.id
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedProviderId = provider.id },
                    label = { Text(provider.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurface,
                        labelColor = TextPrimaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) NeonPurple else DarkSurfaceBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Natural Language Prompt
        Text(
            text = "Project Description & Functional Requirements",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("Describe the application, UI components, states, logic, and APIs to include...", fontSize = 13.sp, color = TextSecondaryDark) },
            modifier = Modifier.fillMaxWidth().height(120.dp).testTag("input_project_prompt"),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedIndicatorColor = ElectricCyan,
                unfocusedIndicatorColor = DarkSurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Generate Action Button
        Button(
            onClick = {
                if (prompt.isBlank()) {
                    Toast.makeText(context, "Please enter a project description", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isGenerating = true
                analysisResult = ""
                scope.launch {
                    val project = appBuilderGenerator.generateProject(
                        name = projectName.ifBlank { "Untitled Project" },
                        description = prompt,
                        platform = selectedPlatform,
                        preferredProvider = selectedProviderId
                    )
                    generatedProject = project
                    selectedFileIndex = 0
                    isGenerating = false
                    Toast.makeText(context, "Generated ${project.getFiles().size} project files", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = DarkBackground
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_generate_app")
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBackground, strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Synthesizing Architecture & Code...", fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Project Structure & Code", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Generated Project Inspector
        generatedProject?.let { project ->
            val files = project.getFiles()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = project.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "${files.size} source files generated",
                                fontSize = 12.sp,
                                color = ElectricCyan
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val id = studioRepository.createProject(project)
                                    val saved = project.copy(id = id)
                                    onProjectSaved(saved)
                                    Toast.makeText(context, "Saved to your Projects!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_save_generated_project")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // File selection tabs
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(files.indices.toList()) { index ->
                            val f = files[index]
                            val isSelected = selectedFileIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ElectricCyan else DarkSurfaceElevated)
                                    .clickable {
                                        selectedFileIndex = index
                                        analysisResult = ""
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = f.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) DarkBackground else TextPrimaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Analysis Tools Bar (Code, Explain, Debug, Improve)
                    TabRow(
                        selectedTabIndex = selectedToolTab,
                        containerColor = DarkSurfaceElevated,
                        contentColor = ElectricCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedToolTab]),
                                color = ElectricCyan
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedToolTab == 0,
                            onClick = { selectedToolTab = 0 },
                            text = { Text("Code Editor", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedToolTab == 1,
                            onClick = {
                                selectedToolTab = 1
                                if (files.isNotEmpty()) {
                                    isAnalyzing = true
                                    scope.launch {
                                        analysisResult = appBuilderGenerator.explainCode(files[selectedFileIndex])
                                        isAnalyzing = false
                                    }
                                }
                            },
                            text = { Text("Explain", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedToolTab == 2,
                            onClick = {
                                selectedToolTab = 2
                                if (files.isNotEmpty()) {
                                    isAnalyzing = true
                                    scope.launch {
                                        analysisResult = appBuilderGenerator.debugCode(files[selectedFileIndex])
                                        isAnalyzing = false
                                    }
                                }
                            },
                            text = { Text("Find Bugs", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedToolTab == 3,
                            onClick = {
                                selectedToolTab = 3
                                if (files.isNotEmpty()) {
                                    isAnalyzing = true
                                    scope.launch {
                                        analysisResult = appBuilderGenerator.improveCode(files[selectedFileIndex])
                                        isAnalyzing = false
                                    }
                                }
                            },
                            text = { Text("Optimize", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedToolTab == 0 && files.isNotEmpty()) {
                        val currentFile = files[selectedFileIndex]
                        CodeViewer(
                            fileName = currentFile.path,
                            content = currentFile.content,
                            language = currentFile.language,
                            onContentSaved = { updatedContent ->
                                val updatedFiles = files.toMutableList()
                                updatedFiles[selectedFileIndex] = currentFile.copy(content = updatedContent)
                                generatedProject = project.withUpdatedFiles(updatedFiles)
                            }
                        )
                    } else {
                        // AI Tool Output (Explainer, Debugger, Optimizer)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (isAnalyzing) {
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ElectricCyan, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyzing code with AI...", color = TextSecondaryDark, fontSize = 13.sp)
                                }
                            } else {
                                Text(
                                    text = analysisResult.ifBlank { "Select an action tab above to run technical analysis on this file." },
                                    fontSize = 13.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
