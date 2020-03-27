package com.ibile.features.shared.subscriptionrequired

import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.data.repositiories.BillingRepository
import org.koin.android.ext.android.get

class SubscriptionRequiredViewModel(
    initialState: State,
    billingRepository: BillingRepository
) :
    BaseViewModel<SubscriptionRequiredViewModel.State>(initialState) {

    init {
        with(billingRepository) {
            startConnection()
            this.subscriptionStatus.execute { copy(subscriptionStatus = it) }
        }
    }

    data class State(val subscriptionStatus: Async<BillingRepository.SubscriptionStatus> = Uninitialized) :
        MvRxState

    companion object : MvRxViewModelFactory<SubscriptionRequiredViewModel, State> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): SubscriptionRequiredViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return SubscriptionRequiredViewModel(state, fragment.get())
        }
    }
}
