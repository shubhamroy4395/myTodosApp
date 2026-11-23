package com.shubham.todos.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shubham.todos.data.Message
import com.shubham.todos.data.TodoStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- What the list shows: date pills and bubbles ----------

private sealed interface ChatRow {
    val key: String
}

private data class DayRow(val dayStart: Long) : ChatRow {
    override val key: String get() = "day-$dayStart"
}

private data class MessageRow(val message: Message, val showTail: Boolean) : ChatRow {
    override val key: String get() = message.id
}

/**
 * Walks the messages oldest -> newest and puts a date pill before the first message of each day.
 * Returned newest-first, because the chat list is drawn from the bottom up (like WhatsApp).
 */
private fun buildRows(messages: List<Message>): List<ChatRow> {
    val rows = ArrayList<ChatRow>()
    var lastDay = Long.MIN_VALUE
    for (m in messages) {
        val day = startOfDay(m.sentAt)
        val firstOfDay = day != lastDay
        if (firstOfDay) {
            rows.add(DayRow(day))
            lastDay = day
        }
        rows.add(MessageRow(m, showTail = firstOfDay))
    }
    rows.reverse()
    return rows
}

// ---------- The screen ----------

/**
 * @param resumedAt changes every time you come back to the app, so "Today" / "Yesterday"
 *                  labels are always fresh.
 */
@Composable
fun ChatScreen(store: TodoStore, resumedAt: Long) {
    val messages = store.messages
    val rows by remember { derivedStateOf { buildRows(messages) } }
    var draft by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // If the app stays open past midnight, roll "Today" over to "Yesterday".
    var midnightTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilTomorrow() + 1_000L)
            midnightTick += 1
        }
    }
    val today = remember(resumedAt, midnightTick) { startOfDay(System.currentTimeMillis()) }

    val total = messages.sumOf { it.lines.size }
    val done = messages.sumOf { it.doneCount }

    fun send() {
        if (draft.isBlank()) return
        store.send(draft)
        draft = ""
        scope.launch { listState.animateScrollToItem(0) }   // jump to the newest message
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatColors.Background)
    ) {
        TopBar(pending = total - done, done = done)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .dottedWallpaper()
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,                    // newest at the bottom, starts scrolled down
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(rows, key = { it.key }) { row ->
                    when (row) {
                        is DayRow -> DatePill(label = dayLabel(row.dayStart, today))
                        is MessageRow -> MessageBubble(
                            message = row.message,
                            showTail = row.showTail,
                            onToggle = { lineIndex ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                store.toggle(row.message.id, lineIndex)
                            },
                            onLongPress = { pendingDelete = row.message }
                        )
                    }
                }
            }
            if (messages.isEmpty()) {
                EmptyHint(Modifier.align(Alignment.Center))
            }
        }

        InputBar(
            value = draft,
            onValueChange = { draft = it },
            onSend = { send() },
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()                             // moves up with the keyboard
        )
    }

    pendingDelete?.let { msg ->
        DeleteDialog(
            message = msg,
            onConfirm = {
                store.delete(msg.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

// ---------- Pieces of the screen ----------

/** Header: avatar, "To dos", and a live pending/done count where WhatsApp shows "You". */
@Composable
private fun TopBar(pending: Int, done: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatColors.TopBar)
            .statusBarsPadding()
            .height(62.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(ChatColors.Avatar),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✓", color = ChatColors.AvatarIcon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = "To dos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                text = if (pending + done == 0) "You" else "$pending pending · $done done",
                color = ChatColors.Subtle,
                fontSize = 13.sp
            )
        }
    }
}

/** "Today" / "Yesterday" / "Wednesday" / "5 November 2026" pill. */
@Composable
private fun DatePill(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = ChatColors.DatePillText,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ChatColors.DatePill)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/** Rounded "Message" box + round send button. Enter = new line, so you can type lists. */
@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ChatColors.InputPill)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(text = "Message", color = ChatColors.InputHint, fontSize = 17.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = ChatColors.BubbleText, fontSize = 17.sp),
                cursorBrush = SolidColor(ChatColors.Send),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(6.dp))
        SendButton(enabled = value.isNotBlank(), onClick = onSend)
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) ChatColors.Send else ChatColors.Send.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val arrow = Path().apply {
                moveTo(w * 0.12f, h * 0.10f)
                lineTo(w * 0.95f, h * 0.50f)
                lineTo(w * 0.12f, h * 0.90f)
                lineTo(w * 0.24f, h * 0.50f)
                close()
            }
            drawPath(path = arrow, color = Color.White)
        }
    }
}

@Composable
private fun EmptyHint(modifier: Modifier = Modifier) {
    Text(
        text = "Type your todos below, one per line.\nTap a line to tick it. Hold a message to delete it.",
        color = ChatColors.DatePillText,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ChatColors.DatePill)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun DeleteDialog(message: Message, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val preview = message.title ?: message.lines.firstOrNull()?.text.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ChatColors.Dialog,
        titleContentColor = Color.White,
        textContentColor = ChatColors.DatePillText,
        title = { Text("Delete this message?") },
        text = { Text(preview, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = ChatColors.Send) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ChatColors.Subtle) }
        }
    )
}

/** Faint dot pattern, standing in for WhatsApp's doodle wallpaper. */
private fun Modifier.dottedWallpaper(): Modifier = this.drawBehind {
    val step = 40.dp.toPx()
    val radius = 1.6.dp.toPx()
    var y = step / 2f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) step / 2f else step
        while (x < size.width) {
            drawCircle(color = ChatColors.Pattern, radius = radius, center = Offset(x, y))
            x += step
        }
        y += step
        row++
    }
}