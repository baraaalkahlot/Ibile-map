package com.ibile

import com.ibile.core.BaseFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.simpleController

class MainFragment : BaseFragment() {
    override fun epoxyController(): MvRxEpoxyController = simpleController {
        fragmentMain {
            id("main fragment")
            name("Ibile")
        }
    }
}
