package com.sonia.gatepass.ui.gatepass

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.data.model.Movement
import com.sonia.gatepass.databinding.ActivityGatePassDetailsBinding
import com.sonia.gatepass.ui.adapter.AuditLogAdapter
import com.sonia.gatepass.ui.adapter.MovementAdapter
import com.sonia.gatepass.ui.gatepass.MovementEntryActivity
import com.sonia.gatepass.ui.viewmodel.GatePassViewModel
import com.sonia.gatepass.ui.viewmodel.MovementViewModel
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.DateUtil
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class GatePassDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGatePassDetailsBinding
    private val gatePassViewModel: GatePassViewModel by viewModels()
    private val movementViewModel: MovementViewModel by viewModels()
    private lateinit var sharedPref: SharedPrefUtil
    private lateinit var movementAdapter: MovementAdapter
    private lateinit var auditLogAdapter: AuditLogAdapter
    
    private var currentGatePass: GatePass? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGatePassDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPref = SharedPrefUtil(this)
        
        val gpid = intent.getStringExtra("GPID") ?: run { finish(); return }
        
        setupToolbar()
        setupRecyclerViews()
        loadData(gpid)
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerViews() {
        movementAdapter = MovementAdapter()
        binding.movementRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GatePassDetailsActivity)
            adapter = movementAdapter
        }
        
        auditLogAdapter = AuditLogAdapter()
        binding.auditLogRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GatePassDetailsActivity)
            adapter = auditLogAdapter
        }
    }
    
    private fun loadData(gpid: String) {
        gatePassViewModel.getGatePass(gpid)
        movementViewModel.observeMovements(gpid)
    }
    
    private fun setupClickListeners() {
        binding.movementEntryButton.setOnClickListener {
            val intent = Intent(this, MovementEntryActivity::class.java).apply {
                putExtra("GPID", currentGatePass?.gpid)
            }
            movementLauncher.launch(intent)
        }
        
        binding.approveButton.setOnClickListener {
            currentGatePass?.let { gp ->
                gatePassViewModel.approveGatePass(
                    gp.gpid,
                    sharedPref.getUserId() ?: "",
                    sharedPref.getUserName() ?: "Admin"
                )
            }
        }

        binding.rejectButton.setOnClickListener {
            currentGatePass?.let { gp ->
                gatePassViewModel.rejectGatePass(
                    gp.gpid,
                    sharedPref.getUserId() ?: "",
                    sharedPref.getUserName() ?: "Admin"
                )
            }
        }
        
        binding.completeButton.setOnClickListener {
            currentGatePass?.let { gp ->
                gatePassViewModel.markCompleted(gp.gpid)
            }
        }
        
        binding.reopenButton.setOnClickListener {
            currentGatePass?.let { gp ->
                gatePassViewModel.reopenGatePass(
                    gp.gpid,
                    sharedPref.getUserId() ?: "",
                    sharedPref.getUserName() ?: "Super Admin"
                )
            }
        }
        
        binding.generatePdfButton.setOnClickListener {
            currentGatePass?.let { generatePDF(it) }
        }

        binding.deleteButton.setOnClickListener {
            currentGatePass?.let { gp ->
                showDeleteConfirmationDialog(gp)
            }
        }
    }

    private fun showDeleteConfirmationDialog(gatePass: GatePass) {
        AlertDialog.Builder(this)
            .setTitle("Delete Gate Pass")
            .setMessage("Are you sure you want to permanently delete gate pass ${gatePass.gpid}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                gatePassViewModel.deleteGatePass(gatePass.gpid)
                currentGatePass = null // Clear reference so observer knows to navigate back
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private val movementLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Refresh data after movement entry
        currentGatePass?.let { loadData(it.gpid) }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            gatePassViewModel.gatePass.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.let { updateUI(it) }
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@GatePassDetailsActivity, resource.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
        
        lifecycleScope.launch {
            movementViewModel.movements.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.let { movementAdapter.submitList(it) }
                    }
                    else -> {}
                }
            }
        }
        
        lifecycleScope.launch {
            gatePassViewModel.actionResult.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        // Check if this was a delete action by seeing if currentGatePass is null
                        if (currentGatePass == null) {
                            Toast.makeText(this@GatePassDetailsActivity, "Gate pass deleted successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@GatePassDetailsActivity, "Action successful", Toast.LENGTH_SHORT).show()
                            currentGatePass?.let { loadData(it.gpid) }
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@GatePassDetailsActivity, resource.message, Toast.LENGTH_SHORT).show()
                    }
                    null -> {}
                }
            }
        }
    }
    
    private fun updateUI(gatePass: GatePass) {
        currentGatePass = gatePass
        
        binding.gpidTextView.text = gatePass.gpid
        binding.statusTextView.text = gatePass.status
        binding.statusTextView.setBackgroundColor(getStatusColor(gatePass.status))
        
        // Details
        binding.styleNoRow.labelTextView.text = "Style No"
        binding.styleNoRow.valueTextView.text = gatePass.styleNo
        
        binding.goodsNameRow.labelTextView.text = "Goods Name"
        binding.goodsNameRow.valueTextView.text = gatePass.goodsName
        
        binding.concernedPeopleRow.labelTextView.text = "Concerned People"
        binding.concernedPeopleRow.valueTextView.text = gatePass.concernedPeopleEmail
        
        binding.destinationRow.labelTextView.text = "Destination"
        binding.destinationRow.valueTextView.text = gatePass.destination
        
        binding.purposeRow.labelTextView.text = "Purpose"
        binding.purposeRow.valueTextView.text = gatePass.purpose
        
        binding.returnableDateRow.labelTextView.text = "Returnable Date"
        binding.returnableDateRow.valueTextView.text = gatePass.returnableDate
        
        binding.createdByRow.labelTextView.text = "Created By"
        binding.createdByRow.valueTextView.text = gatePass.createdByName
        
        binding.approvedByRow.labelTextView.text = "Approved By"
        binding.approvedByRow.valueTextView.text = if (gatePass.approvedByName.isNotBlank()) gatePass.approvedByName else "-"
        
        // Quantities
        binding.totalSentTextView.text = gatePass.totalSent.toString()
        binding.totalReturnedTextView.text = gatePass.totalReturned.toString()
        binding.totalRedispatchedTextView.text = gatePass.totalRedispatched.toString()
        binding.balanceTextView.text = gatePass.calculateBalance().toString()
        
        // Audit Log
        auditLogAdapter.submitList(gatePass.auditLog)
        
        // Show/hide action buttons based on role and status
        updateActionButtons(gatePass)
    }
    
    private fun updateActionButtons(gatePass: GatePass) {
        val role = sharedPref.getUserRole()
        val isSuperAdmin = role == Constants.ROLE_SUPER_ADMIN
        val isAdmin = role == Constants.ROLE_ADMIN || isSuperAdmin
        val isProduction = role == Constants.ROLE_USER || isAdmin
        
        // Reset visibility
        binding.movementEntryButton.visibility = View.GONE
        binding.approveButton.visibility = View.GONE
        binding.rejectButton.visibility = View.GONE
        binding.completeButton.visibility = View.GONE
        binding.reopenButton.visibility = View.GONE
        binding.deleteButton.visibility = View.GONE
        binding.generatePdfButton.visibility = View.VISIBLE

        // Show delete button only for Super Admin (on any status)
        if (isSuperAdmin) {
            binding.deleteButton.visibility = View.VISIBLE
        }

        when (gatePass.status) {
            Constants.STATUS_PENDING -> {
                if (isAdmin) {
                    binding.approveButton.visibility = View.VISIBLE
                    binding.rejectButton.visibility = View.VISIBLE
                }
            }
            Constants.STATUS_APPROVED, Constants.STATUS_IN_PROGRESS,
            Constants.STATUS_PARTIALLY_RETURNED, Constants.STATUS_REDISPATCHED,
            Constants.STATUS_REOPENED -> {
                if (isProduction) {
                    binding.movementEntryButton.visibility = View.VISIBLE
                }
                if (isAdmin) {
                    binding.completeButton.visibility = View.VISIBLE
                }
            }
            Constants.STATUS_COMPLETED -> {
                if (isAdmin) {
                    binding.reopenButton.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun getStatusColor(status: String): Int {
        return when (status) {
            Constants.STATUS_PENDING -> Color.parseColor("#FF9800")
            Constants.STATUS_APPROVED -> Color.parseColor("#4CAF50")
            Constants.STATUS_IN_PROGRESS -> Color.parseColor("#2196F3")
            Constants.STATUS_PARTIALLY_RETURNED -> Color.parseColor("#9C27B0")
            Constants.STATUS_REDISPATCHED -> Color.parseColor("#00BCD4")
            Constants.STATUS_COMPLETED -> Color.parseColor("#8BC34A")
            Constants.STATUS_REOPENED -> Color.parseColor("#FF5722")
            Constants.STATUS_REJECTED -> Color.parseColor("#F44336")
            else -> Color.parseColor("#9E9E9E")
        }
    }
    
    private fun generatePDF(gatePass: GatePass) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            
            // Title
            paint.textSize = 24f
            paint.color = Color.parseColor("#1976D2")
            paint.isFakeBoldText = true
            canvas.drawText("Gate Pass - ${gatePass.gpid}", 50f, 50f, paint)

            // Company Name
            paint.textSize = 16f
            paint.color = Color.BLACK
            paint.isFakeBoldText = false
            canvas.drawText("Sonia & Sweaters Limited", 50f, 80f, paint)
            
            // Details
            paint.textSize = 14f
            paint.color = Color.DKGRAY
            var yPosition = 130
            val lineHeight = 25
            
            val details = listOf(
                "Style No: ${gatePass.styleNo}",
                "Goods Name: ${gatePass.goodsName}",
                "Concerned People: ${gatePass.concernedPeopleEmail}",
                "Destination: ${gatePass.destination}",
                "Purpose: ${gatePass.purpose}",
                "Returnable Date: ${gatePass.returnableDate}",
                "",
                "Status: ${gatePass.status}",
                "",
                "Total Sent: ${gatePass.totalSent}",
                "Total Returned: ${gatePass.totalReturned}",
                "Re-dispatched: ${gatePass.totalRedispatched}",
                "Balance: ${gatePass.calculateBalance()}",
                "",
                "Created By: ${gatePass.createdByName}",
                "Approved By: ${if (gatePass.approvedByName.isNotBlank()) gatePass.approvedByName else "Pending"}",
                "Created At: ${gatePass.createdAt}"
            )
            
            for (line in details) {
                canvas.drawText(line, 50f, yPosition.toFloat(), paint)
                yPosition += lineHeight
            }
            
            document.finishPage(page)
            
            // Save PDF
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "GatePass_${gatePass.gpid}.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()
            
            Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
