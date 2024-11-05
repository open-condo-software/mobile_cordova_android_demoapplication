package ai.doma.miniappdemo.presentation

import ai.doma.miniappdemo.ext.showFragment
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class QuestionDialog : DialogFragment() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            title: CharSequence,
            message: CharSequence,
            positiveFunc: (() -> Unit) = {},
            negativeFunc: (() -> Unit) = {},
            neutralFunc: (() -> Unit) = {},
            positiveButtonName: String = "Ок",
            negativeButtonName: String? = null,
            neutralButtonName: String? = null,
            onCancel: (() -> Unit) = {},
            isCancelable: Boolean = true
        ) {
            val dialog = QuestionDialog().apply {
                this.title = title
                this.message = message
                positiveCallback = positiveFunc
                negativeCallback = negativeFunc
                neutralCallback = neutralFunc
                this.positiveButtonName = positiveButtonName
                this.negativeButtonName = negativeButtonName
                this.neutralButtonName = neutralButtonName
                this.onCancel = onCancel
                this.isCancelable = isCancelable
            }

            fragmentManager.showFragment(dialog)
        }
    }

    private var positiveCallback: (() -> Unit)? = null
    private var negativeCallback: (() -> Unit)? = null
    private var neutralCallback: (() -> Unit)? = null
    private var title: CharSequence = ""
    private var message: CharSequence = ""
    private var positiveButtonName: String = ""
    private var negativeButtonName: String? = null
    private var neutralButtonName: String? = null
    private var onCancel: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(positiveButtonName) { _, _ ->
                positiveCallback?.invoke()
                dismissAllowingStateLoss()
            }.apply {
                negativeButtonName?.let {
                    setNegativeButton(it) { _, _ ->
                        negativeCallback?.invoke()
                        dismissAllowingStateLoss()
                    }
                }
                neutralButtonName?.let {
                    setNeutralButton(it){ _, _ ->
                        neutralCallback?.invoke()
                        dismissAllowingStateLoss()
                    }
                }
            }.create().also {
                it.show()
                if(negativeButtonName != null && neutralButtonName != null){
                    it.getButton(DialogInterface.BUTTON_POSITIVE)?.gravity = Gravity.END
                    it.getButton(DialogInterface.BUTTON_NEGATIVE)?.gravity = Gravity.END
                    it.getButton(DialogInterface.BUTTON_NEUTRAL)?.gravity = Gravity.END
                }
            }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancel?.invoke()
    }
}