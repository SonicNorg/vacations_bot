package name.nepavel.vacation_bot.service

import name.nepavel.vacation_bot.model.Vacation
import name.nepavel.vacation_bot.repository.DaysRemainingRepository
import org.springframework.stereotype.Component
import java.time.Year

@Component
class DaysRemainingService(private val daysRepo: DaysRemainingRepository) {
    fun getUserDays(chatId: Long, userId: Long, year: Year = Year.now()): Int {
        return daysRepo.getUserDays(chatId, userId, year)
    }

    fun getAllDays(chatId: Long, year: Year = Year.now()): Map<Long, Int> {
        return daysRepo.getAllDays(chatId, year)
    }

    fun plusDays(chatId: Long, userId: Long, days: Int, vacation: Vacation): Int {
        return daysRepo.plusDays(chatId, userId, days, Year.from(vacation.dateStart))
    }

    fun minusDays(chatId: Long, userId: Long, days: Int, vacation: Vacation): Int {
        return daysRepo.minusDays(chatId, userId, days, Year.from(vacation.dateStart))
    }
}