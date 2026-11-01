package com.logoforge.app.editor.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.logoforge.app.data.LogoElement
import com.logoforge.app.data.LogoProject
import com.logoforge.app.icons.IconSet
import kotlin.math.cos
import kotlin.math.sin

/** Дизайн-пространство логотипа — всегда 1000×1000 условных единиц. */
const val DESIGN_SIZE = 1000f

/** Кэш загруженных изображений. */
object RenderCache {
    private val bitmaps = mutableMapOf<String, ImageBitmap>()

    fun image(path: String, maxDim: Int = 2048): ImageBitmap? {
        if (path.isBlank()) return null
        synchronized(bitmaps) {
            bitmaps[path]?.let { return it }
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sample = 1
            while (maxOf(opts.outWidth, opts.outHeight) / sample > maxDim) sample *= 2
            val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                inSampleSize = sample
            }) ?: return null
            val ib = bmp.asImageBitmap()
            bitmaps[path] = ib
            return ib
        }
    }

    fun iconVector(name: String): ImageVector = IconSet.byName(name) ?: IconSet.byName("Star")!!
}

private fun Color(argb: Long) = Color(argb.toInt())

private fun typeface(el: LogoElement): Typeface = when (el.fontFamily) {
    "serif" -> Typeface.SERIF
    "mono" -> Typeface.MONOSPACE
    "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
    else -> Typeface.SANS_SERIF
}

private fun android.graphics.Paint.forText(el: LogoElement) {
    textSize = el.fontSize
    typeface = Typeface.create(typeface(el), if (el.fontWeight >= 600) Typeface.BOLD else Typeface.NORMAL)
    letterSpacing = el.letterSpacing / el.fontSize
    textAlign = android.graphics.Paint.Align.CENTER
    color = android.graphics.Color.argb(
        (el.opacity * 255).toInt(),
        android.graphics.Color.red(el.color.toInt()),
        android.graphics.Color.green(el.color.toInt()),
        android.graphics.Color.blue(el.color.toInt()),
    )
}

/** Габариты элемента в дизайн-единицах (без учёта scale). */
fun elementBounds(el: LogoElement): Rect {
    val w: Float
    val h: Float
    when (el.type) {
        "text" -> {
            val p = android.graphics.Paint().apply { forText(el) }
            val txt = if (el.uppercase) el.text.uppercase() else el.text
            val lines = txt.split("\n")
            w = (lines.maxOfOrNull { p.measureText(it) } ?: 0f) + el.letterSpacing
            h = lines.size * el.fontSize * 1.15f
        }
        "icon" -> { w = el.w; h = el.w }
        "image" -> {
            val bmp = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(el.imagePath, bmp)
            val ratio = if (bmp.outHeight > 0) bmp.outWidth.toFloat() / bmp.outHeight else 1f
            w = el.w; h = if (ratio >= 1f) el.w / ratio else el.w * ratio
        }
        else -> { w = el.w; h = el.h }
    }
    return Rect(el.x - w / 2f, el.y - h / 2f, el.x + w / 2f, el.y + h / 2f)
}

/** Хит-тест: в какой элемент попала точка (в дизайн-координатах). Идём сверху вниз. */
fun hitTest(project: LogoProject, px: Float, py: Float): LogoElement? {
    for (el in project.elements.asReversed()) {
        val dx = px - el.x
        val dy = py - el.y
        val a = Math.toRadians(-el.rotation.toDouble())
        val lx = (dx * cos(a) - dy * sin(a)).toFloat() / el.scale
        val ly = (dx * sin(a) + dy * cos(a)).toFloat() / el.scale
        val b = elementBounds(el)
        if (lx >= b.left && lx <= b.right && ly >= b.top && ly <= b.bottom) return el
    }
    return null
}

