package ai.doma.miniappdemo.base

import ai.doma.miniappdemo.getViewScope
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseFragment : Fragment() {

    abstract val layout: Int
    abstract val vb: ViewBinding

    var isSavingViewState = false
    var persistentView: View? = null
    var wasInit = false

    lateinit var viewScope: CoroutineScope // cancel on view detach (reinit with onViewCreated e.g. on popBackStack)



    private val repeatedActions = mutableListOf<suspend CoroutineScope.() -> Unit>()

    abstract fun onViewInit(view: View)
    open fun onNewViewLifecycle(view: View){}
    open fun onInitTheme(){}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return if (isSavingViewState) {
            persistentView.apply { (this?.parent as? ViewGroup)?.removeView(rootView) }
                ?: inflater.inflate(layout, container, false).apply {
                    persistentView = this
                }
        } else {
            inflater.inflate(layout, container, false)
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewScope = view.getViewScope()
        repeatedActions.forEach { viewScope.launch(block = it) }



        if(isSavingViewState){
            if(!wasInit){
                onViewInit(view)
                wasInit = true
            }
        } else {
            onViewInit(view)
            wasInit = true
        }
        onNewViewLifecycle(view)
    }



    object None : (View) -> Unit {
        override fun invoke(v: View) {
        }
    }

    fun BaseFragment.launchOnLifecycle(block: suspend CoroutineScope.() -> Unit){
        repeatedActions.add(block)
        viewScope.launch(block = block)
    }
}