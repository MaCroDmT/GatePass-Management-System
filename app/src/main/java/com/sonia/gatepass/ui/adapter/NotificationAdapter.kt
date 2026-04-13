package com.sonia.gatepass.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonia.gatepass.data.model.Notification
import com.sonia.gatepass.databinding.ItemNotificationBinding
import com.sonia.gatepass.util.Constants

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification, onItemClick)
    }
    
    class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(notification: Notification, onItemClick: (Notification) -> Unit) {
            binding.titleTextView.text = notification.title
            binding.messageTextView.text = notification.message
            binding.dateTextView.text = notification.createdAt
            
            // Show/hide unread indicator
            binding.unreadIndicator.visibility = if (notification.status == Constants.NOTIFICATION_UNREAD) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            // Change background for unread
            if (notification.status == Constants.NOTIFICATION_UNREAD) {
                binding.root.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            } else {
                binding.root.setCardBackgroundColor(Color.WHITE)
            }
            
            binding.root.setOnClickListener { onItemClick(notification) }
        }
    }
    
    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.notificationId == newItem.notificationId
        }
        
        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}
