package com.ibile.features.organizemarkers

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
import com.ibile.data.database.entities.FolderWithMarkers
import com.maltaisn.icondialog.pack.IconPack
import org.koin.core.KoinComponent
import org.koin.core.get

class MarkersActionTargetFolderOptionsArrayAdapter(
    context: Context,
    resource: Int,
    items: List<FolderWithMarkers>
) : ArrayAdapter<FolderWithMarkers>(context, resource, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder = if (convertView == null) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.organize_markers_action_target_folder_option, parent, false)
            with(view) {
                ViewHolder(
                    findViewById(R.id.iv_folder_icon),
                    findViewById(R.id.view_folder_color),
                    findViewById(R.id.tv_folder_name),
                    findViewById(R.id.tv_folder_count)
                ).apply {
                    this@with.tag = this
                }
            }
        } else {
            view = convertView
            convertView.tag as ViewHolder
        }
        viewHolder.bind(getItem(position)!!)
        return view
    }

    private class ViewHolder(
        val iconImageView: ImageView,
        val folderColorView: View,
        val folderNameTextView: TextView,
        val markersCountTextView: TextView
    ) : KoinComponent {
        val iconPack = get<IconPack>()

        fun bind(folderWithMarkers: FolderWithMarkers) {
            val (folder, markers) = folderWithMarkers

            val drawable = iconPack.getIconDrawable(folder.iconId)?.mutate()?.setColor(Color.WHITE)
            iconImageView.setImageDrawable(drawable)

            folderColorView.background.mutate().setColor(folder.color)

            folderNameTextView.text = folder.title

            markersCountTextView.text = markersCountTextView.context.getString(
                R.string.fmt_folder_markers_count,
                markers.size.toString()
            )
        }
    }
}
