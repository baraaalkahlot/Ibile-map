package com.ibile.features.main.mapfiles

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class MapFilesOptionsContainerDialogFragment : DialogFragment() {

    val callback: Callback
        get() = parentFragment as Callback

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return callback.getDialog_FileOptionsDialogFragment()
    }

    override fun onCancel(dialog: DialogInterface) {
        callback.onCancelDialog_FileOptionsDialogFragment()
        super.onCancel(dialog)
    }

    interface Callback {
        fun getDialog_FileOptionsDialogFragment(): Dialog
        fun onCancelDialog_FileOptionsDialogFragment()
    }
}
