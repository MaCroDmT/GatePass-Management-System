package com.sonia.gatepass.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sonia.gatepass.R
import com.sonia.gatepass.databinding.ActivityMainBinding
import com.sonia.gatepass.ui.admin.ApprovalActivity
import com.sonia.gatepass.ui.auth.LoginActivity
import com.sonia.gatepass.ui.gatepass.CreateGatePassActivity
import com.sonia.gatepass.ui.main.fragments.DashboardFragment
import com.sonia.gatepass.ui.notifications.NotificationsActivity
import com.sonia.gatepass.ui.reports.ReportsActivity
import com.sonia.gatepass.ui.viewmodel.AuthViewModel
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.SharedPrefUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sharedPref: SharedPrefUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefUtil(this)

        setupHeader()
        setupBottomNavigation()
        observeUser()

        // Load current user data from Firebase
        viewModel.loadCurrentUser()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    private fun setupHeader() {
        // Set date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.dateTextView.text = dateFormat.format(Date())

        // Set logout button
        binding.logoutIconButton.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.navigation_create -> {
                    val intent = Intent(this, CreateGatePassActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_approval -> {
                    val intent = Intent(this, ApprovalActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_notifications -> {
                    val intent = Intent(this, NotificationsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_reports -> {
                    val intent = Intent(this, ReportsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Show/hide approval tab based on role
        try {
            val menu = binding.bottomNavigation.menu
            val approvalItem = menu.findItem(R.id.navigation_approval)
            approvalItem.isVisible = sharedPref.isAdmin()
        } catch (e: Exception) {
            // Ignore if menu not ready yet
        }
    }

    private fun observeUser() {
        lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                if (user != null) {
                    // Update header with user info
                    binding.userNameTextView.text = user.name
                    binding.userRoleChip.text = getRoleDisplayName(user.role)
                }
            }
        }
    }

    private fun getRoleDisplayName(role: String): String {
        return when (role) {
            Constants.ROLE_SUPER_ADMIN -> "Super Admin"
            Constants.ROLE_ADMIN -> "Admin"
            Constants.ROLE_USER -> "Production User"
            else -> role
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
                sharedPref.clear()
                navigateToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
