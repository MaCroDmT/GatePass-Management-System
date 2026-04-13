package com.sonia.gatepass.ui.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sonia.gatepass.GatePassApplication
import com.sonia.gatepass.R
import com.sonia.gatepass.data.repository.NotificationRepository
import com.sonia.gatepass.databinding.ActivityLoginBinding
import com.sonia.gatepass.ui.main.MainActivity
import com.sonia.gatepass.ui.viewmodel.AuthViewModel
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import com.sonia.gatepass.util.ValidationUtil
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sharedPref: SharedPrefUtil

    // Request notification permission on Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            GatePassApplication.instance.log("LoginActivity", "Notification permission granted")
        } else {
            GatePassApplication.instance.log("LoginActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GatePassApplication.instance.log("LoginActivity", "onCreate START")

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        GatePassApplication.instance.log("LoginActivity", "view binding inflated")

        sharedPref = SharedPrefUtil(this)
        GatePassApplication.instance.log("LoginActivity", "sharedPref created, isLoggedIn=${sharedPref.isLoggedIn()}")

        // Check if already logged in
        if (sharedPref.isLoggedIn()) {
            GatePassApplication.instance.log("LoginActivity", "Already logged in, navigating to main")
            navigateToMain()
            return  // Stop executing — activity is being finished
        }

        GatePassApplication.instance.log("LoginActivity", "Setting up click listeners")
        setupClickListeners()
        GatePassApplication.instance.log("LoginActivity", "Setting up viewmodel observation")
        observeViewModel()
        GatePassApplication.instance.log("LoginActivity", "onComplete END")
    }
    
    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            attemptLogin()
        }
    }
    
    private fun attemptLogin() {
        val email = binding.emailEditText.text?.toString()?.trim() ?: ""
        val password = binding.passwordEditText.text?.toString()?.trim() ?: ""

        // Validate
        val emailError = ValidationUtil.validateEmail(email)
        val passwordError = ValidationUtil.validatePassword(password)

        if (emailError != null) {
            binding.emailTextInputLayout.error = emailError
            return
        } else {
            binding.emailTextInputLayout.error = null
        }

        if (passwordError != null) {
            binding.passwordTextInputLayout.error = passwordError
            return
        } else {
            binding.passwordTextInputLayout.error = null
        }

        // Clear any previous errors
        binding.errorTextView.visibility = View.GONE

        // Proceed with login
        showLoading(true)
        viewModel.login(email, password)
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collect { resource ->
                resource ?: return@collect // Ignore null initial state

                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        resource.data?.let { user ->
                            // Save user data to shared preferences
                            sharedPref.saveUserId(user.userId)
                            sharedPref.saveUserRole(user.role)
                            sharedPref.saveUserName(user.name)
                            sharedPref.saveIsLoggedIn(true)

                            // Subscribe to FCM topics for push notifications
                            NotificationRepository().subscribeToRoleTopics(user.role)

                            Toast.makeText(
                                this@LoginActivity,
                                "Welcome ${user.name}!",
                                Toast.LENGTH_SHORT
                            ).show()

                            navigateToMain()
                        }
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        binding.errorTextView.apply {
                            text = resource.message
                            visibility = View.VISIBLE
                        }
                        Toast.makeText(
                            this@LoginActivity,
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
        binding.errorTextView.visibility = View.GONE
    }
    
    private fun navigateToMain() {
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
