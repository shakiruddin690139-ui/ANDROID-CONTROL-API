package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.TextTertiaryDark

@Composable
fun CodeViewer(
    fileName: String,
    content: String,
    language: String,
    modifier: Modifier = Modifier,
    onContentSaved: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editableContent by remember(content) { mutableStateOf(content) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
    ) {
        // Code header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = language.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark
                )
            }

            Row {
                if (onContentSaved != null) {
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                onContentSaved(editableContent)
                                isEditing = false
                                Toast.makeText(context, "Saved changes", Toast.LENGTH_SHORT).show()
                            } else {
                                isEditing = true
                            }
                        },
                        modifier = Modifier.size(32.dp).testTag("btn_edit_code")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit",
                            tint = if (isEditing) ElectricCyan else TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(if (isEditing) editableContent else content))
                        isCopied = true
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp).testTag("btn_copy_code")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCopied) ElectricCyan else TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Code content area
        if (isEditing) {
            OutlinedTextField(
                value = editableContent,
                onValueChange = { editableContent = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(8.dp)
                    .testTag("code_editor_input"),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextPrimaryDark
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = ElectricCyan,
                    unfocusedIndicatorColor = DarkSurfaceBorder
                )
            )
        } else {
            val lines = remember(content) { content.lines() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                // Line numbers
                Column(
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    lines.indices.forEach { index ->
                        Text(
                            text = "${index + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextTertiaryDark,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Code lines
                Column {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (line.trimStart().startsWith("//") || line.trimStart().startsWith("#")) {
                                TextTertiaryDark
                            } else if (line.contains("fun ") || line.contains("class ") || line.contains("import ") || line.contains("val ") || line.contains("var ")) {
                                ElectricCyan
                            } else {
                                TextPrimaryDark
                            },
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
