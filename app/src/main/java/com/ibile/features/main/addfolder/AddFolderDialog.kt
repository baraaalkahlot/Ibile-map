package com.ibile.features.main.addfolder

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import com.airbnb.mvrx.fragmentViewModel
import com.ibile.R
import com.ibile.core.*
import com.ibile.core.Extensions.dp
import com.ibile.data.database.entities.Folder
import com.ibile.databinding.DialogAddMarkerFolderBinding
import com.maltaisn.icondialog.IconDialog
import com.maltaisn.icondialog.IconDialogSettings
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import org.koin.android.ext.android.inject
import petrov.kristiyan.colorpicker.ColorPicker

class AddFolderDialog : BaseDialogFragment(), IconDialog.Callback {
    private val callback: Callback
        get() = parentFragment as Callback

    private val viewModel: AddFolderViewModel by fragmentViewModel()

    private val iconDialog: IconDialog
        get() = childFragmentManager.findFragmentByTag(IconDialog::class.simpleName) as IconDialog?
            ?: IconDialog.newInstance(IconDialogSettings { showSelectBtn = false })

    private val colorPicker: ColorPicker
        get() {
            val colorPicker = ColorPicker(requireActivity())
                .disableDefaultButtons(true)
                .setColumns(5)
                .setDefaultColorButton(viewModel.state.folder.color)
                .setTitle("Choose folder color")
                .setRoundColorButton(true)
                .setColors(R.array.marker_colors)
                .setOnFastChooseColorListener(object : ColorPicker.OnFastChooseColorListener {
                    override fun setOnFastChooseColorListener(position: Int, color: Int) {
                        viewModel.updateState { copy(folder = folder.copy(color = color)) }
                    }

                    override fun onCancel() {}
                })
            with(colorPicker.dialogViewLayout) {
                setBackgroundColor(context.getResColor(R.color.dark_gray))
                val titleView =
                    findViewById<AppCompatTextView>(petrov.kristiyan.colorpicker.R.id.title)
                titleView.setTextColor(currentContext.getResColor(R.color.white))
                titleView.textSize = 12f.dp
            }
            return colorPicker
        }

    override val iconDialogIconPack: IconPack by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAddMarkerFolderBinding.inflate(LayoutInflater.from(context))

        binding.data = viewModel
        binding.setOnFolderNameInputChange { handleFolderTitleInputChange(it.toString()) }
        binding.ibFolderColor.setOnClickListener { view -> handleFolderColorBtnClick(view) }
        binding.ibFolderIcon.setOnClickListener { handleFolderIconBtnClick() }

        return AlertDialog
            .Builder(currentContext, R.style.AlertDialog)
            .setTitle("Add folder")
            .setView(binding.root)
            .setPositiveButton(R.string.text_ok) { _, _ ->
                callback.onAddFolderDialogOkBtnClick(viewModel.state.folder)
            }
            .setNegativeButton(R.string.text_cancel) { _, _ -> }
            .create()
    }

    private fun handleFolderIconBtnClick() {
        iconDialog
            .show(childFragmentManager, IconDialog::class.simpleName)
    }

    override fun onIconDialogIconsSelected(dialog: IconDialog, icons: List<Icon>) {
        viewModel.updateState { copy(folder = folder.copy(iconId = icons[0].id)) }
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        // no use
    }

    private fun handleFolderTitleInputChange(value: String) {
        viewModel.updateState { copy(folder = folder.copy(title = value)) }
    }

    private fun handleFolderColorBtnClick(clickedView: View) {
        when (clickedView.id) {
            R.id.ib_folder_icon -> iconDialog
                .show(childFragmentManager, IconDialog::class.simpleName)
            R.id.ib_folder_color -> colorPicker.show()
        }
    }

    interface Callback {
        fun onAddFolderDialogOkBtnClick(folder: Folder)
    }

    companion object {
        fun newInstance(): AddFolderDialog {
            return AddFolderDialog()
        }
    }
}
