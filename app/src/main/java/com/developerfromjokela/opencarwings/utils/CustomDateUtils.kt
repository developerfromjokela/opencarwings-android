package com.developerfromjokela.opencarwings.utils

import org.openapitools.client.models.CommandTimerSetting
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object CustomDateUtils {
    private fun createFormatter(format: String): SimpleDateFormat {
        return SimpleDateFormat(format, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun formatMinutesToHHMM(minutes: Int): String {
        if (minutes == 2047 || minutes == 4095) {
            return "--:--"
        }
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, remainingMinutes)
    }

    fun formatToLocalTime(
        date: OffsetTime?
    ): String? {
        if (date == null) return null
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(date.withOffsetSameInstant(ZonedDateTime.now().offset).toLocalTime())
    }

    fun formatToUTCTimerTime(date: OffsetTime?): String? {
        if (date == null) return null
        val localTime = date.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime()
        return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        localTime.hour,
        localTime.minute,
        )
    }

    fun parseToLocalTimerTime(date: String?): OffsetTime? {
        if (date == null) return null

        val formats = listOf("HH:mm", "HH:mm:ss")
        for (format in formats) {
            try {
                return createFormatter(format).parse(date).toInstant().atOffset(ZoneOffset.UTC).toOffsetTime()
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    fun parseAndFormatToLocalTimerTime(date: String?): String? {
        return formatToLocalTime(parseToLocalTimerTime(date))
    }

    fun getFullWeekdays(): Map<Int, String> {
        val calendar = Calendar.getInstance()
        return calendar.getDisplayNames(
            Calendar.DAY_OF_WEEK,
            Calendar.LONG,
            Locale.getDefault()
        ).entries.associateBy({ it.value }) { it.key }
    }

    fun formatCommandTimerDays(timer: CommandTimerSetting): String {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
        }
        val days = calendar.getDisplayNames(
            Calendar.DAY_OF_WEEK,
            Calendar.SHORT,
            Locale.getDefault()
        ).entries.associateBy({ it.value }) { it.key }

        return buildString {
            if (timer.weekdayMon == true) append("${days[Calendar.MONDAY]} | ")
            if (timer.weekdayTue == true) append("${days[Calendar.TUESDAY]} | ")
            if (timer.weekdayWed == true) append("${days[Calendar.WEDNESDAY]} | ")
            if (timer.weekdayThu == true) append("${days[Calendar.THURSDAY]} | ")
            if (timer.weekdayFri == true) append("${days[Calendar.FRIDAY]} | ")
            if (timer.weekdaySat == true) append("${days[Calendar.SATURDAY]} | ")
            if (timer.weekdaySun == true) append("${days[Calendar.SUNDAY]} | ")
        }
    }
}