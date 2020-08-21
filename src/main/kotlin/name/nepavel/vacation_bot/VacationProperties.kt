package name.nepavel.vacation_bot

import org.springframework.boot.context.properties.ConfigurationProperties
import javax.annotation.PostConstruct

@ConfigurationProperties("vacation")
class VacationProperties {
    var system = SystemProps()
    var app = AppProps()

    class SystemProps {
        lateinit var botUsername: String
        lateinit var botToken: String
        lateinit var maxThreads: String
        lateinit var creatorId: String
        lateinit var dbPath: String
    }

    class AppProps {
        lateinit var defaultDays: String
    }

    @PostConstruct
    fun log() {
        println(system.botUsername)
        println(system.maxThreads)
    }
}