package com.sonia.gatepass.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.databinding.ItemGatePassBinding
import com.sonia.gatepass.util.Constants

class GatePassAdapter(
    private val onItemClick: (GatePass) -> Unit
) : ListAdapter<GatePass, GatePassAdapter.GatePassViewHolder>(GatePassDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GatePassViewHolder {
        val binding = ItemGatePassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GatePassViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: GatePassViewHolder, position: Int) {
        val gatePass = getItem(position)
        holder.bind(gatePass)
        holder.itemView.setOnClickListener { onItemClick(gatePass) }
    }
    
    class GatePassViewHolder(
        private val binding: ItemGatePassBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(gatePass: GatePass) {
            binding.gpidTextView.text = gatePass.gpid
            binding.styleNoTextView.text = "Style: ${gatePass.styleNo}"
            binding.goodsNameTextView.text = "Goods: ${gatePass.goodsName}"
            binding.quantityTextView.text = "Qty: ${gatePass.totalSent}"
            binding.dateTextView.text = gatePass.createdAt
            binding.sentTextView.text = gatePass.totalSent.toString()
            binding.returnedTextView.text = gatePass.totalReturned.toString()
            binding.balanceTextView.text = gatePass.calculateBalance().toString()
            
            // Status
            binding.statusTextView.text = gatePass.status
            val statusColor = getStatusColor(gatePass.status)
            binding.statusTextView.setBackgroundColor(statusColor)
        }
        
        private fun getStatusColor(status: String): Int {
            return when (status) {
                Constants.STATUS_PENDING -> Color.parseColor("#FF9800")
                Constants.STATUS_APPROVED -> Color.parseColor("#4CAF50")
                Constants.STATUS_IN_PROGRESS -> Color.parseColor("#2196F3")
                Constants.STATUS_PARTIALLY_RETURNED -> Color.parseColor("#9C27B0")
                Constants.STATUS_REDISPATCHED -> Color.parseColor("#00BCD4")
                Constants.STATUS_COMPLETED -> Color.parseColor("#8BC34A")
                Constants.STATUS_REOPENED -> Color.parseColor("#FF5722")
                Constants.STATUS_REJECTED -> Color.parseColor("#F44336")
                else -> Color.parseColor("#9E9E9E")
            }
        }
    }
    
    class GatePassDiffCallback : DiffUtil.ItemCallback<GatePass>() {
        override fun areItemsTheSame(oldItem: GatePass, newItem: GatePass): Boolean {
            return oldItem.gpid == newItem.gpid
        }
        
        override fun areContentsTheSame(oldItem: GatePass, newItem: GatePass): Boolean {
            return oldItem == newItem
        }
    }
}
