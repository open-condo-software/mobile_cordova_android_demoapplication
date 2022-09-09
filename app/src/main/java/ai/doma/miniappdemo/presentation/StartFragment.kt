package ai.doma.miniappdemo.presentation

import ai.doma.feature_miniapps.domain.MINIAPP_SERVER_AUTH_BY_URL_ID
import ai.doma.feature_miniapps.domain.MINIAPP_SERVER_AUTH_ID
import ai.doma.miniappdemo.R
import ai.doma.miniappdemo.databinding.FragmentStartBinding
import ai.doma.miniappdemo.ext.viewBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController




class StartFragment: Fragment() {

    val vb by viewBinding(FragmentStartBinding::bind)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb.button.isVisible = false
        vb.button.setOnClickListener {
            findNavController().navigate(StartFragmentDirections.actionStartFragmentToMiniappFragment(MINIAPP_SERVER_AUTH_ID))
        }

        vb.button2.setOnClickListener {
            findNavController().navigate(StartFragmentDirections.actionStartFragmentToMiniappFragment(MINIAPP_SERVER_AUTH_BY_URL_ID))
        }
    }
}