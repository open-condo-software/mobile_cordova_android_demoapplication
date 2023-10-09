package ai.doma.miniappdemo.presentation

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.InjectHelper
import ai.doma.feature_miniapps.domain.MINIAPP_SERVER_AUTH_BY_URL_ID
import ai.doma.feature_miniapps.domain.MINIAPP_SERVER_AUTH_ID
import ai.doma.feature_miniapps.domain.MiniappInteractor
import ai.doma.feature_miniapps.presentation.view.MiniappDialogFragment
import ai.doma.miniappdemo.R
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.data.MiniappRepository
import ai.doma.miniappdemo.databinding.FragmentStartBinding
import ai.doma.miniappdemo.domain.MiniappEntity
import ai.doma.miniappdemo.ext.viewBinding
import ai.doma.miniappdemo.getViewScope
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject


class StartFragment: Fragment() {

    val vb by viewBinding(FragmentStartBinding::bind)

    lateinit var miniappInteractor: MiniappInteractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        miniappInteractor = InjectHelper.provideCoreComponent(requireContext().applicationContext).miniappInteractor
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb.button.isVisible = false
        vb.button.setOnClickListener {
           // findNavController().navigate(StartFragmentDirections.actionStartFragmentToMiniappFragment(MINIAPP_SERVER_AUTH_ID))
        }

        vb.button2.setOnClickListener {
            it.getViewScope().launch {
                miniappInteractor.getOrDownloadMiniapp(MINIAPP_SERVER_AUTH_BY_URL_ID)
                    .flowOn(Dispatchers.IO)
                    .collectAndTrace {(file, config) ->
                        val miniappEntity = MiniappEntity(MINIAPP_SERVER_AUTH_BY_URL_ID, "test name", "test description", "1.0.0", System.currentTimeMillis(),null,null,null,null)
                        if(config.isModal){
                            MiniappDialogFragment.show(
                                fragmentManager = childFragmentManager,
                                miniappEntity = miniappEntity,
                                presentationStyle = config.presentationStyle
                            )
                        } else {
                            findNavController().navigate(StartFragmentDirections.actionStartFragmentToMiniappFragment(miniappEntity = miniappEntity, presentationStyle = config.presentationStyle, payload = null))
                        }
                    }
            }
        }
        vb.button2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#39CE66"))
    }
}