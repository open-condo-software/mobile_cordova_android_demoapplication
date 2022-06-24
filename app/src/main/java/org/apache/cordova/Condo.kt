package org.apache.cordova

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.CoreModule
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.data.MiniappFullAuthInterceptor
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
    var miniappInterceptor: MiniappFullAuthInterceptor? = null
    init {
        api = CoreComponent.get()?.retrofitApi
        miniappInterceptor = CoreComponent.get()?.miniappInterceptor
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

        const val ACTION_CLOSE_MINIAPP = "closeApplication"
    }


}