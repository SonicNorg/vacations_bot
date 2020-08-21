package name.nepavel.vacation_bot.bot

import name.nepavel.vacation_bot.KnownException
import name.nepavel.vacation_bot.VacationProperties
import name.nepavel.vacation_bot.dateFormatter
import name.nepavel.vacation_bot.model.Vacation
import name.nepavel.vacation_bot.repository.DaysRemainingRepository
import name.nepavel.vacation_bot.service.VacationService
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.db.DBContext
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Locality
import org.telegram.abilitybots.api.objects.Privacy
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Component
class VacBot(
    val props: VacationProperties,
    val vacService: VacationService,
    val daysRepo: DaysRemainingRepository,
    dbContext: DBContext
) : AbilityBot(
    props.system.botToken,
    props.system.botUsername,
    dbContext,
    DefaultBotOptions().apply { maxThreads = props.system.maxThreads.toInt() }
) {
    private val twoFormatter = DateTimeFormatter.ofPattern("yy")
    private val threeFormatter = DateTimeFormatter.ofPattern("yyy")
    private val fourFormatter = DateTimeFormatter.ofPattern("yyyy")

    override fun creatorId(): Int = props.system.creatorId.toInt()

    private val manual =
        ("Здравствуйте, я Смотритель отпусков. Я заменяю экселечки, гугл-таблички и другие способы учета графика отпусков команды.\n" +
                "Я работаю через команды с параметрами. Например, для добавления одного дня отпуска/отгула напишите `/add 05.08.2000`, диапазона - `/add 05.08.2000 12.08.2000`\n" +
                "По умолчанию у всех 28 дней отпуска. Если у вас есть дополнительные отгулы, добавьте их: `/days +3`, например. Тем же образом можно и отнять, но не забывайте: если вы идете в отгул/отпуск, надо просто добавить даты в бота, и оставшиеся дни уменьшатся сами.\n" +
                "Посмотреть оставшиеся дни отпуска - `/days`, `/all`, `/days 2021`, `/all 21`\n" +
                "Посмотреть график отпусков - `/list`, `/total`\n" +
                "Те, кто ни разу не писал боту, не будут участвовать в статистике: телеграм не дает ботам доступ к списку членов группы.").escape()

    fun start(): Ability {
        return Ability.builder()
            .name("start")
            .info("Первому отпускнику приготовиться")
            .privacy(Privacy.PUBLIC)
            .locality(Locality.USER)
            .enableStats()
            .action { ctx ->
                sender.execute(SendMessage().setChatId(ctx.chatId()).setText(manual).enableMarkdownV2(true))
            }
            .build()
    }

    fun help(): Ability {
        return Ability.builder()
            .name("help")
            .info("Первому отпускнику что сделать?")
            .privacy(Privacy.PUBLIC)
            .locality(Locality.ALL)
            .enableStats()
            .action { ctx ->
                sender.execute(SendMessage().setChatId(ctx.chatId()).setText(manual).enableMarkdownV2(true))
            }
            .build()
    }

    fun userDays(): Ability {
        val help =
            "/days - Посмотреть остаток дней в текущем году\n/days +|-X - добавить/отнять число дней в текущем году\n/days YY +X - добавить X дней (например, дополнительных отгулов) в 20YY году\n/days YY -X - отнять X дней от отпуска в 20YY году"
        return Ability.builder()
            .name("days")
            .info("<YY> <(+|-)X> Посмотреть/добавить/отнять остаток дней отпуска в текущем/YY году. ")
            .privacy(Privacy.PUBLIC)
            .locality(Locality.ALL)
            .enableStats()
            .action { ctx ->
                handling(ctx.chatId(), ctx.update().message.messageId) {
                    val text = when (ctx.arguments().size) {
                        0 -> "Осталось ${daysRepo.getUserDays(
                            ctx.chatId(),
                            ctx.user().id.toLong()
                        )} дней отпуска/отгулов в ${Year.now()} году"
                        1 -> {
                            when {
                                ctx.firstArg().startsWith("+") || ctx.firstArg().startsWith("-") ->
                                    changeYearDays(ctx.chatId(), ctx.user().id.toLong(), ctx.firstArg())
                                ctx.firstArg().matches("^\\d{2,4}$".toRegex()) ->
                                    readYearDays(
                                        ctx.chatId(),
                                        ctx.user().id.toLong(),
                                        parseYear(ctx.firstArg())
                                    )
                                else -> {
                                    help
                                }
                            }
                        }
                        2 -> changeYearDays(
                            ctx.chatId(),
                            ctx.user().id.toLong(),
                            ctx.secondArg(),
                            parseYear(ctx.firstArg())
                        )
                        else -> help
                    }
                    sender.execute(
                        SendMessage()
                            .setChatId(ctx.chatId())
                            .setText(text)
                            .setReplyToMessageId(ctx.update().message.messageId)
                    )
                }
            }
            .build()
    }

    fun allDays(): Ability {
        val help = "/all <YY> - Остаток отпусков у всех в текущем либо YY году"
        return Ability.builder()
            .name("all")
            .info("<YY> Остаток отпусков у всех в текущем/YY году")
            .privacy(Privacy.PUBLIC)
            .locality(Locality.ALL)
            .action { ctx ->
                handling(ctx.chatId(), ctx.update().message.messageId) {
                    val text = when (ctx.arguments().size) {
                        0 -> "Оставшиеся отпуска за ${Year.now()} год:\n" + usersWithDaysToString(
                            daysRepo.getAllDays(ctx.chatId())
                        )
                        1 -> "Оставшиеся отпуска за ${parseYear(ctx.firstArg())} год:\n" + usersWithDaysToString(
                            daysRepo.getAllDays(ctx.chatId(), parseYear(ctx.firstArg()))
                        )
                        else -> help
                    }
                    sender.execute(
                        SendMessage()
                            .setChatId(ctx.chatId())
                            .setText(text.escape())
                            .setReplyToMessageId(ctx.update().message.messageId)
                            .enableMarkdownV2(true)
                    )
                }
            }
            .build()
    }

    fun addVacation(): Ability {
        return Ability.builder()
            .name("add")
            .info("dd.MM.yyyy <dd.MM.yyyy> Добавить отпуск, 1 или 2 даты")
            .privacy(Privacy.PUBLIC)
            .locality(Locality.ALL)
            .action { ctx ->
                handling(ctx.chatId(), ctx.update().message.messageId) {
                    val vacation = when (ctx.arguments().size) {
                        1 -> Vacation(LocalDate.parse(ctx.firstArg(), dateFormatter))
                        2 -> Vacation(
                            LocalDate.parse(ctx.firstArg(), dateFormatter),
                            LocalDate.parse(ctx.secondArg(), dateFormatter)
                        )
                        else -> throw KnownException("Неверный формат команды! Надо указать 1 или 2 даты в формате dd.MM.yyyy!")
                    }
                    val days = vacService.addVacation(ctx.chatId(), ctx.user().id.toLong(), vacation)
                    sender.execute(
                        SendMessage()
                            .setChatId(ctx.chatId())
                            .setText("Добавлен отпуск на ${vacation.daysCount()} календарных дней. Осталось $days дней отпуска.".escape())
                            .setReplyToMessageId(ctx.update().message.messageId)
                            .enableMarkdownV2(true)
                    )
                }
            }
            .build()
    }

    fun removeVacation(): Ability {
        return Ability.builder()
            .name("del")
            .info("X Удалить введенный отпуск под номером X (см. /list)")
            .privacy(Privacy.PUBLIC)
            .locality(Locality.ALL)
            .input(1)
            .action { ctx ->
                handling(ctx.chatId(), ctx.update().message.messageId) {
                    val days = vacService.removeVacation(ctx.chatId(), ctx.user().id.toLong(), ctx.firstArg().toInt())
                    sender.execute(
                        SendMessage()
                            .setChatId(ctx.chatId())
                            .setText("Отпуск удалён, осталось $days дней отпуска".escape())
                            .setReplyToMessageId(ctx.update().message.messageId)
                            .enableMarkdownV2(true)
                    )
                }
            }
            .build()
    }

    fun listVacation(): Ability {
        return Ability.builder()
            .name("list")
            .info("Посмотреть свой график отпусков")
            .privacy(Privacy.PUBLIC)
            .locality(Locality.ALL)
            .action { ctx ->
                handling(ctx.chatId(), ctx.update().message.messageId) {
                    val listVacations = vacService.listVacations(ctx.chatId(), ctx.user().id.toLong())
                    sender.execute(
                        SendMessage()
                            .setChatId(ctx.chatId())
                            .setText(("График ваших отпусков:\n" + listVacations.mapIndexed { i, v -> "$i\t$v" }
                                .joinToString("\n")).escape())
                            .setReplyToMessageId(ctx.update().message.messageId)
                            .enableMarkdownV2(true)
                    )
                }
            }
            .build()
    }

    fun listAllVacation(): Ability {
        return Ability.builder()
            .name("total")
            .info("Посмотреть все графики отпусков")
            .privacy(Privacy.PUBLIC)
            .locality(Locality.ALL)
            .action { ctx ->
                handling(ctx.chatId(), ctx.update().message.messageId) {
                    val users = users()
                    val listVacations = vacService.listAllVacations(ctx.chatId())
                    sender.execute(
                        SendMessage()
                            .setChatId(ctx.chatId())
                            .setText(("График всех отпусков:\n" + listVacations.entries.joinToString("\n") { e ->
                                "${users[e.key.toInt()]?.name(
                                    true
                                )}\n\t${e.value.joinToString("\n\t")}"
                            }).escape())
                            .setReplyToMessageId(ctx.update().message.messageId)
                            .enableMarkdownV2(true)
                    )
                }
            }
            .build()
    }

    private fun handling(chatId: Long, replyToId: Int? = null, action: () -> Unit) {
        try {
            action()
        } catch (e: KnownException) {
            val msg = SendMessage().setChatId(chatId).setText(e.message)
            if (replyToId != null) {
                msg.setReplyToMessageId(replyToId)
            }
            sender.execute(msg)
        } catch (e: NumberFormatException){
            val msg = SendMessage().setChatId(chatId).setText("Неверный формат числа!")
            if (replyToId != null) {
                msg.setReplyToMessageId(replyToId)
            }
            sender.execute(msg)
        } catch (e: DateTimeParseException) {
            val msg = SendMessage().setChatId(chatId).setText("Неизвестный формат даты!")
            if (replyToId != null) {
                msg.setReplyToMessageId(replyToId)
            }
            sender.execute(msg)
        } catch (e: TelegramApiRequestException) {
            silent.send(
                "Exception ${e.message} while sending: code ${e.errorCode}, response ${e.apiResponse}",
                chatId
            )
        } catch (e: Exception) {
            silent.send(e.toString(), chatId)
        }
    }

    private fun parseYear(year: String): Year = Year.parse(
        year, when (year.length) {
            2 -> twoFormatter
            3 -> threeFormatter
            4 -> fourFormatter
            else -> throw KnownException("Неизвестный формат года: $year! Используйте 2, 3 или 4 цифры для обозначения года!")
        }
    )

    private fun readYearDays(chatId: Long, userId: Long, year: Year = Year.now()) =
        "Осталось ${daysRepo.getUserDays(chatId, userId, year)} дней отпуска/отгулов в $year году"

    private fun changeYearDays(chatId: Long, userId: Long, daysWithSign: String, year: Year = Year.now()) = when {
        daysWithSign.startsWith("+") ->
            "Осталось ${daysRepo.plusDays(
                chatId,
                userId,
                daysWithSign.substring(1).toInt(),
                year
            )} дней отпуска/отгулов в $year году"
        daysWithSign.startsWith("-") ->
            "Осталось ${daysRepo.minusDays(
                chatId,
                userId,
                daysWithSign.substring(1).toInt(),
                year
            )} дней отпуска/отгулов в $year году"
        else -> throw KnownException("Изменение оставшихся дней должно начинаться с '+' или '-', а затем число дней!")
    }

    private fun usersWithDaysToString(usersDays: Map<Long, Int>): String {
        val users = users()
        return usersDays.map { userDays -> users[userDays.key.toInt()]!!.name(true) to userDays.value }
            .joinToString("\n") {
                it.first + "\t" + it.second + " дней"
            }
    }

    private fun User.name(link: Boolean): String {
        var result = (firstName ?: "") + " " + (lastName ?: "")
        if (result.isBlank()) {
            result = userName
        }
        if (result.isBlank()) {
            result = id.toString()
        }
        return if (link) "[$result](tg://user?id=${id})" else result
    }

    private fun String.escape(): String = this
        .replace(".", "\\.")
        .replace("-", "\\-")
        .replace("!", "\\!")
}