package name.nepavel.vacation_bot.service

import name.nepavel.vacation_bot.KnownException
import name.nepavel.vacation_bot.model.Vacation
import name.nepavel.vacation_bot.repository.VacationsRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Year

@Component
class VacationService(private val daysService: DaysRemainingService, private val vacRepo: VacationsRepository) {
    fun addVacation(chatId: Long, userId: Long, vacation: Vacation): Int {
        if (Year.from(vacation.dateStart) != Year.from(vacation.dateEnd)) {
            throw KnownException("Я пока не понимаю отпуск, переходящий с одного года на другой! Разделите его на два отпуска!")
        }
        listVacations(chatId, userId).forEach { vac ->
            if (vacation.dateEnd.isBetween(vac.dateStart, vac.dateEnd) || vacation.dateStart.isBetween(vac.dateStart, vac.dateEnd))
                throw KnownException("Такой отпуск уже есть! Вот он:\n$vac")
        }
        vacRepo.addVacation(chatId, userId, vacation)
        return daysService.minusDays(chatId, userId, vacation.daysCount(), vacation)
    }

    fun removeVacation(chatId: Long, userId: Long, vacationIndex: Int): Int {
        val vacation = vacRepo.listVacations(chatId, userId)[vacationIndex]
        vacRepo.removeVacation(chatId, userId, vacationIndex)
        return daysService.plusDays(chatId, userId, vacation.daysCount(), vacation)
    }

    fun listVacations(chatId: Long, userId: Long) = vacRepo.listVacations(chatId, userId)

    fun listAllVacations(chatId: Long) = vacRepo.listAllVacations(chatId)

    private fun LocalDate.isBetween(left: LocalDate, right: LocalDate): Boolean =
        isEqual(left)
                || isEqual(right)
                || (isBefore(right) && isAfter(left))
}