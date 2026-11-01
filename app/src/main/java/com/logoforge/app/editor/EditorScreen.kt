@file:OptIn(ExperimentalMaterial3Api::class)

package com.logoforge.app.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.painter.Painter
import com.logoforge.app.data.LogoElement
import com.logoforge.app.data.LogoProject
import com.logoforge.app.data.ProjectStore
import com.logoforge.app.editor.render.DESIGN_SIZE
import com.logoforge.app.editor.render.drawProject
import com.logoforge.app.editor.render.elementBounds
import com.logoforge.app.icons.IconSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private val ShapeNames = listOf(
    "circle" to "Круг", "ring" to "Кольцо", "square" to "Квадрат",
    "rounded" to "Скругление", "triangle" to "Треугольник", "diamond" to "Ромб",
    "hexagon" to "Шестиугольник", "pentagon" to "Пятиугольник", "star" to "Звезда",
    "heart" to "Сердце", "line" to "Линия",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    project: LogoProject,
    onSaveProject: (LogoProject) -> Unit,
    onBack: () -> Unit,
    onOpenAi: () -> Unit,
) {
    val context = LocalContext.current
    val state = remember { EditorState(ProjectStore.deepCopy(project)) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // sheets
    var showAddSheet by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showBg by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }
    var showLayers by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    val filesDir = context.filesDir

    // Сохранение при выходе и по кнопке
    fun save() {
        val p = state.project
        scope.launch(Dispatchers.IO) {
            ProjectStore.save(filesDir, p)
            runCatching { ProjectStore.saveThumb(filesDir, p.id, Exporter.thumbnail(p)) }
        }
    }
    LaunchedEffect(Unit) { save() }

    // Импорт фото
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val path = importImage(context, uri)
                if (path != null) withContext(Dispatchers.Main) {
                    state.addElement(
                        LogoElement(
                            type = "image",
                            imagePath = path,
                            x = 500f, y = 500f, w = 600f, scale = 1f, cornerRadius = 0f,
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.project.name, maxLines = 1,
                        modifier = Modifier.clickable { showRename = true },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { save(); onBack() }) { Icon(Icons.Filled.ArrowBack, "Назад") }
                },
                actions = {
                    IconButton(onClick = { state.undo() }, enabled = state.canUndo) {
                        Icon(Icons.Filled.Undo, "Отменить")
                    }
                    IconButton(onClick = { state.redo() }, enabled = state.canRedo) {
                        Icon(Icons.Filled.Redo, "Вернуть")
                    }
                    IconButton(onClick = { showLayers = true }) {
                        Icon(Icons.Filled.Layers, "Слои")
                    }
                    IconButton(onClick = { showExport = true }) {
                        Icon(Icons.Filled.Save, "Экспорт")
                    }
                },
            )
        },
        bottomBar = { EditorBottomBar(state, onAdd = { showAddSheet = true }, onBg = { showBg = true }) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                EditorCanvas(state)
            }
            if (state.selected != null) {
                PropertyPanel(state, onColorTarget = { colorTarget = it })
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            AddSheetContent(
                onShape = { shape ->
                    showAddSheet = false
                    state.addElement(LogoElement(type = "shape", shape = shape, x = 500f, y = 500f, w = 400f, h = 400f))
                },
                onIcon = { showAddSheet = false; showIconPicker = true },
                onText = { showAddSheet = false; showTextDialog = true },
                onPhoto = {
                    showAddSheet = false
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onAi = { showAddSheet = false; onOpenAi() },
            )
        }
    }

    if (showIconPicker) {
        IconPickerSheet(onDismiss = { showIconPicker = false }) { name ->
            showIconPicker = false
            state.addElement(LogoElement(type = "icon", icon = name, x = 500f, y = 400f, w = 380f, color = 0xFF6D3FC4))
        }
    }

    if (showTextDialog) {
        TextInputDialog(
            initial = "Логотип",
            onDismiss = { showTextDialog = false },
            onConfirm = { txt ->
                showTextDialog = false
                state.addElement(
                    LogoElement(type = "text", text = txt, x = 500f, y = 700f, fontSize = 120f, color = 0xFF1D1B20)
                )
            },
        )
    }

    if (showBg) {
        BackgroundSheet(
            state = state,
            onDismiss = { showBg = false },
            onPickColor = { colorTarget = it },
        )
    }

    if (showLayers) {
        LayersSheet(state, onDismiss = { showLayers = false })
    }

    if (showRename) {
        var name by remember(state.project.id) { mutableStateOf(state.project.name) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Название проекта") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Введите название") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) state.mutate { it.name = name.trim() }
                    showRename = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRename = false }) { Text("Отмена") }
            },
        )
    }

    if (showExport) {
        ExportSheet(
            state = state,
            onDismiss = { showExport = false },
            onExported = { file, format ->
                showExport = false
                scope.launch {
                    snackbar.showSnackbar("Экспортировано: ${file.name}")
                    // автоматическое предложение «Поделиться»
                    runCatching {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = format.mime
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, "Поделиться"))
                    }
                }
            },
        )
    }

    colorTarget?.let { target ->
        ColorPickerDialog(
            initial = target.currentColor(state),
            onDismiss = { colorTarget = null },
            onConfirm = { c ->
                target.apply(state, c)
                colorTarget = null
            },
        )
    }
}

