package com.ibile.core

import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.withState

abstract class BaseViewModel<S : MvRxState>(initialState: S) :
    BaseMvRxViewModel<S>(initialState) {
    val state: S
        get() = withState(this) { it }

    fun updateState(newState: S.() -> S) {
        setState { newState() }
    }
}
