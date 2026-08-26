package com.developerfromjokela.opencarwings.ui.main.health.tabs.dtc

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.FragmentEvInfoBinding
import com.developerfromjokela.opencarwings.utils.dtc.DTC_Code

class DTCInfoRecyclerViewAdapter(
    var values: List<DTC_Code>
) : RecyclerView.Adapter<DTCInfoRecyclerViewAdapter.ViewHolder>() {

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
        holder.idView.text = item.ecuLabel ?: holder.contentView.context.getString(R.string.dtc_ecu_id, item.ecuId.toHexString())
        holder.contentView.text = item.codeLabel
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentEvInfoBinding) : RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content

    }

}