package com.sonia.gatepass.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonia.gatepass.databinding.ActivityNotificationsBinding
import com.sonia.gatepass.ui.adapter.NotificationAdapter
import com.sonia.gatepass.ui.gatepass.GatePassDetailsActivity
import com.sonia.gatepass.ui.viewmodel.NotificationViewModel
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val viewModel: NotificationViewModel by viewModels()
    private lateinit var sharedPref: SharedPrefUtil
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefUtil(this)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        // Handle intent extra to open gate pass details directly from notification
        val gpid = intent.getStringExtra("gpid")
        if (gpid != null) {
            openGatePassDetails(gpid)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notification ->
            // Navigate to gate pass details if gpid is available
            if (notification.gpid.isNotBlank()) {
                openGatePassDetails(notification.gpid)
            }
            // Mark as read silently (don't show error for old notifications)
            viewModel.markAsRead(notification.notificationId)
        }

        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = this@NotificationsActivity.adapter
        }
    }

    private fun openGatePassDetails(gpid: String) {
        val intent = Intent(this, GatePassDetailsActivity::class.java).apply {
            putExtra("GPID", gpid)
        }
        startActivity(intent)
    }

    private fun observeViewModel() {
        val userId = sharedPref.getUserId()
        if (userId != null) {
            viewModel.observeNotifications(userId)
        }

        lifecycleScope.launch {
            viewModel.notifications.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyTextView.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        resource.data?.let { notifications ->
                            if (notifications.isEmpty()) {
                                binding.emptyTextView.visibility = View.VISIBLE
                                adapter.submitList(emptyList())
                            } else {
                                binding.emptyTextView.visibility = View.GONE
                                adapter.submitList(notifications)
                            }
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@NotificationsActivity, resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