/** Общая отрисовка — используется и в превью, и при экспорте. */
fun DrawScope.drawProject(
    project: LogoProject,
    canvasSizePx: Float,
    checkerBackground: Boolean = false,
) {
    val full = Size(canvasSizePx, canvasSizePx)

    if (checkerBackground) {
        // шахматка для прозрачного фона
        val cell = canvasSizePx / 20f
        var y = 0f
        var row = 0
        while (y < canvasSizePx) {
            var x = 0f
            var col = 0
            while (x < canvasSizePx) {
                if ((row + col) % 2 == 0) drawRect(
                    color = Color(0xFFD7CCDD),
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                )
                x += cell; col++
            }
            y += cell; row++
        }
    } else if (project.transparentBg) {
        drawRect(color = Color.Transparent, size = full)
    } else if (project.gradient) {
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color(project.bgColor), Color(project.bgColor2)),
                start = Offset(0f, 0f),
                end = Offset(canvasSizePx, canvasSizePx),
            ),
            size = full,
        )
    } else {
        drawRect(color = Color(project.bgColor), size = full)
    }

    for (el in project.elements) drawElement(el, canvasSizePx / DESIGN_SIZE)
}

fun DrawScope.drawElement(el: LogoElement, s: Float) {
    val alpha = el.opacity.coerceIn(0f, 1f)
    withTransform({
        translate(el.x * s, el.y * s)
        rotate(el.rotation)
        scale(
            el.scale * s * (if (el.flipX) -1f else 1f),
            el.scale * s * (if (el.flipY) -1f else 1f),
            Offset.Zero,
        )
    }) {
        when (el.type) {
            "shape" -> drawShape(el, alpha)
            "text" -> drawTextElement(el, alpha)
            "icon" -> drawIconElement(el, alpha)
            "image" -> drawImageElement(el, alpha)
        }
    }
}

