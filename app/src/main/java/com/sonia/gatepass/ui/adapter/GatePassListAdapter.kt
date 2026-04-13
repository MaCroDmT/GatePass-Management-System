package com.sonia.gatepass.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.databinding.ItemGatePassListBinding
import com.sonia.gatepass.util.Constants

/**
 * Adapter for displaying gate passes in a list with optional approve/reject buttons.
 * Shows approve/reject buttons only for PENDING status.
 */
class GatePassListAdapter(
    private val onItemClick: (GatePass) -> Unit,
    private val onApprove: (GatePass) -> Unit = {},
    private val onReject: (GatePass) -> Unit = {}
) : ListAdapter<GatePass, GatePassListAdapter.GatePassListViewHolder>(GatePassListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GatePassListViewHolder {
        val binding = ItemGatePassListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GatePassListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GatePassListViewHolder, position: Int) {
        val gatePass = getItem(position)
        holder.bind(gatePass, onItemClick, onApprove, onReject)
    }

    class GatePassListViewHolder(
        private val binding: ItemGatePassListBinding
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
            binding.dateTextView.text = gatePass.createdAt
            binding.sentTextView.text = gatePass.totalSent.toString()
            binding.returnedTextView.text = gatePass.totalReturned.toString()
            binding.balanceTextView.text = gatePass.calculateBalance().toString()
            binding.statusTextView.text = gatePass.status
            binding.statusTextView.setBackgroundColor(getStatusColor(gatePass.status))

            // Show/hide approve/reject buttons based on status
            val showActionButtons = gatePass.status == Constants.STATUS_PENDING
            binding.approveButton.visibility = if (showActionButtons) android.view.View.VISIBLE else android.view.View.GONE
            binding.rejectButton.visibility = if (showActionButtons) android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener { onItemClick(gatePass) }
            binding.approveButton.setOnClickListener { onApprove(gatePass) }
            binding.rejectButton.setOnClickListener { onReject(gatePass) }
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

    class GatePassListDiffCallback : DiffUtil.ItemCallback<GatePass>() {
        override fun areItemsTheSame(oldItem: GatePass, newItem: GatePass): Boolean {
            return oldItem.gpid == newItem.gpid
        }

        override fun areContentsTheSame(oldItem: GatePass, newItem: GatePass): Boolean {
            return oldItem == newItem
        }
    }
}
