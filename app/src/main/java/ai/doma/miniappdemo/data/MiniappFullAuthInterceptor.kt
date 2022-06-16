package ai.doma.miniappdemo.data

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response

class MiniappFullAuthInterceptor(): Interceptor {
    private var listeners = mutableMapOf<String, suspend (completionUrl: String)->Unit>()

    fun addResultWaiter(requestUrl: String, action: suspend (String)->Unit){
        listeners[requestUrl] = action
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request).let { response ->
            val redirect = response.header("Location")
            var watchedRedirect: String? = null
            listeners.keys.forEach {
                if (redirect.orEmpty().startsWith(it)) {
                    watchedRedirect = it
                }
            }
            watchedRedirect?.let{
                GlobalScope.launch {
                    listeners[it]?.invoke(redirect.orEmpty())
                    listeners.remove(it)
                }
                response.newBuilder()
                    .removeHeader("Location")
                    .build()
            } ?: response
        }
    }

}