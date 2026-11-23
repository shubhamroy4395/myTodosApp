package com.shubham.todos

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableLongStateOf
import com.shubham.todos.data.TodoStore
import com.shubham.todos.ui.chat.ChatColors
import com.shubham.todos.ui.chat.ChatScreen

class MainActivity : ComponentActivity() {

    // Bumped every time you come back to the app, so "Today"/"Yesterday" labels refresh.
    private val resumedAt = mutableLongStateOf(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dark status bar + nav bar with light icons, drawn behind the app (like WhatsApp dark mode)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        // Keyboard pushes the input box up instead of covering it (needed on older Android versions)
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val store = TodoStore.get(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = ChatColors.Send)) {
                ChatScreen(store = store, resumedAt = resumedAt.longValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumedAt.longValue = System.currentTimeMillis()
    }
}