private fun importImage(context: android.content.Context, uri: Uri): String? = runCatching {
    val dir = ProjectStore.imagesDir(context.filesDir)
    val f = File(dir, "${UUID.randomUUID()}.png")
    context.contentResolver.openInputStream(uri)?.use { input ->
        f.outputStream().use { input.copyTo(it) }
    } ?: return null
    f.absolutePath
}.getOrNull()

@Composable
private fun EditorCanvas(state: EditorState) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun toDesign(offset: Offset): Pair<Float, Float> {
        val side = minOf(canvasSize.width, canvasSize.height).toFloat()
        if (side <= 0f) return 0f to 0f
        val s = side / DESIGN_SIZE
        val ox = (canvasSize.width - side) / 2f
        val oy = (canvasSize.height - side) / 2f
        return ((offset.x - ox) / s) to ((offset.y - oy) / s)
    }

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onSizeChanged { canvasSize = it }
            .background(Color(0x11000000), RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val (x, y) = toDesign(offset)
                    state.selectAt(x, y)
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (state.selectedId != null) {
                        state.beginGesture()
                        val side = minOf(canvasSize.width, canvasSize.height).toFloat()
                        val s = if (side > 0) side / DESIGN_SIZE else 1f
                        state.applyLive { p ->
                            val el = p.elements.find { it.id == state.selectedId } ?: return@applyLive
                            el.x = (el.x + pan.x / s).coerceIn(-500f, 1500f)
                            el.y = (el.y + pan.y / s).coerceIn(-500f, 1500f)
                            el.scale = (el.scale * zoom).coerceIn(0.05f, 20f)
                            el.rotation = (el.rotation + rotation) % 360f
                        }
                    }
                }
            },
    ) {
        val side = minOf(size.width, size.height)
        drawProject(state.project, side, checkerBackground = state.project.transparentBg)
        // рамка выделения
        state.selected?.let { el ->
            val b = elementBounds(el)
            val s = side / DESIGN_SIZE
            val ox = (size.width - side) / 2f
            val oy = (size.height - side) / 2f
            withTransform2(el, s, ox, oy) {
                drawRect(
                    color = Color(0xFF6D3FC4),
                    topLeft = Offset(b.left - 8f, b.top - 8f),
                    size = androidx.compose.ui.geometry.Size(b.width + 16f, b.height + 16f),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    ),
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.withTransform2(
    el: LogoElement,
    s: Float,
    ox: Float,
    oy: Float,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    withTransform({
        translate(el.x * s + ox, el.y * s + oy)
        rotate(el.rotation)
        scale(el.scale, el.scale, Offset.Zero)
    }) { block() }
}

@Composable
private fun EditorBottomBar(state: EditorState, onAdd: () -> Unit, onBg: () -> Unit) {
    BottomAppBar {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomAction(Icons.Filled.Add, "Добавить") { onAdd() }
            BottomAction(Icons.Filled.Palette, "Фон") { onBg() }
            if (state.selected != null) {
                BottomAction(Icons.Filled.ContentCopy, "Копия") { state.duplicateSelected() }
                BottomAction(Icons.Filled.Delete, "Удалить") { state.deleteSelected() }
            }
        }
    }
}

@Composable
private fun BottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp),
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AddSheetContent(
    onShape: (String) -> Unit,
    onIcon: () -> Unit,
    onText: () -> Unit,
    onPhoto: () -> Unit,
    onAi: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        Text("Добавить элемент", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetCard(Icons.Filled.Category, "Фигура") { onShape("circle") }
            SheetCard(Icons.Filled.Extension, "Иконка") { onIcon() }
            SheetCard(Icons.Filled.TextFields, "Текст") { onText() }
            SheetCard(Icons.Filled.Image, "Фото") { onPhoto() }
            SheetCard(Icons.Filled.AutoAwesome, "Logotipen ИИ") { onAi() }
        }
        Spacer(Modifier.height(16.dp))
        Text("Фигуры", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShapeNames.forEach { (key, label) ->
                AssistChip(onClick = { onShape(key) }, label = { Text(label) })
            }
        }
    }
}

