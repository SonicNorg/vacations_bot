package name.nepavel.vacation_bot.repository

import org.telegram.abilitybots.api.db.DBContext

fun <T> persistMap(dbContext: DBContext, key: String, chatId: Long, userId: Long, default: T, op: (T) -> T): T {
    val chatsOfKey = dbContext.getMap<Long, Map<Long, T>>(key)!!
    val usersOfChat = chatsOfKey.getOrDefault(chatId, mapOf()).toMutableMap()
    val userInfo: T = usersOfChat.getOrDefault(userId, default)
    usersOfChat[userId] = op(userInfo)
    chatsOfKey[chatId] = usersOfChat
    return usersOfChat[userId]!!
}

fun <T> readMap(dbContext: DBContext, key: String, chatId: Long, userId: Long, default: T): T {
    val chatsOfKey = dbContext.getMap<Long, Map<Long, T>>(key)!!
    val usersOfChat = chatsOfKey.getOrDefault(chatId, mapOf()).toMutableMap()
    return usersOfChat.getOrDefault(userId, default)
}