package com.logoforge.app.templates

import com.logoforge.app.data.LogoElement
import com.logoforge.app.data.LogoProject
import java.util.UUID

data class Template(val id: String, val name: String, val build: () -> LogoProject)

private fun el(
    type: String, x: Float, y: Float,
    w: Float = 400f, h: Float = 400f, color: Long = 0xFF6750A4,
    scale: Float = 1f, rotation: Float = 0f, opacity: Float = 1f,
    shape: String = "circle", text: String = "Logo", fontSize: Float = 120f,
    fontFamily: String = "sans", fontWeight: Int = 700, icon: String = "Star",
    cornerRadius: Float = 0f,
) = LogoElement(
    id = UUID.randomUUID().toString(),
    type = type, x = x, y = y, w = w, h = h, color = color,
    scale = scale, rotation = rotation, opacity = opacity,
    shape = shape, text = text, fontSize = fontSize,
    fontFamily = fontFamily, fontWeight = fontWeight, icon = icon,
    cornerRadius = cornerRadius,
)

val Templates = listOf(
    Template("coffee", "Кофейня") {
        LogoProject(name = "Кофейня").apply {
            bgColor = 0xFFEFEBE9
            elements = listOf(
                el("shape", 500f, 420f, 460f, 460f, 0xFF6D4C41, shape = "rounded"),
                el("icon", 500f, 420f, 240f, 240f, 0xFFEFEBE9, icon = "Coffee"),
                el("text", 500f, 750f, color = 0xFF4E342E, text = "COFFEE", fontSize = 110f, fontWeight = 700),
            )
        }
    },
    Template("tech", "Тех-стартап") {
        LogoProject(name = "Тех-стартап").apply {
            bgColor = 0xFF121212
            elements = listOf(
                el("shape", 500f, 380f, 480f, 480f, 0xFF7C4DFF, shape = "hexagon"),
                el("icon", 500f, 380f, 230f, 230f, 0xFFFFFFFF, icon = "Bolt"),
                el("text", 500f, 740f, color = 0xFFB39DDB, text = "TECH", fontSize = 130f, fontWeight = 700),
            )
        }
    },
    Template("sport", "Спорт") {
        LogoProject(name = "Спорт").apply {
            bgColor = 0xFFE8F5E9
            elements = listOf(
                el("shape", 500f, 400f, 470f, 470f, 0xFF2E7D32, shape = "circle"),
                el("icon", 500f, 400f, 240f, 240f, 0xFFFFFFFF, icon = "FitnessCenter"),
                el("text", 500f, 740f, color = 0xFF1B5E20, text = "POWER", fontSize = 120f, fontWeight = 700),
            )
        }
    },
    Template("beauty", "Салон красоты") {
        LogoProject(name = "Салон красоты").apply {
            bgColor = 0xFFFCE4EC
            elements = listOf(
                el("shape", 500f, 410f, 440f, 440f, 0xFFAD1457, shape = "rounded"),
                el("icon", 500f, 410f, 220f, 220f, 0xFFF8BBD0, icon = "Spa"),
                el("text", 500f, 745f, color = 0xFF880E4F, text = "BEAUTY", fontSize = 105f,
                    fontFamily = "serif", fontWeight = 700),
            )
        }
    },
    Template("games", "Гейминг") {
        LogoProject(name = "Гейминг").apply {
            bgColor = 0xFF1A237E
            elements = listOf(
                el("shape", 500f, 400f, 500f, 500f, 0xFF3949AB, shape = "square", rotation = 45f),
                el("icon", 500f, 400f, 250f, 250f, 0xFFEA80FC, icon = "SportsEsports"),
                el("text", 500f, 745f, color = 0xFFEA80FC, text = "GG CLUB", fontSize = 100f, fontWeight = 700),
            )
        }
    },
    Template("eco", "Эко-бренд") {
        LogoProject(name = "Эко-бренд").apply {
            bgColor = 0xFFF1F8E9
            elements = listOf(
                el("shape", 500f, 410f, 450f, 450f, 0xFF558B2F, shape = "circle"),
                el("icon", 500f, 410f, 230f, 230f, 0xFFFFFFFF, icon = "Eco"),
                el("text", 500f, 750f, color = 0xFF33691E, text = "ECO", fontSize = 120f, fontWeight = 700),
            )
        }
    },
    Template("music", "Музыка") {
        LogoProject(name = "Музыка").apply {
            bgColor = 0xFF311B92
            elements = listOf(
                el("shape", 500f, 400f, 460f, 460f, 0xFF673AB7, shape = "circle"),
                el("icon", 500f, 400f, 230f, 230f, 0xFFFFFFFF, icon = "MusicNote"),
                el("text", 500f, 740f, color = 0xFFD1C4E9, text = "SOUND", fontSize = 115f, fontWeight = 700),
            )
        }
    },
    Template("photo", "Фотостудия") {
        LogoProject(name = "Фотостудия").apply {
            bgColor = 0xFF212121
            elements = listOf(
                el("shape", 500f, 400f, 470f, 470f, 0xFF424242, shape = "rounded"),
                el("icon", 500f, 400f, 220f, 220f, 0xFFFFD54F, icon = "Camera"),
                el("text", 500f, 745f, color = 0xFFEEEEEE, text = "STUDIO", fontSize = 110f, fontWeight = 700),
            )
        }
    },
)
