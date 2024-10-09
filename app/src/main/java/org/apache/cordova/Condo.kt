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
            val testJson = "{\"_label_\":\"16fcab4c-ae07-43cf-9bdb-1148f40dbcf1\",\"address\":\"Свердловская обл, Сысертский р-н, г Арамиль, ул Гарнизон, д 19Б\",\"addressKey\":\"a0963123-f9b4-4ba3-b764-1d24dfe27701\",\"addressMeta\":{\"__typename\":\"AddressMetaField\",\"data\":{\"area\":\"Сысертский\",\"areaFiasId\":\"4f51e06b-3dd4-4948-bfce-c0dc7b09f504\",\"areaKladrId\":\"6602500000000\",\"areaType\":\"р-н\",\"areaTypeFull\":\"район\",\"areaWithType\":\"Сысертский р-н\",\"capitalMarker\":\"0\",\"city\":\"Арамиль\",\"cityFiasId\":\"c99ffa11-6b7e-4040-9f9e-baa1eeca8a91\",\"cityKladrId\":\"6602500200000\",\"cityType\":\"г\",\"cityTypeFull\":\"город\",\"cityWithType\":\"г Арамиль\",\"country\":\"Россия\",\"countryIsoCode\":\"RU\",\"federalDistrict\":\"Уральский\",\"fiasActualityState\":\"0\",\"fiasCode\":\"66025002000000000730063\",\"fiasId\":\"ecae1340-370e-4d6e-8def-a5c91284b5bd\",\"fiasLevel\":\"8\",\"geoLat\":\"56.69668\",\"geoLon\":\"60.799694\",\"geonameId\":\"1511466\",\"house\":\"19Б\",\"houseFiasId\":\"ecae1340-370e-4d6e-8def-a5c91284b5bd\",\"houseKladrId\":\"6602500200000730063\",\"houseType\":\"д\",\"houseTypeFull\":\"дом\",\"kladrId\":\"6602500200000730063\",\"okato\":\"65241503000\",\"oktmo\":\"65729000001\",\"postalCode\":\"624003\",\"qcGeo\":\"1\",\"region\":\"Свердловская\",\"regionFiasId\":\"92b30014-4d52-4e2e-892d-928142b924bf\",\"regionIsoCode\":\"RU-SVE\",\"regionKladrId\":\"6600000000000\",\"regionType\":\"обл\",\"regionTypeFull\":\"область\",\"regionWithType\":\"Свердловская обл\",\"street\":\"Гарнизон\",\"streetFiasId\":\"6991bd71-cf77-4cd3-b29a-3560c21b0974\",\"streetKladrId\":\"66025002000007300\",\"streetType\":\"ул\",\"streetTypeFull\":\"улица\",\"streetWithType\":\"ул Гарнизон\",\"taxOffice\":\"6685\",\"taxOfficeLegal\":\"6685\"}},\"createdAt\":\"2022-05-27T13:01:38.463Z\",\"createdBy\":{\"_label_\":\"Deleted User -- <03f4c547-3557-4050-a392-dd4519d8ee36>\",\"createdAt\":\"2021-08-04T14:47:03.562Z\",\"dv\":1,\"id\":\"03f4c547-3557-4050-a392-dd4519d8ee36\",\"name\":\"Deleted User\",\"type\":\"resident\",\"updatedAt\":\"2023-03-31T07:51:29.443Z\",\"v\":76},\"dv\":1,\"id\":\"16fcab4c-ae07-43cf-9bdb-1148f40dbcf1\",\"organization\":{\"_label_\":\"ООО \\\"УПРАВЛЯЮЩАЯ КОМПАНИЯ \\\"СТРИЖИ\\\" -- <f4e54e9e-bb6b-4265-9a75-a7222d704cb4>\",\"country\":\"ru\",\"dv\":1,\"id\":\"f4e54e9e-bb6b-4265-9a75-a7222d704cb4\",\"name\":\"ООО \\\"УПРАВЛЯЮЩАЯ КОМПАНИЯ \\\"СТРИЖИ\\\"\",\"tin\":\"6679133393\",\"type\":\"MANAGING_COMPANY\",\"updatedAt\":\"2022-03-20T13:57:53.092Z\",\"v\":1},\"organizationFeatures\":{\"__typename\":\"OrganizationFeatures\",\"organizationFeaturesData\":{\"hasBillingData\":true,\"hasMeters\":false}},\"paymentCategories\":[{\"__typename\":\"PaymentCategory\",\"paymentCategoryData\":{\"acquiringName\":\"Расчетный Банк\",\"billingName\":\"РБ-ТЕСТ\",\"categoryName\":\"Квартплата\",\"id\":\"1\"}},{\"__typename\":\"PaymentCategory\",\"paymentCategoryData\":{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Электричество\",\"id\":\"3\"}},{\"__typename\":\"PaymentCategory\",\"paymentCategoryData\":{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Газ\",\"id\":\"4\"}},{\"__typename\":\"PaymentCategory\",\"paymentCategoryData\":{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Холодная вода\",\"id\":\"5\"}},{\"__typename\":\"PaymentCategory\",\"paymentCategoryData\":{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Отопление и горячая вода\",\"id\":\"6\"}},{\"__typename\":\"PaymentCategory\",\"paymentCategoryData\":{\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Мусор\",\"id\":\"7\"}},{\"__typename\":\"PaymentCategory\",\"paymentCategoryData\":{\"acquiringName\":\"Расчетный Банк\",\"billingName\":\"РБ-ТЕСТ\",\"categoryName\":\"Капремонт\",\"id\":\"9\"}}],\"property\":{\"id\":\"864d3091-7aac-4f55-904c-0559ff524163\",\"name\":\"Стрижи\"},\"unitName\":\"33\",\"unitType\":\"flat\",\"updatedAt\":\"2023-10-17T07:20:09.276Z\",\"user\":{\"_label_\":\"Deleted User -- <03f4c547-3557-4050-a392-dd4519d8ee36>\",\"createdAt\":\"2021-08-04T14:47:03.562Z\",\"dv\":1,\"id\":\"03f4c547-3557-4050-a392-dd4519d8ee36\",\"name\":\"Deleted User\",\"type\":\"resident\",\"updatedAt\":\"2023-03-31T07:51:29.443Z\",\"v\":76},\"v\":2,\"updatedBy\":null,\"organizationId\":\"f4e54e9e-bb6b-4265-9a75-a7222d704cb4\",\"organizationName\":\"ООО \\\"УПРАВЛЯЮЩАЯ КОМПАНИЯ \\\"СТРИЖИ\\\"\",\"propertyId\":\"864d3091-7aac-4f55-904c-0559ff524163\",\"propertyName\":\"Стрижи\"}"
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