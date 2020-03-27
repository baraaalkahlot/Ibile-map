package com.ibile.utils.extensions

import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelGroup

fun EpoxyController.modelGroup(
    layout: Int,
    vararg models: EpoxyModel<*>
): EpoxyModelGroup {
    return EpoxyModelGroup(layout, *models)
        .apply { addTo(this@modelGroup) }
}
