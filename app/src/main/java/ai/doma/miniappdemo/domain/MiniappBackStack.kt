package ai.doma.miniappdemo.domain

import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import kotlin.math.abs

data class MiniappBackStackEntry(val name: String, val state: Any?)
object MiniappBackStack {

    /**
     * Root entry not in backstack!, empty when root attached
     */
    val backstack = MutableStateFlow(listOf<MiniappBackStackEntry>())

    fun reset() {
        backstack.value = listOf()
    }

    fun push(route: MiniappBackStackEntry) {
        backstack.value = backstack.value + route
    }

    fun pop(steps: Long = 1) {
        val stack = backstack.value
        if (stack.size > 0) {
            backstack.value = stack.dropLast(abs(steps).toInt())
        }
    }

    fun replace(route: MiniappBackStackEntry) {
        val stack = backstack.value
        if (stack.size > 0) {
            backstack.value = stack.dropLast(1) + route
        } else {
            backstack.value = listOf(route)
        }
    }

//    fun popToRoot() {
//        val stack = backstack.value
//        if (stack.size > 1) {
//            backstack.value = listOf()
//        }
//    }

}