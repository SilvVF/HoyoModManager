import java.io.File
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs



object OS {

    private const val APP_NAME = "HoyoModManager"

    enum class OperatingSystem {
        Windows, Linux, MacOS, Unknown
    }

    fun humanReadableByteCountBin(bytes: Long): String {
        val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes.toDouble())
            .toLong()
        if (absB < 1024) {
            return "$bytes B"
        }
        var value = absB
        val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
        var i = 40
        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            ci.next()
            i -= 10
        }
        value *= java.lang.Long.signum(bytes).toLong()
        return String.format("%.1f %ciB", value / 1024.0, ci.current())
    }

    private val currentOperatingSystem: OperatingSystem
        get() {
            val operSys = System.getProperty("os.name").lowercase()
            return if (operSys.contains("win")) {
                OperatingSystem.Windows
            } else if (operSys.contains("nix") || operSys.contains("nux") ||
                operSys.contains("aix")
            ) {
                OperatingSystem.Linux
            } else if (operSys.contains("mac")) {
                OperatingSystem.MacOS
            } else {
                OperatingSystem.Unknown
            }
        }

    fun getDataDir() = when (currentOperatingSystem) {
        OperatingSystem.Windows -> File(System.getenv("AppData"), APP_NAME)
        OperatingSystem.Linux -> File(System.getProperty("user.home"), APP_NAME)
        OperatingSystem.MacOS -> File(System.getProperty("user.home"), APP_NAME)
        else -> throw IllegalStateException("Unsupported operating system")
    }

    // about currentOperatingSystem, see app
    fun getCacheDir() = when (currentOperatingSystem) {
        OperatingSystem.Windows -> File(System.getenv("AppData"), "$APP_NAME/cache")
        OperatingSystem.Linux -> File(System.getProperty("user.home"), ".cache/$APP_NAME")
        OperatingSystem.MacOS -> File(System.getProperty("user.home"), "Library/Caches/$APP_NAME")
        else -> throw IllegalStateException("Unsupported operating system")
    }

    fun getRelativeTimeSpanString(epochSecond: Long): String {
        val now = Instant.now()
        val inputTime = Instant.ofEpochSecond(epochSecond)

        val duration = Duration.between(inputTime, now)

        val seconds = duration.seconds
        val minutes = duration.toMinutes()
        val hours = duration.toHours()
        val days = duration.toDays()

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            days < 7 -> "$days days ago"
            else -> {
                val dateTime = LocalDateTime.ofInstant(inputTime, ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                dateTime.format(formatter)
            }
        }
    }
}