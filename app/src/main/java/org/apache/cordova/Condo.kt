package org.apache.cordova

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.CoreModule
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.data.RetrofitApi
import ai.doma.miniappdemo.domain.MiniappBackStack
import ai.doma.miniappdemo.domain.MiniappBackStackEntry
import ai.doma.miniappdemo.ext.logD
import android.webkit.ValueCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.Interceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.Buffer
import java.util.*
object CondoPluginState {
    var payload: String? = null
}
class Condo : CordovaPlugin() {
    private var authState: String? = null

    var api: RetrofitApi? = null
    init {
        api = CoreComponent.get()?.retrofitApi
        logD { "CondoPlugin init api: $api" }
    }

    val scope = CoroutineScope(Dispatchers.IO)
    protected fun finalize() {
        scope.cancel()
    }

    @Throws(JSONException::class)
    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        if (action == ACTION_REQUEST_AUTH_CODE) {
            scope.launch {
                requestAuthCode(clientID = args.getString(0))
                    .collectAndTrace {
                        callbackContext.success(it)
                    }
            }
        }
        if (action == ACTION_REQUEST_AUTH) {
            scope.launch {
                requestAuthCode(clientID = args.getString(0))
                    .flatMapLatest { code ->
                        requestAuth(
                            code = code,
                            clientID = args.getString(0),
                            clientSecret = args.getString(1)
                        )
                    }
                    .collectAndTrace {
                        callbackContext.success(it)
                    }
            }
        }
        if (action == ACTION_REQUEST_SERVER_AUTH) {
            scope.launch {
                requestFullAuth(
                    client_id = args.getString(0),
                    redirect_uri = args.getString(1),
                )
                    .collectAndTrace { response ->

                        if(response.isSuccessful){
                            val request = response.raw().request
                            val lastRedirect = request.url.toString()
                            callbackContext.success(lastRedirect)
                        } else {
                            callbackContext.sendPluginResult(PluginResult(PluginResult.Status.ERROR))
                        }
                    }
            }
        }

