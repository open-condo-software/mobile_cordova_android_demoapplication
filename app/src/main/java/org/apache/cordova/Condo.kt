package org.apache.cordova

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.CoreModule
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.data.RetrofitApi
import ai.doma.miniappdemo.ext.logD
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
                        callbackContext.success(json.toString())
                    } else {
                        callbackContext.sendPluginResult(PluginResult(PluginResult.Status.ERROR))
                    }


                }
            }
        }
        if (action == ACTION_GET_CURRENT_RESIDENT){
            val testJson = "{\"__typename\":\"Resident\",\"_label_\":\"0528a5dd-af4f-412c-bf87-2125cdec03c7\",\"dv\":1,\"user\":{\"__typename\":\"User\",\"_label_\":\"Дмитрий\",\"dv\":1,\"name\":\"Дмитрий\",\"type\":\"resident\",\"id\":\"03f4c547-3557-4050-a392-dd4519d8ee36\",\"v\":75,\"createdAt\":\"2021-08-04T14:47:03.562Z\",\"updatedAt\":\"2022-06-22T15:35:26.999Z\"},\"organization\":{\"__typename\":\"Organization\",\"_label_\":\"Тестирую API\",\"dv\":1,\"country\":\"ru\",\"name\":\"Тестирую API\",\"tin\":\"6671095805\",\"id\":\"9c2f63c8-9288-4942-b24f-e11da26f6bec\",\"v\":1,\"updatedAt\":\"2022-04-18T10:23:45.431Z\"},\"property\":{\"__typename\":\"Property\",\"id\":\"94e2f48c-24b3-4f0d-bc9d-74ab863805a1\",\"name\":\"Лермонтова, д 7\"},\"organizationFeatures\":{\"__typename\":\"OrganizationFeatures\",\"hasBillingData\":false,\"hasMeters\":true},\"paymentCategories\":[{\"__typename\":\"PaymentCategory\",\"categoryName\":\"Квартплата\",\"billingName\":\"default\",\"acquiringName\":\"default\",\"id\":\"1\"},{\"__typename\":\"PaymentCategory\",\"categoryName\":\"Капремонт\",\"billingName\":\"default\",\"acquiringName\":\"default\",\"id\":\"9\"}],\"address\":\"г Новосибирск, ул Лермонтова, д 7\",\"addressMeta\":{\"__typename\":\"AddressMetaField\",\"dv\":1,\"value\":\"г Новосибирск, ул Лермонтова, д 7\",\"unrestricted_value\":\"630091, Новосибирская обл, г Новосибирск, ул Лермонтова, д 7\",\"data\":{\"__typename\":\"AddressMetaDataField\",\"postal_code\":\"630091\",\"country\":\"Россия\",\"country_iso_code\":\"RU\",\"federal_district\":\"Сибирский\",\"region_fias_id\":\"1ac46b49-3209-4814-b7bf-a509ea1aecd9\",\"region_kladr_id\":\"5400000000000\",\"region_iso_code\":\"RU-NVS\",\"region_with_type\":\"Новосибирская обл\",\"region_type\":\"обл\",\"region_type_full\":\"область\",\"region\":\"Новосибирская\",\"city_fias_id\":\"8dea00e3-9aab-4d8e-887c-ef2aaa546456\",\"city_kladr_id\":\"5400000100000\",\"city_with_type\":\"г Новосибирск\",\"city_type\":\"г\",\"city_type_full\":\"город\",\"city\":\"Новосибирск\",\"street_fias_id\":\"25327053-e12e-4e6a-ba9e-a25ee594c281\",\"street_kladr_id\":\"54000001000073400\",\"street_with_type\":\"ул Лермонтова\",\"street_type\":\"ул\",\"street_type_full\":\"улица\",\"street\":\"Лермонтова\",\"house_fias_id\":\"2cac22cd-721c-4f5a-b25b-97a771da7ef4\",\"house_kladr_id\":\"5400000100007340013\",\"house_type\":\"д\",\"house_type_full\":\"дом\",\"house\":\"7\",\"fias_id\":\"2cac22cd-721c-4f5a-b25b-97a771da7ef4\",\"fias_code\":\"54000001000000007340013\",\"fias_level\":\"8\",\"fias_actuality_state\":\"0\",\"kladr_id\":\"5400000100007340013\",\"geoname_id\":\"1496747\",\"capital_marker\":\"2\",\"okato\":\"50401386000\",\"oktmo\":\"50701000001\",\"tax_office\":\"5406\",\"tax_office_legal\":\"5406\",\"geo_lat\":\"55.044805\",\"geo_lon\":\"82.926862\",\"qc_geo\":\"2\"}},\"unitName\":\"1\",\"unitType\":\"flat\",\"id\":\"0528a5dd-af4f-412c-bf87-2125cdec03c7\",\"v\":1,\"createdAt\":\"2022-06-20T11:48:35.149Z\",\"updatedAt\":\"2022-06-20T11:48:35.149Z\",\"createdBy\":{\"__typename\":\"User\",\"_label_\":\"Дмитрий\",\"dv\":1,\"name\":\"Дмитрий\",\"type\":\"resident\",\"id\":\"03f4c547-3557-4050-a392-dd4519d8ee36\",\"v\":75,\"createdAt\":\"2021-08-04T14:47:03.562Z\",\"updatedAt\":\"2022-06-22T15:35:26.999Z\"},\"updatedBy\":{\"__typename\":\"User\",\"_label_\":\"Дмитрий\",\"dv\":1,\"name\":\"Дмитрий\",\"type\":\"resident\",\"id\":\"03f4c547-3557-4050-a392-dd4519d8ee36\",\"v\":75,\"createdAt\":\"2021-08-04T14:47:03.562Z\",\"updatedAt\":\"2022-06-22T15:35:26.999Z\"}}"
            val testJson2 = "{\"__typename\":\"Resident\",\"_label_\":\"29065b98-8dfc-4c67-9870-a67b7ea3f67f\",\"dv\":1,\"user\":{\"__typename\":\"User\",\"_label_\":\"Дмитрий\",\"dv\":1,\"name\":\"Дмитрий\",\"type\":\"resident\",\"id\":\"03f4c547-3557-4050-a392-dd4519d8ee36\",\"v\":75,\"createdAt\":\"2021-08-04T14:47:03.562Z\",\"updatedAt\":\"2022-06-22T15:35:26.999Z\"},\"organization\":{\"__typename\":\"Organization\",\"_label_\":\"Тестирую API\",\"dv\":1,\"country\":\"ru\",\"name\":\"Тестирую API\",\"tin\":\"6671095805\",\"id\":\"9c2f63c8-9288-4942-b24f-e11da26f6bec\",\"v\":1,\"updatedAt\":\"2022-04-18T10:23:45.431Z\"},\"property\":{\"__typename\":\"Property\",\"id\":\"3addaa10-64b2-4367-a2d0-c43634af9615\",\"name\":\"Владимира Высоцкого\"},\"organizationFeatures\":{\"__typename\":\"OrganizationFeatures\",\"hasBillingData\":false,\"hasMeters\":true},\"paymentCategories\":[{\"__typename\":\"PaymentCategory\",\"categoryName\":\"Квартплата\",\"billingName\":\"default\",\"acquiringName\":\"default\",\"id\":\"1\"},{\"__typename\":\"PaymentCategory\",\"categoryName\":\"Капремонт\",\"billingName\":\"default\",\"acquiringName\":\"default\",\"id\":\"9\"}],\"address\":\"г Екатеринбург, ул Владимира Высоцкого, д 3\",\"addressMeta\":{\"__typename\":\"AddressMetaField\",\"dv\":1,\"value\":\"г Екатеринбург, ул Владимира Высоцкого, д 3\",\"unrestricted_value\":\"Свердловская обл, г Екатеринбург, ул Владимира Высоцкого, д 3\",\"data\":{\"__typename\":\"AddressMetaDataField\",\"country\":\"Россия\",\"country_iso_code\":\"RU\",\"region_fias_id\":\"92b30014-4d52-4e2e-892d-928142b924bf\",\"region_kladr_id\":\"6600000000000\",\"region_iso_code\":\"RU-SVE\",\"region_with_type\":\"Свердловская обл\",\"region_type\":\"обл\",\"region_type_full\":\"область\",\"region\":\"Свердловская\",\"city_fias_id\":\"2763c110-cb8b-416a-9dac-ad28a55b4402\",\"city_kladr_id\":\"6600000100000\",\"city_with_type\":\"г Екатеринбург\",\"city_type\":\"г\",\"city_type_full\":\"город\",\"city\":\"Екатеринбург\",\"street_fias_id\":\"c0bc419f-8ea9-40b8-b2ed-bf5705f02ac9\",\"street_kladr_id\":\"66000001000025500\",\"street_with_type\":\"ул Владимира Высоцкого\",\"street_type\":\"ул\",\"street_type_full\":\"улица\",\"street\":\"Владимира Высоцкого\",\"house_type\":\"д\",\"house_type_full\":\"дом\",\"house\":\"3\",\"fias_id\":\"c0bc419f-8ea9-40b8-b2ed-bf5705f02ac9\",\"fias_level\":\"7\",\"fias_actuality_state\":\"0\",\"kladr_id\":\"66000001000025500\",\"geoname_id\":\"1486209\",\"capital_marker\":\"2\",\"okato\":\"65401373000\",\"oktmo\":\"65701000001\",\"tax_office\":\"6670\",\"tax_office_legal\":\"6670\",\"geo_lat\":\"56.84355\",\"geo_lon\":\"60.671729\",\"qc_geo\":\"0\",\"history_values\":[\"ул Риммы Юровской\"]}},\"unitName\":\"10\",\"unitType\":\"flat\",\"id\":\"29065b98-8dfc-4c67-9870-a67b7ea3f67f\",\"v\":1,\"createdAt\":\"2022-06-30T06:26:03.757Z\",\"updatedAt\":\"2022-06-30T06:26:03.757Z\",\"createdBy\":{\"__typename\":\"User\",\"_label_\":\"Дмитрий\",\"dv\":1,\"name\":\"Дмитрий\",\"type\":\"resident\",\"id\":\"03f4c547-3557-4050-a392-dd4519d8ee36\",\"v\":75,\"createdAt\":\"2021-08-04T14:47:03.562Z\",\"updatedAt\":\"2022-06-22T15:35:26.999Z\"},\"updatedBy\":{\"__typename\":\"User\",\"_label_\":\"Дмитрий\",\"dv\":1,\"name\":\"Дмитрий\",\"type\":\"resident\",\"id\":\"03f4c547-3557-4050-a392-dd4519d8ee36\",\"v\":75,\"createdAt\":\"2021-08-04T14:47:03.562Z\",\"updatedAt\":\"2022-06-22T15:35:26.999Z\"}}"
            callbackContext.success(JSONObject(testJson2))
        }
        if (action == ACTION_CLOSE_MINIAPP) {
            this.cordova.onMessage(ACTION_CLOSE_MINIAPP, Unit)
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


    companion object {
        private const val ACTION_REQUEST_AUTH_CODE = "requestAuthorizationCode"
        private const val ACTION_REQUEST_AUTH = "requestAuthorization"
        private const val ACTION_REQUEST_SERVER_AUTH = "requestServerAuthorization"
        private const val ACTION_REQUEST_SERVER_AUTH_BY_URL = "requestServerAuthorizationByUrl"
        private const val ACTION_GET_CURRENT_RESIDENT = "getCurrentResident"

        const val ACTION_CLOSE_MINIAPP = "closeApplication"
    }


}