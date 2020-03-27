package com.ibile.features.subscription

import android.view.View
import com.airbnb.epoxy.EpoxyModelGroup
import com.airbnb.mvrx.fragmentViewModel
import com.ibile.R
import com.ibile.SubscriptionActiveStateBindingModel_
import com.ibile.SubscriptionSkuItemBindingModel_
import com.ibile.core.BaseFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.simpleController
import com.ibile.utils.extensions.modelGroup
import com.ibile.utils.extensions.navController
import com.ibile.utils.views.ListModel_

class SubscriptionFragment : BaseFragment() {
    private val viewModel: SubscriptionViewModel by fragmentViewModel()

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        val stateModel = if (viewModel.state.subscriptionState()?.isActive == true) {
            SubscriptionActiveStateBindingModel_()
                .id("SubscriptionActiveState")
                .onClick { _: View? -> navController.popBackStack() }
        } else {
            val subscriptionSkuModelItems =
                (viewModel.state.getSubscriptionsListAsyncResult() ?: listOf()).map {
                    SubscriptionSkuItemBindingModel_()
                        .id(it.sku)
                        .sku(it)
                        .onClick { model, _, _, _ ->
                            viewModel.launchBillingFlow(requireActivity(), model.sku())
                        }
                }
            val subscriptionsSkusList = ListModel_()
                .id("SubscriptionsSkusList")
                .gridCount(2)
                .models(subscriptionSkuModelItems)
            EpoxyModelGroup(
                R.layout.partial_group_subscription_inactive_state,
                subscriptionsSkusList
            )
        }
        modelGroup(R.layout.fragment_subscription, stateModel)
    }
}
