package com.ibile.features.dataimport

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.fragment.navArgs
import com.airbnb.mvrx.UniqueOnly
import com.airbnb.mvrx.activityViewModel
import com.ibile.core.BaseFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.simpleController
import com.ibile.features.dataimport.DataImportViewModel.Companion.ViewCommand
import com.ibile.utils.extensions.navController

class DataImportHandlerFragment : BaseFragment() {

    private val args by navArgs<DataImportHandlerFragmentArgs>()

    private val dataImportViewModel: DataImportViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataImportViewModel.onNewIntent(args.intent)

        dataImportViewModel.selectSubscribe(
            DataImportViewModel.State::viewCommand,
            UniqueOnly(mvrxViewId)
        ) {
            if (it == null) return@selectSubscribe
            when (it) {
                is ViewCommand.ShowCreateImportedMarkerView -> DataImportHandlerFragmentDirections
                    .actionDataImportHandlerFragmentToCreateImportedMarkerFragment(it.latLng)
                    .navigate()
                is ViewCommand.ShowImportConfirmationDialog -> {
                    DataImportHandlerFragmentDirections
                        .actionDataImportHandlerFragmentToImportConfirmationDialogFragment()
                        .navigate()
                }
                is ViewCommand.Exit -> {
                    Log.d("AAA", "onViewCreated: stage 1 here")
                    navController.popBackStack()
                }
            }
        }
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        /* no-op */
    }
}
