import java.io.File
import java.text.CharacterIterator
import java.text.StringCharacterIterator
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
}