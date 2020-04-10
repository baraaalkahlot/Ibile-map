package com.ibile.features.main.datasharing

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ibile.R
import com.ibile.core.currentContext
import com.ibile.utils.views.OptionWithIconArrayAdapter
import com.ibile.utils.views.OptionWithIconArrayAdapter.ItemOptionWithIcon

/**
 * Dialog used to display share action options to the user.
 *
 * Dialog is reused for all the share actions to be displayed to the user. The options displayed is
 * determined by the parent fragment.
 */
class ShareOptionsDialogFragment : DialogFragment() {

    val callback: Callback
        get() = parentFragment as Callback

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(currentContext)
            .setTitle(callback.title_ShareDataOptionsDialogFragment)
            .setNegativeButton(R.string.text_cancel) { _, _ ->

            }
            .setAdapter(
                OptionWithIconArrayAdapter(
                    currentContext, callback.optionItems_ShareDataOptionsDialogFragment
                )
            ) { _, which ->
                callback.onSelectOption_ShareDataOptionsDialogFragment(which)
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        callback.onCancel_ShareDataOptionsDialogFragment()
        super.onCancel(dialog)
    }

    @Suppress("PropertyName", "FunctionName")
    interface Callback {
        val optionItems_ShareDataOptionsDialogFragment: List<ItemOptionWithIcon>
        val title_ShareDataOptionsDialogFragment: String
        fun onCancel_ShareDataOptionsDialogFragment()
        fun onSelectOption_ShareDataOptionsDialogFragment(optionIndex: Int)
    }
}
