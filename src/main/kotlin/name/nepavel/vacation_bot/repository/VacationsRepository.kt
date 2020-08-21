package name.nepavel.vacation_bot.repository

import name.nepavel.vacation_bot.model.Vacation
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.db.DBContext

@Service
class VacationsRepository(private val db: DBContext) {
    private val key = "VACS"

    fun addVacation(chatId: Long, userId: Long, vacation: Vacation) {
        persistMap(db, key, chatId, userId, listOf<Vacation>()) { userVacations ->
            userVacations.toMutableList().apply {
                add(vacation)
                sortBy { it.dateStart }
            }
        }
    }

    fun removeVacation(chatId: Long, userId: Long, vacationIndex: Int) {
        persistMap(db, key, chatId, userId, listOf<Vacation>()) { userVacations ->
            userVacations.toMutableList().apply {
                removeAt(vacationIndex)
                sortBy { it.dateStart }
            }
        }
    }

    fun listVacations(chatId: Long, userId: Long): List<Vacation> {
        val chatUsers = db.getMap<Long, Map<Long, List<Vacation>>>(key)!!
        return chatUsers.getOrDefault(chatId, mapOf())!!.getOrDefault(userId, listOf())
    }

    fun listAllVacations(chatId: Long): Map<Long, List<Vacation>> {
        val chatUsers = db.getMap<Long, Map<Long, List<Vacation>>>(key)!!
        return chatUsers.getOrDefault(chatId, mapOf())
    }
}