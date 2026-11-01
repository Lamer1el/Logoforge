package com.logoforge.app.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import com.logoforge.app.data.LogoElement
import com.logoforge.app.data.LogoProject
import com.logoforge.app.editor.render.DESIGN_SIZE
import com.logoforge.app.editor.render.RenderCache
import com.logoforge.app.editor.render.drawElement
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val label: String, val ext: String, val mime: String) {
    PNG("PNG", "png", "image/png"),
    JPEG("JPEG", "jpg", "image/jpeg"),
    WEBP("WebP", "webp", "image/webp"),
    SVG("SVG", "svg", "image/svg+xml"),
    PDF("PDF", "pdf", "application/pdf"),
}

object Exporter {

    fun renderBitmap(project: LogoProject, sizePx: Int): Bitmap {
        return com.logoforge.app.editor.render.renderProject(project, sizePx)
    }

    private fun bitmapBytes(bmp: Bitmap, format: ExportFormat, quality: Int = 95): ByteArray {
        val bos = ByteArrayOutputStream()
        when (format) {
            ExportFormat.PNG -> bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
            ExportFormat.JPEG -> {
                if (bmp.config == Bitmap.Config.ARGB_8888) {
                    val white = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.RGB_565)
                    val c = AndroidCanvas(white)
                    c.drawColor(android.graphics.Color.WHITE)
                    c.drawBitmap(bmp, 0f, 0f, null)
                    white.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                } else bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            }
            ExportFormat.WEBP -> bmp.compress(Bitmap.CompressFormat.WEBP, quality, bos)
            else -> error("not a bitmap format")
        }
        return bos.toByteArray()
    }

    fun export(
        context: Context,
        project: LogoProject,
        format: ExportFormat,
        sizePx: Int,
    ): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = "${project.name.replace(' ', '_')}_$stamp.${format.ext}"

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val out = File(dir, name)

        when (format) {
            ExportFormat.PNG, ExportFormat.JPEG, ExportFormat.WEBP -> {
                val bmp = renderBitmap(project, sizePx)
                out.writeBytes(bitmapBytes(bmp, format))
            }
            ExportFormat.SVG -> out.writeText(buildSvg(project, sizePx))
            ExportFormat.PDF -> out.writeBytes(buildPdf(project, sizePx))
        }

        // Копия в общую папку (MediaStore)
        runCatching { publishToMediaStore(context, out, name, format) }
        return out
    }

    private fun publishToMediaStore(context: Context, file: File, name: String, format: ExportFormat) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relPath = if (format == ExportFormat.PDF || format == ExportFormat.SVG)
                    Environment.DIRECTORY_DOCUMENTS + "/LogoForge"
                else Environment.DIRECTORY_PICTURES + "/LogoForge"
                put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(
            if (format == ExportFormat.PDF || format == ExportFormat.SVG)
                MediaStore.Files.getContentUri("external")
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(file.readBytes())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
    }

    /** SVG: фигуры и текст — вектором, иконки и фото — встроенными PNG (base64). */
    fun buildSvg(project: LogoProject, sizePx: Int): String {
        val k = sizePx / DESIGN_SIZE
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$sizePx\" height=\"$sizePx\" viewBox=\"0 0 1000 1000\">\n")
        sb.append("<defs>")
        if (project.gradient && !project.transparentBg) {
            sb.append("<linearGradient id=\"bgg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">")
            sb.append("<stop offset=\"0\" stop-color=\"${hex(project.bgColor)}\"/>")
            sb.append("<stop offset=\"1\" stop-color=\"${hex(project.bgColor2)}\"/>")
            sb.append("</linearGradient>")
        }
        sb.append("</defs>\n")
        if (!project.transparentBg) {
            if (project.gradient) sb.append("<rect width=\"1000\" height=\"1000\" fill=\"url(#bgg)\"/>\n")
            else sb.append("<rect width=\"1000\" height=\"1000\" fill=\"${hex(project.bgColor)}\"/>\n")
        }

        val fontMap = mapOf("sans" to "sans-serif", "serif" to "serif", "mono" to "monospace", "cursive" to "cursive")

        for (el in project.elements) {
            val alpha = el.opacity
            val op = if (alpha < 1f) " opacity=\"${"%.2f".format(alpha)}\"" else ""
            val fx = if (el.flipX) -1f else 1f
            val fy = if (el.flipY) -1f else 1f
            sb.append("<g transform=\"translate(${el.x} ${el.y}) rotate(${el.rotation}) scale(${el.scale * fx} ${el.scale * fy})\"$op>\n")
            when (el.type) {
                "shape" -> {
                    val fill = hex(el.color)
                    when (el.shape) {
                        "circle" -> sb.append("<circle r=\"${el.w / 2}\" fill=\"$fill\"/>\n")
                        "ring" -> sb.append("<circle r=\"${el.w / 2 * 0.85}\" fill=\"none\" stroke=\"$fill\" stroke-width=\"${el.w * 0.14}\"/>\n")
                        "square" -> sb.append("<rect x=\"${-el.w / 2}\" y=\"${-el.h / 2}\" width=\"${el.w}\" height=\"${el.h}\" fill=\"$fill\"/>\n")
                        "rounded" -> sb.append("<rect x=\"${-el.w / 2}\" y=\"${-el.h / 2}\" width=\"${el.w}\" height=\"${el.h}\" rx=\"${el.w * 0.22}\" fill=\"$fill\"/>\n")
                        "triangle" -> sb.append("<path d=\"M0 ${-el.h / 2} L${el.w / 2} ${el.h / 2} L${-el.w / 2} ${el.h / 2} Z\" fill=\"$fill\"/>\n")
                        "diamond" -> sb.append("<path d=\"M0 ${-el.h / 2} L${el.w / 2} 0 L0 ${el.h / 2} L${-el.w / 2} 0 Z\" fill=\"$fill\"/>\n")
                        "hexagon" -> {
                            val r = el.w / 2
                            val pts = (0 until 6).joinToString(" ") {
                                val a = Math.toRadians(60.0 * it - 30.0)
                                "${"%.1f".format(r * kotlin.math.cos(a).toFloat())},${"%.1f".format(r * kotlin.math.sin(a).toFloat())}"
                            }
                            sb.append("<polygon points=\"$pts\" fill=\"$fill\"/>\n")
                        }
                        "star" -> {
                            val outer = el.w / 2
                            val inner = outer * 0.45f
                            val pts = (0 until 10).joinToString(" ") {
                                val r = if (it % 2 == 0) outer else inner
                                val a = Math.toRadians(36.0 * it - 90.0)
                                "${"%.1f".format(r * kotlin.math.cos(a).toFloat())},${"%.1f".format(r * kotlin.math.sin(a).toFloat())}"
                            }
                            sb.append("<polygon points=\"$pts\" fill=\"$fill\"/>\n")
                        }
                        "pentagon" -> {
                            val r = el.w / 2
                            val pts = (0 until 5).joinToString(" ") {
                                val a = Math.toRadians(72.0 * it - 90.0)
                                "${"%.1f".format(r * kotlin.math.cos(a).toFloat())},${"%.1f".format(r * kotlin.math.sin(a).toFloat())}"
                            }
                            sb.append("<polygon points=\"$pts\" fill=\"$fill\"/>\n")
                        }
                        "heart" -> {
                            val r = el.w / 2
                            sb.append("<path d=\"M0 ${r * 0.35f} C${-r * 1.1f} ${-r * 0.55f} ${-r * 0.45f} ${-r * 1.15f} 0 ${-r * 0.35f} C${r * 0.45f} ${-r * 1.15f} ${r * 1.1f} ${-r * 0.55f} 0 ${r * 0.35f} Z\" fill=\"$fill\"/>\n")
                        }
                        "line" -> sb.append("<rect x=\"${-el.w / 2}\" y=\"${-el.h / 20}\" width=\"${el.w}\" height=\"${el.h / 10}\" fill=\"$fill\"/>\n")
                    }
                }
                "text" -> {
                    val fam = fontMap[el.fontFamily] ?: "sans-serif"
                    val weight: String = if (el.fontWeight >= 600) {
                        "bold"
                    } else {
                        "normal"
                    }
                    val lines = (if (el.uppercase) el.text.uppercase() else el.text).split("\n")
                    val lineH = el.fontSize * 1.15
                    val totalH = lines.size * lineH
                    lines.forEachIndexed { i, line ->
                        val y = -totalH / 2 + lineH * (i + 0.78)
                        sb.append(
                            "<text x=\"0\" y=\"${"%.1f".format(y)}\" font-family=\"$fam\" font-size=\"${el.fontSize}\" " +
                                "font-weight=\"$weight\" fill=\"${hex(el.color)}\" text-anchor=\"middle\" " +
                                "letter-spacing=\"${el.letterSpacing}\" xml:space=\"preserve\">${escapeXml(line)}</text>\n"
                        )
                    }
                }
                "icon", "image" -> {
                    // Растеризуем иконку/фото в PNG и встраиваем как data URI
                    val size = (el.w * k * el.scale).toInt().coerceAtLeast(8)
                    val bmp: Bitmap? = if (el.type == "icon") {
                        rasterizeIcon(el.icon, size, Color(el.color))
                    } else {
                        val b = RenderCache.image(el.imagePath)
                        if (b != null) {
                            val ab = b.asAndroidBitmap()
                            val bounds = com.logoforge.app.editor.render.elementBounds(el)
                            val h = (size * (bounds.height / maxOf(bounds.width, 1f))).toInt().coerceAtLeast(8)
                            Bitmap.createScaledBitmap(ab, size, h, true)
                        } else null
                    }
                    if (bmp != null) {
                        val b64 = ByteArrayOutputStream().also {
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }.toByteArray().let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
                        sb.append("<image href=\"data:image/png;base64,$b64\" x=\"${-bmp.width / 2}\" y=\"${-bmp.height / 2}\" width=\"${bmp.width}\" height=\"${bmp.height}\"$op/>\n")
                    }
                }
            }
            sb.append("</g>\n")
        }
        sb.append("</svg>\n")
        return sb.toString()
    }

    private fun rasterizeIcon(name: String, sizePx: Int, tint: Color): Bitmap? {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        val scope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
        val el = LogoElement(
            type = "icon", icon = name, x = 0f, y = 0f, w = sizePx.toFloat(),
            scale = 1f, rotation = 0f, color = tint.value.toLong() and 0xFFFFFFFFL,
        )
        scope.draw(
            androidx.compose.ui.unit.Density(1f),
            androidx.compose.ui.unit.LayoutDirection.Ltr,
            androidx.compose.ui.graphics.Canvas(canvas),
            androidx.compose.ui.geometry.Size(sizePx.toFloat(), sizePx.toFloat()),
        ) {
            drawElement(el, 1f)
        }
        return bmp
    }

    private fun buildPdf(project: LogoProject, sizePx: Int): ByteArray {
        val bmp = renderBitmap(project, sizePx)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(sizePx, sizePx, 1).create())
        page.canvas.drawBitmap(bmp, 0f, 0f, null)
        doc.finishPage(page)
        val bos = ByteArrayOutputStream()
        doc.writeTo(bos)
        doc.close()
        return bos.toByteArray()
    }

    private fun hex(c: Long): String = String.format("#%08X", c)
    private fun escapeXml(s: String): String {
        val amp = "&" + "amp;"
        val lt = "&" + "lt;"
        val gt = "&" + "gt;"
        val quot = "&" + "quot;"
        val apos = "&" + "#39;"
        return s.replace("&", amp).replace("<", lt).replace(">", gt)
            .replace("\"", quot).replace("'", apos)
    }

    /** Превью проекта (миниатюра для галереи проектов). */
    fun thumbnail(project: LogoProject, sizePx: Int = 400): ByteArray {
        val bmp = renderBitmap(project, sizePx)
        val bos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 90, bos)
        return bos.toByteArray()
    }
}
