package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.ProjectTemplates
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkEditorBg

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, template: String, language: String, desc: String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectDesc by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(ProjectTemplates.TEMPLATE_HTML) }

    val templates = listOf(
        Triple(ProjectTemplates.TEMPLATE_HTML, "HTML", "🌐 HTML5, CSS3, JavaScript"),
        Triple(ProjectTemplates.TEMPLATE_PYTHON, "Python", "🐍 Python 3.12 Analytics"),
        Triple(ProjectTemplates.TEMPLATE_KOTLIN_ANDROID, "Kotlin", "🤖 Android Jetpack Compose"),
        Triple(ProjectTemplates.TEMPLATE_JAVA, "Java", "☕ Java Console Application"),
        Triple(ProjectTemplates.TEMPLATE_MARKDOWN, "Markdown", "📝 Markdown & Documentation")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create New Project",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. MyMobileApp") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input")
                )

                OutlinedTextField(
                    value = projectDesc,
                    onValueChange = { projectDesc = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Brief project summary") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_desc_input")
                )

                Text(
                    "Project Template & Language:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    templates.forEach { (tmpl, lang, label) ->
                        val isSelected = selectedTemplate == tmpl
                        Surface(
                            onClick = { selectedTemplate = tmpl },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("template_option_${lang.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTemplate = tmpl }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        val matched = templates.firstOrNull { it.first == selectedTemplate }
                        val lang = matched?.second ?: "Code"
                        onCreate(projectName.trim(), selectedTemplate, lang, projectDesc.trim())
                    }
                },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_project_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SimpleInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    confirmText: String = "Confirm",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simple_input_field")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        onConfirm(value.trim())
                    }
                },
                enabled = value.isNotBlank(),
                modifier = Modifier.testTag("simple_input_confirm_button")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("simple_input_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Confirmation", fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to permanently delete \"$itemName\"? This cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CodeBlockView(
    code: String,
    language: String = "",
    onInsertIntoEditor: ((String) -> Unit)? = null
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkEditorBg,
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "Code" },
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onInsertIntoEditor != null) {
                        TextButton(
                            onClick = { onInsertIntoEditor(code) },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("insert_code_button")
                        ) {
                            Text("Insert", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            copied = true
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("copy_code_button")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (copied) CyberCyan else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
