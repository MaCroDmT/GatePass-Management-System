package com.sonia.gatepass.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonia.gatepass.data.model.User
import com.sonia.gatepass.databinding.ItemUserBinding
import com.sonia.gatepass.util.Constants

class UserManagementAdapter(
    private val currentUserId: String,
    private val onEdit: (User) -> Unit,
    private val onToggleActive: (User) -> Unit,
    private val onDelete: (User) -> Unit
) : ListAdapter<User, UserManagementAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, currentUserId, onEdit, onToggleActive, onDelete)
    }

    class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            user: User,
            currentUserId: String,
            onEdit: (User) -> Unit,
            onToggleActive: (User) -> Unit,
            onDelete: (User) -> Unit
        ) {
            binding.nameTextView.text = user.name
            binding.emailTextView.text = user.email
            binding.roleTextView.text = getRoleDisplayName(user.role)
            binding.statusTextView.text = if (user.isActive) "Active" else "Inactive"
            binding.statusTextView.setBackgroundColor(
                if (user.isActive) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            )

            // Disable edit/delete for own account
            val isSelf = user.userId == currentUserId
            binding.editButton.isEnabled = !isSelf
            binding.deleteButton.isEnabled = !isSelf
            binding.editButton.alpha = if (isSelf) 0.4f else 1.0f
            binding.deleteButton.alpha = if (isSelf) 0.4f else 1.0f

            binding.editButton.setOnClickListener { onEdit(user) }
            binding.toggleActiveButton.setOnClickListener { onToggleActive(user) }
            binding.deleteButton.setOnClickListener { onDelete(user) }

            binding.toggleActiveButton.text = if (user.isActive) "Deactivate" else "Activate"
        }

        private fun getRoleDisplayName(role: String): String {
            return when (role) {
                Constants.ROLE_SUPER_ADMIN -> "Super Admin"
                Constants.ROLE_ADMIN -> "Admin"
                Constants.ROLE_USER -> "Production User"
                else -> role
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
