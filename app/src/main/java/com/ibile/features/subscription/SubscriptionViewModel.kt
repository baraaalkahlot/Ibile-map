package com.ibile.features.subscription

import android.app.Activity
import android.util.Log
import androidx.navigation.NavDirections
import com.airbnb.mvrx.*
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.ibile.core.BaseViewModel
import com.ibile.data.repositiories.BillingRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class SubscriptionViewModel(
    initialState: State,
    private val billingRepository: BillingRepository
) : BaseViewModel<SubscriptionViewModel.State>(initialState) {

    init {
        billingRepository.subscriptionStatus
            .execute { copy(subscriptionState = it) }
        asyncSubscribe(State::subscriptionState) {
            if (!it.isActive) {
                getSubscriptionsSkusList()
            }
        }
    }

    fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails) {
        billingRepository.launchSubscriptionBillingFlow(activity, skuDetails)
    }

    private fun getSubscriptionsSkusList() {
        billingRepository
            .getSubscriptionsSkusDetails()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute {
                copy(getSubscriptionsListAsyncResult = it)
            }
    }

    data class State(
        val subscriptionState: Async<BillingRepository.SubscriptionStatus> = Uninitialized,
        val getSubscriptionsListAsyncResult: Async<List<SkuDetails>> = Uninitialized,
        val command: Command? = null
    ) : MvRxState

    sealed class Command {
        data class Navigate(val direction: NavDirections) : Command()
    }

    companion object : MvRxViewModelFactory<SubscriptionViewModel, State> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): SubscriptionViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return SubscriptionViewModel(state, fragment.get())
        }
    }
}
