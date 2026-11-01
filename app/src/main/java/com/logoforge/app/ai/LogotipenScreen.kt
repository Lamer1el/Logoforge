package com.logoforge.app.ai

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.logoforge.app.data.LogoElement
import com.logoforge.app.data.LogoProject
import com.logoforge.app.data.ProjectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Лимит генераций ИИ в день. */
const val DAILY_AI_LIMIT = 2

private fun todayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

fun remainingToday(prefs: android.content.SharedPreferences): Int {
    val day = prefs.getString("ai_day", null)
    val used = prefs.getInt("ai_used", 0)
    return if (day == todayKey()) (DAILY_AI_LIMIT - used).coerceAtLeast(0) else DAILY_AI_LIMIT
}

private fun markUsed(prefs: android.content.SharedPreferences) {
    val day = prefs.getString("ai_day", null)
    val used = if (day == todayKey()) prefs.getInt("ai_used", 0) else 0
    prefs.edit()
        .putString("ai_day", todayKey())
        .putInt("ai_used", used + 1)
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogotipenScreen(
    onAddToEditor: (LogoProject) -> Unit,
    onOpenEditor: (LogoProject) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("logoforge", android.content.Context.MODE_PRIVATE) }
    var remaining by remember { mutableStateOf(remainingToday(prefs)) }
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var lastPrompt by remember { mutableStateOf<String?>(null) }
    var savedPath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    fun generate() {
        if (prompt.isBlank()) return
        if (remaining <= 0) {
            scope.launch { snackbar.showSnackbar("Дневной лимит исчерпан — приходите завтра!") }
            return
        }
        busy = true
        val userPrompt = prompt
        scope.launch {
            try {
                val enhanced = PollinationsApi.enhancePrompt(userPrompt)
                val bmp = PollinationsApi.generateLogo(enhanced)
                withContext(Dispatchers.Main) {
                    bitmap = bmp
                    lastPrompt = enhanced
                    savedPath = null
                    markUsed(prefs)
                    remaining = remainingToday(prefs)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbar.showSnackbar("Ошибка генерации: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) { busy = false }
            }
        }
    }

    fun saveToGallery() {
        val bmp = bitmap ?: return
        scope.launch(Dispatchers.IO) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "logoforge_ai_${System.currentTimeMillis()}.png")
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/LogoForge")
                }
            }
            context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os)
                }
            }
            withContext(Dispatchers.Main) { snackbar.showSnackbar("Сохранено в галерею") }
        }
    }

    fun openInEditor() {
        val bmp = bitmap ?: return
        scope.launch(Dispatchers.IO) {
            val path = PollinationsApi.saveBitmap(bmp, ProjectStore.imagesDir(context.filesDir))
            withContext(Dispatchers.Main) {
                val project = LogoProject(name = "Logotipen")
                project.elements = listOf(
                    LogoElement(
                        type = "image",
                        imagePath = path,
                        x = 500f, y = 500f, w = 900f,
                    )
                )
                ProjectStore.save(context.filesDir, project)
                onOpenEditor(project)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text("Logotipen — ИИ-дизайнер") })
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Опишите логотип, который хотите получить — Logotipen создаст его по вашему описанию.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {},
                label = { Text("Осталось генераций сегодня: $remaining из $DAILY_AI_LIMIT") },
                leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) },
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Например: логотип кофейни в форме чашки, минимализм, бежево-коричневая палитра") },
                minLines = 3,
            )
            Spacer(Modifier.height(12.dp))
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(12.dp))
                    Text("Logotipen думает…")
                }
            } else {
                Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AutoAwesome, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Создать логотип")
                }
            }

            bitmap?.let { bmp ->
                Spacer(Modifier.height(20.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Сгенерированный логотип",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent, RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { openInEditor() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.OpenInNew, null)
                        Spacer(Modifier.size(6.dp))
                        Text("В конструктор")
                    }
                    OutlinedButton(onClick = { saveToGallery() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Save, null)
                        Spacer(Modifier.size(6.dp))
                        Text("В галерею")
                    }
                    OutlinedButton(onClick = { generate() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Refresh, null)
                        Spacer(Modifier.size(6.dp))
                        Text("Ещё")
                    }
                }
                lastPrompt?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Промпт: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Logotipen — ИИ-агент LogoForge. Работает через Pollinations AI.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
