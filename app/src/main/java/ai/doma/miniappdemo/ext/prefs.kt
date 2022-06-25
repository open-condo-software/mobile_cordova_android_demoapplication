package ai.doma.miniappdemo.ext

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapterFactory
import java.io.Serializable

inline fun <reified T : Serializable> SharedPreferences.Editor.putSerializable(key: String, value: T, adapters: List<TypeAdapterFactory> = listOf()): SharedPreferences.Editor{
    val gson = Gson().newBuilder().apply {
        adapters.forEach {
            registerTypeAdapterFactory(it)
        }
    }.create()

    val json = gson.toJson(value)
    putString(key, json)
    return this
}

inline fun <reified T : Serializable> SharedPreferences.getSerializable(key: String, adapters: List<TypeAdapterFactory> = listOf()): T? {
    val gson = Gson().newBuilder().apply {
        adapters.forEach {
            registerTypeAdapterFactory(it)
        }
    }.create()

    return gson.fromJson(getString(key,""), T::class.java)
}