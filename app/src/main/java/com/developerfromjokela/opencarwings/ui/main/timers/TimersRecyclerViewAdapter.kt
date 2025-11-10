package com.developerfromjokela.opencarwings.ui.main.timers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.FragmentTimerItemBinding
import com.developerfromjokela.opencarwings.databinding.RecyclerviewEmptyviewBinding
import com.developerfromjokela.opencarwings.utils.CustomDateUtils
import com.developerfromjokela.opencarwings.utils.EmptyViewViewHolder
import org.openapitools.client.models.CommandTimerSetting
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class TimersRecyclerViewAdapter(
    var values: List<CommandTimerSetting>,
    val clickCallback: (itm: CommandTimerSetting) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_EMPTY = 0
        const val VIEW_TIMER = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_EMPTY) {
            return EmptyViewViewHolder(
                RecyclerviewEmptyviewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
        return ViewHolder(
            FragmentTimerItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return if (values.isEmpty()) {
            1
        } else {
            values.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (values.isEmpty()) {
            return VIEW_EMPTY
        } else {
            return VIEW_TIMER
        }
    }

    override fun onBindViewHolder(genericHolder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_EMPTY) {
            val emptyRecyclerView = genericHolder as EmptyViewViewHolder
            emptyRecyclerView.emptyLabel.setText(R.string.no_timers)
            return
        }
        val holder = genericHolder as ViewHolder
        val item = values[position]
        holder.timerEnabled.isChecked = item.enabled == true
        holder.timerName.text = item.name
        val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        val lastExecDate = item.lastCommandExecution?.atZoneSameInstant(ZoneId.systemDefault())
            ?.toLocalDateTime()?.format(dateTimeFormatter)
            ?: "---"
        val configText = when (item.timerType) {
            0 -> {
                "${item.commandTypeDisplay ?: "--"} | ${
                    dateFormatter.format(item.date) ?: "--"
                } | ${CustomDateUtils.parseAndFormatToLocalTimerTime(item.time) ?: "--"}"
            }
            1 -> {
                "${item.commandTypeDisplay ?: "--"} | ${
                    CustomDateUtils.formatCommandTimerDays(item)
                }${CustomDateUtils.parseAndFormatToLocalTimerTime(item.time) ?: "--"}"
            }
            else -> "--"
        }
        holder.timerConfig.text = configText
        holder.timerCard.setOnClickListener {
            clickCallback(item)
        }
        holder.timerLog.text = holder.timerLog.context.getString(R.string.timer_log, lastExecDate, item.lastCommandResultDisplay ?: "--")
    }

    inner class ViewHolder(binding: FragmentTimerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val timerEnabled = binding.timerEnabled
        val timerName = binding.timerName
        val timerConfig = binding.timerConfig
        val timerLog = binding.timerLog

        val timerCard = binding.timerCard
    }

}