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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


private object ExtractItemsStandard {

    fun extract(ins: InputStream, outputDir: File) {

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
                ExtractToFileCallback(inArchive, outputDir)
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
    ) : IArchiveExtractCallback {

        private var hash = 0
        private var size = 0
        private var index = 0
        private var outputStream: OutputStream? = null

        @Throws(SevenZipException::class)
        override fun getStream(
            index: Int,
            extractAskMode: ExtractAskMode
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
        override fun prepareOperation(extractAskMode: ExtractAskMode) = Unit

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
                outputStream?.close()
                hash = 0
                size = 0
            }
        }

        @Throws(SevenZipException::class)
        override fun setCompleted(completeValue: Long)= Unit
        @Throws(SevenZipException::class)
        override fun setTotal(total: Long)= Unit
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

    @Throws(IOException::class)
    fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir, zipEntry.name)

        val destDirPath = destinationDir.canonicalPath
        val destFilePath = destFile.canonicalPath

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw IOException("Entry is outside of the target dir: " + zipEntry.name)
        }

        return destFile
    }


    fun extractRar(inputStream: InputStream, outputDir: File) {
        ExtractItemsStandard.extract(inputStream, outputDir)
    }

    fun extract7Zip(inputStream: InputStream, outputDir: File) {
        ExtractItemsStandard.extract(inputStream, outputDir)
    }


    fun extractZip(inputStream: InputStream, outputDir: File) {

        val buffer = ByteArray(1024)

        ZipInputStream(inputStream).use { zis ->

            var zipEntry: ZipEntry?
            while (zis.nextEntry.also { zipEntry = it } != null) {
                val newFile = newFile(outputDir, zipEntry!!)
                if (zipEntry!!.isDirectory) {
                    if (!newFile.isDirectory && !newFile.mkdirs()) {
                        throw IOException("Failed to create directory $newFile")
                    }
                } else {
                    // fix for Windows-created archives
                    val parent = newFile.parentFile
                    if (!parent.isDirectory && !parent.mkdirs()) {
                        throw IOException("Failed to create directory $parent")
                    }

                    // write file content
                    FileOutputStream(newFile).use { fos ->
                        var len: Int
                        while ((zis.read(buffer).also { len = it }) > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
            }
            zis.closeEntry()
        }
    }

}
