package com.sonia.gatepass.ui.main.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sonia.gatepass.R
import com.sonia.gatepass.data.model.Movement
import com.sonia.gatepass.data.repository.MovementRepository
import com.sonia.gatepass.databinding.DialogAddEditUserBinding
import com.sonia.gatepass.databinding.FragmentDashboardBinding
import com.sonia.gatepass.ui.admin.UserManagementActivity
import com.sonia.gatepass.ui.gatepass.GatePassListActivity
import com.sonia.gatepass.ui.viewmodel.GatePassViewModel
import com.sonia.gatepass.ui.viewmodel.UserManagementViewModel
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.ExcelExportUtil
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import com.sonia.gatepass.util.ValidationUtil
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GatePassViewModel by viewModels()
    private val userViewModel: UserManagementViewModel by viewModels()
    private val movementRepository = MovementRepository()
    private lateinit var sharedPref: SharedPrefUtil

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPref = SharedPrefUtil(requireContext())

        viewModel.observeAllGatePasses()
        userViewModel.observeAllUsers()
        observeGatePasses()
        observeUsers()
        setupCardClickListeners()
        observeUserActions()
    }

    private fun setupCardClickListeners() {
        // Total Gate Passes - show all
        binding.totalGatePassesCard.setOnClickListener {
            openGatePassList("all")
        }

        // Pending Approval - show pending
        binding.pendingCard.setOnClickListener {
            openGatePassList("pending")
        }

        // In Progress - show user's in-progress gate passes
        binding.inProgressCard.setOnClickListener {
            openGatePassList("in_progress")
        }

        // Completed - show completed
        binding.completedCard.setOnClickListener {
            openGatePassList("completed")
        }

        // Partially Returned - show partially returned
        binding.partiallyReturnedCard.setOnClickListener {
            openGatePassList("partial")
        }

        // My Gate Passes - show current user's gate passes
        binding.myGatePassesCard.setOnClickListener {
            openGatePassList("my_gatepasses")
        }

        // Users card - open User Management
        binding.usersCard.setOnClickListener {
            val intent = Intent(requireContext(), UserManagementActivity::class.java)
            startActivity(intent)
        }

        // Add User FAB
        binding.addUserFab.setOnClickListener {
            showAddUserDialog()
        }

        // Generate Excel button
        binding.generateExcelButton.setOnClickListener {
            generateExcelExport()
        }
    }

    private fun showAddUserDialog() {
        val dialogBinding = DialogAddEditUserBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val roles = listOf("SuperAdmin", "Admin", "User")
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        dialogBinding.roleAutoCompleteTextView.setAdapter(roleAdapter)

        dialogBinding.dialogTitleTextView.text = getString(R.string.add_user)
        dialogBinding.roleAutoCompleteTextView.setText(Constants.ROLE_USER, false)
        dialogBinding.passwordTextInputLayout.isVisible = true
        dialogBinding.passwordEditText.isVisible = true
        dialogBinding.passwordHintTextView.isVisible = true

        dialog.show()

        // Add buttons
        val positiveButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "Create"
            setOnClickListener {
                val name = dialogBinding.nameEditText.text.toString().trim()
                val email = dialogBinding.emailEditText.text.toString().trim()
                val password = dialogBinding.passwordEditText.text.toString().trim()
                val role = dialogBinding.roleAutoCompleteTextView.text.toString().trim()

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

                if (password.length < 6) {
                    dialogBinding.passwordTextInputLayout.error = "Password must be at least 6 characters"
                    return@setOnClickListener
                } else {
                    dialogBinding.passwordTextInputLayout.error = null
                }

                if (role.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select a role", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                userViewModel.createUser(email, password, name, role)
                dialog.dismiss()
            }
        }

        val negativeButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "Cancel"
            setOnClickListener { dialog.dismiss() }
            setBackgroundColor(requireContext().getColor(android.R.color.transparent))
            setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val buttonLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(48, 0, 48, 24)
            gravity = android.view.Gravity.END
            addView(negativeButton, android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(0, 0, 16, 0) })
            addView(positiveButton, android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ))
        }
        (dialogBinding.root as android.view.ViewGroup).addView(buttonLayout)
    }

    private fun openGatePassList(filterType: String) {
        val intent = Intent(requireContext(), GatePassListActivity::class.java).apply {
            putExtra("filter_type", filterType)
        }
        startActivity(intent)
    }

    private fun generateExcelExport() {
        binding.generateExcelButton.isEnabled = false
        binding.generateExcelButton.text = "Generating..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Get all gate passes
                val gatePassesResult = com.sonia.gatepass.data.repository.GatePassRepository().getAllGatePasses()
                val gatePasses = if (gatePassesResult is Resource.Success) gatePassesResult.data ?: emptyList() else emptyList()

                if (gatePasses.isEmpty()) {
                    Toast.makeText(requireContext(), "No gate passes to export", Toast.LENGTH_SHORT).show()
                    binding.generateExcelButton.isEnabled = true
                    binding.generateExcelButton.text = "Generate Excel Sheet"
                    return@launch
                }

                // Fetch movements for each gate pass
                val movementsMap = mutableMapOf<String, List<Movement>>()
                for (gp in gatePasses) {
                    val result = movementRepository.getMovementsByGPID(gp.gpid)
                    if (result is Resource.Success) {
                        movementsMap[gp.gpid] = result.data ?: emptyList()
                    }
                }

                // Generate Excel
                val context = requireContext()
                val filePath = ExcelExportUtil.generateExcel(context, gatePasses, movementsMap)

                if (filePath != null) {
                    Toast.makeText(context, "Excel saved to Documents: $filePath", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.generateExcelButton.isEnabled = true
                binding.generateExcelButton.text = "Generate Excel Sheet"
            }
        }
    }

    private fun observeGatePasses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.gatePasses.collect { resource ->
                val binding = _binding ?: return@collect
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        resource.data?.let { gatePasses ->
                            updateStatistics(gatePasses)
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.users.collect { resource ->
                val binding = _binding ?: return@collect
                when (resource) {
                    is Resource.Success -> {
                        val users = resource.data ?: emptyList()
                        binding.usersCountTextView.text = users.size.toString()

                        // Show card only for Super Admin
                        binding.usersCard.visibility = if (sharedPref.isSuperAdmin()) View.VISIBLE else View.GONE
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeUserActions() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.createResult.collect { resource ->
                resource ?: return@collect
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        Toast.makeText(requireContext(), "User created successfully", Toast.LENGTH_SHORT).show()
                        userViewModel.resetCreateResult()
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                        userViewModel.resetCreateResult()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.actionResult.collect { resource ->
                resource ?: return@collect
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        Toast.makeText(requireContext(), "Action successful", Toast.LENGTH_SHORT).show()
                        userViewModel.resetActionResult()
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                        userViewModel.resetActionResult()
                    }
                }
            }
        }
    }

    private fun updateStatistics(gatePasses: List<com.sonia.gatepass.data.model.GatePass>) {
        binding.totalGatePassesTextView.text = gatePasses.size.toString()

        val pending = gatePasses.count { it.status == Constants.STATUS_PENDING }
        val inProgress = gatePasses.count { it.status == Constants.STATUS_IN_PROGRESS }
        val completed = gatePasses.count { it.status == Constants.STATUS_COMPLETED }
        val partiallyReturned = gatePasses.count { it.status == Constants.STATUS_PARTIALLY_RETURNED }

        binding.pendingTextView.text = pending.toString()
        binding.inProgressTextView.text = inProgress.toString()
        binding.completedTextView.text = completed.toString()
        binding.partiallyReturnedTextView.text = partiallyReturned.toString()

        // Update "My Gate Passes" count - gate passes created by current user
        val currentUserId = sharedPref.getUserId() ?: ""
        val myGatePasses = gatePasses.filter { it.createdBy == currentUserId }
        binding.myGatePassesCountTextView.text = myGatePasses.size.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
