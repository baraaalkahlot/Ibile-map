package com.ibile.features.markeractiontargetfolderselection

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.ibile.R
import com.ibile.core.setColor
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import com.maltaisn.icondialog.pack.IconPack
import org.koin.core.KoinComponent
import org.koin.core.inject

class MarkersActionTargetFolderOptionsArrayAdapter(
    context: Context,
    items: List<FolderWithMarkersCount>
) :
    ArrayAdapter<FolderWithMarkersCount>(
        context,
        R.layout.organize_markers_action_target_folder_option,
        items
    ), KoinComponent {

    private val iconPack: IconPack by inject()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder = if (convertView == null) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.organize_markers_action_target_folder_option, parent, false)
            ViewHolder(view, iconPack).apply { view.tag = this }
        } else {
            view = convertView
            convertView.tag as ViewHolder
        }
        viewHolder.bind(getItem(position)!!)
        return view
    }

    private class ViewHolder(private val view: View, private val iconPack: IconPack) {

        val iconImageView: ImageView by lazy { view.findViewById<ImageView>(R.id.iv_folder_icon) }
        val folderColorView: View by lazy { view.findViewById<View>(R.id.view_folder_color) }
        val folderNameTextView: TextView by lazy { view.findViewById<TextView>(R.id.tv_folder_name) }
        val markersCountTextView: TextView by lazy { view.findViewById<TextView>(R.id.tv_folder_count) }

        fun bind(folder: FolderWithMarkersCount) {

            val drawable = iconPack.getIconDrawable(folder.iconId)?.mutate()?.setColor(Color.WHITE)
            iconImageView.setImageDrawable(drawable)

            folderColorView.background.mutate().setColor(folder.color)

            folderNameTextView.text = folder.title

            markersCountTextView.text = markersCountTextView.context.getString(
                R.string.fmt_folder_markers_count,
                folder.totalMarkers.toString()
            )
        }
    }
}
