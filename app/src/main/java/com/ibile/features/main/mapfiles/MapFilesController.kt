package com.ibile.features.main.mapfiles

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.widget.EditText
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.UniqueOnly
import com.ibile.R
import com.ibile.core.currentContext
import com.ibile.currentMapFileName
import com.ibile.features.main.MainFragment
import com.ibile.features.main.mapfiles.MapFilesViewModel.Command
import com.ibile.markerFolderTitle
import com.ibile.utils.extensions.restartApp
import com.ibile.utils.views.OptionWithIconArrayAdapter

class MapFilesController(
    private val parent: MainFragment,
    private val viewModel: MapFilesViewModel
) : MapFilesOptionsContainerDialogFragment.Callback {

    private fun getDialogsContainer(name: String): MapFilesOptionsContainerDialogFragment {
        return parent.childFragmentManager.findFragmentByTag(name) as? MapFilesOptionsContainerDialogFragment
            ?: MapFilesOptionsContainerDialogFragment()
    }

    init {
        viewModel.selectSubscribe(
            parent,
            MapFilesViewModel.State::command,
            UniqueOnly(parent.mvrxViewId)
        ) {
            when (it) {
                is Command.ShowMapFilesOptions,
                is Command.ShowCreateNewMapFile,
                is Command.ShowMapFileRename,
                is Command.ShowMapFileDeleteConfirmation,
                is Command.ShowMapFileDeleteSuccessMsg,
                is Command.ShowMapFileOptions -> getDialogsContainer(it.javaClass.name)
                    .show(parent.childFragmentManager, it.javaClass.name)
                is Command.MapFileChange -> parent.currentContext.restartApp(parent.requireActivity()::class)
            }
        }
    }


    fun buildModels(controller: EpoxyController) {
        with(controller) {
            markerFolderTitle {
                id("DrawerViewSectionTitle_MapFile")
                text("Map file")
            }
            viewModel.getCurrentMapFile()?.let {
                currentMapFileName {
                    id(it.id)
                    name(it.name)
                    onClick { _ ->
                        viewModel.onClickCurrentMapFile()
                    }
                }
            }
        }
    }

    override fun getDialog_FileOptionsDialogFragment(): Dialog =
        when (val command = viewModel.state.command) {
            is Command.ShowMapFilesOptions -> {
                val options = (viewModel.state.command as Command.ShowMapFilesOptions).options
                val adapter = FileOptionsAndMapFilesArrayAdapter(parent.currentContext, options)
                AlertDialog.Builder(parent.currentContext)
                    .setTitle(R.string.title_dialog_file_options)
                    .setAdapter(adapter) { _, which ->
                        viewModel.onSelectMapFilesOptionsItem(which)
                    }
                    .setNegativeButton(R.string.text_cancel) { _, _ ->
                        /**
                         * this need not be called here again, but for some reasons,
                         * [onCancelDialog_FileOptionsDialogFragment] is not called when this
                         * negative button is pressed
                         */
                        viewModel.onCancelMapsFilesAction()
                    }
                    .create()
            }
            is Command.ShowMapFileRename -> {
                AlertDialog.Builder(parent.currentContext)
                    .setTitle(R.string.title_dialog_map_file_rename)
                    .setView(R.layout.dialog_view_rename_map_file_input)
                    .setPositiveButton(R.string.text_ok) { dialog: DialogInterface, _ ->
                        val et = (dialog as AlertDialog)
                            .findViewById<EditText>(R.id.et_new_map_file_name)
                        val value = et.text.toString()
                        viewModel.onClickRenameMapFileViewPositiveBtn(value)
                    }
                    .setNegativeButton(R.string.text_cancel) { _, _ -> }
                    .create()
            }
            is Command.ShowCreateNewMapFile -> {
                AlertDialog.Builder(parent.currentContext)
                    .setTitle(R.string.title_dialog_create_new_map_file)
                    .setView(R.layout.dialog_view_rename_map_file_input)
                    .setPositiveButton(R.string.text_ok) { dialog, _ ->
                        val et = (dialog as AlertDialog)
                            .findViewById<EditText>(R.id.et_new_map_file_name)
                        val value = et.text.toString()
                        viewModel.onClickCreateNewMapViewPositiveBtn(value)
                    }
                    .setNegativeButton(R.string.text_cancel) { _, _ -> }
                    .create()
            }
            is Command.ShowMapFileOptions -> {
                val mapFile = command.mapFile
                AlertDialog.Builder(parent.currentContext)
                    .setTitle(mapFile.name)
                    .setAdapter(
                        OptionWithIconArrayAdapter(parent.currentContext, MAP_FILE_OPTIONS)
                    ) { _, which ->
                        viewModel.onSelectMapFileOption(which)
                    }
                    .setNegativeButton(R.string.text_cancel) { _, _ -> }
                    .create()
            }
            is Command.ShowMapFileDeleteConfirmation -> {
                AlertDialog.Builder(parent.currentContext)
                    .setMessage(R.string.msg_dialog_confirm_delete_map_file)
                    .setPositiveButton(R.string.text_yes) { _, _ ->
                        viewModel.onClickDeleteMapFileConfirm()
                    }
                    .setNegativeButton(R.string.text_no) { _, _ -> }
                    .create()
            }
            is Command.ShowMapFileDeleteSuccessMsg -> {
                AlertDialog.Builder(parent.currentContext)
                    .setMessage(R.string.msg_dialog_delete_map_file_success)
                    .setPositiveButton(R.string.text_ok) { _, _ -> }
                    .create()
            }
            else -> throw RuntimeException("Unknown command")
        }

    override fun onCancelDialog_FileOptionsDialogFragment() {
        viewModel.onCancelMapsFilesAction()
    }

    companion object {
        internal val MAP_FILE_OPTIONS = listOf(
            OptionWithIconArrayAdapter.ItemOptionWithIcon(
                "Switch to map file",
                "Switch to this map file",
                R.drawable.ic_swap_horiz
            ),
            OptionWithIconArrayAdapter.ItemOptionWithIcon(
                "Delete map file",
                "This action will delete the map file from this device",
                R.drawable.ic_delete_24
            )
        )
    }
}

