package ai.doma.miniappdemo.base


import ai.doma.miniappdemo.R
import ai.doma.miniappdemo.databinding.DialogBaseBinding
import ai.doma.miniappdemo.ext.viewBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment

abstract class BaseDialog: DialogFragment() {
    abstract val layout: Int
    private val internalVb by viewBinding(DialogBaseBinding::bind)
    var container: ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_Miniappdemo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_base, container, false).also {
            this.container = it.findViewById(R.id.contentFrame)
            inflater.inflate(layout, this.container, true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(dialog?.window!!, false)
    }


}