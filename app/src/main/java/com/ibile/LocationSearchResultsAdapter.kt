package com.ibile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.ibile.databinding.ItemSearchPlacesResultBinding

class LocationSearchResultsAdapter(
    private var searchResults: MutableList<AutocompletePrediction> = arrayListOf(),
    private val itemClickListener: (clickedResultItem: AutocompletePrediction) -> Unit
) :
    RecyclerView.Adapter<LocationSearchResultsAdapter.LocationSearchResultsViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LocationSearchResultsViewHolder {
        val binding = ItemSearchPlacesResultBinding.inflate(LayoutInflater.from(parent.context))
        return LocationSearchResultsViewHolder(binding)
    }

    override fun getItemCount(): Int = searchResults.size

    override fun onBindViewHolder(holder: LocationSearchResultsViewHolder, position: Int) {
        val result = searchResults[position]
        holder.bind(result, itemClickListener)
    }

    fun updateSearchResults(newResults: List<AutocompletePrediction>) {
        searchResults.clear()
        searchResults.addAll(newResults)
        this.notifyDataSetChanged()
    }

    class LocationSearchResultsViewHolder(private val binding: ItemSearchPlacesResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            result: AutocompletePrediction,
            clickListener: (clickedResult: AutocompletePrediction) -> Unit
        ) {
            binding.result = with(result) {
                ResultViewModel(getPrimaryText(null).toString(), getSecondaryText(null).toString())
            }
            binding.root.setOnClickListener { clickListener(result) }
        }

        data class ResultViewModel(val primaryText: String, val secondaryText: String)
    }
}