@Composable
private fun SheetCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.size(width = 96.dp, height = 92.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun IconPickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val icons = remember(query, category) {
        IconSet.search(query).filter { category == null || it.category == category }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Поиск иконки…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Все") })
                IconSet.categories.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth().height(420.dp).padding(horizontal = 12.dp),
            ) {
                items(icons) { entry ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onPick(entry.name) }
                            .padding(8.dp),
                    ) {
                        Icon(
                            entry.vector, entry.name,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Текст") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Введите текст") })
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Добавить") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun PropertyPanel(state: EditorState, onColorTarget: (ColorTarget) -> Unit) {
    val el = state.selected ?: return
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                when (el.type) {
                    "shape" -> "Фигура"
                    "text" -> "Текст"
                    "icon" -> "Иконка"
                    "image" -> "Изображение"
                    else -> "Элемент"
                },
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.width(8.dp))
            PropIconButton(Icons.Filled.ContentCopy, "Дублировать") { state.duplicateSelected() }
            PropIconButton(Icons.Filled.KeyboardArrowUp, "Слой выше") { state.moveSelectedUp() }
            PropIconButton(Icons.Filled.KeyboardArrowDown, "Слой ниже") { state.moveSelectedDown() }
            PropIconButton(Icons.Filled.FlipToFront, "На передний") { state.bringToFront() }
            PropIconButton(Icons.Filled.FlipToBack, "На задний") { state.bringToBack() }
            PropIconButton(Icons.Filled.Delete, "Удалить") { state.deleteSelected() }
        }

        // Выравнивание и отражение
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(onClick = { state.centerSelectedH() }, label = { Text("По центру X") })
            AssistChip(onClick = { state.centerSelectedV() }, label = { Text("По центру Y") })
            AssistChip(onClick = { state.flipSelectedX() }, label = { Text("Отразить ↔") })
            AssistChip(onClick = { state.flipSelectedY() }, label = { Text("Отразить ↕") })
        }

        if (el.type == "text") {
            var txt by remember(el.id) { mutableStateOf(el.text) }
            OutlinedTextField(
                value = txt,
                onValueChange = {
                    txt = it
                    state.updateSelected { e -> e.text = it }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Текст") },
                minLines = 1,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("sans" to "Sans", "serif" to "Serif", "mono" to "Mono", "cursive" to "Cursive").forEach { (k, l) ->
                    FilterChip(
                        selected = el.fontFamily == k,
                        onClick = { state.updateSelected { e -> e.fontFamily = k } },
                        label = { Text(l) },
                    )
                }
                FilterChip(
                    selected = el.fontWeight >= 600,
                    onClick = { state.updateSelected { e -> e.fontWeight = if (e.fontWeight >= 600) 400 else 700 } },
                    label = { Text("Жирный") },
                )
                FilterChip(
                    selected = el.uppercase,
                    onClick = { state.updateSelected { e -> e.uppercase = !e.uppercase } },
                    label = { Text("ЗАГЛАВНЫЕ") },
                )
            }
        }

        if (el.type == "shape") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShapeNames.forEach { (k, label) ->
                    FilterChip(
                        selected = el.shape == k,
                        onClick = { state.updateSelected { e -> e.shape = k } },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (el.type == "image") {
            LabeledSlider("Скругление", el.cornerRadius, 0f, 0.5f) { v ->
                state.updateSelected { e -> e.cornerRadius = v }
            }
        }

        LabeledSlider("Размер", el.scale, 0.05f, 4f) { v ->
            state.updateSelected { e -> e.scale = v }
        }
        LabeledSlider("Поворот", el.rotation, -180f, 180f) { v ->
            state.updateSelected { e -> e.rotation = v }
        }
        LabeledSlider("Прозрачность", el.opacity, 0f, 1f) { v ->
            state.updateSelected { e -> e.opacity = v }
        }
        if (el.type == "text") {
            LabeledSlider("Шрифт", el.fontSize, 24f, 400f) { v ->
                state.updateSelected { e -> e.fontSize = v }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { onColorTarget(ColorTarget.Element) }) {
                Icon(Icons.Filled.Palette, null); Spacer(Modifier.width(6.dp)); Text("Цвет")
            }
            if (el.type == "text") {
                var spacing by remember(el.id) { mutableStateOf(el.letterSpacing.toString()) }
                OutlinedTextField(
                    value = spacing,
                    onValueChange = { v ->
                        spacing = v
                        v.toFloatOrNull()?.let { state.updateSelected { e -> e.letterSpacing = it } }
                    },
                    label = { Text("Межбуквенный интервал") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun PropIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, desc, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${"%.2f".format(value)}", style = MaterialTheme.typography.labelSmall)
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max)
    }
}

sealed class ColorTarget {
    object Element : ColorTarget()
    data class Background(val first: Boolean) : ColorTarget()

    fun currentColor(state: EditorState): Color = when (this) {
        Element -> Color(state.selected?.color ?: 0xFF6750A4)
        is Background -> if (first) Color(state.project.bgColor) else Color(state.project.bgColor2)
    }

    fun apply(state: EditorState, c: Color) {
        when (this) {
            Element -> state.updateSelected { e -> e.color = c.value.toLong() and 0xFFFFFFFFL }
            is Background -> state.mutate { p ->
                if (first) p.bgColor = c.value.toLong() and 0xFFFFFFFFL
                else p.bgColor2 = c.value.toLong() and 0xFFFFFFFFL
            }
        }
    }
}

@Composable
private fun BackgroundSheet(state: EditorState, onDismiss: () -> Unit, onPickColor: (ColorTarget) -> Unit) {
    val p = state.project
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Фон", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = p.transparentBg,
                    onClick = { state.mutate { it.transparentBg = !it.transparentBg } },
                    label = { Text("Прозрачный") },
                )
                FilterChip(
                    selected = p.gradient && !p.transparentBg,
                    onClick = { state.mutate { it.gradient = !it.gradient } },
                    label = { Text("Градиент") },
                )
            }
            if (!p.transparentBg) {
                Button(onClick = { onPickColor(ColorTarget.Background(true)) }) {
                    Icon(Icons.Filled.Palette, null); Spacer(Modifier.width(6.dp)); Text("Основной цвет")
                }
                if (p.gradient) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onPickColor(ColorTarget.Background(false)) }) {
                        Icon(Icons.Filled.Palette, null); Spacer(Modifier.width(6.dp)); Text("Второй цвет градиента")
                    }
                }
            }
        }
    }
}

