package com.sonia.gatepass.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.databinding.ItemApprovalBinding

class ApprovalAdapter(
    private val onItemClick: (GatePass) -> Unit,
    private val onApprove: (GatePass) -> Unit,
    private val onReject: (GatePass) -> Unit
) : ListAdapter<GatePass, ApprovalAdapter.ApprovalViewHolder>(ApprovalDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalViewHolder {
        val binding = ItemApprovalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ApprovalViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ApprovalViewHolder, position: Int) {
        val gatePass = getItem(position)
        holder.bind(gatePass, onItemClick, onApprove, onReject)
    }
    
    class ApprovalViewHolder(
        private val binding: ItemApprovalBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(
            gatePass: GatePass,
            onItemClick: (GatePass) -> Unit,
            onApprove: (GatePass) -> Unit,
            onReject: (GatePass) -> Unit
        ) {
            binding.gpidTextView.text = gatePass.gpid
            binding.styleNoTextView.text = "Style: ${gatePass.styleNo}"
            binding.goodsNameTextView.text = gatePass.goodsName
            binding.quantityTextView.text = "Qty: ${gatePass.totalSent}"
            binding.purposeTextView.text = gatePass.purpose
            binding.statusTextView.text = gatePass.status
            binding.statusTextView.setBackgroundColor(getStatusColor(gatePass.status))
            
            binding.root.setOnClickListener { onItemClick(gatePass) }
            binding.approveButton.setOnClickListener { onApprove(gatePass) }
            binding.rejectButton.setOnClickListener { onReject(gatePass) }
        }
        
        private fun getStatusColor(status: String): Int {
            return when (status) {
                com.sonia.gatepass.util.Constants.STATUS_PENDING -> Color.parseColor("#FF9800")
                com.sonia.gatepass.util.Constants.STATUS_APPROVED -> Color.parseColor("#4CAF50")
                com.sonia.gatepass.util.Constants.STATUS_IN_PROGRESS -> Color.parseColor("#2196F3")
                com.sonia.gatepass.util.Constants.STATUS_PARTIALLY_RETURNED -> Color.parseColor("#9C27B0")
                com.sonia.gatepass.util.Constants.STATUS_REDISPATCHED -> Color.parseColor("#00BCD4")
                com.sonia.gatepass.util.Constants.STATUS_COMPLETED -> Color.parseColor("#8BC34A")
                com.sonia.gatepass.util.Constants.STATUS_REOPENED -> Color.parseColor("#FF5722")
                com.sonia.gatepass.util.Constants.STATUS_REJECTED -> Color.parseColor("#F44336")
                else -> Color.parseColor("#9E9E9E")
            }
        }
    }
    
    class ApprovalDiffCallback : DiffUtil.ItemCallback<GatePass>() {
        override fun areItemsTheSame(oldItem: GatePass, newItem: GatePass): Boolean {
            return oldItem.gpid == newItem.gpid
        }
        
        override fun areContentsTheSame(oldItem: GatePass, newItem: GatePass): Boolean {
            return oldItem == newItem
        }
    }
}
