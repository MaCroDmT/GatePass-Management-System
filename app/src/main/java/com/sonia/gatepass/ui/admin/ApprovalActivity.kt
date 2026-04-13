package com.sonia.gatepass.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.databinding.ActivityApprovalBinding
import com.sonia.gatepass.ui.adapter.ApprovalAdapter
import com.sonia.gatepass.ui.gatepass.GatePassDetailsActivity
import com.sonia.gatepass.ui.viewmodel.GatePassViewModel
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import kotlinx.coroutines.launch

class ApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApprovalBinding
    private val viewModel: GatePassViewModel by viewModels()
    private lateinit var adapter: ApprovalAdapter
    private lateinit var sharedPref: SharedPrefUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPrefUtil(this)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val userId = sharedPref.getUserId() ?: ""
        val userName = sharedPref.getUserName() ?: "Admin"

        adapter = ApprovalAdapter(
            onItemClick = { gatePass ->
                val intent = Intent(this, GatePassDetailsActivity::class.java).apply {
                    putExtra("GPID", gatePass.gpid)
                }
                startActivity(intent)
            },
            onApprove = { gatePass ->
                viewModel.approveGatePass(
                    gatePass.gpid,
                    userId,
                    userName
                )
            },
            onReject = { gatePass ->
                viewModel.rejectGatePass(
                    gatePass.gpid,
                    userId,
                    userName
                )
            }
        )
        
        binding.gatePassRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ApprovalActivity)
            adapter = this@ApprovalActivity.adapter
        }
    }
    
    private fun observeViewModel() {
        viewModel.observeGatePassesByStatus(Constants.STATUS_PENDING)
        
        lifecycleScope.launch {
            viewModel.gatePasses.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyTextView.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        resource.data?.let { gatePasses ->
                            if (gatePasses.isEmpty()) {
                                binding.emptyTextView.visibility = View.VISIBLE
                                adapter.submitList(emptyList())
                            } else {
                                binding.emptyTextView.visibility = View.GONE
                                adapter.submitList(gatePasses)
                            }
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@ApprovalActivity, resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.actionResult.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        Toast.makeText(this@ApprovalActivity, "Action successful", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@ApprovalActivity, resource.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}
