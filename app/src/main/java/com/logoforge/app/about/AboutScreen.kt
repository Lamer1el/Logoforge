package com.logoforge.app.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.logoforge.app.R

private fun openLink(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("О приложении") }) },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher),
                contentDescription = "Логотип LogoForge",
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(12.dp))
            Text("LogoForge", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Конструктор логотипов • версия $version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Создатели", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    CreatorRow("Skitfe & Lamer", "https://t.me/lamer42")
                    Spacer(Modifier.height(8.dp))
                    CreatorRow("ForkStudio", "https://t.me/Forkstudio_official")
                }
            }
            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Telegram проекта", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    LinkRow("t.me/logoforgeapp", "https://t.me/logoforgeapp")
                }
            }
            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Возможности", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Конструктор с фигурами, иконками и текстом",
                        "Импорт фотографий из галереи",
                        "Слои, поворот, масштаб, прозрачность",
                        "Logotipen — ИИ, создающий логотип по описанию",
                        "Экспорт в PNG, JPEG, WebP, SVG и PDF",
                        "Material You — динамическая тема",
                        "Шаблоны для быстрого старта",
                        "Undo / Redo, автосохранение проектов",
                    ).forEach {
                        Row(
                            Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Star, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "LogoForge • skitfe & lamer & ForkStudio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreatorRow(name: String, url: String) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { openLink(context, url) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Code, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                url.replace("https://", ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(Icons.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { openLink(context, url) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Groups, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