private fun DrawScope.drawShape(el: LogoElement, alpha: Float) {
    val color = Color(el.color)
    val w = el.w
    val h = el.h
    val path = Path()
    when (el.shape) {
        "circle" -> drawCircle(color, radius = w / 2f, center = Offset.Zero, alpha = alpha)
        "ring" -> drawCircle(
            color,
            radius = w / 2f * 0.85f,
            center = Offset.Zero,
            alpha = alpha,
            style = Stroke(width = w * 0.14f),
        )
        "square" -> drawRect(color, topLeft = Offset(-w / 2, -h / 2), size = Size(w, h), alpha = alpha)
        "rounded" -> drawRoundRect(
            color,
            topLeft = Offset(-w / 2, -h / 2),
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.22f),
            alpha = alpha,
        )
        "triangle" -> {
            path.moveTo(0f, -h / 2); path.lineTo(w / 2, h / 2); path.lineTo(-w / 2, h / 2); path.close()
            drawPath(path, color, alpha = alpha)
        }
        "diamond" -> {
            path.moveTo(0f, -h / 2); path.lineTo(w / 2, 0f); path.lineTo(0f, h / 2); path.lineTo(-w / 2, 0f); path.close()
            drawPath(path, color, alpha = alpha)
        }
        "hexagon" -> {
            val r = w / 2f
            for (i in 0 until 6) {
                val ang = Math.toRadians((60.0 * i - 30.0))
                val px = (r * cos(ang)).toFloat()
                val py = (r * sin(ang)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, color, alpha = alpha)
        }
        "star" -> {
            val outer = w / 2f
            val inner = outer * 0.45f
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outer else inner
                val ang = Math.toRadians((36.0 * i - 90.0))
                val px = (r * cos(ang)).toFloat()
                val py = (r * sin(ang)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, color, alpha = alpha)
        }
        "pentagon" -> {
            val r = w / 2f
            for (i in 0 until 5) {
                val ang = Math.toRadians((72.0 * i - 90.0))
                val px = (r * cos(ang)).toFloat()
                val py = (r * sin(ang)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, color, alpha = alpha)
        }
        "heart" -> {
            // сердце из двух дуг и двух прямых
            val r = w / 2f
            path.moveTo(0f, r * 0.35f)
            path.cubicTo(-r * 1.1f, -r * 0.55f, -r * 0.45f, -r * 1.15f, 0f, -r * 0.35f)
            path.cubicTo(r * 0.45f, -r * 1.15f, r * 1.1f, -r * 0.55f, 0f, r * 0.35f)
            path.close()
            drawPath(path, color, alpha = alpha)
        }
        "line" -> drawRect(color, topLeft = Offset(-w / 2, -h / 20f), size = Size(w, h / 10f), alpha = alpha)
        else -> drawCircle(color, radius = w / 2f, center = Offset.Zero, alpha = alpha)
    }
}

private fun DrawScope.drawTextElement(el: LogoElement, alpha: Float) {
    val paint = android.graphics.Paint().apply { forText(el) }
    val lines = (if (el.uppercase) el.text.uppercase() else el.text).split("\n")
    val lineH = el.fontSize * 1.15f
    val totalH = lines.size * lineH
    val native = drawContext.canvas.nativeCanvas
    val save = native.saveLayerAlpha(null, (alpha * 255).toInt())
    lines.forEachIndexed { i, line ->
        val baseline = -totalH / 2f + lineH * (i + 0.78f)
        native.drawText(line, 0f, baseline, paint)
    }
    native.restoreToCount(save)
}

/** Рисует иконку (ImageVector) произвольным tint-цветом. Работает и в превью, и в экспорте. */
fun DrawScope.drawIconElement(el: LogoElement, alpha: Float) {
    val vector = RenderCache.iconVector(el.icon)
    val vw = vector.viewportWidth
    val vh = vector.viewportHeight
    val k = el.w / maxOf(vw, vh)
    withTransform({
        scale(k, k, Offset.Zero)
        translate(-vw / 2f, -vh / 2f)
    }) {
        walkVector(vector.root, Color(el.color), alpha)
    }
}

private fun DrawScope.walkVector(group: VectorGroup, tint: Color, alpha: Float) {
    group.forEach { node ->
        when (node) {
            is VectorGroup -> withTransform({
                translate(node.translationX, node.translationY)
                rotate(node.rotation, Offset(node.pivotX, node.pivotY))
                scale(node.scaleX, node.scaleY, Offset(node.pivotX, node.pivotY))
            }) { walkVector(node, tint, alpha) }
            is VectorPath -> {
                val path = PathParser().addPathNodes(node.pathData).toPath(Path())
                node.fill?.let { drawPath(path, tint, alpha = alpha * node.fillAlpha) }
                node.stroke?.let {
                    drawPath(path, tint, alpha = alpha * node.strokeAlpha,
                        style = Stroke(width = node.strokeLineWidth))
                }
            }
        }
    }
}

private fun DrawScope.drawImageElement(el: LogoElement, alpha: Float) {
    val bmp = RenderCache.image(el.imagePath) ?: return
    val b = elementBounds(el)
    val w = b.width
    val h = b.height
    if (el.cornerRadius <= 0f) {
        drawImage(bmp, dstOffset = IntOffset((-w / 2).toInt(), (-h / 2).toInt()),
            dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)), alpha = alpha)
    } else {
        val r = kotlin.math.min(w, h) * el.cornerRadius
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(-w / 2, -h / 2, w / 2, h / 2,
                    androidx.compose.ui.geometry.CornerRadius(r, r))
            )
        }
        clipPath(path) {
            drawImage(bmp, dstOffset = IntOffset((-w / 2).toInt(), (-h / 2).toInt()),
                dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)), alpha = alpha)
        }
    }
}

/** Рендер проекта в bitmap (для экспорта и миниатюр). */
fun renderProject(project: LogoProject, sizePx: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val androidCanvas = android.graphics.Canvas(bmp)
    val composeCanvas = Canvas(androidCanvas)
    val scope = CanvasDrawScope()
    scope.draw(
        Density(1f),
        LayoutDirection.Ltr,
        composeCanvas,
        Size(sizePx.toFloat(), sizePx.toFloat()),
    ) {
        drawProject(project, sizePx.toFloat())
    }
    return bmp
}
