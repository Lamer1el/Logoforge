package com.logoforge.app.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.logoforge.app.data.LogoProject
import com.logoforge.app.data.ProjectStore
import com.logoforge.app.editor.Exporter
import com.logoforge.app.templates.Templates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenProject: (LogoProject) -> Unit,
    onOpenAi: () -> Unit,
    onOpenAbout: () -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<LogoProject>>(emptyList()) }
    var thumbs by remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }
    var settingsOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<LogoProject?>(null) }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val list = ProjectStore.load(context.filesDir)
            val t = mutableMapOf<String, ByteArray>()
            list.forEach { p -> ProjectStore.thumb(context.filesDir, p.id)?.let { t[p.id] = it } }
            withContext(Dispatchers.Main) { projects = list; thumbs = t }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenProject(LogoProject(name = "Мой логотип")) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("С нуля") },
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("LogoForge") },
                actions = {
                    IconButton(onClick = { onOpenAi() }) { Icon(Icons.Filled.AutoAwesome, "Logotipen") }
                    IconButton(onClick = { settingsOpen = true }) { Icon(Icons.Filled.Palette, "Тема") }
                },
            )
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                Card(
                    onClick = onOpenAi,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text("Logotipen", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Создать логотип по описанию с помощью ИИ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                ElevatedCard(
                    onClick = { onOpenProject(LogoProject(name = "Мой логотип")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Add, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text("Начать с нуля", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Пустой холст — полностью своя идея, без образца",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Column {
                    Text("Или начните с шаблона", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(Templates) { tpl ->
                ElevatedCard(
                    onClick = { onOpenProject(tpl.build()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .background(Color(0xFFF3EDF7)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val bmp = remember(tpl.id) {
                                val p = tpl.build()
                                runCatching { Exporter.renderBitmap(p, 300) }.getOrNull()
                            }
                            bmp?.let {
                                Image(
                                    it.asImageBitmap(), null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                        Text(
                            tpl.name,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Column(Modifier.padding(top = 8.dp)) {
                    Text("Мои проекты", style = MaterialTheme.typography.titleMedium)
                    if (projects.isEmpty()) {
                        Text(
                            "Пока пусто — создайте первый логотип",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(projects, key = { it.id }) { p ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onOpenProject(p) },
                            onLongClick = { deleteTarget = p },
                        ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .background(Color(0xFFF3EDF7)),
                            contentAlignment = Alignment.Center,
                        ) {
                            thumbs[p.id]?.let { bytes ->
                                Image(
                                    bitmap = android.graphics.BitmapFactory.decodeByteArray(
                                        bytes, 0, bytes.size
                                    ).asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                        Text(
                            p.name,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            // Подвал с именами создателей
            item(span = { GridItemSpan(2) }) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "skitfe & lamer & ForkStudio",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Telegram: @logoforgeapp · @lamer42 · @Forkstudio_official",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("Оформление") },
            text = {
                Column {
                    Text("Material You (динамическая тема по обоям)")
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = { onDynamicColorChange(it) },
                    )
                }
            },
            confirmButton = { TextButton(onClick = { settingsOpen = false }) { Text("Готово") } },
        )
    }

    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить проект?") },
            text = { Text("«${p.name}» будет удалён безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    ProjectStore.delete(context.filesDir, p.id)
                    deleteTarget = null
                    refresh()
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Отмена") } },
        )
    }
}