@Composable
private fun LayersSheet(state: EditorState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Слои", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
            Column(Modifier.fillMaxWidth().height(360.dp)) {
                state.project.elements.asReversed().forEach { el ->
                    val label = when (el.type) {
                        "text" -> "Текст: ${el.text.take(20)}"
                        "icon" -> "Иконка: ${el.icon}"
                        "image" -> "Фото"
                        else -> "Фигура: ${el.shape}"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state.selectedId = el.id; onDismiss() }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            when (el.type) {
                                "text" -> Icons.Filled.TextFields
                                "icon" -> Icons.Filled.Extension
                                "image" -> Icons.Filled.Image
                                else -> Icons.Filled.Category
                            },
                            null,
                            tint = if (state.selectedId == el.id) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportSheet(
    state: EditorState,
    onDismiss: () -> Unit,
    onExported: (File, ExportFormat) -> Unit,
) {
    var format by remember { mutableStateOf(ExportFormat.PNG) }
    var size by remember { mutableStateOf(1024) }
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Экспорт логотипа", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text("Формат", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExportFormat.entries.forEach { f ->
                    FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.label) })
                }
            }
            Text("Размер (px)", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(512, 1024, 2048).forEach { s ->
                    FilterChip(
                        selected = size == s,
                        onClick = { size = s },
                        label = { Text("$s×$s") },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Экспорт…")
                }
            } else {
                Button(
                    onClick = {
                        busy = true
                        val p = state.project
                        Thread {
                            val file = runCatching { Exporter.export(context, p, format, size) }.getOrNull()
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                busy = false
                                if (file != null) onExported(file, format)
                            }
                        }.start()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Экспортировать и поделиться")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
