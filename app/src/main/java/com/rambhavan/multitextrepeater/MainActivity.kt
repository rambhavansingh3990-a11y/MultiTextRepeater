package com.rambhavan.multitextrepeater

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF5B5FEF)
private val Canvas = Color(0xFFF5F6FC)
private val Ink = Color(0xFF172033)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Canvas)) { RepeaterScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeaterScreen() {
    var source by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("10") }
    var separator by remember { mutableStateOf("New line") }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(output) }
            }.onSuccess { Toast.makeText(context, "Text file saved", Toast.LENGTH_SHORT).show() }
             .onFailure { Toast.makeText(context, "Could not save file", Toast.LENGTH_SHORT).show() }
        }
    }

    fun generate() {
        val lines = source.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        val count = countText.toIntOrNull()
        if (lines.isEmpty()) { error = "Enter at least one text line."; output = ""; return }
        if (count == null || count !in 1..10000) { error = "Repeat count must be between 1 and 10,000."; output = ""; return }
        val joiner = when (separator) { "Space" -> " "; "Comma" -> ", "; else -> "\n" }
        val estimated = lines.sumOf { it.length.toLong() + prefix.length + suffix.length } * count + (lines.size.toLong() * count * joiner.length)
        if (estimated > 2_000_000L) { error = "Output is too large. Reduce the repeat count or text length."; output = ""; return }
        output = lines.flatMap { line -> List(count) { "$prefix$line$suffix" } }.joinToString(joiner)
        error = ""
    }

    Scaffold(containerColor = Canvas) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("MULTI TEXT", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Repeater", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text("Repeat one or many lines in seconds.", color = Color(0xFF687086), fontSize = 14.sp)
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("YOUR TEXT", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = source, onValueChange = { source = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                        placeholder = { Text("Type or paste text here…\nPut each different text on a new line.") },
                        minLines = 4
                    )
                    Text("Each non-empty line is treated as a separate text.", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("REPEAT SETTINGS", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = countText, onValueChange = { countText = it.filter(Char::isDigit).take(5) },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Repeat count (1–10,000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    Text("SEPARATOR", color = Color(0xFF687086), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("New line", "Space", "Comma").forEach { item ->
                            FilterChip(selected = separator == item, onClick = { separator = item }, label = { Text(item) })
                        }
                    }
                    OutlinedTextField(value = prefix, onValueChange = { prefix = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Prefix (optional)") }, singleLine = true)
                    OutlinedTextField(value = suffix, onValueChange = { suffix = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Suffix (optional)") }, singleLine = true)
                }
            }
            Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) {
                Text("Generate text", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            if (output.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GENERATED OUTPUT", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${output.length} characters", color = Color.Gray, fontSize = 11.sp)
                        }
                        Text(output.take(5000) + if (output.length > 5000) "\n… preview limited" else "", color = Ink, fontSize = 13.sp, lineHeight = 19.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { clipboard.setText(AnnotatedString(output)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) { Text("Copy all") }
                            OutlinedButton(onClick = { saveLauncher.launch("repeated-text.txt") }, modifier = Modifier.weight(1f)) { Text("Save TXT") }
                        }
                        OutlinedButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, output) }
                            context.startActivity(Intent.createChooser(intent, "Share text"))
                        }, modifier = Modifier.fillMaxWidth()) { Text("Share output") }
                    }
                }
            }
            Text("Multi Text Repeater • Version 1.0", modifier = Modifier.fillMaxWidth(), color = Color.Gray, fontSize = 11.sp)
        }
    }
}
