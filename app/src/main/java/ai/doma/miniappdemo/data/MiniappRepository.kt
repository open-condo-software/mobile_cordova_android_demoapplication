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
                ${foundPlugins.mapNotNull { it.meta }.toSet().joinToString(separator = ",\n") { it }}
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
            WHITELIST("cordova-plugin-whitelist", "\"cordova-plugin-whitelist\": \"1.3.5\"", null),
            SHARE(
                "nl.madebymark.share",
                "\"cordova-plugin-share\": \"0.1.3\"",
                "{ \"id\": \"nl.madebymark.share.Share\",\"file\": \"plugins/nl.madebymark.share/www/share.js\",\"pluginId\": \"nl.madebymark.share\",\"clobbers\": [\"window.navigator.share\" ]}"
            ),
            CAMERA(
                "cordova-plugin-camera",
                "\"cordova-plugin-camera\": \"7.0.0\"",
                "{ \"id\": \"cordova-plugin-camera.Camera\",\"file\": \"plugins/cordova-plugin-camera/www/CameraConstants.js\",\"pluginId\": \"cordova-plugin-camera\",\"clobbers\": [\"Camera\" ]}"

            ),
            CAMERA2(
                "cordova-plugin-camera",
                "\"cordova-plugin-camera\": \"7.0.0\"",
                "{ \"id\": \"cordova-plugin-camera.CameraPopoverOptions\",\"file\": \"plugins/cordova-plugin-camera/www/CameraPopoverOptions.js\",\"pluginId\": \"cordova-plugin-camera\",\"clobbers\": [\"CameraPopoverOptions\" ]}"

            ),
            CAMERA3(
                "cordova-plugin-camera",
                "\"cordova-plugin-camera\": \"7.0.0\"",
                "{ \"id\": \"cordova-plugin-camera.camera\",\"file\": \"plugins/cordova-plugin-camera/www/Camera.js\",\"pluginId\": \"cordova-plugin-camera\",\"clobbers\": [\"navigator.camera\" ]}"

            ),
            CAMERA4(
                "cordova-plugin-camera",
                "\"cordova-plugin-camera\": \"7.0.0\"",
                "{ \"id\": \"cordova-plugin-camera.CameraPopoverHandle\",\"file\": \"plugins/cordova-plugin-camera/www/CameraPopoverHandle.js\",\"pluginId\": \"cordova-plugin-camera\",\"clobbers\": [\"CameraPopoverHandle\" ]}"
            ),
            MEDIA_CAPRUTE(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-media-capture.capture\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-media-capture/www/capture.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-media-capture\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"navigator.device.capture\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            MEDIA_CAPRUTE_HELPERS(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                "    {\n" +
                        "      \"id\": \"cordova-plugin-media-capture.helpers\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-media-capture/www/helpers.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-media-capture\",\n" +
                        "      \"runs\": true\n" +
                        "    }"
            ),
            MEDIA_CAPRUTE_CAO(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-media-capture.CaptureAudioOptions\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-media-capture/www/CaptureAudioOptions.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-media-capture\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"CaptureAudioOptions\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            MEDIA_CAPRUTE_CIO(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-media-capture.CaptureImageOptions\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-media-capture/www/CaptureImageOptions.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-media-capture\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"CaptureImageOptions\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            MEDIA_CAPRUTE_CVO(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-media-capture.CaptureVideoOptions\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-media-capture/www/CaptureVideoOptions.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-media-capture\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"CaptureVideoOptions\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            MEDIA_CAPRUTE_ERROR(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-media-capture.CaptureError\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-media-capture/www/CaptureError.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-media-capture\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"CaptureError\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            MEDIA_CAPRUTE_MEDIA_FILE_DATA(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-media-capture.MediaFileData\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-media-capture/www/MediaFileData.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-media-capture\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"MediaFileData\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            MEDIA_CAPRUTE_MEDIA_FILE(
                "cordova-plugin-media-capture",
                "\"cordova-plugin-media-capture\": \"1.4.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-media-capture.MediaFile\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-media-capture/www/MediaFile.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-media-capture\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"MediaFile\"\n" +
                        "      ]\n" +
                        "    }"
            ),



            FILE_DIRECTORY_ENTRY(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.DirectoryEntry\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/DirectoryEntry.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.DirectoryEntry\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            FILE_DIRECTORY_READER(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.DirectoryReader\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/DirectoryReader.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.DirectoryReader\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            FILE_ENTRY(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.Entry\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/Entry.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.Entry\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            FILE(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.File\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/File.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.File2\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            FILE_FILE_ENTRY(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.FileEntry\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/FileEntry.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.FileEntry\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            FILE_FILE_ERROR(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.FileError\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/FileError.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.FileError\"\n" +
                        "      ]\n" +
                        "    }"
            ),
//            FILE_READER(
//                "cordova-plugin-file",
//                "\"cordova-plugin-file\": \"4.3.3\"",
//                "{\n" +
//                        "      \"id\": \"cordova-plugin-file.FileReader\",\n" +
//                        "      \"file\": \"plugins/cordova-plugin-file/www/FileReader.js\",\n" +
//                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
//                        "      \"clobbers\": [\n" +
//                        "        \"window.FileReader\"\n" +
//                        "      ]\n" +
//                        "    }"
//            ),
            FILE_SYSTEM(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.FileSystem\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/FileSystem.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.FileSystem\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            FILE_UPLOAD_OPT(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.FileUploadOptions\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/FileUploadOptions.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.FileUploadOptions\"\n" +
                        "      ]\n" +
                        "    }"
            ),
            FILE_UPLOAD_RESULT(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "      \"id\": \"cordova-plugin-file.FileUploadResult\",\n" +
                        "      \"file\": \"plugins/cordova-plugin-file/www/FileUploadResult.js\",\n" +
                        "      \"pluginId\": \"cordova-plugin-file\",\n" +
                        "      \"clobbers\": [\n" +
                        "        \"window.FileUploadResult\"\n" +
                        "      ]\n" +
                        "    }"
            ),

            FILE_WRITER(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.FileWriter\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/FileWriter.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"clobbers\": [\n" +
                        "     \"window.FileWriter\"\n" +
                        "     ]\n" +
                        "}"
            ),
            FILE_FLAGS(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.Flags\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/Flags.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"clobbers\": [\n" +
                        "     \"window.Flags\"\n" +
                        "     ]\n" +
                        "}"
            ),
            FILE_LFS(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.LocalFileSystem\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/LocalFileSystem.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"clobbers\": [\n" +
                        "     \"window.LocalFileSystem\"\n" +
                        "     ],\n" +
                        "     \"merges\": [\n" +
                        "     \"window\"\n" +
                        "     ]\n" +
                        "}"
            ),
            FILE_META(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.Metadata\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/Metadata.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"clobbers\": [\n" +
                        "     \"window.Metadata\"\n" +
                        "     ]\n" +
                        "}"
            ),
            FILE_PROGRESS_EVENT(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.ProgressEvent\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/ProgressEvent.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"clobbers\": [\n" +
                        "     \"window.ProgressEvent\"\n" +
                        "     ]\n" +
                        "}"
            ),
            FILE_(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.fileSystems\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/fileSystems.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\"\n" +
                        "}"
            ),
            FILE_REQUEST_FILE_SYSTEM(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.requestFileSystem\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/requestFileSystem.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"clobbers\": [\n" +
                        "     \"window.requestFileSystem\"\n" +
                        "     ]\n" +
                        "}"
            ),
            FILE_W(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.resolveLocalFileSystemURI\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/resolveLocalFileSystemURI.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"merges\": [\n" +
                        "     \"window\"\n" +
                        "     ]\n" +
                        "}"
            ),
            FILE_IS_CHROME(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.isChrome\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/browser/isChrome.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"runs\": true\n" +
                        "}"
            ),
            FILE_FILE_SYSTEM(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.iosFileSystem\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/ios/FileSystem.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"merges\": [\n" +
                        "     \"FileSystem\"\n" +
                        "     ]\n" +
                        "}"
            ),
            FILE_FSR(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.fileSystems-roots\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/fileSystems-roots.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"runs\": true\n" +
                        "}"
            ),
            FILE_FSP(
                "cordova-plugin-file",
                "\"cordova-plugin-file\": \"4.3.3\"",
                "{\n" +
                        "     \"id\": \"cordova-plugin-file.fileSystemPaths\",\n" +
                        "     \"file\": \"plugins/cordova-plugin-file/www/fileSystemPaths.js\",\n" +
                        "     \"pluginId\": \"cordova-plugin-file\",\n" +
                        "     \"merges\": [\n" +
                        "     \"cordova\"\n" +
                        "     ],\n" +
                        "     \"runs\": true\n" +
                        "}"
            ),

        }
    }

}