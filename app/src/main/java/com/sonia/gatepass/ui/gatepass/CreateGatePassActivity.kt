package com.sonia.gatepass.ui.gatepass

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sonia.gatepass.R
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.databinding.ActivityCreateGatePassBinding
import com.sonia.gatepass.ui.viewmodel.GatePassViewModel
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.DateUtil
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import com.sonia.gatepass.util.ValidationUtil
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateGatePassActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateGatePassBinding
    private val viewModel: GatePassViewModel by viewModels()
    private lateinit var sharedPref: SharedPrefUtil
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGatePassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPref = SharedPrefUtil(this)
        
        setupToolbar()
        setupDatePicker()
        setupClickListeners()
        observeViewModel()
        
        // Get next GPID
        viewModel.getNextGPID()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupDatePicker() {
        binding.returnableDateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    binding.returnableDateEditText.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = calendar.timeInMillis
            datePicker.show()
        }
    }
    
    private fun setupClickListeners() {
        binding.submitButton.setOnClickListener {
            attemptSubmit()
        }
        
        binding.cancelButton.setOnClickListener {
            finish()
        }
    }
    
    private fun attemptSubmit() {
        val styleNo = binding.styleNoEditText.text?.toString()?.trim() ?: ""
        val goodsName = binding.goodsNameEditText.text?.toString()?.trim() ?: ""
        val concernedPeople = binding.concernedPeopleEditText.text?.toString()?.trim() ?: ""
        val quantity = binding.quantityEditText.text?.toString()?.trim() ?: ""
        val destination = binding.destinationEditText.text?.toString()?.trim() ?: ""
        val purpose = binding.purposeEditText.text?.toString()?.trim() ?: ""
        val returnableDate = binding.returnableDateEditText.text?.toString()?.trim() ?: ""
        
        // Validate
        var hasError = false
        
        val styleError = ValidationUtil.validateRequired(styleNo, "Style No")
        if (styleError != null) {
            binding.styleNoTextInputLayout.error = styleError
            hasError = true
        } else {
            binding.styleNoTextInputLayout.error = null
        }
        
        val goodsError = ValidationUtil.validateRequired(goodsName, "Goods Name")
        if (goodsError != null) {
            binding.goodsNameTextInputLayout.error = goodsError
            hasError = true
        } else {
            binding.goodsNameTextInputLayout.error = null
        }
        
        val peopleError = ValidationUtil.validateRequired(concernedPeople, "Concerned People")
        if (peopleError != null) {
            binding.concernedPeopleTextInputLayout.error = peopleError
            hasError = true
        } else {
            binding.concernedPeopleTextInputLayout.error = null
        }
        
        val quantityError = ValidationUtil.validateQuantity(quantity)
        if (quantityError != null) {
            binding.quantityTextInputLayout.error = quantityError
            hasError = true
        } else {
            binding.quantityTextInputLayout.error = null
        }
        
        val destError = ValidationUtil.validateRequired(destination, "Destination")
        if (destError != null) {
            binding.destinationTextInputLayout.error = destError
            hasError = true
        } else {
            binding.destinationTextInputLayout.error = null
        }
        
        val purposeError = ValidationUtil.validateRequired(purpose, "Purpose")
        if (purposeError != null) {
            binding.purposeTextInputLayout.error = purposeError
            hasError = true
        } else {
            binding.purposeTextInputLayout.error = null
        }
        
        val dateError = ValidationUtil.validateRequired(returnableDate, "Returnable Date")
        if (dateError != null) {
            binding.returnableDateTextInputLayout.error = dateError
            hasError = true
        } else {
            binding.returnableDateTextInputLayout.error = null
        }
        
        if (hasError) return
        
        // Create gate pass
        val gpid = binding.gpidTextView.text.toString().replace("GPID: ", "")
        val userId = sharedPref.getUserId() ?: ""
        val userName = sharedPref.getUserName() ?: ""
        
        val gatePass = GatePass(
            gpid = gpid,
            styleNo = styleNo,
            goodsName = goodsName,
            concernedPeopleEmail = concernedPeople,
            destination = destination,
            purpose = purpose,
            totalSent = quantity.toInt(),
            totalReturned = 0,
            totalRedispatched = 0,
            balanceQuantity = quantity.toInt(),
            returnableDate = returnableDate,
            status = Constants.STATUS_PENDING,
            createdBy = userId,
            createdByName = userName,
            approvedBy = "",
            approvedByName = "",
            createdAt = DateUtil.getCurrentDateTime(),
            updatedAt = DateUtil.getCurrentDateTime(),
            completedAt = "",
            reopeningCount = 0,
            auditLog = emptyList()
        )
        
        showLoading(true)
        viewModel.createGatePass(gatePass)
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.nextGPID.collect { resource ->
                if (resource is Resource.Success) {
                    binding.gpidTextView.text = "GPID: ${resource.data}"
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.createResult.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        Toast.makeText(
                            this@CreateGatePassActivity,
                            getString(R.string.success_gate_pass_created),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        binding.errorTextView.apply {
                            text = resource.message
                            visibility = View.VISIBLE
                        }
                        Toast.makeText(
                            this@CreateGatePassActivity,
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    null -> {
                        // Do nothing
                    }
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
