package com.shubham.todos.ui.chat

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val DAY_MS = 86_400_000L

private val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)
private val weekdayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
private val fullDateFormat = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)

/** "8:58 pm" — the time inside each bubble. */
fun timeLabel(millis: Long): String =
    timeFormat.format(Date(millis)).lowercase(Locale.ENGLISH)

/** Midnight at the start of that day. Used to group messages by day. */
fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Milliseconds until the next midnight (so "Today" becomes "Yesterday" on time). */
fun millisUntilTomorrow(now: Long = System.currentTimeMillis()): Long =
    startOfDay(now) + DAY_MS - now

/**
 * WhatsApp-style date pill:
 * Today, Yesterday, a weekday name for the last week, else "5 November 2026".
 */
fun dayLabel(dayStart: Long, todayStart: Long): String {
    val daysAgo = ((todayStart - dayStart).toDouble() / DAY_MS).roundToInt()
    return when (daysAgo) {
        0 -> "Today"
        1 -> "Yesterday"
        in 2..6 -> weekdayFormat.format(Date(dayStart))
        else -> fullDateFormat.format(Date(dayStart))
    }
}