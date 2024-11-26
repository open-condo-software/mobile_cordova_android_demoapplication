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
            if (cordovajsFile.exists()) {
                cordovajsFile.delete()
            }
            cordovajsFile.createNewFile()
            cordovajsFile.writeBytes(context.resources.openRawResource(R.raw.cordova).readBytes())

            val cordova_pluginsjsFile = File(dir, "cordova_plugins.js")
            if (cordova_pluginsjsFile.exists()) {
                cordova_pluginsjsFile.delete()
            }
            cordova_pluginsjsFile.createNewFile()
            cordova_pluginsjsFile.writeBytes(
                makePluginConfig(dir).encodeToByteArray()
            )

            val cordovaJsSrcDir = File(dir, "cordova-js-src")
            if (cordovaJsSrcDir.exists() && cordovaJsSrcDir.isDirectory) {
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
            nativeapiproviderFile.writeBytes(
                context.resources.openRawResource(R.raw.nativeapiprovider).readBytes()
            )

            val promptbasednativeapiFile = File(cordovaJsSrcDirAndroid, "promptbasednativeapi.js")
            promptbasednativeapiFile.createNewFile()
            promptbasednativeapiFile.writeBytes(
                context.resources.openRawResource(R.raw.promptbasednativeapi).readBytes()
            )

            val appFile = File(cordovaJsSrcDirPluginAndroid, "app.js")
            appFile.createNewFile()
            appFile.writeBytes(context.resources.openRawResource(R.raw.app).readBytes())

            val condoPluginFile = File(dir, "plugins/cordova-plugin-condo/www/condo.js")
            if (condoPluginFile.exists()) {
                condoPluginFile.delete()
            }
            condoPluginFile.createNewFile()
            condoPluginFile.writeBytes(
                context.resources.openRawResource(R.raw.condo_plugin).readBytes()
            )

            val sharePluginDir = File(dir, "plugins/nl.madebymark.share/www")
            if (sharePluginDir.exists()) {
                sharePluginDir.deleteRecursively()
            }
            if (!sharePluginDir.exists()) {
                sharePluginDir.mkdirs()
            }

            val sharePluginFile = File(sharePluginDir, "share.js")
            if (!sharePluginFile.exists()) {
                sharePluginFile.createNewFile()
                sharePluginFile.writeBytes(
                    context.resources.openRawResource(R.raw.share).readBytes()
                )
            }

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
                ${
            foundPlugins.flatMap { it.configJson.toList() }.joinToString(separator = ",\n") { it }
        }
              ];
              module.exports.metadata = {
                ${
            foundPlugins.mapNotNull { it.meta }.toSet().joinToString(separator = ",\n") { it }
        }
              };
            });
        """.trimIndent()
    }

    companion object {
        fun getMiniapp(context: Context, miniappId: String): File {
            return File(context.filesDir.path + File.separator + MINIAPPS_PATH + File.separator + miniappId)
        }

        @Keep
        enum class MiniappPlugin(val dir: String, val meta: String, val configJson: Array<String>) {
            CONDO(
                "cordova-plugin-condo",
                "\"cordova-plugin-condo\": \"0.0.2\"",
                arrayOf("{\"id\": \"cordova-plugin-condo.Condo\", \"file\": \"plugins/cordova-plugin-condo/www/condo.js\", \"pluginId\": \"cordova-plugin-condo\", \"clobbers\": [ \"cordova.plugins.condo\" ]}")
            ),
            BLE(
                "cordova-plugin-ble-central",
                "\"cordova-plugin-ble-central\": \"1.5.0\"",
                arrayOf("{\"id\": \"cordova-plugin-ble-central.ble\", \"file\": \"plugins/cordova-plugin-ble-central/www/ble.js\", \"pluginId\": \"cordova-plugin-ble-central\", \"clobbers\": [ \"ble\" ]}")
            ),
            DEVICE(
                "cordova-plugin-device",
                "\"cordova-plugin-device\": \"2.1.0\"",
                arrayOf("{\"id\": \"cordova-plugin-device.device\", \"file\": \"plugins/cordova-plugin-device/www/device.js\", \"pluginId\": \"cordova-plugin-device\", \"clobbers\": [ \"device\" ]}")
            ),
            WHITELIST(
                "cordova-plugin-whitelist",
                "\"cordova-plugin-whitelist\": \"1.3.5\"",
                arrayOf()
            ),
            SHARE(
                "nl.madebymark.share",
                "\"cordova-plugin-share\": \"0.1.3\"",
                arrayOf("{ \"id\": \"nl.madebymark.share.Share\",\"file\": \"plugins/nl.madebymark.share/www/share.js\",\"pluginId\": \"nl.madebymark.share\",\"clobbers\": [\"window.navigator.share\" ]}")
            ),
            CAMERA(
                "cordova-plugin-camera",
                "\"cordova-plugin-camera\": \"7.0.0\"",
                arrayOf(
                    "{ \"id\": \"cordova-plugin-camera.Camera\",\"file\": \"plugins/cordova-plugin-camera/www/CameraConstants.js\",\"pluginId\": \"cordova-plugin-camera\",\"clobbers\": [\"Camera\" ]}",
                    "{ \"id\": \"cordova-plugin-camera.CameraPopoverOptions\",\"file\": \"plugins/cordova-plugin-camera/www/CameraPopoverOptions.js\",\"pluginId\": \"cordova-plugin-camera\",\"clobbers\": [\"CameraPopoverOptions\" ]}",
                    "{ \"id\": \"cordova-plugin-camera.camera\",\"file\": \"plugins/cordova-plugin-camera/www/Camera.js\",\"pluginId\": \"cordova-plugin-camera\",\"clobbers\": [\"navigator.camera\" ]}",
                    "{ \"id\": \"cordova-plugin-camera.CameraPopoverHandle\",\"file\": \"plugins/cordova-plugin-camera/www/CameraPopoverHandle.js\",\"pluginId\": \"cordova-plugin-camera\",\"clobbers\": [\"CameraPopoverHandle\" ]}"
                )
            ),

            MEDIA_CAPRUTE(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                arrayOf(
                    "{\"id\": \"cordova-plugin-media-capture.capture\",\"file\": \"plugins/cordova-plugin-media-capture/www/capture.js\",\"pluginId\": \"cordova-plugin-media-capture\",\"clobbers\": [\"navigator.device.capture\" ] }",
                    "{\"id\": \"cordova-plugin-media-capture.helpers\",\"file\": \"plugins/cordova-plugin-media-capture/www/helpers.js\",\"pluginId\": \"cordova-plugin-media-capture\",\"runs\": true }",
                    "{\"id\": \"cordova-plugin-media-capture.CaptureAudioOptions\",\"file\": \"plugins/cordova-plugin-media-capture/www/CaptureAudioOptions.js\",\"pluginId\": \"cordova-plugin-media-capture\",\"clobbers\": [\"CaptureAudioOptions\" ] }",
                    "{\"id\": \"cordova-plugin-media-capture.CaptureImageOptions\",\"file\": \"plugins/cordova-plugin-media-capture/www/CaptureImageOptions.js\",\"pluginId\": \"cordova-plugin-media-capture\",\"clobbers\": [\"CaptureImageOptions\" ] }",
                    "{\"id\": \"cordova-plugin-media-capture.CaptureVideoOptions\",\"file\": \"plugins/cordova-plugin-media-capture/www/CaptureVideoOptions.js\",\"pluginId\": \"cordova-plugin-media-capture\",\"clobbers\": [\"CaptureVideoOptions\" ] }",
                    "{\"id\": \"cordova-plugin-media-capture.MediaFileData\",\"file\": \"plugins/cordova-plugin-media-capture/www/MediaFileData.js\",\"pluginId\": \"cordova-plugin-media-capture\",\"clobbers\": [\"MediaFileData\" ] }",
                    "{\"id\": \"cordova-plugin-media-capture.CaptureError\",\"file\": \"plugins/cordova-plugin-media-capture/www/CaptureError.js\",\"pluginId\": \"cordova-plugin-media-capture\",\"clobbers\": [\"CaptureError\" ] }",
                    "{\"id\": \"cordova-plugin-media-capture.MediaFile\",\"file\": \"plugins/cordova-plugin-media-capture/www/MediaFile.js\",\"pluginId\": \"cordova-plugin-media-capture\",\"clobbers\": [\"MediaFile\" ] }"
                ),
            ),
            FILES(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                arrayOf(
                    "{\"id\": \"cordova-plugin-file.DirectoryEntry\",  \"file\": \"plugins/cordova-plugin-file/www/DirectoryEntry.js\",  \"pluginId\": \"cordova-plugin-file\",  \"clobbers\": [    \"window.DirectoryEntry\"  ]}",
                    "{\"id\": \"cordova-plugin-file.DirectoryReader\",\"file\": \"plugins/cordova-plugin-file/www/DirectoryReader.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.DirectoryReader\" ] }",
                    "{\"id\": \"cordova-plugin-file.Entry\",\"file\": \"plugins/cordova-plugin-file/www/Entry.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.Entry\" ] }",
                    "{\"id\": \"cordova-plugin-file.File\",\"file\": \"plugins/cordova-plugin-file/www/File.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.File2\" ] }",
                    "{\"id\": \"cordova-plugin-file.FileEntry\",\"file\": \"plugins/cordova-plugin-file/www/FileEntry.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.FileEntry\" ] }",
                    "{\"id\": \"cordova-plugin-file.FileError\",\"file\": \"plugins/cordova-plugin-file/www/FileError.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.FileError\" ] }",
                    "{\"id\": \"cordova-plugin-file.FileSystem\",\"file\": \"plugins/cordova-plugin-file/www/FileSystem.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.FileSystem\" ] }",
                    "{\"id\": \"cordova-plugin-file.FileUploadOptions\",\"file\": \"plugins/cordova-plugin-file/www/FileUploadOptions.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.FileUploadOptions\" ] }",
                    "{\"id\": \"cordova-plugin-file.FileUploadResult\",\"file\": \"plugins/cordova-plugin-file/www/FileUploadResult.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.FileUploadResult\" ] }",
                    "{\"id\": \"cordova-plugin-file.FileWriter\",\"file\": \"plugins/cordova-plugin-file/www/FileWriter.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.FileWriter\" ] }",
                    "{\"id\": \"cordova-plugin-file.Flags\",\"file\": \"plugins/cordova-plugin-file/www/Flags.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.Flags\" ] }",
                    "{\"id\": \"cordova-plugin-file.LocalFileSystem\",\"file\": \"plugins/cordova-plugin-file/www/LocalFileSystem.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.LocalFileSystem\" ],\"merges\": [\"window\" ] }",
                    "{\"id\": \"cordova-plugin-file.Metadata\",\"file\": \"plugins/cordova-plugin-file/www/Metadata.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.Metadata\" ] }",
                    "{\"id\": \"cordova-plugin-file.ProgressEvent\",\"file\": \"plugins/cordova-plugin-file/www/ProgressEvent.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.ProgressEvent\" ] }",
                    "{\"id\": \"cordova-plugin-file.fileSystems\",\"file\": \"plugins/cordova-plugin-file/www/fileSystems.js\",\"pluginId\": \"cordova-plugin-file\" }",
                    "{\"id\": \"cordova-plugin-file.requestFileSystem\",\"file\": \"plugins/cordova-plugin-file/www/requestFileSystem.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.requestFileSystem\" ] }",
                    "{\"id\": \"cordova-plugin-file.resolveLocalFileSystemURI\",\"file\": \"plugins/cordova-plugin-file/www/resolveLocalFileSystemURI.js\",\"pluginId\": \"cordova-plugin-file\",\"merges\": [\"window\" ] }",
                    "{\"id\": \"cordova-plugin-file.isChrome\",\"file\": \"plugins/cordova-plugin-file/www/browser/isChrome.js\",\"pluginId\": \"cordova-plugin-file\",\"runs\": true }",
                    "{\"id\": \"cordova-plugin-file.iosFileSystem\",\"file\": \"plugins/cordova-plugin-file/www/ios/FileSystem.js\",\"pluginId\": \"cordova-plugin-file\",\"merges\": [\"FileSystem\" ] }",
                    "{\"id\": \"cordova-plugin-file.fileSystems-roots\",\"file\": \"plugins/cordova-plugin-file/www/fileSystems-roots.js\",\"pluginId\": \"cordova-plugin-file\",\"runs\": true }",
                    "{\"id\": \"cordova-plugin-file.fileSystemPaths\",\"file\": \"plugins/cordova-plugin-file/www/fileSystemPaths.js\",\"pluginId\": \"cordova-plugin-file\",\"merges\": [\"cordova\" ],\"runs\": true }",
                    "{\"id\": \"cordova-plugin-file.FileReader\",\"file\": \"plugins/cordova-plugin-file/www/FileReader.js\",\"pluginId\": \"cordova-plugin-file\",\"clobbers\": [\"window.FileReader\"] }"
                )
            )

        }
    }

}