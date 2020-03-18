package com.ibile.features.main.markerslist

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.libraries.maps.model.LatLng
import com.ibile.R
import com.ibile.core.currentContext

class MarkerNavigationOptionsDialogFragment : DialogFragment() {
    private lateinit var point: LatLng

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        point = (savedInstanceState ?: arguments)?.getParcelable(ARG_MARKER_COORDS)!!

        val options = buildOptions()
        val adapter = buildAdapter(options)

        return AlertDialog.Builder(currentContext, R.style.AlertDialog)
            .setTitle("Navigation options")
            .setAdapter(adapter) { _, which ->
                val option = options[which]
                startActivity(option.intent)
            }
            .setNegativeButton(R.string.text_cancel) { _, _ -> }
            .create()
    }

    private fun buildOptions(): List<NavigationOption> {
        val queryIntent = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("geo:${point.latitude},${point.longitude}"))

        val pm = currentContext.packageManager
        val results = pm.queryIntentActivities(queryIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .toMutableList()

        val googleMapNavOptions =
            results.find { it.activityInfo.packageName == GOOGLE_MAP_PACKAGE_NAME }?.let {
                results.removeAt(results.indexOf(it))
                val option = NavigationOption(it.loadLabel(pm), it.loadIcon(pm), queryIntent)
                createGoogleMapOptions(option)
            } ?: listOf()

        return results
            .map { item ->
                val intent = Intent(queryIntent).setPackage(item.activityInfo.packageName)
                NavigationOption(item.loadLabel(pm), item.loadIcon(pm), intent)
            }
            .toMutableList()
            .apply { addAll(0, googleMapNavOptions) }
    }

    private fun createGoogleMapOptions(option: NavigationOption): List<NavigationOption> {
        return listOf(
            option.copy(
                intent = Intent(Intent.ACTION_VIEW)
                    .setPackage(GOOGLE_MAP_PACKAGE_NAME)
                    .setData(Uri.parse("google.navigation:q=${point.latitude},${point.longitude}")),
                actionText = "Enter navigation mode in this app to get directions to the marker"
            ),
            option.copy(
                intent = Intent(Intent.ACTION_VIEW)
                    .setPackage(GOOGLE_MAP_PACKAGE_NAME)
                    .setData(Uri.parse("geo:0,0?q=${point.latitude},${point.longitude}"))
            )
        )
    }

    private fun buildAdapter(options: List<NavigationOption>): ArrayAdapter<NavigationOption> {
        return object : ArrayAdapter<NavigationOption>(
            currentContext,
            R.layout.marker_navigation_option_item,
            options
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (convertView
                    ?: LayoutInflater.from(currentContext)
                        .inflate(R.layout.marker_navigation_option_item, parent, false))
                    .apply {
                        val option = getItem(position)!!
                        bindDataToView(this, option)
                    }
            }

            private fun bindDataToView(view: View, option: NavigationOption) {
                val iconImageView = view.findViewById<ImageView>(R.id.iv_option_icon)
                val nameTextView = view.findViewById<TextView>(R.id.tv_option_name)
                val optionTextView = view.findViewById<TextView>(R.id.tv_navigation_option)

                iconImageView.setImageDrawable(option.icon)
                nameTextView.text = option.name
                optionTextView.text = option.actionText
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(
            ARG_MARKER_COORDS,
            arguments!!.getParcelable<LatLng>(ARG_MARKER_COORDS)
        )
    }

    companion object {
        fun newInstance(markerCoords: LatLng): MarkerNavigationOptionsDialogFragment {
            val args = Bundle()

            args.putParcelable(ARG_MARKER_COORDS, markerCoords)

            val fragment = MarkerNavigationOptionsDialogFragment()
            fragment.arguments = args
            return fragment
        }

        private const val ARG_MARKER_COORDS = "ARG_MARKER_COORDS"
        private const val GOOGLE_MAP_PACKAGE_NAME = "com.google.android.apps.maps"
    }

    private data class NavigationOption(
        val name: CharSequence,
        val icon: Drawable,
        val intent: Intent,
        val actionText: String = "Open the marker location in this app"
    )
}
