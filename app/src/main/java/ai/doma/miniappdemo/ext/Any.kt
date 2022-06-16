package ai.doma.miniappdemo.ext

import android.annotation.SuppressLint
import android.util.Log

@SuppressLint("LogConditional")
fun Any?.logD(tag: String? = null, message: (() -> String)? = null) {

    Log.d("${tag ?: this?.javaClass?.simpleName}", "${message?.invoke() ?: this}")
}

fun Any?.logW(tag: String? = null, message: (() -> String)? = null) {

    Log.w("${tag ?: this?.javaClass?.simpleName}", "${message?.invoke() ?: this}")
}

fun Any?.logE(tag: String? = null, message: (() -> String)? = null) {

    Log.e("${tag ?: this?.javaClass?.simpleName}", "${message?.invoke() ?: this}")
}