package com.developerfromjokela.opencarwings.ui.main.notifications

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.FragmentNotificationItemBinding
import com.developerfromjokela.opencarwings.databinding.FragmentTcuSettingBinding
import org.openapitools.client.models.AlertHistory
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class NotificationsRecyclerViewAdapter(
    private val values: MutableList<AlertHistory>
) : RecyclerView.Adapter<NotificationsRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentNotificationItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.idView.text = item.typeDisplay
        holder.contentView.text = item.additionalData
        holder.contentView.visibility = if (item.additionalData == null) View.GONE else View.VISIBLE
        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        val alertTimestamp = item.timestamp?.atZoneSameInstant(ZoneId.systemDefault())
            ?.toLocalDateTime()?.format(dateFormatter)
            ?: "---"
        holder.timestampView.text = alertTimestamp
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentNotificationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content
        val timestampView: TextView = binding.timestamp

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}