        if (action == ACTION_REQUEST_SERVER_AUTH_BY_URL) {
            scope.launch {
                requestFullAuthByUrl(url = args.getString(0)).collectAndTrace { response ->

                    if(response.isSuccessful){
                        val request = response.raw().request
                        val lastRedirect = request.url.toString()
                        val lastRedirectBody = okio.Buffer().apply{ request.body?.writeTo(this) }.readUtf8()
                        val json = JSONObject()
                        json.put("url", lastRedirect)
                        json.put("body", lastRedirectBody)
                        json.put("status", lastRedirectBody)
                        callbackContext.success(json)
                    } else {
                        callbackContext.sendPluginResult(PluginResult(PluginResult.Status.ERROR))
                    }


                }
            }
        }
        if (action == ACTION_GET_CURRENT_RESIDENT){
            val testJson = "{\"_label_\":\"37c0f190-3daf-4836-b47e-8251d6004274\",\"address\":\"г Владивосток, Океанский пр-кт, д 20\",\"addressKey\":\"d6af941f-a495-4331-9724-2048bf47ff0a\",\"addressMeta\":{\"data\":{\"capital_marker\":\"2\",\"city\":\"Владивосток\",\"city_district\":\"Ленинский\",\"city_district_type\":\"р-н\",\"city_district_type_full\":\"район\",\"city_district_with_type\":\"Ленинский р-н\",\"city_fias_id\":\"7b6de6a5-86d0-4735-b11a-499081111af8\",\"city_kladr_id\":\"2500000100000\",\"city_type\":\"г\",\"city_type_full\":\"город\",\"city_with_type\":\"г Владивосток\",\"country\":\"Россия\",\"country_iso_code\":\"RU\",\"federal_district\":\"Дальневосточный\",\"fias_actuality_state\":\"0\",\"fias_id\":\"21440ab7-280c-4401-aab6-32f6176ece60\",\"fias_level\":\"8\",\"geo_lat\":\"43.119392\",\"geo_lon\":\"131.88748\",\"geoname_id\":\"2013348\",\"house\":\"20\",\"house_fias_id\":\"21440ab7-280c-4401-aab6-32f6176ece60\",\"house_kladr_id\":\"2500000100003660131\",\"house_type\":\"д\",\"house_type_full\":\"дом\",\"kladr_id\":\"2500000100003660131\",\"okato\":\"05401364000\",\"oktmo\":\"05701000001\",\"postal_code\":\"690091\",\"qc_geo\":\"0\",\"region\":\"Приморский\",\"region_fias_id\":\"43909681-d6e1-432d-b61f-ddac393cb5da\",\"region_iso_code\":\"RU-PRI\",\"region_kladr_id\":\"2500000000000\",\"region_type\":\"край\",\"region_type_full\":\"край\",\"region_with_type\":\"Приморский край\",\"street\":\"Океанский\",\"street_fias_id\":\"d759fede-e5dc-4cf7-9d95-53a851778847\",\"street_kladr_id\":\"25000001000036600\",\"street_type\":\"пр-кт\",\"street_type_full\":\"проспект\",\"street_with_type\":\"Океанский пр-кт\",\"tax_office\":\"2536\",\"tax_office_legal\":\"2536\"},\"unrestricted_value\":\"690091, Приморский край, г Владивосток, Ленинский р-н, Океанский пр-кт, д 20\",\"value\":\"г Владивосток, Океанский пр-кт, д 20\"},\"createdAt\":\"2023-06-26T15:16:48.908Z\",\"createdBy\":{\"_label_\":\"Дзампаев Батраз Олегович -- <d6e45786-9c22-4deb-a011-3440c05c0318>\",\"createdAt\":\"2022-02-24T12:54:09.711Z\",\"dv\":1,\"id\":\"d6e45786-9c22-4deb-a011-3440c05c0318\",\"name\":\"Дзампаев Батраз Олегович\",\"type\":\"resident\",\"updatedAt\":\"2022-08-22T09:39:13.837Z\",\"v\":3},\"dv\":1,\"id\":\"37c0f190-3daf-4836-b47e-8251d6004274\",\"organization\":{\"_label_\":\"Владивосток Топ -- <fb0d418c-f104-44c5-815d-15265c9925f8>\",\"country\":\"ru\",\"dv\":1,\"id\":\"fb0d418c-f104-44c5-815d-15265c9925f8\",\"name\":\"Владивосток Топ\",\"tin\":\"7451405877\",\"type\":\"MANAGING_COMPANY\",\"updatedAt\":\"2024-02-15T13:45:18.854Z\",\"v\":3},\"organizationFeatures\":{\"hasBillingData\":true,\"hasMeters\":true},\"paymentCategories\":[{\"acquiringName\":\"Расчетный Банк\",\"billingName\":\"Реестровый обмен\",\"categoryName\":\"Квартплата\",\"id\":\"1\"},{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Электричество\",\"id\":\"3\"},{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Газ\",\"id\":\"4\"},{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Холодная вода\",\"id\":\"5\"},{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Отопление и горячая вода\",\"id\":\"6\"},{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Мусор\",\"id\":\"7\"},{\"acquiringName\":\"Расчетный Банк\",\"billingName\":\"Реестровый обмен\",\"categoryName\":\"Капремонт\",\"id\":\"9\"}],\"property\":{\"id\":\"032f84ad-a36f-484b-bc69-a1f806d080f1\",\"name\":\"\"},\"unitName\":\"1\",\"unitType\":\"flat\",\"updatedAt\":\"2023-06-26T15:16:48.908Z\",\"updatedBy\":{\"_label_\":\"Дзампаев Батраз Олегович -- <d6e45786-9c22-4deb-a011-3440c05c0318>\",\"createdAt\":\"2022-02-24T12:54:09.711Z\",\"dv\":1,\"id\":\"d6e45786-9c22-4deb-a011-3440c05c0318\",\"name\":\"Дзампаев Батраз Олегович\",\"type\":\"resident\",\"updatedAt\":\"2022-08-22T09:39:13.837Z\",\"v\":3},\"user\":{\"_label_\":\"Дзампаев Батраз Олегович -- <d6e45786-9c22-4deb-a011-3440c05c0318>\",\"createdAt\":\"2022-02-24T12:54:09.711Z\",\"dv\":1,\"id\":\"d6e45786-9c22-4deb-a011-3440c05c0318\",\"name\":\"Дзампаев Батраз Олегович\",\"phone\":\"+79897456577\",\"type\":\"resident\",\"updatedAt\":\"2022-08-22T09:39:13.837Z\",\"v\":3},\"v\":1}"
            callbackContext.success(JSONObject(testJson))
        }
        if (action == ACTION_CLOSE_MINIAPP) {
            this.cordova.onMessage(ACTION_CLOSE_MINIAPP, Unit)
        }

