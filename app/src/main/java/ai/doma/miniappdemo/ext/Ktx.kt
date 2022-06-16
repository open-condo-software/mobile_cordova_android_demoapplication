package ai.doma.miniappdemo


import android.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.cancellation.CancellationException

private val onNextStub: suspend (Any) -> Unit = {}
private val onErrorStub: suspend (Throwable) -> Unit = {}
private val onCompleteStub: suspend () -> Unit = {}



fun View.getViewScope(): CoroutineScope{
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    this.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener{
        override fun onViewAttachedToWindow(v: View?) {

        }

        override fun onViewDetachedFromWindow(v: View?) {
            this@getViewScope.removeOnAttachStateChangeListener(this)
            scope.cancel(CancellationException(Exception("onViewDetachedFromWindow")))
        }
    })
    return scope
}


suspend fun <T : Any> Flow<T>.collectAndTrace(
    onError: suspend (Throwable) -> Unit = onErrorStub,
    onComplete: suspend () -> Unit = onCompleteStub,
    onNext: suspend (T) -> Unit = onNextStub
) {
    this
        .catch {
            if(it is CancellationException){
                onError(it)
                return@catch
            }
            val writer = StringWriter()
            it.printStackTrace(PrintWriter(writer))
            val s = writer.toString()
            //Tracker.logEvent(NonFatalException(it.message.orEmpty(), s))
            it.printStackTrace()
            onError(it)
        }.onCompletion {
            if (it == null) {
                try {
                    onComplete()
                } catch (e: Exception) {
                    //Tracker.logEvent(NonFatalException(e.message.orEmpty(), e.stackAsString()))
                    e.printStackTrace()
                }
            } else {
                if(it !is CancellationException){
                    it.printStackTrace()
                    onError(it)
                }
            }
        }
        .collect {
            try {
                onNext(it)
            } catch (e: Exception) {
                //Tracker.logEvent(NonFatalException(e.message.orEmpty(), e.stackAsString()))
                e.printStackTrace()
            }
        }
}



fun tickerFlow(period: Long, initialDelay: Long = 0L) = flow<Unit> {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

fun tickerFlowIndexed(period: Long, initialDelay: Long = 0L) = flow<Long> {
    var i = 0L
    delay(initialDelay)
    while (true) {
        emit(i)
        delay(period)
        i++
    }
}