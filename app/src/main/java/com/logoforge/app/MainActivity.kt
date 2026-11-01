package com.logoforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.logoforge.app.about.AboutScreen
import com.logoforge.app.ai.LogotipenScreen
import com.logoforge.app.data.LogoProject
import com.logoforge.app.editor.EditorScreen
import com.logoforge.app.home.HomeScreen
import com.logoforge.app.ui.theme.LogoForgeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dynamicColor by rememberSaveable { mutableStateOf(true) }
            LogoForgeTheme(dynamicColor = dynamicColor) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LogoForgeApp(dynamicColor = dynamicColor, onDynamicColorChange = { dynamicColor = it })
                }
            }
        }
    }
}

private sealed class Tab { object Home : Tab(); object Ai : Tab(); object About : Tab() }
private data class EditorRoute(val project: LogoProject)

@Composable
fun LogoForgeApp(dynamicColor: Boolean, onDynamicColorChange: (Boolean) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var editor by remember { mutableStateOf<EditorRoute?>(null) }

    val tabs = listOf(
        Triple(0, "Конструктор", Icons.Filled.Home),
        Triple(1, "Logotipen", Icons.Filled.AutoAwesome),
        Triple(2, "О проекте", Icons.Filled.Info),
    )

    BackHandler(enabled = editor != null) {
        editor = null
    }

    val route = editor
    if (route != null) {
        EditorScreen(
            project = route.project,
            onSaveProject = {},
            onBack = { editor = null },
            onOpenAi = { editor = null; tab = 1 },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (idx, label, icon) ->
                    NavigationBarItem(
                        selected = tab == idx,
                        onClick = { tab = idx },
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { pad ->
        when (tab) {
            0 -> HomeScreen(
                onOpenProject = { editor = EditorRoute(it) },
                onOpenAi = { tab = 1 },
                onOpenAbout = { tab = 2 },
                dynamicColor = dynamicColor,
                onDynamicColorChange = onDynamicColorChange,
            )
            1 -> LogotipenScreen(
                onAddToEditor = { editor = EditorRoute(it) },
                onOpenEditor = { editor = EditorRoute(it) },
            )
            2 -> AboutScreen()
        }
    }
}
