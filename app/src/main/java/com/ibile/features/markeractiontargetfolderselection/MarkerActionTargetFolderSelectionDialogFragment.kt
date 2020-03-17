package com.ibile.features.markeractiontargetfolderselection

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import com.ibile.R
import com.ibile.core.BaseDialogFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.currentContext
import com.ibile.core.simpleController
import com.ibile.features.main.folderlist.FolderWithMarkersCount

class MarkerActionTargetFolderSelectionDialogFragment : BaseDialogFragment() {
    private val callback: Callback
        get() = parentFragment as Callback

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = MarkersActionTargetFolderOptionsArrayAdapter(
            currentContext,
            callback.markerActionTargetFolderOptionsList
        )
        return AlertDialog.Builder(context, R.style.AlertDialog)
            .setTitle(callback.markerActionTargetFolderSelectionDialogTitle)
            .setNegativeButton(R.string.text_cancel) { _, _ -> }
            .setAdapter(adapter) { _, which ->
                val selectedFolder = callback.markerActionTargetFolderOptionsList[which]
                callback.onSelectTargetFolder(selectedFolder.id)
            }
            .create()
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {}


    interface Callback {
        val markerActionTargetFolderSelectionDialogTitle: String
        fun onSelectTargetFolder(folderId: Long)
        val markerActionTargetFolderOptionsList: List<FolderWithMarkersCount>
    }
}
