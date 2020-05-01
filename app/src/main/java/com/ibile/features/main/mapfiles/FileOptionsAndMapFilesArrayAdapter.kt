package com.ibile.features.main.mapfiles

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.ibile.R
import com.ibile.core.Extensions.dp
import com.ibile.features.main.mapfiles.FileOptionsAndMapFilesArrayAdapter.MapFilesOptionsItem

class FileOptionsAndMapFilesArrayAdapter(context: Context, list: List<MapFilesOptionsItem>) :
    ArrayAdapter<MapFilesOptionsItem>(context, 0, list) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position)!!.viewType
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)!!
        val view: View
        val viewHolder = if (convertView != null) {
            view = convertView
            view.tag as ViewHolder
        } else {
            view = getViewForItem(item)
            ViewHolder(view).apply { view.tag = this }
        }
        viewHolder.bind(item)
        return view
    }

    private fun getViewForItem(item: MapFilesOptionsItem): View {
        val inflater = LayoutInflater.from(context)
        return when (item) {
            is MapFilesOptionsItem.Header -> inflater.inflate(R.layout.epoxy_view_marker_folder_title, null)
            is MapFilesOptionsItem.Item -> inflater.inflate(R.layout.item_alert_dialog_option_with_icon, null)
        }
    }

    private class ViewHolder(private val view: View) {
        private val imageViewItemIcon by lazy { view.findViewById<ImageView>(R.id.iv_option_icon) }
        private val textViewItemTitle by lazy { view.findViewById<TextView>(R.id.tv_option_title) }
        private val textViewItemSubtitle by lazy { view.findViewById<TextView>(R.id.tv_option_subtitle) }

        fun bind(item: MapFilesOptionsItem) {
            when (item) {
                is MapFilesOptionsItem.Header -> {
                    (view as TextView).text = item.text
                    view.setOnClickListener(null)
                }
                is MapFilesOptionsItem.Item -> {
                    imageViewItemIcon.setImageResource(item.icon)
                    textViewItemTitle.text = item.title
                    textViewItemSubtitle.text = item.subtitle

                    if (item.subtitle == null) {
                        textViewItemSubtitle.visibility = View.GONE
                        textViewItemTitle.setPadding(0, 8f.dp.toInt(), 0, 8f.dp.toInt())
                    } else {
                        textViewItemSubtitle.visibility = View.VISIBLE
                        textViewItemTitle.setPadding(0, 0, 0, 0)
                    }
                }
            }
        }
    }

    sealed class MapFilesOptionsItem(val viewType: Int) {
        data class Header(val text: String) : MapFilesOptionsItem(0)
        data class Item(val icon: Int, val title: String, val subtitle: String? = null) :
            MapFilesOptionsItem(1)
    }
}
