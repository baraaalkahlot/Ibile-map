package com.ibile.features.main.datasharing

import android.content.Intent
import com.airbnb.mvrx.UniqueOnly
import com.ibile.R
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.features.main.MainFragment
import com.ibile.features.main.datasharing.DataSharingViewModel.Companion.Command
import com.ibile.utils.extensions.startResolvableActivityForResult
import com.ibile.utils.views.OptionWithIconArrayAdapter.ItemOptionWithIcon

class DataSharingHandler(val fragment: MainFragment, private val viewModel: DataSharingViewModel) :
    ShareOptionsDialogFragment.Callback {

    init {
        viewModel.selectSubscribe(
            fragment,
            DataSharingViewModel.State::viewCommand,
            deliveryMode = UniqueOnly(this.javaClass.name)
        ) {
            if (it == null) return@selectSubscribe
            when (it) {
                is Command.ShowInitialShareOptionsDialog,
                is Command.ShowInitialShareOptionsWithMarkerOptionsDialog,
                is Command.ShowSelectFolderToExportDialog,
                is Command.ShowEntityDataExportOptionsDialog -> showShareOptionsDialogFragment(it)
                is Command.ShareData ->
                    fragment.startResolvableActivityForResult(it.data, RC_DATA_SHARING)
                is Command.TakeMapSnapshot -> takeMapSnapshot()
            }
        }
    }

    private fun showShareOptionsDialogFragment(command: Command) {
        val tag = command.javaClass.name

        val dialogFragment =
            fragment.childFragmentManager.findFragmentByTag(tag) as? ShareOptionsDialogFragment
        if (dialogFragment != null && dialogFragment.dialog?.isShowing == true) return

        ShareOptionsDialogFragment().show(fragment.childFragmentManager, tag)
    }

    fun init(activeMarkerId: Long?) {
        viewModel.init(activeMarkerId)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_DATA_SHARING) viewModel.onCompleteOrCancelShareOperation()
    }

    private fun takeMapSnapshot() {
        fragment.map.snapshot { bitmap -> viewModel.onCompleteTakeMapShot(bitmap) }
    }

    override val optionItems_ShareDataOptionsDialogFragment: List<ItemOptionWithIcon>
        get() {
            return when (val command = viewModel.state.viewCommand) {
                is Command.ShowInitialShareOptionsDialog -> initialShareOptionsItems
                is Command.ShowInitialShareOptionsWithMarkerOptionsDialog -> initialShareOptionsWithMarkerOptionsItems
                is Command.ShowEntityDataExportOptionsDialog -> exportEntityDataFileOptions
                is Command.ShowSelectFolderToExportDialog ->
                    convertFoldersToShareActionDialogItems(command.foldersWithMarkers)
                else -> emptyList()
            }
        }

    private fun convertFoldersToShareActionDialogItems(folders: List<FolderWithMarkers>): List<ItemOptionWithIcon> {
        return folders.map {
            ItemOptionWithIcon(
                "Folder: ${it.folder.title}",
                "Share as file",
                R.drawable.ic_action_share_folder
            )
        }
    }

    override val title_ShareDataOptionsDialogFragment: String
        get() {
            return when (viewModel.state.viewCommand) {
                is Command.ShowInitialShareOptionsDialog,
                is Command.ShowSelectFolderToExportDialog,
                is Command.ShowInitialShareOptionsWithMarkerOptionsDialog -> "Share"
                is Command.ShowEntityDataExportOptionsDialog -> "Which format do you want to export?"
                else -> ""
            }
        }

    override fun onCancel_ShareDataOptionsDialogFragment() {
        viewModel.onCancelShareOperation()
    }

    override fun onSelectOption_ShareDataOptionsDialogFragment(optionIndex: Int) {
        when (viewModel.state.viewCommand) {
            is Command.ShowInitialShareOptionsDialog ->
                viewModel.onSelectInitialShareOptionItem(optionIndex)
            is Command.ShowInitialShareOptionsWithMarkerOptionsDialog ->
                viewModel.onSelectInitialShareOptionWithMarkerOptionsItem(optionIndex)
            is Command.ShowSelectFolderToExportDialog ->
                viewModel.onSelectFolderToExportData(optionIndex)
            is Command.ShowEntityDataExportOptionsDialog ->
                viewModel.onSelectEntityDataExportFormat(optionIndex)
        }
    }

    companion object {

        val initialShareOptionsItems = listOf(
            ItemOptionWithIcon(
                "Snapshot",
                "Share a snapshot of the map",
                R.drawable.ic_image_28
            ),
            ItemOptionWithIcon(
                "All markers and folders",
                "Share as file",
                R.drawable.ic_action_share_folder
            ),
            ItemOptionWithIcon(
                "Other options",
                iconSrc = R.drawable.ic_more_vert_24
            )
        )

        val initialShareOptionsWithMarkerOptionsItems = listOf(
            ItemOptionWithIcon(
                "Marker as plain text",
                "Share the selected marker as plain text into another app",
                R.drawable.ic_share_24
            ),
            ItemOptionWithIcon(
                "Marker as file",
                "Share the selected marker as file into another app",
                R.drawable.ic_share_24
            )
        ) + initialShareOptionsItems

        val exportEntityDataFileOptions = listOf(
            ItemOptionWithIcon(
                "KML",
                "Standard Google Earth format. Cannot contain media."
            ),
            ItemOptionWithIcon(
                "KMZ",
                "Zip Google Earth format. Can contain media."
            ),
            ItemOptionWithIcon(
                "CSV",
                "Standard text spreadsheet format. Cannot contain media. Cannot contain lines or polygons."
            )
        )

        const val RC_DATA_SHARING = 10065
    }
}
