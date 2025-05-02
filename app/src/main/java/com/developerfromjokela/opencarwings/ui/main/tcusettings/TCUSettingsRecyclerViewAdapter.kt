package com.developerfromjokela.opencarwings.ui.main.tcusettings

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.FragmentEvInfoBinding
import com.developerfromjokela.opencarwings.databinding.FragmentTcuSettingBinding


class TCUSettingsRecyclerViewAdapter(
    var values: List<TCUSettingItem>
) : RecyclerView.Adapter<TCUSettingsRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentTcuSettingBinding.inflate(
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

    inner class ViewHolder(binding: FragmentTcuSettingBinding) : RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}