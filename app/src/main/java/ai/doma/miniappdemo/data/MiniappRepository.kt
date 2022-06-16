package ai.doma.miniappdemo.data

import ai.doma.miniappdemo.R
import android.content.Context
import com.dwsh.storonnik.DI.FeatureScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton


const val TEST_MINIAPP_URL = "https://dl.dropboxusercontent.com/s/51aocd9cqoh1s8l/www.zip"
const val MINIAPPS_PATH = "miniapps"

@Singleton
class MiniappRepository @Inject constructor(
    val api: RetrofitApi,
    val context: Context
) {

    suspend fun preloadMiniapp(miniappId: String, url: String): String {
        downloadMiniappFromUrl(miniappId, url)
        val rootFile = getMiniapp(context, miniappId)

        // for test
        run {
            val file1 = File(rootFile,"www1")
            if(file1.exists()) file1.renameTo(File(rootFile,"www"))
            val file2 = File(rootFile,"www2")
            if(file2.exists()) file2.renameTo(File(rootFile,"www"))
        }

        val meta = File(rootFile, "www"+ File.separator + "native_config.json")
        if (meta.exists()){
            val json = JSONObject(meta.readText())
            return json.getString("presentation-style")
        }
        return "push_with_navigation"
    }


    suspend fun downloadMiniappFromUrl(miniappId: String, url: String): Boolean {
        val stream = context.resources.openRawResource(R.raw.www)
        return unpackZip(miniappId, stream)
    }


    private fun unpackZip(miniappId: String, inputStream: InputStream): Boolean {
        val path = getMiniapp(context, miniappId)
        val zis: ZipInputStream
        try {
            var filename: String
            zis = ZipInputStream(BufferedInputStream(inputStream))
            var ze: ZipEntry?
            val buffer = ByteArray(1024)
            var count: Int
            while (zis.nextEntry.also { ze = it } != null) {
                filename = ze!!.name
                if (ze!!.isDirectory) {
                    val fmd = File(path, filename)
                    ensureZipPathSafety(fmd, path.path)
                    fmd.mkdirs()
                    continue
                }
                val fout = FileOutputStream("$path/$filename")
                while (zis.read(buffer).also { count = it } != -1) {
                    fout.write(buffer, 0, count)
                }
                fout.close()
                zis.closeEntry()
            }
            zis.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    @Throws(Exception::class)
    private fun ensureZipPathSafety(outputFile: File, destDirectory: String) {
        val destDirCanonicalPath = File(destDirectory).canonicalPath
        val outputFileCanonicalPath = outputFile.canonicalPath
        if (!outputFileCanonicalPath.startsWith(destDirCanonicalPath)) {
            throw Exception(
                java.lang.String.format(
                    "Found Zip Path Traversal Vulnerability with %s",
                    outputFileCanonicalPath
                )
            )
        }
    }

    companion object {
        fun getMiniapp(context: Context, miniappId: String): File {
            return File(context.filesDir.path + File.separator + MINIAPPS_PATH + File.separator + miniappId)
        }
    }

}