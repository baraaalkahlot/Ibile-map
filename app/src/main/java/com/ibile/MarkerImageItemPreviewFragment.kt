package com.ibile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toFile
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.*
import com.airbnb.mvrx.activityViewModel
import com.bumptech.glide.request.RequestOptions
import com.ibile.core.BaseDialogFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.currentContext
import com.ibile.core.simpleController
import com.ibile.data.database.entities.Marker
import com.ibile.utils.Misc
import com.ibile.utils.extensions.getProviderUri

class MarkerImageItemPreviewFragment : BaseDialogFragment() {
    private val markersViewModel: MarkersViewModel by activityViewModel()
    private val args: MarkerImageItemPreviewFragmentArgs by navArgs()
    private val imageItemRequestOptions
        get() = RequestOptions().centerInside().placeholder(Misc.createCircularProgressDrawable())

    private var currentImageIndex: Int = 0
    private lateinit var originalMarker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar)
        currentImageIndex = args.currentItem
        if (args.editMode) originalMarker =
            markersViewModel.getMarkerById(markersViewModel.state.markerForEdit!!.id)
    }

    override fun epoxyController(): MvRxEpoxyController =
        simpleController(markersViewModel) { markersViewModelState ->
            val marker = if (args.editMode) markersViewModelState.markerForEdit!!
            else markersViewModel.getActiveMarker()!!
            if (marker.imageUris.isEmpty()) {
                findNavController().popBackStack()
                return@simpleController
            }
            EpoxyModelGroup(R.layout.dialog_fragment_marker_images_preview,
                PreviewImagesActionBarBindingModel_()
                    .id("ActionBar")
                    .editMode(args.editMode)
                    .onClickCloseBtn { _ -> findNavController().popBackStack() }
                    .onClickShareBtn { _ ->
                        if (args.editMode) return@onClickShareBtn
                        shareMarkerImage(marker.imageUris[currentImageIndex])
                    }
                    .onClickShareBtn { _ -> shareMarkerImage(marker.imageUris[currentImageIndex]) }
                    .onClickDeleteBtn { _ ->
                        if (!args.editMode) return@onClickDeleteBtn
                        updateMarkerImages()
                    },
                FullScreenCarouselModel_()
                    .id("ImagePreviewCarousel")
                    .numViewsToShowOnScreen(1f)
                    .onPositionChanged { currentImageIndex = it }
                    .currentItem(args.currentItem)
                    .models(marker.imageUris.map { uri ->
                        MarkerFullImageItemBindingModel_()
                            .id(uri.toString())
                            .uri(uri)
                            .requestOptions(imageItemRequestOptions)
                    })
            )
                .id("ImagesPreview")
                .addTo(this)
        }

    private fun shareMarkerImage(imageUri: Uri) {
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

    private fun updateMarkerImages() {
        markersViewModel.editMarker {
            val updatedUris = imageUris.filterIndexed { index, uri ->
                if (index == currentImageIndex && !originalMarker.imageUris.contains(uri)) {
                    markersViewModel.deleteUnsavedMarkerImage(uri)
                }
                index != currentImageIndex
            }
            copy(imageUris = updatedUris)
        }
    }
}

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT)
class FullScreenCarousel(context: Context) : Carousel(context) {
    override fun createLayoutManager(): LayoutManager {
        return LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    var onPositionChanged: ((newPosition: Int) -> Unit)? = null
        @CallbackProp set

    @ModelProp
    fun setCurrentItem(item: Int) {
        this.scrollToPosition(item)
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