package com.sonia.gatepass.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonia.gatepass.data.model.Movement
import com.sonia.gatepass.databinding.ItemMovementBinding

class MovementAdapter : ListAdapter<Movement, MovementAdapter.MovementViewHolder>(MovementDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val binding = ItemMovementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovementViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val movement = getItem(position)
        holder.bind(movement)
    }
    
    class MovementViewHolder(
        private val binding: ItemMovementBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(movement: Movement) {
            binding.typeTextView.text = movement.type
            binding.quantityTextView.text = movement.quantity.toString()
            binding.dateTextView.text = movement.date
            binding.recordedByTextView.text = movement.recordedByName
            
            if (movement.remarks.isNotBlank()) {
                binding.remarksTextView.apply {
                    text = movement.remarks
                    visibility = android.view.View.VISIBLE
                }
            }
        }
    }
    
    class MovementDiffCallback : DiffUtil.ItemCallback<Movement>() {
        override fun areItemsTheSame(oldItem: Movement, newItem: Movement): Boolean {
            return oldItem.movementId == newItem.movementId
        }
        
        override fun areContentsTheSame(oldItem: Movement, newItem: Movement): Boolean {
            return oldItem == newItem
        }
    }
}
