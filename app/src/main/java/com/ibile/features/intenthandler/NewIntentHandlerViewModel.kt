package com.ibile.features.intenthandler

import android.content.Intent
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.BaseViewModel
import com.ibile.features.intenthandler.NewIntentHandlerViewModel.State

class NewIntentHandlerViewModel(initialState: State) : BaseViewModel<State>(initialState) {

    fun onNewIntent(intent: Intent) {
        if (intent.scheme == "geo") handleLocationIntent(intent)
    }

    private fun handleLocationIntent(intent: Intent) {
        val latLng = intent.data?.toString()
            ?.removePrefix("geo:")
            ?.replaceAfterLast("?", "")
            ?.trimEnd('?')
            ?.split(",")
        if (latLng == null || latLng.size < 2) return
        val latitude = latLng[0].toDoubleOrNull()
        val longitude = latLng[1].toDoubleOrNull()

        val coordIsValid = latitude?.toInt() in -90 until 90 && longitude?.toInt() in -180 until 180
        if (!coordIsValid) return

        val importedMarkerLatLng = LatLng(latitude!!, longitude!!)
        setState { copy(viewCommand = ViewCommand.ShowCreateImportedMarkerView(importedMarkerLatLng)) }
    }

    data class State(val viewCommand: ViewCommand? = null) : MvRxState

    companion object : MvRxViewModelFactory<NewIntentHandlerViewModel, State> {
        sealed class ViewCommand {
            data class ShowCreateImportedMarkerView(val latLng: LatLng) : ViewCommand()
        }
    }
}
