package com.shubham.todos.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shubham.todos.data.Message
import com.shubham.todos.data.TodoLine

private val TAIL = 8.dp                     // size of the little triangle at the bubble's top-right
private const val TIME_SPACE_ID = "time-space"

/**
 * An invisible gap at the end of the last line. The time ("8:58 pm ✓✓") is drawn on top of it,
 * so it sits on the same line when there's room, and drops to its own line when there isn't —
 * the same trick WhatsApp uses.
 */
private val timeSpace = mapOf(
    TIME_SPACE_ID to InlineTextContent(
        Placeholder(
            width = 76.sp,
            height = 12.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom
        )
    ) { }
)

/** One WhatsApp-style message bubble with tickable lines. */
@Composable
fun MessageBubble(
    message: Message,
    showTail: Boolean,
    onToggle: (lineIndex: Int) -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 6.dp, top = if (showTail) 6.dp else 2.dp),
        contentAlignment = Alignment.CenterEnd       // your messages sit on the right
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .background(ChatColors.Bubble, if (showTail) TailShape else PlainShape)
                .padding(start = 6.dp, top = 4.dp, end = 6.dp + TAIL, bottom = 4.dp)
        ) {
            Column(Modifier.width(IntrinsicSize.Max)) {
                message.title?.let { title ->
                    TitleLine(title = title, onLongPress = onLongPress)
                }
                message.lines.forEachIndexed { index, line ->
                    TodoLineRow(
                        line = line,
                        isLast = index == message.lines.lastIndex,
                        onToggle = { onToggle(index) },
                        onLongPress = onLongPress
                    )
                }
            }
            // Time + ticks, bottom-right of the bubble
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = timeLabel(message.sentAt), color = ChatColors.Meta, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Ticks(doneCount = message.doneCount, total = message.lines.size)
            }
        }
    }
}

/** "Jazz" above a numbered list. Bold, not tickable. */
@Composable
private fun TitleLine(title: String, onLongPress: () -> Unit) {
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    Text(
        text = title,
        color = ChatColors.BubbleText,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .pointerInput(Unit) { detectTapGestures(onLongPress = { currentOnLongPress() }) }
            .padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 2.dp)
    )
}

/** One todo line: circle + text. Tap = tick / untick. Hold = delete menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoLineRow(
    line: TodoLine,
    isLast: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val text = buildAnnotatedString {
        if (line.done) {
            withStyle(
                SpanStyle(
                    textDecoration = TextDecoration.LineThrough,
                    color = ChatColors.BubbleText.copy(alpha = 0.55f)
                )
            ) {
                append(line.text)
            }
        } else {
            append(line.text)
        }
        if (isLast) appendInlineContent(TIME_SPACE_ID, " ")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        CheckCircle(done = line.done, modifier = Modifier.padding(top = 3.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = ChatColors.BubbleText,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            inlineContent = if (isLast) timeSpace else emptyMap()
        )
    }
}

/** Empty circle = pending. Blue circle with a check = done. */
@Composable
private fun CheckCircle(done: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(16.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        if (done) {
            drawCircle(color = ChatColors.TickBlue)
            val check = Path().apply {
                moveTo(size.width * 0.27f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.69f)
                lineTo(size.width * 0.74f, size.height * 0.35f)
            }
            drawPath(
                path = check,
                color = Color.White,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else {
            drawCircle(
                color = ChatColors.BubbleText.copy(alpha = 0.6f),
                radius = size.minDimension / 2f - strokeWidth / 2f,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

/**
 * WhatsApp ticks, reused to show progress:
 * ✓ grey = nothing done, ✓✓ grey = some done, ✓✓ blue = everything in this message done.
 */
@Composable
private fun Ticks(doneCount: Int, total: Int) {
    val allDone = total > 0 && doneCount == total
    val color = if (allDone) ChatColors.TickBlue else ChatColors.TickGrey
    val double = doneCount > 0
    Canvas(Modifier.size(width = 16.dp, height = 11.dp)) {
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        fun tick(dx: Float) {
            val p = Path().apply {
                moveTo(dx + w * 0.02f, h * 0.55f)
                lineTo(dx + w * 0.22f, h * 0.88f)
                lineTo(dx + w * 0.62f, h * 0.12f)
            }
            drawPath(path = p, color = color, style = stroke)
        }
        if (double) {
            tick(0f)
            tick(w * 0.32f)
        } else {
            tick(w * 0.18f)
        }
    }
}

private val TailShape: Shape = BubbleShape(tail = true)
private val PlainShape: Shape = BubbleShape(tail = false)

/** Rounded bubble. The first message of each day gets WhatsApp's little tail at the top-right. */
private class BubbleShape(private val tail: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { 8.dp.toPx() }
        val t = with(density) { TAIL.toPx() }
        val right = size.width - t           // space on the right is kept for the tail on every bubble
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = right,
                    bottom = size.height,
                    topLeftCornerRadius = CornerRadius(r),
                    topRightCornerRadius = if (tail) CornerRadius.Zero else CornerRadius(r),
                    bottomRightCornerRadius = CornerRadius(r),
                    bottomLeftCornerRadius = CornerRadius(r)
                )
            )
            if (tail) {
                moveTo(right, 0f)
                lineTo(right + t, 0f)
                lineTo(right, t * 1.4f)
                close()
            }
        }
        return Outline.Generic(path)
    }
}