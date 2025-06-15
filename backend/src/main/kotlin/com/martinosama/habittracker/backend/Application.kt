package com.martinosama.habittracker.backend

import com.martinosama.habittracker.backend.controller.habitRoutes
import com.martinosama.habittracker.backend.controller.reminderRoutes
import com.martinosama.habittracker.backend.controller.userRoutes
import com.martinosama.habittracker.backend.entity.Habits
import com.martinosama.habittracker.backend.entity.Reminders
import com.martinosama.habittracker.backend.entity.Users
import com.martinosama.habittracker.backend.repository.HabitRepository
import com.martinosama.habittracker.backend.repository.ReminderRepository
import com.martinosama.habittracker.backend.repository.UserRepository
import com.martinosama.habittracker.backend.service.HabitService
import com.martinosama.habittracker.backend.service.ReminderService
import com.martinosama.habittracker.backend.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

val logger = LoggerFactory.getLogger("HabitTrackerBackend")

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    configureSerialization()
    connectToDatabase()

    routing {
        val userRepository = UserRepository()
        val userService = UserService(userRepository)

        val habitRepository = HabitRepository()
        val habitService = HabitService(habitRepository)

        val reminderRepository = ReminderRepository()
        val reminderService = ReminderService(reminderRepository)

        userRoutes(userService)
        habitRoutes(habitService)
        reminderRoutes(reminderService)

        get("/") {
            call.respondText("✅ Ktor Backend is running!")
        }
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            serializersModule = SerializersModule {
                contextual(UUID::class, UUIDSerializer)
                contextual(LocalDate::class, LocalDateSerializer)
                contextual(LocalTime::class, LocalTimeSerializer)
            }
        })
    }
}

fun connectToDatabase() {
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/habit_db"
    val dbUser = System.getenv("DB_USER") ?: "habit_user"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "habit_pass"

    try {
        Database.connect(
            url = dbUrl,
            driver = "org.postgresql.Driver",
            user = dbUser,
            password = dbPassword
        )
        transaction {
            try {
                SchemaUtils.create(Users, Habits, Reminders)
                logger.info("Database tables created successfully")
            } catch (e: Exception) {
                logger.error("Failed to create tables: ${e.message}", e)
                throw e
            }
        }
    } catch (e: Exception) {
        logger.error("Database connection failed: ${e.message}", e)
        throw e
    }
}

suspend fun ApplicationCall.respondException(e: Exception) {
    logger.error("Internal server error: ${e.message}", e)
    respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal Server Error"))
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

object LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), formatter)
    }
}

object LocalTimeSerializer : KSerializer<LocalTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override val descriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.parse(decoder.decodeString(), formatter)
    }
}