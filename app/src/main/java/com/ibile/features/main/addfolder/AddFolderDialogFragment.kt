package com.ibile.features.main.addfolder

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.fragmentViewModel
import com.ibile.R
import com.ibile.core.BaseDialogFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.currentContext
import com.ibile.core.simpleController
import com.ibile.databinding.DialogAddMarkerFolderBinding
import com.ibile.features.main.utils.ColorPickerWrapper
import com.ibile.features.main.utils.ColorPickerWrapper.Options
import com.maltaisn.icondialog.IconDialog
import com.maltaisn.icondialog.IconDialogSettings
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import org.koin.android.ext.android.inject
import petrov.kristiyan.colorpicker.ColorPicker

class AddFolderDialogFragment : BaseDialogFragment(), IconDialog.Callback,
    ColorPicker.OnFastChooseColorListener {

    private val viewModel: AddFolderViewModel by fragmentViewModel()

    override val iconDialogIconPack: IconPack by inject()

    private val iconDialog: IconDialog
        get() = childFragmentManager.findFragmentByTag(IconDialog::class.simpleName) as IconDialog?
            ?: IconDialog.newInstance(IconDialogSettings { showSelectBtn = false })

    private val colorPickerDialog: ColorPicker
        get() {
            val options = Options(viewModel.state.folder.color, "Choose folder color", this)
            return ColorPickerWrapper(requireActivity(), options).create()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.asyncSubscribe(AddFolderViewModel.State::addFolderAsync) { this.dismiss() }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAddMarkerFolderBinding.inflate(LayoutInflater.from(context))

        binding.data = viewModel
        binding.setOnFolderNameInputChange { handleFolderTitleInputChange(it.toString()) }
        binding.ibFolderColor.setOnClickListener { handleFolderColorBtnClick() }
        binding.ibFolderIcon.setOnClickListener { handleFolderIconBtnClick() }

        return AlertDialog
            .Builder(currentContext, R.style.AlertDialog)
            .setTitle("Add folder")
            .setView(binding.root)
            .setPositiveButton(R.string.text_ok, null)
            .setNegativeButton(R.string.text_cancel) { _, _ -> }
            .create()
            .apply {
                setOnShowListener { dialog ->
                    (dialog as AlertDialog)
                        .getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener { handleSaveBtnClick() }
                }
            }
    }

    private fun handleFolderIconBtnClick() {
        iconDialog.show(childFragmentManager, IconDialog::class.simpleName)
    }

    private fun handleFolderColorBtnClick() {
        colorPickerDialog.show()
    }

    private fun handleFolderTitleInputChange(value: String) {
        viewModel.updateState { copy(folder = folder.copy(title = value)) }
    }

    private fun handleSaveBtnClick() {
        val folder = viewModel.state.folder
        if (folder.title.isBlank()) {
            this.dismiss()
            return
        }
        viewModel.addFolder(folder)
        this.dismiss()

    }

    override fun onIconDialogIconsSelected(dialog: IconDialog, icons: List<Icon>) {
        viewModel.updateState { copy(folder = folder.copy(iconId = icons[0].id)) }
    }

    override fun setOnFastChooseColorListener(position: Int, color: Int) {
        viewModel.updateState { copy(folder = folder.copy(color = color)) }
    }

    override fun onCancel() {}

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        // can't use epoxy because of an issue with keyboard not showing when edittext is focused
        //  in this dialog
    }

    companion object {
        fun newInstance(): AddFolderDialogFragment {
            return AddFolderDialogFragment()
        }
    }
}
