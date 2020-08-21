package name.nepavel.vacation_bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Scope
import org.telegram.abilitybots.api.db.DBContext
import org.telegram.abilitybots.api.db.MapDBContext
import org.telegram.telegrambots.ApiContextInitializer
import java.time.format.DateTimeFormatter

val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")!!

@SpringBootApplication
@EnableConfigurationProperties(VacationProperties::class)
class VacationContext {
    @Bean
    @Scope("singleton")
    fun dbContext(props: VacationProperties): DBContext = MapDBContext.offlineInstance(props.system.dbPath)
}

fun main(args: Array<String>) {
    ApiContextInitializer.init()
    runApplication<VacationContext>(*args)
}