package com.logoforge.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Один элемент логотипа. Координаты — в «дизайн-пространстве» 1000×1000. */
@Serializable
data class LogoElement(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "shape", // shape | text | icon | image
    var x: Float = 500f,
    var y: Float = 500f,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var opacity: Float = 1f,
    var flipX: Boolean = false,
    var flipY: Boolean = false,
    // Фигуры
    var shape: String = "circle",
    var w: Float = 400f,
    var h: Float = 400f,
    var color: Long = 0xFF6750A4,
    // Текст
    var text: String = "Logo",
    var fontSize: Float = 120f,
    var fontFamily: String = "sans", // sans | serif | mono | cursive
    var fontWeight: Int = 700,
    var letterSpacing: Float = 0f,
    var uppercase: Boolean = false,
    // Иконка
    var icon: String = "Star",
    // Изображение (путь к файлу внутри filesDir/images)
    var imagePath: String = "",
    var cornerRadius: Float = 0f, // 0..0.5 — доля от половины меньшей стороны
)

@Serializable
data class LogoProject(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Логотип",
    var elements: List<LogoElement> = emptyList(),
    var bgColor: Long = 0xFFFFFFFF,
    var bgColor2: Long = 0xFFEADDFF,
    var gradient: Boolean = false,
    var transparentBg: Boolean = false,
    var updatedAt: Long = System.currentTimeMillis(),
)

object ProjectStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun projectsDir(filesDir: File): File =
        File(filesDir, "projects").apply { mkdirs() }

    fun imagesDir(filesDir: File): File =
        File(filesDir, "images").apply { mkdirs() }

    fun save(filesDir: File, project: LogoProject) {
        val f = File(projectsDir(filesDir), "${project.id}.json")
        f.writeText(json.encodeToString(project))
    }

    fun load(filesDir: File): List<LogoProject> =
        projectsDir(filesDir).listFiles { f -> f.name.endsWith(".json") }
            ?.mapNotNull {
                runCatching { json.decodeFromString<LogoProject>(it.readText()) }.getOrNull()
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()

    fun loadOne(filesDir: File, id: String): LogoProject? =
        runCatching {
            json.decodeFromString<LogoProject>(File(projectsDir(filesDir), "$id.json").readText())
        }.getOrNull()

    fun delete(filesDir: File, id: String) {
        File(projectsDir(filesDir), "$id.json").delete()
        File(projectsDir(filesDir), "${id}_thumb.png").delete()
    }

    fun saveThumb(filesDir: File, id: String, pngBytes: ByteArray) {
        File(projectsDir(filesDir), "${id}_thumb.png").writeBytes(pngBytes)
    }

    fun thumb(filesDir: File, id: String): ByteArray? =
        runCatching { File(projectsDir(filesDir), "${id}_thumb.png").readBytes() }.getOrNull()

    /** Глубокая копия через сериализацию. */
    fun deepCopy(p: LogoProject): LogoProject = json.decodeFromString(json.encodeToString(p))

    fun encode(p: LogoProject): String = json.encodeToString(p)

    fun decode(s: String): LogoProject = json.decodeFromString(s)
}
