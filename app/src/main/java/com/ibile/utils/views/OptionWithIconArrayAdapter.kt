package com.ibile.utils.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.ibile.R
import com.ibile.utils.views.OptionWithIconArrayAdapter.ItemOptionWithIcon
import kotlin.collections.List


/**
 * Generic ArrayAdapter used across the app for displaying choice options in an alert dialog.
 *
 * @constructor
 * TODO
 *
 * @param context
 * @param items
 */
class OptionWithIconArrayAdapter(context: Context, items: List<ItemOptionWithIcon>) :
    ArrayAdapter<ItemOptionWithIcon>(context, R.layout.item_alert_dialog_option_with_icon, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder = if (convertView == null) {
            LayoutInflater.from(context)
                .inflate(R.layout.item_alert_dialog_option_with_icon, parent, false)
                .run {
                    view = this
                    ViewHolder(this).apply { view.tag = this }
                }
        } else {
            view = convertView
            convertView.tag as ViewHolder
        }
        viewHolder.bind(getItem(position)!!)
        return view
    }

    private class ViewHolder(private val view: View) {
        private val titleView by lazy { view.findViewById<TextView>(R.id.tv_option_title) }
        private val subtitleView by lazy { view.findViewById<TextView>(R.id.tv_option_subtitle) }
        private val iconView by lazy { view.findViewById<ImageView>(R.id.iv_option_icon) }

        fun bind(item: ItemOptionWithIcon) {
            with(item) {
                titleView.text = title
                if (subtitle != null) {
                    subtitleView.visibility = View.VISIBLE
                    subtitleView.text = subtitle
                } else {
                    subtitleView.visibility = View.GONE
                }
                iconSrc?.let { iconView.setImageResource(it) }
            }
        }
    }

    data class ItemOptionWithIcon(
        val title: String,
        val subtitle: String? = null,
        @DrawableRes val iconSrc: Int? = null
    )
}
