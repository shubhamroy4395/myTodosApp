package com.shubham.todos.data

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Keeps every message in memory (so the screen updates instantly) and
 * saves them to "todos.json" in the app's private storage after every change.
 * No database, no extra libraries.
 */
class TodoStore private constructor(context: Context) {

    private val dir = context.filesDir
    private val file = AtomicFile(File(dir, "todos.json"))
    private val writer = Executors.newSingleThreadExecutor()   // saves happen off the main thread, in order

    /** Oldest first, like a chat. The screen reads this list directly. */
    val messages = mutableStateListOf<Message>()

    init {
        load()
    }

    /** Called when you press send. Each non-empty line becomes a todo. */
    fun send(raw: String) {
        val (title, rows) = parseMessage(raw)
        if (rows.isEmpty()) return
        messages.add(
            Message(
                id = UUID.randomUUID().toString(),
                sentAt = System.currentTimeMillis(),
                title = title,
                lines = rows.map { TodoLine(text = it) }
            )
        )
        save()
    }

    /** Tap a line: done <-> not done. */
    fun toggle(messageId: String, lineIndex: Int) {
        val i = messages.indexOfFirst { it.id == messageId }
        if (i == -1) return
        val msg = messages[i]
        val line = msg.lines.getOrNull(lineIndex) ?: return
        val updated = if (line.done) {
            line.copy(done = false, doneAt = 0L)
        } else {
            line.copy(done = true, doneAt = System.currentTimeMillis())
        }
        val newLines = msg.lines.toMutableList()
        newLines[lineIndex] = updated
        messages[i] = msg.copy(lines = newLines)
        save()
    }

    /** Long-press a bubble -> Delete. */
    fun delete(messageId: String) {
        val i = messages.indexOfFirst { it.id == messageId }
        if (i == -1) return
        messages.removeAt(i)
        save()
    }

    private fun load() {
        if (!file.baseFile.exists()) return          // first launch
        try {
            val text = String(file.readFully(), Charsets.UTF_8)
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                messages.add(messageFromJson(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // File couldn't be read. Keep a copy so nothing is lost, then start empty.
            messages.clear()
            try {
                file.baseFile.copyTo(File(dir, "todos-unreadable.json"), overwrite = true)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun save() {
        val arr = JSONArray()
        for (m in messages) arr.put(m.toJson())
        val bytes = arr.toString().toByteArray(Charsets.UTF_8)
        writer.execute {
            val out = try {
                file.startWrite()
            } catch (e: IOException) {
                return@execute
            }
            try {
                out.write(bytes)
                file.finishWrite(out)
            } catch (e: IOException) {
                file.failWrite(out)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: TodoStore? = null

        /** One shared store for the whole app (survives screen rotation). */
        fun get(context: Context): TodoStore =
            instance ?: synchronized(this) {
                instance ?: TodoStore(context.applicationContext).also { instance = it }
            }
    }
}