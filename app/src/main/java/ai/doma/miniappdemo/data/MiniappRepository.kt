package ai.doma.miniappdemo.data

import ai.doma.feature_miniapps.domain.MINIAPP_SERVER_AUTH_BY_URL_ID
import ai.doma.feature_miniapps.domain.MINIAPP_SERVER_AUTH_ID
import ai.doma.miniappdemo.R
import android.content.Context
import androidx.annotation.Keep
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton


const val TEST_MINIAPP_URL = "https://dl.dropboxusercontent.com/s/51aocd9cqoh1s8l/www.zip"
const val MINIAPPS_PATH = "miniapps"
const val VERSION_META_FILE_NAME = "android.version.meta"


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
            val file1 = File(rootFile, "www1")
            if (file1.exists()) file1.renameTo(File(rootFile, "www"))
            val file2 = File(rootFile, "www2")
            if (file2.exists()) file2.renameTo(File(rootFile, "www"))
        }

        val meta = File(rootFile, "www" + File.separator + "native_config.json")
        if (meta.exists()) {
            val json = JSONObject(meta.readText())
            return json.optString("presentationStyle").ifEmpty { null }
                ?: json.optString("presentation-style").ifEmpty { null }
                ?: "push_with_navigation"
        }
        return "push_with_navigation"
    }



    suspend fun downloadMiniappFromUrl(miniappId: String, url: String): Boolean {
        val stream = when (miniappId) {
            MINIAPP_SERVER_AUTH_BY_URL_ID -> context.resources.openRawResource(R.raw.www)
            else -> throw Exception("only test id supported here")
        }
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
                if (filename.contains("__MACOSX"))
                    continue
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

        val dir = File(path, "www")
        if (dir.isDirectory && dir.exists()) {
            val versionMetaFile = File(dir, VERSION_META_FILE_NAME)
            if (versionMetaFile.exists()) {
                versionMetaFile.delete()
            }
            versionMetaFile.createNewFile()
            versionMetaFile.writeText("1.0.0")


            //меняем файлы кордовы на свои собственные
            val cordovajsFile = File(dir, "cordova.js")
            if(cordovajsFile.exists()){
                cordovajsFile.delete()
            }
            cordovajsFile.createNewFile()
            cordovajsFile.writeBytes(context.resources.openRawResource(R.raw.cordova).readBytes())

            val cordova_pluginsjsFile = File(dir, "cordova_plugins.js")
            if(cordova_pluginsjsFile.exists()){
                cordova_pluginsjsFile.delete()
            }
            cordova_pluginsjsFile.createNewFile()
            cordova_pluginsjsFile.writeBytes(
                makePluginConfig(dir).encodeToByteArray()
            )

            val cordovaJsSrcDir = File(dir, "cordova-js-src")
            if(cordovaJsSrcDir.exists() && cordovaJsSrcDir.isDirectory){
                cordovaJsSrcDir.deleteRecursively()
            }
            cordovaJsSrcDir.mkdir()
            val cordovaJsSrcDirAndroid = File(cordovaJsSrcDir, "android")
            cordovaJsSrcDirAndroid.mkdir()
            val cordovaJsSrcDirPlugin = File(cordovaJsSrcDir, "plugin")
            cordovaJsSrcDirPlugin.mkdir()
            val cordovaJsSrcDirPluginAndroid = File(cordovaJsSrcDirPlugin, "android")
            cordovaJsSrcDirPluginAndroid.mkdir()

            val execFile = File(cordovaJsSrcDir, "exec.js")
            execFile.createNewFile()
            execFile.writeBytes(context.resources.openRawResource(R.raw.exec).readBytes())

            val platformFile = File(cordovaJsSrcDir, "platform.js")
            platformFile.createNewFile()
            platformFile.writeBytes(context.resources.openRawResource(R.raw.platform).readBytes())

            val nativeapiproviderFile = File(cordovaJsSrcDirAndroid, "nativeapiprovider.js")
            nativeapiproviderFile.createNewFile()
            nativeapiproviderFile.writeBytes(context.resources.openRawResource(R.raw.nativeapiprovider).readBytes())

            val promptbasednativeapiFile = File(cordovaJsSrcDirAndroid, "promptbasednativeapi.js")
            promptbasednativeapiFile.createNewFile()
            promptbasednativeapiFile.writeBytes(context.resources.openRawResource(R.raw.promptbasednativeapi).readBytes())

            val appFile = File(cordovaJsSrcDirPluginAndroid, "app.js")
            appFile.createNewFile()
            appFile.writeBytes(context.resources.openRawResource(R.raw.app).readBytes())
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

    private fun makePluginConfig(wwwFile: File): String {
        val foundPlugins = MiniappPlugin.values().filter {
            File(wwwFile, "plugins" + File.separator + it.dir).exists()
        }

        return """
            cordova.define('cordova/plugin_list', function(require, exports, module) {
              module.exports = [
                ${foundPlugins.mapNotNull { it.configJson }.joinToString(separator = ",\n") { it }}
              ];
              module.exports.metadata = {
                ${foundPlugins.mapNotNull { it.meta }.joinToString(separator = ",\n") { it }}
              };
            });
        """.trimIndent()
    }

    companion object {
        fun getMiniapp(context: Context, miniappId: String): File {
            return File(context.filesDir.path + File.separator + MINIAPPS_PATH + File.separator + miniappId)
        }

        @Keep
        enum class MiniappPlugin(val dir: String, val meta: String, val configJson: String?) {
            CONDO(
                "cordova-plugin-condo",
                "\"cordova-plugin-condo\": \"0.0.2\"",
                "{\"id\": \"cordova-plugin-condo.Condo\", \"file\": \"plugins/cordova-plugin-condo/www/condo.js\", \"pluginId\": \"cordova-plugin-condo\", \"clobbers\": [ \"cordova.plugins.condo\" ]}"
            ),
            BLE(
                "cordova-plugin-ble-central",
                "\"cordova-plugin-ble-central\": \"1.5.0\"",
                "{\"id\": \"cordova-plugin-ble-central.ble\", \"file\": \"plugins/cordova-plugin-ble-central/www/ble.js\", \"pluginId\": \"cordova-plugin-ble-central\", \"clobbers\": [ \"ble\" ]}"
            ),
            DEVICE(
                "cordova-plugin-device",
                "\"cordova-plugin-device\": \"2.1.0\"",
                "{\"id\": \"cordova-plugin-device.device\", \"file\": \"plugins/cordova-plugin-device/www/device.js\", \"pluginId\": \"cordova-plugin-device\", \"clobbers\": [ \"device\" ]}"
            ),
            WHITELIST("cordova-plugin-whitelist", "\"cordova-plugin-whitelist\": \"1.3.5\"", null)
        }
    }

}