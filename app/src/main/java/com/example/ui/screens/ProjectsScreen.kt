package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.core.content.FileProvider
import com.example.data.generator.AppBuilderGenerator
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import com.example.data.repository.AiStudioRepository
import com.example.ui.components.CodeViewer
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
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProjectsScreen(
    studioRepository: AiStudioRepository,
    appBuilderGenerator: AppBuilderGenerator,
    activeProject: Project? = null,
    onCloseProject: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val projectsFlow = remember { studioRepository.observeProjects() }
    val projects by projectsFlow.collectAsState(initial = emptyList())

    var currentInspectingProject by remember(activeProject) { mutableStateOf(activeProject) }
    var selectedFileIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var filterPlatform by remember { mutableStateOf("all") }

    // Dialogs state
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<Project?>(null) }
    var showAddFileDialog by remember { mutableStateOf(false) }

    // SAF Import ZIP Launcher
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val imported = appBuilderGenerator.importProjectFromZip(stream, "Imported Project")
                        val id = studioRepository.createProject(imported)
                        currentInspectingProject = imported.copy(id = id)
                        Toast.makeText(context, "Project imported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Detail View: Project & File Explorer
    if (currentInspectingProject != null) {
        val project = currentInspectingProject!!
        val files = project.getFiles()

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            currentInspectingProject = null
                            onCloseProject()
                        },
                        modifier = Modifier.testTag("btn_back_to_projects")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = project.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "${project.platform.uppercase()} - ${files.size} files",
                            fontSize = 11.sp,
                            color = ElectricCyan
                        )
                    }
                }

                Row {
                    // Export ZIP
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val zipBytes = appBuilderGenerator.exportProjectAsZip(project)
                                    val cacheFile = File(context.cacheDir, "${project.name.replace(" ", "_")}.zip")
                                    FileOutputStream(cacheFile).use { it.write(zipBytes) }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export Project ZIP"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_export_zip")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export ZIP", tint = ElectricCyan)
                    }

                    // Add File
                    IconButton(
                        onClick = { showAddFileDialog = true },
                        modifier = Modifier.testTag("btn_add_file")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add File", tint = ElectricCyan)
                    }
                }
            }

            // File Tabs
            if (files.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(files.indices.toList()) { index ->
                        val f = files[index]
                        val isSelected = selectedFileIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricCyan else DarkSurfaceElevated)
                                .clickable { selectedFileIndex = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = f.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DarkBackground else TextPrimaryDark
                            )
                        }
                    }
                }

                val currentFile = files.getOrNull(selectedFileIndex) ?: files.first()
                CodeViewer(
                    fileName = currentFile.path,
                    content = currentFile.content,
                    language = currentFile.language,
                    modifier = Modifier.weight(1f),
                    onContentSaved = { newContent ->
                        val updatedFiles = files.toMutableList()
                        updatedFiles[selectedFileIndex] = currentFile.copy(content = newContent)
                        val updatedProject = project.withUpdatedFiles(updatedFiles)
                        currentInspectingProject = updatedProject
                        scope.launch {
                            studioRepository.updateProject(updatedProject)
                        }
                    }
                )

                // Delete file row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            if (files.size > 1) {
                                val updatedFiles = files.toMutableList()
                                updatedFiles.removeAt(selectedFileIndex)
                                selectedFileIndex = 0
                                val updatedProject = project.withUpdatedFiles(updatedFiles)
                                currentInspectingProject = updatedProject
                                scope.launch { studioRepository.updateProject(updatedProject) }
                                Toast.makeText(context, "Deleted file", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Cannot delete only remaining file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete File", color = AccentPink, fontSize = 12.sp)
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No files in project. Tap + to add one.", color = TextSecondaryDark)
                }
            }
        }
    } else {
        // Project Explorer List View
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PROJECTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Project Hub",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Row {
                    // Import ZIP
                    IconButton(
                        onClick = { importZipLauncher.launch("application/zip") },
                        modifier = Modifier.testTag("btn_import_zip")
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = "Import ZIP", tint = NeonPurple)
                    }

                    // Create New Project
                    IconButton(
                        onClick = { showNewProjectDialog = true },
                        modifier = Modifier.testTag("btn_create_project_dialog")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Project", tint = ElectricCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search projects...", fontSize = 13.sp, color = TextSecondaryDark) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedIndicatorColor = ElectricCyan,
                    unfocusedIndicatorColor = DarkSurfaceBorder
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            val filtered = projects.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = DarkSurfaceBorder, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No projects found", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimaryDark)
                        Text("Create a project or import a ZIP file to get started.", fontSize = 12.sp, color = TextSecondaryDark)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered) { proj ->
                        ProjectItemCard(
                            project = proj,
                            onOpen = { currentInspectingProject = proj },
                            onRename = { showRenameDialog = proj },
                            onDuplicate = {
                                scope.launch {
                                    val dup = proj.copy(
                                        id = "",
                                        name = "${proj.name} (Copy)"
                                    )
                                    studioRepository.createProject(dup)
                                    Toast.makeText(context, "Project duplicated", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    studioRepository.deleteProject(proj.id)
                                    Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add File Dialog
    if (showAddFileDialog && currentInspectingProject != null) {
        var newFilePath by remember { mutableStateOf("") }
        var newFileLang by remember { mutableStateOf("kotlin") }

        AlertDialog(
            onDismissRequest = { showAddFileDialog = false },
            containerColor = DarkSurface,
            title = { Text("Add Project File", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("File Path / Name:", fontSize = 12.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newFilePath,
                        onValueChange = { newFilePath = it },
                        placeholder = { Text("e.g. src/util/Helpers.kt") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFilePath.isNotBlank()) {
                            val proj = currentInspectingProject!!
                            val files = proj.getFiles().toMutableList()
                            val name = newFilePath.substringAfterLast('/')
                            files.add(ProjectFile(path = newFilePath, name = name, content = "// New file\n", language = newFileLang))
                            val updated = proj.withUpdatedFiles(files)
                            currentInspectingProject = updated
                            scope.launch { studioRepository.updateProject(updated) }
                            showAddFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkBackground)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Create New Project Dialog
    if (showNewProjectDialog) {
        var newName by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        var newPlatform by remember { mutableStateOf("android") }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            containerColor = DarkSurface,
            title = { Text("Create Blank Project", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Project Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            scope.launch {
                                val proj = Project(
                                    name = newName,
                                    description = newDesc,
                                    platform = newPlatform,
                                    filesJson = "[{\"path\":\"Main.kt\",\"name\":\"Main.kt\",\"content\":\"// Project initialized\\n\",\"language\":\"kotlin\"}]"
                                )
                                val id = studioRepository.createProject(proj)
                                currentInspectingProject = proj.copy(id = id)
                                showNewProjectDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkBackground)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Dialog
    showRenameDialog?.let { proj ->
        var renameText by remember { mutableStateOf(proj.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            containerColor = DarkSurface,
            title = { Text("Rename Project", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val updated = proj.copy(name = renameText)
                            studioRepository.updateProject(updated)
                            showRenameDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkBackground)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProjectItemCard(
    project: Project,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${project.getFiles().size} files",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                Row {
                    IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentPink, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Text(
                text = project.description.ifBlank { "No description" },
                fontSize = 12.sp,
                color = TextSecondaryDark,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
