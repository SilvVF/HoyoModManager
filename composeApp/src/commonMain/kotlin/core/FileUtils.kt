package core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.exists


fun interface ProgressListener {

    fun onProgress(total: Long, complete: Long)
}

fun ProgressListener.asKtorListener() = object : io.ktor.client.content.ProgressListener  {
    override suspend fun onProgress(bytesSentTotal: Long, contentLength: Long?) {
        contentLength?.let {
            this@asKtorListener.onProgress(total = contentLength, complete = bytesSentTotal)
        }
    }
}


private object ExtractItemsStandard {

    fun extract(ins: InputStream, outputDir: File, progressListener: ProgressListener) {

        lateinit var  randomAccessFile: RandomAccessFile
        lateinit var inArchive: IInArchive

        val temp = FileUtils.createTempFileFrom(ins)

        try {
            randomAccessFile = RandomAccessFile(temp, "r")
            inArchive = SevenZip.openInArchive(
                null,  // autodetect archive type
                RandomAccessFileInStream(randomAccessFile)
            )

            val itemsToExtract = buildList {
                for (i in 0 ..< inArchive.numberOfItems) {
                    val notFolder = !(inArchive.getProperty(i, PropID.IS_FOLDER) as Boolean)
                    if (notFolder) {
                        add(i)
                    }
                }
            }

            val items = IntArray(itemsToExtract.size) { i -> itemsToExtract[i] }
            inArchive.extract(
                items, false,  // Non-test mode
                ExtractToFileCallback(inArchive, outputDir, progressListener)
            )
        } catch (e: Exception) {
            System.err.println("Error occurs: $e")
        } finally {
            try {
                inArchive.close()
            } catch (e: SevenZipException) {
                System.err.println("Error closing archive: $e")
            }
            try {
                randomAccessFile.close()
            } catch (e: IOException) {
                System.err.println("Error closing file: $e")
            }
        }
    }

    class ExtractToFileCallback(
        private val inArchive: IInArchive,
        private val outputDir: File,
        private val progressListener: ProgressListener
    ) : IArchiveExtractCallback {

        private var hash = 0
        private var size = 0
        private var index = 0
        private var total = 0L
        private var complete = 0L
        private var outputStream: OutputStream? = null

        @Throws(SevenZipException::class)
        override fun getStream(
            index: Int,
            extractAskMode: ExtractAskMode,
        ): ISequentialOutStream {

            this.index = index
            val outputPath = File(outputDir, inArchive.getProperty(index, PropID.PATH).toString())

            outputPath.parentFile.mkdirs()

            outputStream = FileOutputStream(outputPath).takeIf { outputPath.isFile }

            return ISequentialOutStream { data ->
                hash = hash xor data.contentHashCode()
                size += data.size
                outputStream?.write(data)
                data.size // Return amount of processed data
            }
        }

        @Throws(SevenZipException::class)
        override fun setOperationResult(extractOperationResult: ExtractOperationResult) {
            if (extractOperationResult != ExtractOperationResult.OK) {
                System.err.println("Extraction error")
            } else {
                println(
                    String.format(
                        "%9X | %10s | %s", hash, size,
                        inArchive.getProperty(index, PropID.PATH)
                    )
                )
                hash = 0
                size = 0
            }
            outputStream?.close()
        }

        @Throws(SevenZipException::class)
        override fun prepareOperation(extractAskMode: ExtractAskMode) = Unit

        @Throws(SevenZipException::class)
        override fun setCompleted(completeValue: Long) = progressListener.onProgress(total, completeValue.also { complete = it })

        @Throws(SevenZipException::class)
        override fun setTotal(total: Long) = progressListener.onProgress(total.also { this.total = it }, complete)
    }
}

object FileUtils {

    fun createTempFileFrom(inputStream: InputStream): File {
        return File.createTempFile("hmm${UUID.randomUUID()}", ".tmp").apply {
            deleteOnExit()
            outputStream().use { tempOutput ->
                inputStream.copyTo(tempOutput)
            }
        }.also {
            inputStream.close()
        }
    }

    suspend fun renameFolder(file: File, name: String): Result<File>  = withContext(Dispatchers.IO) {
        runCatching {
            val rename = Paths.get(file.parentFile.path, name).also {
                it.toFile().mkdirs()
            }

            Files.move(file.toPath(), rename, StandardCopyOption.REPLACE_EXISTING).toFile()
        }
    }

    fun seperate(vararg parts: String) = parts.reduceIndexed { i, acc, s ->
        if (i == 0) acc + s
        else acc + File.separator + s
    }


    /*
        taken from
        https://stackoverflow.com/questions/36140368/java-renaming-output-file-if-name-already-exists-with-an-increment-taking-int
     */
    private val PATTERN = Pattern.compile("(.*?)(?:\\((\\d+)\\))?(\\.[^.]*)?")

    fun getNewName(path: String): String {

        var filename = path
        val checkExists = { name: String -> Paths.get(name).exists() }

        if (checkExists(filename)) {
            val m: Matcher = PATTERN.matcher(filename)
            if (m.matches()) {
                val prefix: String = m.group(1)
                val last: String? = m.group(2)
                var suffix: String? = m.group(3)
                if (suffix == null) suffix = ""

                var count = last?.toInt() ?: 0

                do {
                    count++
                    filename = "$prefix ($count)$suffix"
                } while (checkExists(filename))
            }
        }
        return filename
    }

    fun extractUsing7z(inputStream: InputStream, outputDir: File, progressListener: ProgressListener) {
        ExtractItemsStandard.extract(inputStream, outputDir, progressListener)
    }
}
