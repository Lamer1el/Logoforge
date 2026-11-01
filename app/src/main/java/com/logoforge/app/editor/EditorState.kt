package com.logoforge.app.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.logoforge.app.data.LogoElement
import com.logoforge.app.data.LogoProject
import com.logoforge.app.data.ProjectStore

/**
 * Состояние редактора: проект, выделение, история (undo/redo).
 * Все изменения элементов выполняются через [mutate] — он фиксирует историю.
 */
class EditorState(initial: LogoProject) {

    var project by mutableStateOf(initial)
        private set
    var selectedId by mutableStateOf<String?>(null)

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    val selected: LogoElement?
        get() = project.elements.find { it.id == selectedId }

    /** Записать текущее состояние в историю перед изменением. */
    fun pushHistory() {
        undoStack.addLast(ProjectStore.encode(project))
        if (undoStack.size > 60) undoStack.removeFirst()
        redoStack.clear()
    }

    fun mutate(block: (LogoProject) -> Unit) {
        pushHistory()
        val copy = ProjectStore.decode(undoStack.last())
        block(copy)
        copy.updatedAt = System.currentTimeMillis()
        project = copy
    }

    fun undo(): Boolean {
        val last = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(ProjectStore.encode(project))
        project = ProjectStore.decode(last)
        val sel = selectedId
        if (sel != null && project.elements.none { it.id == sel }) selectedId = null
        return true
    }

    fun redo(): Boolean {
        val last = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(ProjectStore.encode(project))
        project = ProjectStore.decode(last)
        return true
    }

    fun addElement(el: LogoElement) {
        mutate { p -> p.elements = p.elements + el }
        selectedId = el.id
    }

    fun updateSelected(block: (LogoElement) -> Unit) {
        val id = selectedId ?: return
        mutate { p ->
            p.elements.map { if (it.id == id) { block(it); it } else it }
        }
    }

    fun deleteSelected() {
        val id = selectedId ?: return
        mutate { p -> p.elements.filter { it.id != id } }
        selectedId = null
    }

    fun duplicateSelected() {
        val el = selected ?: return
        val copy = el.copy(id = java.util.UUID.randomUUID().toString(), x = el.x + 60f, y = el.y + 60f)
        mutate { p -> p.elements + copy }
        selectedId = copy.id
    }

    fun moveSelectedUp() {
        val id = selectedId ?: return
        mutate { p ->
            val i = p.elements.indexOfFirst { it.id == id }
            if (i in 0 until p.elements.size - 1) {
                val l = p.elements.toMutableList()
                val tmp = l[i]; l[i] = l[i + 1]; l[i + 1] = tmp
                p.elements = l
            }
        }
    }

    fun moveSelectedDown() {
        val id = selectedId ?: return
        mutate { p ->
            val i = p.elements.indexOfFirst { it.id == id }
            if (i > 0) {
                val l = p.elements.toMutableList()
                val tmp = l[i]; l[i] = l[i - 1]; l[i - 1] = tmp
                p.elements = l
            }
        }
    }

    fun bringToBack() {
        val id = selectedId ?: return
        mutate { p ->
            val el = p.elements.find { it.id == id } ?: return@mutate
            p.elements = listOf(el) + p.elements.filter { it.id != id }
        }
    }

    fun bringToFront() {
        val id = selectedId ?: return
        mutate { p ->
            val el = p.elements.find { it.id == id } ?: return@mutate
            p.elements = p.elements.filter { it.id != id } + el
        }
    }

    fun selectAt(x: Float, y: Float) {
        selectedId = hitTestInDesign(project, x, y)?.id
    }

    /** Непрерывное изменение (жесты) — история пишется один раз на жест (с паузой >350 мс — новая запись). */
    private var gestureActive = false
    private var lastGestureAt = 0L

    fun beginGesture() {
        val now = System.currentTimeMillis()
        if (!gestureActive || now - lastGestureAt > 350) pushHistory()
        gestureActive = true
        lastGestureAt = now
    }

    /** Выравнивание по центру холста. */
    fun centerSelectedH() = updateSelected { it.x = 500f }
    fun centerSelectedV() = updateSelected { it.y = 500f }

    /** Отражение по горизонтали/вертикали. */
    fun flipSelectedX() = updateSelected { it.flipX = !it.flipX }
    fun flipSelectedY() = updateSelected { it.flipY = !it.flipY }

    fun endGesture() { gestureActive = false }

    fun applyLive(block: (LogoProject) -> Unit) {
        val copy = ProjectStore.decode(ProjectStore.encode(project))
        block(copy)
        project = copy
    }
}

private fun hitTestInDesign(project: LogoProject, x: Float, y: Float) =
    com.logoforge.app.editor.render.hitTest(project, x, y)
