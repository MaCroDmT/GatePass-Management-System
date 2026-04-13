package com.sonia.gatepass.ui.gatepass

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonia.gatepass.GatePassApplication
import com.sonia.gatepass.R
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.databinding.ActivityGatePassListBinding
import com.sonia.gatepass.ui.adapter.GatePassListAdapter
import com.sonia.gatepass.ui.viewmodel.GatePassViewModel
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import kotlinx.coroutines.launch

/**
 * Activity to display a filtered list of gate passes.
 * 
 * Intent extras:
 * - "filter_type": Type of filter to apply
 *     - "all"         → Show all gate passes (Total Gate Passes)
 *     - "pending"     → Show pending gate passes (Pending Approval)
 *     - "in_progress" → Show gate passes created by current user that are in progress
 *     - "completed"   → Show completed gate passes
 *     - "partial"     → Show partially returned gate passes
 */
class GatePassListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGatePassListBinding
    private val viewModel: GatePassViewModel by viewModels()
    private lateinit var adapter: GatePassListAdapter
    private lateinit var sharedPref: SharedPrefUtil

    private var filterType: String = "all"
    private var allGatePasses: List<GatePass> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGatePassListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefUtil(this)
        filterType = intent.getStringExtra("filter_type") ?: "all"

        setupToolbar()
        setupRecyclerView()
        loadData()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Set toolbar title based on filter type
        binding.toolbar.title = when (filterType) {
            "all" -> "All Gate Passes"
            "pending" -> "Pending Approval"
            "in_progress" -> "In Progress"
            "completed" -> "Completed Gate Passes"
            "partial" -> "Partially Returned"
            "my_gatepasses" -> "My Gate Passes"
            else -> "Gate Passes"
        }
    }

    private fun setupRecyclerView() {
        adapter = GatePassListAdapter(
            onItemClick = { gatePass ->
                // Navigate to gate pass details
                val intent = Intent(this, GatePassDetailsActivity::class.java).apply {
                    putExtra("GPID", gatePass.gpid)
                }
                startActivity(intent)
            },
            onApprove = { gatePass ->
                viewModel.approveGatePass(
                    gatePass.gpid,
                    sharedPref.getUserId() ?: "",
                    sharedPref.getUserName() ?: "Admin"
                )
            },
            onReject = { gatePass ->
                viewModel.rejectGatePass(
                    gatePass.gpid,
                    sharedPref.getUserId() ?: "",
                    sharedPref.getUserName() ?: "Admin"
                )
            }
        )

        binding.gatePassRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GatePassListActivity)
            adapter = this@GatePassListActivity.adapter
        }
    }

    private fun loadData() {
        // Load all gate passes, then filter client-side
        viewModel.observeAllGatePasses()
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.gatePasses.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyTextView.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        allGatePasses = resource.data ?: emptyList()
                        applyFilterAndDisplay()
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyTextView.text = resource.message ?: "Failed to load data"
                        binding.emptyTextView.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Observe action results (for approve/reject)
        lifecycleScope.launch {
            viewModel.actionResult.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        Toast.makeText(this@GatePassListActivity, "Action successful", Toast.LENGTH_SHORT).show()
                        // Refresh data
                        loadData()
                        viewModel.resetActionResult()
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@GatePassListActivity, resource.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetActionResult()
                    }
                    null -> {}
                }
            }
        }
    }

    private fun applyFilterAndDisplay() {
        val filtered = when (filterType) {
            "all" -> allGatePasses
            "pending" -> allGatePasses.filter { it.status == Constants.STATUS_PENDING }
            "in_progress" -> {
                // Show gate passes created by current user that are in progress
                val userId = sharedPref.getUserId() ?: ""
                allGatePasses.filter { gp ->
                    gp.status in listOf(
                        Constants.STATUS_APPROVED,
                        Constants.STATUS_IN_PROGRESS,
                        Constants.STATUS_REOPENED
                    ) && gp.createdBy == userId
                }
            }
            "completed" -> allGatePasses.filter { it.status == Constants.STATUS_COMPLETED }
            "partial" -> allGatePasses.filter { it.status == Constants.STATUS_PARTIALLY_RETURNED }
            "my_gatepasses" -> {
                // Show ALL gate passes created by current user
                val userId = sharedPref.getUserId() ?: ""
                allGatePasses.filter { it.createdBy == userId }
            }
            else -> allGatePasses
        }

        if (filtered.isEmpty()) {
            binding.emptyTextView.text = getEmptyMessage()
            binding.emptyTextView.visibility = View.VISIBLE
            adapter.submitList(emptyList())
        } else {
            binding.emptyTextView.visibility = View.GONE
            adapter.submitList(filtered)
        }
    }

    private fun getEmptyMessage(): String {
        return when (filterType) {
            "all" -> "No gate passes found"
            "pending" -> "No pending gate passes"
            "in_progress" -> "No gate passes in progress"
            "completed" -> "No completed gate passes"
            "partial" -> "No partially returned gate passes"
            "my_gatepasses" -> "You haven't created any gate passes yet"
            else -> "No data found"
        }
    }
}