        if (action == ACTION_GET_LAUNCH_CONTEXT) {
            callbackContext.success(CondoPluginState.payload.orEmpty())
        }
        if (action == ACTION_NOTIFY_CALL_ENDED) {
            callbackContext.success()
        }
        if (action == ACTION_HISTORY_PUSH_STATE) {
            MiniappBackStack.push(MiniappBackStackEntry(args.getString(1), args.get(0)))

            sendHistoryStateToJs()
            callbackContext.success()
        }
        if (action == ACTION_HISTORY_BACK) {
            MiniappBackStack.pop()
            sendHistoryStateToJs()
            callbackContext.success()
        }
        if (action == ACTION_HISTORY_GO) {
            MiniappBackStack.pop(args.getLong(0))
            sendHistoryStateToJs()
            callbackContext.success()
        }
        if (action == ACTION_HISTORY_REPLACE_STATE) {
            MiniappBackStack.replace(MiniappBackStackEntry(args.getString(1), args.get(0)))
            sendHistoryStateToJs()
            callbackContext.success()
        }

        logD { "CondoPlugin execute: action: ${action} args: ${args.toString()}" }
        return true
    }


    private fun requestAuthCode(clientID: String) = flow {
        authState = UUID.randomUUID().toString()
        val redirectURI = "https%3A%2F%2Fmobile.doma.ai"

        emit(
            api?.miniappRequestAuthCode(
                url = "https://condo.d.doma.ai/oidc/auth?response_type=code&client_id=${clientID}&redirect_uri=${redirectURI}&scope=openid&state=${authState}",
                cookie = "keystone.sid=${URLEncoder.encode("s:${CoreModule.access_token}", "UTF-8")}"
            ) ?: throw Exception("no retrofit instance found")
        )
    }.map {
        it.raw().request.url.queryParameter("code") ?: ""
    }

    private fun requestAuth(code: String, clientID: String, clientSecret: String) = flow {
        authState = UUID.randomUUID().toString()

        val testAuthHeader = "Basic ${Base64.getEncoder().encodeToString("$clientID:$clientSecret".toByteArray())}"

        val body = FormBody.Builder()
            .addEncoded("grant_type", "authorization_code")
            .addEncoded("code", code)
            .addEncoded("redirect_uri", "https%3A%2F%2Fmobile.doma.ai")
            .build()

        emit(
            api?.miniappRequestAuth(
                url = "https://condo.d.doma.ai/oidc/token",
                authorizationHeader = testAuthHeader,
                body = body
            ) ?: throw Exception("no retrofit instance found")
        )
    }.map {
        it.string()
    }



    private fun requestFullAuth(client_id: String, redirect_uri: String) = flow {
        authState = UUID.randomUUID().toString()
        val url = "https://condo.d.doma.ai/oidc/auth?response_type=code&client_id=${client_id}&redirect_uri=${redirect_uri}&scope=openid&state=${authState})"
        val cookie = "keystone.sid=${URLEncoder.encode("s:${CoreModule.access_token}", "UTF-8")}"
        emit(api?.miniappRequestFullAuth(url, cookie) ?: throw Exception("no retrofit instance found"))
    }

    private fun requestFullAuthByUrl(url: String) = flow {
        val cookie = "keystone.sid=${URLEncoder.encode("s:${CoreModule.access_token}", "UTF-8")}"
        emit(api?.miniappRequestFullAuth(url, cookie) ?: throw Exception("no retrofit instance found"))
    }

    fun sendHistoryStateToJs(){
        val state = MiniappBackStack.backstack.value.lastOrNull()?.state
        val stateStr = if (state is String){
            """"$state""""
        } else state.toString()
        scope.launch(Dispatchers.Main) {

            this@Condo.webView.engine.evaluateJavascript("""window.dispatchEvent(new PopStateEvent('condoPopstate', { 'state': $stateStr}));""", ValueCallback {
                logD { it }
            })
        }
    }


    companion object {
        private const val ACTION_REQUEST_AUTH_CODE = "requestAuthorizationCode"
        private const val ACTION_REQUEST_AUTH = "requestAuthorization"
        private const val ACTION_REQUEST_SERVER_AUTH = "requestServerAuthorization"
        private const val ACTION_REQUEST_SERVER_AUTH_BY_URL = "requestServerAuthorizationByUrl"
        private const val ACTION_GET_CURRENT_RESIDENT = "getCurrentResident"
        private const val ACTION_GET_LAUNCH_CONTEXT = "getLaunchContext"
        private const val ACTION_NOTIFY_CALL_ENDED = "notifyCallEnded"


        private const val ACTION_HISTORY_PUSH_STATE = "historyPushState"
        private const val ACTION_HISTORY_BACK = "historyBack"
        private const val ACTION_HISTORY_REPLACE_STATE = "historyReplaceState"
        private const val ACTION_HISTORY_GO = "historyGo"

        const val ACTION_CLOSE_MINIAPP = "closeApplication"
    }


}