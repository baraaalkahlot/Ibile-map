package com.ibile.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.*
import com.bumptech.glide.request.RequestOptions
import com.ibile.MarkerFullImageItemBindingModel_
import com.ibile.PreviewImagesActionBarBindingModel_
import com.ibile.R
import com.ibile.core.BaseDialogFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.currentContext
import com.ibile.core.simpleController
import com.ibile.data.database.entities.Marker
import com.ibile.utils.Misc
import com.ibile.utils.extensions.getProviderUri

class MarkerImagesPreviewFragment : BaseDialogFragment() {
    private val callback: Callback
        get() = parentFragment as Callback
    private val marker
        get() = (callback.mode as? Callback.Mode.Edit)?.marker
            ?: (callback.mode as Callback.Mode.View).marker
    private var currentImageItemIndex: Int = 0

    private val imageItemRequestOptions
        get() = RequestOptions().centerInside().placeholder(Misc.createCircularProgressDrawable())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar)
        currentImageItemIndex = callback.mode.initialImageItemIndex
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        if (marker.imageUris.isEmpty()) {
            dismiss()
            return@simpleController
        }
        EpoxyModelGroup(R.layout.dialog_fragment_marker_images_preview,
            PreviewImagesActionBarBindingModel_()
                .id("ActionBar")
                .editMode(callback.mode is Callback.Mode.Edit)
                .onClickCloseBtn { _ -> dismiss() }
                .onClickShareBtn { _ -> shareMarkerImage() }
                .onClickDeleteBtn { _ -> handleDeleteBtnClick() },
            FullScreenCarouselModel_()
                .id("ImagePreviewCarousel")
                .numViewsToShowOnScreen(1f)
                .initialItemIndex(callback.mode.initialImageItemIndex)
                .onPositionChanged { currentImageItemIndex = it }
                .models(imageItemModels)
        )
            .id("ImagesPreview")
            .addTo(this)
    }

    private val imageItemModels
        get() = marker.imageUris.map { uri ->
            MarkerFullImageItemBindingModel_()
                .id(uri.toString())
                .uri(uri)
                .requestOptions(imageItemRequestOptions)
        }

    private fun handleDeleteBtnClick() {
        (callback.mode as Callback.Mode.Edit).onClickDeleteImageBtn()
    }

    private fun shareMarkerImage() {
        val imageUri = marker.imageUris[currentImageItemIndex]
        val file = imageUri.toFile()
        val providerUri = currentContext.getProviderUri(file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, providerUri)
            type = "image/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val intent = Intent.createChooser(shareIntent, "Share via")
        startActivity(intent)
    }

    interface Callback {
        val mode: Mode

        sealed class Mode {
            abstract val marker: Marker

            abstract val initialImageItemIndex: Int

            abstract class Edit(val onClickDeleteImageBtn: () -> Unit) : Mode()

            abstract class View : Mode()
        }
    }

    companion object {
        const val FRAGMENT_TAG_MARKER_IMAGES_PREVIEW = "FRAGMENT_TAG_MARKER_IMAGES_PREVIEW"
    }
}

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT)
class FullScreenCarousel(context: Context) : Carousel(context) {
    override fun createLayoutManager(): LayoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    var onPositionChanged: ((newPosition: Int) -> Unit)? = null
        @CallbackProp set

    @ModelProp
    fun initialItemIndex(index: Int) {
        this.scrollToPosition(index)
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            (this.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()?.let {
                onPositionChanged?.invoke(it)
            }
        }
    }
}