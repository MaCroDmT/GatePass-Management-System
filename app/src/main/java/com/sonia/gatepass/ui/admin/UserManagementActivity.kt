package com.sonia.gatepass.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonia.gatepass.R
import com.sonia.gatepass.data.model.User
import com.sonia.gatepass.databinding.ActivityUserManagementBinding
import com.sonia.gatepass.databinding.DialogAddEditUserBinding
import com.sonia.gatepass.ui.adapter.UserManagementAdapter
import com.sonia.gatepass.ui.viewmodel.UserManagementViewModel
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import com.sonia.gatepass.util.ValidationUtil
import kotlinx.coroutines.launch

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private val viewModel: UserManagementViewModel by viewModels()
    private lateinit var adapter: UserManagementAdapter
    private lateinit var sharedPref: SharedPrefUtil
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefUtil(this)
        currentUserId = sharedPref.getUserId() ?: ""

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeViewModel()
        viewModel.observeAllUsers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = UserManagementAdapter(
            currentUserId = currentUserId,
            onEdit = { user -> showAddEditDialog(user) },
            onToggleActive = { user -> confirmToggleActive(user) },
            onDelete = { user -> confirmDelete(user) }
        )

        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = this@UserManagementActivity.adapter
        }
    }

    private fun setupFab() {
        binding.addUserFab.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun observeViewModel() {
        // Observe users list
        lifecycleScope.launch {
            viewModel.users.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyTextView.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val users = resource.data ?: emptyList()
                        if (users.isEmpty()) {
                            binding.emptyTextView.visibility = View.VISIBLE
                            adapter.submitList(emptyList())
                        } else {
                            binding.emptyTextView.visibility = View.GONE
                            adapter.submitList(users)
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyTextView.text = resource.message
                        binding.emptyTextView.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Observe action results
        lifecycleScope.launch {
            viewModel.actionResult.collect { resource ->
                resource ?: return@collect
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        Toast.makeText(this@UserManagementActivity, "Action successful", Toast.LENGTH_SHORT).show()
                        viewModel.resetActionResult()
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@UserManagementActivity, resource.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetActionResult()
                    }
                }
            }
        }

        // Observe create results
        lifecycleScope.launch {
            viewModel.createResult.collect { resource ->
                resource ?: return@collect
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        Toast.makeText(this@UserManagementActivity, "User created successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetCreateResult()
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@UserManagementActivity, resource.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetCreateResult()
                    }
                }
            }
        }
    }

    private fun showAddEditDialog(existingUser: User?) {
        val dialogBinding = DialogAddEditUserBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val roles = listOf("SuperAdmin", "Admin", "User")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        dialogBinding.roleAutoCompleteTextView.setAdapter(roleAdapter)

        if (existingUser != null) {
            // Edit mode
            dialogBinding.dialogTitleTextView.text = "Edit User"
            dialogBinding.nameEditText.setText(existingUser.name)
            dialogBinding.emailEditText.setText(existingUser.email)
            dialogBinding.roleAutoCompleteTextView.setText(existingUser.role, false)
            dialogBinding.passwordTextInputLayout.isVisible = false
            dialogBinding.passwordEditText.isVisible = false
            dialogBinding.passwordHintTextView.isVisible = false
        } else {
            // Create mode
            dialogBinding.dialogTitleTextView.text = getString(R.string.add_user)
            dialogBinding.roleAutoCompleteTextView.setText(Constants.ROLE_USER, false)
        }

        dialogBinding.dialogTitleTextView.setOnClickListener { /* no-op */ }

        // Positive button
        dialog.show()

        dialog.setOnDismissListener {
            // No-op, handled by button clicks
        }

        // We need to add buttons programmatically since dialog layout doesn't have them
        val positiveButton = com.google.android.material.button.MaterialButton(this).apply {
            text = if (existingUser != null) "Update" else "Create"
            setOnClickListener {
                val name = dialogBinding.nameEditText.text.toString().trim()
                val email = dialogBinding.emailEditText.text.toString().trim()
                val password = dialogBinding.passwordEditText.text.toString().trim()
                val role = dialogBinding.roleAutoCompleteTextView.text.toString().trim()

                // Validate
                if (name.isEmpty()) {
                    dialogBinding.nameTextInputLayout.error = "Name is required"
                    return@setOnClickListener
                } else {
                    dialogBinding.nameTextInputLayout.error = null
                }

                val emailError = ValidationUtil.validateEmail(email)
                if (emailError != null) {
                    dialogBinding.emailTextInputLayout.error = emailError
                    return@setOnClickListener
                } else {
                    dialogBinding.emailTextInputLayout.error = null
                }

                if (existingUser == null) {
                    // Create mode — validate password
                    if (password.length < 6) {
                        dialogBinding.passwordTextInputLayout.error = "Password must be at least 6 characters"
                        return@setOnClickListener
                    } else {
                        dialogBinding.passwordTextInputLayout.error = null
                    }

                    if (role.isEmpty()) {
                        Toast.makeText(this@UserManagementActivity, "Please select a role", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    viewModel.createUser(email, password, name, role)
                } else {
                    // Edit mode
                    if (role.isEmpty()) {
                        Toast.makeText(this@UserManagementActivity, "Please select a role", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    viewModel.updateUser(existingUser.userId, name, email, role)
                }

                dialog.dismiss()
            }
        }

        val negativeButton = com.google.android.material.button.MaterialButton(this).apply {
            text = "Cancel"
            setOnClickListener { dialog.dismiss() }
            setBackgroundColor(getColor(android.R.color.transparent))
            setTextColor(getColor(R.color.text_secondary))
        }

        // Add buttons to dialog
        val buttonLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(48, 0, 48, 24)
            gravity = android.view.Gravity.END
            addView(negativeButton, android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply {
                setMargins(0, 0, 16, 0)
            })
            addView(positiveButton, android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ))
        }
        (dialogBinding.root as ViewGroup).addView(buttonLayout)
    }

    private fun confirmToggleActive(user: User) {
        val action = if (user.isActive) "deactivate" else "activate"
        AlertDialog.Builder(this)
            .setTitle("Confirm $action")
            .setMessage("Are you sure you want to $action ${user.name}?")
            .setPositiveButton("Confirm") { _, _ ->
                viewModel.toggleUserActive(user.userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteUser(user.userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
