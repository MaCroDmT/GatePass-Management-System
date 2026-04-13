package com.sonia.gatepass.ui.gatepass

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sonia.gatepass.R
import com.sonia.gatepass.data.model.Movement
import com.sonia.gatepass.databinding.ActivityMovementEntryBinding
import com.sonia.gatepass.ui.viewmodel.MovementViewModel
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.DateUtil
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import com.sonia.gatepass.util.ValidationUtil
import kotlinx.coroutines.launch

class MovementEntryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMovementEntryBinding
    private val viewModel: MovementViewModel by viewModels()
    private lateinit var sharedPref: SharedPrefUtil
    
    private val movementTypes = arrayOf(
        Constants.MOVEMENT_OUTWARD,
        Constants.MOVEMENT_INWARD,
        Constants.MOVEMENT_RE_DISPATCH
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovementEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPref = SharedPrefUtil(this)
        
        val gpid = intent.getStringExtra("GPID") ?: run { finish(); return }
        
        setupToolbar(gpid)
        setupSpinner()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupToolbar(gpid: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.gpidTextView.text = "GPID: $gpid"
    }
    
    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, movementTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.movementTypeSpinner.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.submitButton.setOnClickListener {
            attemptSubmit()
        }
    }
    
    private fun attemptSubmit() {
        val gpid = intent.getStringExtra("GPID") ?: return
        val movementType = binding.movementTypeSpinner.selectedItem.toString()
        val quantity = binding.quantityEditText.text?.toString()?.trim() ?: ""
        val remarks = binding.remarksEditText.text?.toString()?.trim() ?: ""
        
        // Validate
        val quantityError = ValidationUtil.validateQuantity(quantity)
        if (quantityError != null) {
            binding.quantityTextInputLayout.error = quantityError
            return
        } else {
            binding.quantityTextInputLayout.error = null
        }
        
        val movement = Movement(
            gpid = gpid,
            type = movementType,
            quantity = quantity.toInt(),
            date = DateUtil.getCurrentDate(),
            recordedBy = sharedPref.getUserId() ?: "",
            recordedByName = sharedPref.getUserName() ?: "",
            remarks = remarks,
            createdAt = DateUtil.getCurrentDateTime()
        )
        
        showLoading(true)
        viewModel.recordMovement(movement)
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.recordResult.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        Toast.makeText(
                            this@MovementEntryActivity,
                            getString(R.string.success_movement_record),
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        binding.errorTextView.apply {
                            text = resource.message
                            visibility = View.VISIBLE
                        }
                        Toast.makeText(
                            this@MovementEntryActivity,
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    null -> {}
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.submitButton.isEnabled = !show
        binding.errorTextView.visibility = View.GONE
    }
}
