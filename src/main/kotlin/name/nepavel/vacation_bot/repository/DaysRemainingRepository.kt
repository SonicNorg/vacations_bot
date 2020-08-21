package name.nepavel.vacation_bot.repository

import name.nepavel.vacation_bot.VacationProperties
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.db.DBContext
import java.time.Year

@Service
class DaysRemainingRepository(private val db: DBContext, private val props: VacationProperties) {
    private val key = "DAYS"

    fun getUserDays(chatId: Long, userId: Long, year: Year = Year.now()): Int {
        return readMap(db, key, chatId, userId, mapOf(year to props.app.defaultDays.toInt())).getOrDefault(year, props.app.defaultDays.toInt())
    }

    fun getAllDays(chatId: Long, year: Year = Year.now()): Map<Long, Int> {
        val chatUsers = db.getMap<Long, Map<Long, Map<Year, Int>>>(key)!!
        val daysMap = chatUsers.getOrDefault(chatId, mutableMapOf())
        return daysMap.entries.map {
            it.key to (it.value[year] ?: props.app.defaultDays.toInt())
        }.toMap()
    }

    fun plusDays(chatId: Long, userId: Long, days: Int, year: Year = Year.now()): Int {
        return changeDays(chatId, userId, days, year)
    }

    fun minusDays(chatId: Long, userId: Long, days: Int, year: Year = Year.now()): Int {
        return changeDays(chatId, userId, -days, year)
    }

    private fun changeDays(chatId: Long, userId: Long, days: Int, year: Year): Int {
        return persistMap(db, key, chatId, userId, mapOf(year to props.app.defaultDays.toInt())) {
            it.toMutableMap().apply {
                this[year] = (it[year] ?: props.app.defaultDays.toInt()) + days
            }
        }[year]!!
    }
}