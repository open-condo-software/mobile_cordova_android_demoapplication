package ai.doma.core.DI

import ai.doma.miniappdemo.BuildConfig
import ai.doma.miniappdemo.data.MiniappRepository
import ai.doma.miniappdemo.data.MiniappCookieContextRepository
import ai.doma.miniappdemo.data.RetrofitApi
import android.app.Application
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CompletableDeferred
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


const val KEY_LOCAL_PREFERENCES = "localPrefs"

@Component(modules = [CoreModule::class])
@Singleton
abstract class CoreComponent {
    abstract val retrofitApi: RetrofitApi
    abstract val miniappRepository: MiniappRepository
    abstract val miniappCookieContextRepository: MiniappCookieContextRepository
    abstract val context: Context

    init {
        instance = this
    }

    companion object {

        @Volatile
        private var instance: CoreComponent? = null

        val initializationDeferred = CompletableDeferred<Unit>(null)

        fun get() = instance

    }
}

interface CoreComponentProvider {
    val appBundleId: String
    fun provideCoreComponent(): CoreComponent
}


@Module
class CoreModule(private val app: Application) {


    @Provides
    @Singleton
    fun provideContext(): Context {
        return app.applicationContext
    }

    @Provides
    @Singleton
    fun providePreferences() = app.applicationContext.getSharedPreferences(
        KEY_LOCAL_PREFERENCES,
        Context.MODE_PRIVATE
    )


    companion object {
        // этот токен под которым авторизован реальный пользователь в приложении домов
        var access_token: String =
            "nSRloP9ZLZg0KX1dyIpO8YUs22c92FRZ.1iA5j1hsl+NfHVIFh1u61c2kEk70bFbMZAnsB5Suioo"

        const val API_URL_DEBUG = "https://condo.d.doma.ai/"

    }


    @Provides
    @Singleton
    internal fun provideRetrofit(okHttp: OkHttpClient) =
        Retrofit.Builder()
            .baseUrl(API_URL_DEBUG)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttp)
            .build().create(RetrofitApi::class.java)


    @Provides
    @Singleton
    internal fun provideOkHttpClient(
        context: Context,
        miniappCookieContext: MiniappCookieContextRepository
    ): OkHttpClient {

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            //.authenticator(TokenAuthenticator { api })
            .followRedirects(true)
            .cookieJar(object :
                PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context)) {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    CookieManager.getInstance().acceptCookie()
                    cookies.forEach {
                        CookieManager.getInstance()
                            .setCookie("https://" + it.domain, "$it; SameSite=None")
                        miniappCookieContext.appendCookie(it)
                    }
                    CookieManager.getInstance().flush()
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {


                    val currentDomain = API_URL_DEBUG.substringAfter("://").replace("/", "")

                    return if (url.toString().contains("$API_URL_DEBUG/admin/api")) {
                        listOf()
                    } else if (url.toString().contains("$currentDomain")) {
                        miniappCookieContext.getCookiesForRedirect(url) + listOf(
                            Cookie.Builder()
                                .domain(currentDomain)
                                .path("/")
                                .name("keystone.sid")
                                .value(URLEncoder.encode("s:${access_token}", "UTF-8"))
                                .secure()
                                .httpOnly()
                                .build()
                        )
                    } else {
                        super.loadForRequest(url)
                    }
                }
            })
            .addInterceptor {
                val interceptor =
                    it.proceed(
                        it.request().newBuilder()
                            .apply {
                                if (it.request().url.toString().contains("oidc/token") ||
                                    it.request().url.toString()
                                        .contains("oidc/auth") // miniapps authorization
                                ) {
                                    //without header!!
                                } else {
                                    access_token?.let { addHeader("Authorization", "Bearer $it") }

                                }
                            }
                            .addHeader("client-platform", "Android")
                            .build()
                    )
                interceptor
            }.apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor(ApiLogger()).apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    //addInterceptor(ChuckerInterceptor(context))
                }
            }
            //.addInterceptor(CurlLoggingInterceptor())
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                response
            }
            .build()

        return client
    }

    internal class ApiLogger : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            val logName = "ApiLogger"
            if (message.startsWith("{") || message.startsWith("[")) {
                try {
                    val prettyPrintJson = GsonBuilder().setPrettyPrinting()
                        .serializeNulls()
                        .create().toJson(JsonParser().parse(message))
                    largeLog(logName, prettyPrintJson)
                } catch (m: JsonSyntaxException) {
                    Log.d(logName, message)
                }
            } else {
                Log.d(logName, message)
                return
            }
        }

        fun largeLog(tag: String, content: String) {
            val maxLogSize = 1000
            val stringLength = content.length
            for (i in 0..stringLength / maxLogSize) {
                val start = i * maxLogSize
                var end = (i + 1) * maxLogSize
                end = if (end > content.length) content.length else end
                Log.v(tag, content.substring(start, end))
            }
        }
    }

}