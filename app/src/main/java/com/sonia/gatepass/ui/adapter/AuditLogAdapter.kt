package com.sonia.gatepass.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonia.gatepass.data.model.AuditLogEntry
import com.sonia.gatepass.databinding.ItemAuditLogBinding

class AuditLogAdapter : ListAdapter<AuditLogEntry, AuditLogAdapter.AuditLogViewHolder>(AuditLogDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuditLogViewHolder {
        val binding = ItemAuditLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AuditLogViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: AuditLogViewHolder, position: Int) {
        val auditLog = getItem(position)
        holder.bind(auditLog)
    }
    
    class AuditLogViewHolder(
        private val binding: ItemAuditLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(auditLog: AuditLogEntry) {
            binding.actionTextView.text = auditLog.action
            binding.performedByTextView.text = "By: ${auditLog.performedByName}"
            binding.timestampTextView.text = auditLog.timestamp
        }
    }
    
    class AuditLogDiffCallback : DiffUtil.ItemCallback<AuditLogEntry>() {
        override fun areItemsTheSame(oldItem: AuditLogEntry, newItem: AuditLogEntry): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.action == newItem.action
        }
        
        override fun areContentsTheSame(oldItem: AuditLogEntry, newItem: AuditLogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
