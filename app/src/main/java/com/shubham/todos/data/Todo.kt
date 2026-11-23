package com.shubham.todos.data

import org.json.JSONArray
import org.json.JSONObject

/** One line inside a bubble. Every line is a todo you can tick. */
data class TodoLine(
    val text: String,
    val done: Boolean = false,
    val doneAt: Long = 0L          // when you ticked it (0 = not done yet)
)

/** One bubble = one message you send, exactly like WhatsApp. */
data class Message(
    val id: String,
    val sentAt: Long,              // the time shown in the bubble + used for "Today" / "Wednesday" headers
    val title: String?,            // e.g. "Jazz" above a numbered list. Not tickable.
    val lines: List<TodoLine>
) {
    val doneCount: Int get() = lines.count { it.done }
}

// Matches "1. ", "2) ", "- ", "• ", "* " at the start of a line
private val LIST_MARKER = Regex("""^(\d+[.)]|[-•*])\s+""")

/**
 * Turns what you typed into (title, todo lines).
 * - Blank lines are dropped.
 * - If the first line is NOT numbered but later lines ARE
 *   ("Jazz" then "1. Radiator fan..."), the first line becomes the title.
 * - Otherwise every line is a todo.
 */
fun parseMessage(raw: String): Pair<String?, List<String>> {
    val rows = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val hasTitle = rows.size >= 2 &&
            !LIST_MARKER.containsMatchIn(rows.first()) &&
            rows.drop(1).any { LIST_MARKER.containsMatchIn(it) }
    return if (hasTitle) rows.first() to rows.drop(1) else null to rows
}

// ---------- Saving to / reading from JSON ----------

fun Message.toJson(): JSONObject {
    val lineArray = JSONArray()
    for (line in lines) {
        lineArray.put(
            JSONObject()
                .put("text", line.text)
                .put("done", line.done)
                .put("doneAt", line.doneAt)
        )
    }
    val obj = JSONObject()
        .put("id", id)
        .put("sentAt", sentAt)
        .put("lines", lineArray)
    title?.let { obj.put("title", it) }
    return obj
}

fun messageFromJson(obj: JSONObject): Message {
    val lineArray = obj.getJSONArray("lines")
    val lines = (0 until lineArray.length()).map { i ->
        val l = lineArray.getJSONObject(i)
        TodoLine(
            text = l.getString("text"),
            done = l.optBoolean("done", false),
            doneAt = l.optLong("doneAt", 0L)
        )
    }
    return Message(
        id = obj.getString("id"),
        sentAt = obj.getLong("sentAt"),
        title = if (obj.has("title")) obj.getString("title") else null,
        lines = lines
    )
}