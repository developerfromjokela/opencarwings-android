package com.developerfromjokela.opencarwings.utils

import androidx.recyclerview.widget.RecyclerView
import com.developerfromjokela.opencarwings.databinding.RecyclerviewEmptyviewBinding

class EmptyViewViewHolder(binding: RecyclerviewEmptyviewBinding) : RecyclerView.ViewHolder(binding.root) {
    val emptyLabel = binding.emptyLabel
}
