import java.io.File

object OS {

    private const val APP_NAME = "HoyoModManager"

    enum class OperatingSystem {
        Windows, Linux, MacOS, Unknown
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