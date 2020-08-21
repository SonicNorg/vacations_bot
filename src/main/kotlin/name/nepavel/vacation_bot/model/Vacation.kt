package name.nepavel.vacation_bot.model

import name.nepavel.vacation_bot.KnownException
import name.nepavel.vacation_bot.dateFormatter
import java.io.Serializable
import java.time.LocalDate

data class Vacation(val dateStart: LocalDate, val dateEnd: LocalDate = dateStart): Serializable {
    companion object {
        private const val serialVersionUID = 20180617103938L
    }
    init {
        if (dateEnd.isBefore(dateStart)) throw KnownException("Дата конца отпуска не может быть меньше даты начала!")
    }

    fun daysCount(): Int = java.time.temporal.ChronoUnit.DAYS.between(dateStart, dateEnd).toInt() + 1

    override fun toString(): String {
        return dateStart.format(dateFormatter) + " - " + dateEnd.format(dateFormatter)
    }
}