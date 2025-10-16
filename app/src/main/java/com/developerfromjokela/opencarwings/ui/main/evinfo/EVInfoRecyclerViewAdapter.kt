package com.developerfromjokela.opencarwings.ui.main.evinfo

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.FragmentEvInfoBinding


class EVInfoRecyclerViewAdapter(
    var values: List<EVInfoItem>
) : RecyclerView.Adapter<EVInfoRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentEvInfoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.idView.setText(item.name ?: R.string.unknown)
        holder.contentView.text = item.value
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentEvInfoBinding) : RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}