package com.ibile.features.main.editfolder

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import androidx.databinding.adapters.TextViewBindingAdapter.AfterTextChanged
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.UniqueOnly
import com.airbnb.mvrx.fragmentViewModel
import com.ibile.R
import com.ibile.core.BaseDialogFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.currentContext
import com.ibile.core.simpleController
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.Marker
import com.ibile.databinding.DialogEditMarkerFolderBinding
import com.ibile.databinding.DialogEditMarkerFolderHeaderBinding
import com.ibile.features.main.utils.ColorPickerWrapper
import com.ibile.features.main.utils.ColorPickerWrapper.Options
import com.maltaisn.icondialog.IconDialog
import com.maltaisn.icondialog.IconDialogSettings
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import org.koin.android.ext.android.inject
import petrov.kristiyan.colorpicker.ColorPicker

class EditFolderDialogFragment : BaseDialogFragment(), ViewBindingData, IconDialog.Callback,
    ColorPicker.OnFastChooseColorListener {

    val viewModel: EditFolderViewModel by fragmentViewModel()

    override val iconDialogIconPack: IconPack by inject()

    override val data: EditFolderViewModel by lazy { viewModel }

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
        if (viewModel.state.getFolderAsync !is Success) {
            viewModel.getFolder(viewModel.state.folderId)
        }
        viewModel
            .asyncSubscribe(EditFolderViewModel.State::getFolderAsync, UniqueOnly(mvrxViewId)) {
                viewModel.updateState { copy(folder = it) }
            }
        viewModel.asyncSubscribe(EditFolderViewModel.State::updateFolderAsync) { this.dismiss() }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(currentContext)
        val binding = DialogEditMarkerFolderBinding.inflate(inflater)
            .apply { data = this@EditFolderDialogFragment }
        val headerViewBinding = DialogEditMarkerFolderHeaderBinding.inflate(inflater)
            .apply {
                this.deleteBtnIsVisible = viewModel.state.folderId != 1L
                this.btnDeleteFolder.setOnClickListener { handleDeleteBtnClick() }
            }

        return AlertDialog
            .Builder(currentContext, R.style.AlertDialog)
            .setCustomTitle(headerViewBinding.root)
            .setView(binding.root)
            .setPositiveButton(R.string.text_save, null)
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

    private fun handleSaveBtnClick() {
        val (_, folder, getFolderAsync) = viewModel.state
        val originalFolder = getFolderAsync()!!

        if (originalFolder == folder) this.dismiss()
        if (folder.color == originalFolder.color && folder.iconId == originalFolder.iconId)
            return viewModel.updateFolder(folder)

        val markersFieldsToUpdateMap = mutableMapOf<String, Boolean>()
        if (folder.color != originalFolder.color) markersFieldsToUpdateMap["Update color"] = true
        if (folder.iconId != originalFolder.iconId)
            markersFieldsToUpdateMap["Update pin icon"] = true

        val fieldsToUpdateDialog = getMarkersFieldsToUpdateDialog(
            markersFieldsToUpdateMap,
            { option, value -> markersFieldsToUpdateMap[option] = value },
            { handleMarkersFieldsToUpdateDialogOkBtnClick(markersFieldsToUpdateMap, folder) }
        )
        fieldsToUpdateDialog.show()
    }

    private fun getMarkersFieldsToUpdateDialog(
        optionsMap: Map<String, Boolean>,
        onOptionValueChange: (option: String, value: Boolean) -> Unit,
        onClickPositiveBtn: () -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(currentContext, R.style.AlertDialog)
            .setTitle("Pick the fields you want to apply to this folder's POIs")
            .setMultiChoiceItems(
                optionsMap.keys.toTypedArray(),
                optionsMap.values.toBooleanArray()
            ) { _, which, isChecked ->
                onOptionValueChange(optionsMap.keys.toList()[which], isChecked)
            }
            .setNeutralButton(getText(R.string.text_cancel)) { _, _ -> }
            .setPositiveButton(getText(R.string.text_ok)) { _, _ ->
                onClickPositiveBtn()
            }
            .create()
    }

    private fun handleMarkersFieldsToUpdateDialogOkBtnClick(
        markersFieldsToUpdateMap: Map<String, Boolean>,
        folder: Folder
    ) {
        val noFieldsToUpdate = markersFieldsToUpdateMap.values.all { !it }
        if (noFieldsToUpdate) viewModel.updateFolder(folder)
        else {
            val fieldsToUpdate = markersFieldsToUpdateMap.filter { it.value }.keys.toList()
            viewModel.updateFolderWithMarkers(folder) { markers: List<Marker> ->
                markers.map { marker ->
                    marker.copy(
                        color = if (fieldsToUpdate.contains("Update color")) folder.color else marker.color,
                        icon = if (fieldsToUpdate.contains("Update pin icon")) Marker.Icon(folder.iconId) else marker.icon
                    )
                }
            }
        }
    }

    private fun handleDeleteBtnClick() {
        AlertDialog.Builder(currentContext, R.style.AlertDialog)
            .setMessage(R.string.text_dialog_delete_folder_message)
            .setPositiveButton(R.string.text_yes) { _, _ ->
                handleDeleteFolderConfirmationDialogOkBtnClick()
            }
            .setNegativeButton(R.string.text_no) { _, _ -> }
            .show()
    }

    private fun handleDeleteFolderConfirmationDialogOkBtnClick() {
        viewModel.deleteFolderWithMarkers(viewModel.state.folder)
    }

    override fun setOnFastChooseColorListener(position: Int, color: Int) {
        viewModel.updateState { copy(folder = folder.copy(color = color)) }
    }

    override fun onCancel() {}

    override fun onIconDialogIconsSelected(dialog: IconDialog, icons: List<Icon>) {
        viewModel.updateState { copy(folder = folder.copy(iconId = icons[0].id)) }
    }

    override val iconBtnClickListener = OnClickListener {
        iconDialog.show(childFragmentManager, IconDialog::class.simpleName)
    }

    override val colorBtnClickListener = OnClickListener {
        colorPickerDialog.show()
    }

    override val folderNameInputChangeListener = AfterTextChanged {
        viewModel.updateState { copy(folder = folder.copy(title = it.toString())) }
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        // can't use epoxy because of an issue with keyboard not showing when edittext is focused
        //  in this dialog
    }

    companion object {
        fun newInstance(folderId: Long): EditFolderDialogFragment {
            val args = Bundle()
            args.putLong(ARG_FOLDER_ID, folderId)
            val fragment = EditFolderDialogFragment()
            fragment.arguments = args
            return fragment
        }

        const val ARG_FOLDER_ID = "ARG_FOLDER_ID"
    }
}
