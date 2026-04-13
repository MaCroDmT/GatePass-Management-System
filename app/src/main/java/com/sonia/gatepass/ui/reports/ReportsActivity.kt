package com.sonia.gatepass.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.databinding.ActivityReportsBinding
import com.sonia.gatepass.ui.adapter.GatePassAdapter
import com.sonia.gatepass.ui.gatepass.GatePassDetailsActivity
import com.sonia.gatepass.ui.viewmodel.GatePassViewModel
import com.sonia.gatepass.util.Resource
import kotlinx.coroutines.launch

class ReportsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReportsBinding
    private val viewModel: GatePassViewModel by viewModels()
    private lateinit var adapter: GatePassAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        adapter = GatePassAdapter { gatePass ->
            val intent = Intent(this, GatePassDetailsActivity::class.java).apply {
                putExtra("GPID", gatePass.gpid)
            }
            startActivity(intent)
        }
        
        binding.gatePassRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReportsActivity)
            adapter = this@ReportsActivity.adapter
        }
    }
    
    private fun setupClickListeners() {
        binding.searchButton.setOnClickListener {
            val styleNo = binding.styleNoSearchEditText.text?.toString()?.trim() ?: ""
            if (styleNo.isNotBlank()) {
                searchByStyle(styleNo)
            } else {
                Toast.makeText(this, "Please enter a Style No", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun searchByStyle(styleNo: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.styleSummaryCard.visibility = View.GONE
        binding.gatePassesLabelTextView.visibility = View.GONE
        
        lifecycleScope.launch {
            viewModel.observeAllGatePasses()
            
            viewModel.gatePasses.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        
                        val gatePasses = resource.data?.filter { it.styleNo.equals(styleNo, ignoreCase = true) } ?: emptyList()
                        
                        if (gatePasses.isEmpty()) {
                            Toast.makeText(this@ReportsActivity, "No gate passes found for style: $styleNo", Toast.LENGTH_SHORT).show()
                            adapter.submitList(emptyList())
                        } else {
                            adapter.submitList(gatePasses)
                            
                            // Show summary
                            val totalSent = gatePasses.sumOf { it.totalSent }
                            val totalReturned = gatePasses.sumOf { it.totalReturned }
                            val totalRedispatched = gatePasses.sumOf { it.totalRedispatched }
                            val balance = totalSent - totalReturned + totalRedispatched
                            
                            binding.styleNoSummaryTextView.text = "Style: $styleNo"
                            binding.totalSentSummaryTextView.text = totalSent.toString()
                            binding.totalReturnedSummaryTextView.text = totalReturned.toString()
                            binding.balanceSummaryTextView.text = balance.toString()
                            
                            binding.styleSummaryCard.visibility = View.VISIBLE
                            binding.gatePassesLabelTextView.visibility = View.VISIBLE
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@ReportsActivity, resource.message, Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}
