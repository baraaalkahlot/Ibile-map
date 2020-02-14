package com.ibile.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LifecycleOwner
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.MvRxView
import com.airbnb.mvrx.MvRxViewId
import com.ibile.R

abstract class BaseDialogFragment : DialogFragment(), MvRxView {

    private val mvrxViewIdProperty = MvRxViewId()
    final override val mvrxViewId: String by mvrxViewIdProperty

    protected lateinit var recyclerView: EpoxyRecyclerView
    protected val epoxyController by lazy { epoxyController() }

    open val layoutId: Int = R.layout.fragment_base

    override fun onCreate(savedInstanceState: Bundle?) {
        mvrxViewIdProperty.restoreFrom(savedInstanceState)
        super.onCreate(savedInstanceState)
        epoxyController.onRestoreInstanceState(savedInstanceState)
    }

    override val subscriptionLifecycleOwner: LifecycleOwner
        get() = this.viewLifecycleOwnerLiveData.value ?: this

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerview)
        recyclerView.setController(epoxyController)
    }

    override fun invalidate() {
        recyclerView.requestModelBuild()
    }

    /**
     * Provide the EpoxyController to use when building models for this Fragment.
     * Basic usages can simply use [simpleController]
     */
    abstract fun epoxyController(): MvRxEpoxyController

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mvrxViewIdProperty.saveTo(outState)
        epoxyController.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        epoxyController.cancelPendingModelBuild()
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        // This ensures that invalidate() is called for static screens that don't
        // subscribe to a ViewModel.
        postInvalidate()
    }
}
