package com.ibile.features.intenthandler

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.airbnb.mvrx.UniqueOnly
import com.airbnb.mvrx.activityViewModel
import com.ibile.core.BaseFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.simpleController

class NewIntentHandlerFragment : BaseFragment() {

    private val args by navArgs<NewIntentHandlerFragmentArgs>()

    private val newIntentHandlerViewModel: NewIntentHandlerViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newIntentHandlerViewModel.onNewIntent(args.intent)

        newIntentHandlerViewModel.selectSubscribe(
            NewIntentHandlerViewModel.State::viewCommand,
            UniqueOnly(mvrxViewId)
        ) {
            if (it == null) return@selectSubscribe
            when (it) {
                is NewIntentHandlerViewModel.Companion.ViewCommand.ShowCreateImportedMarkerView -> {
                    NewIntentHandlerFragmentDirections
                        .actionNewIntentHandlerFragmentToCreateImportedMarkerFragment(it.latLng)
                        .navigate()
                }
            }
        }
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        /* no-op */
    }
}
