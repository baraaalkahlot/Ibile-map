package com.ibile.features.shared.subscriptionrequired

import android.os.Bundle
import android.util.Log
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.ibile.NavGraphDirections
import com.ibile.core.BaseFragment
import com.ibile.utils.extensions.navController

abstract class SubscriptionRequiredFragment : BaseFragment() {

    protected val subscriptionRequiredViewModel: SubscriptionRequiredViewModel by fragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscriptionRequiredViewModel.asyncSubscribe(SubscriptionRequiredViewModel.State::subscriptionStatus) {
            if (!it.isActive) {
                navController.navigate(NavGraphDirections.actionGlobalSubscriptionFragment())
            }
        }
    }
}
