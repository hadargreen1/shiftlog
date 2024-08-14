package com.example.shiftlog

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream


class PayManagementActivity : AppCompatActivity() {

    private lateinit var monthlySalaryInput: EditText
    private lateinit var taxInput: EditText
    private lateinit var pensionInput: EditText
    private lateinit var otherDeductionsInput: EditText
    private lateinit var netIncomeTextView: EditText
    private lateinit var calculateButton: Button
    private lateinit var exportDataButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_management)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Link UI elements
        monthlySalaryInput = findViewById(R.id.monthlySalaryInput)
        taxInput = findViewById(R.id.taxInput)
        pensionInput = findViewById(R.id.pensionInput)
        otherDeductionsInput = findViewById(R.id.otherDeductionsInput)
        netIncomeTextView = findViewById(R.id.netIncomeTextView)
        calculateButton = findViewById(R.id.calculateButton)
        exportDataButton = findViewById(R.id.exportDataButton)

        // Fetch current hourly wage and update monthly salary input
        fetchHourlyWage()

        calculateButton.setOnClickListener {
            calculateNetIncome()
        }

        exportDataButton.setOnClickListener {
            exportUserData()
        }
    }

    private fun fetchHourlyWage() {
        val user = auth.currentUser?.uid
        if (user != null) {
            firestore.collection("users").document(user).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val hourlyWage = document.getDouble("hourlyWage") ?: 0.0
                        val monthlySalary = hourlyWage * 160  // Assume 160 working hours in a month
                        monthlySalaryInput.setText(monthlySalary.toString())
                    } else {
                        Toast.makeText(this, "No user data found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching data: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun calculateNetIncome() {
        val grossSalary = monthlySalaryInput.text.toString().toDoubleOrNull() ?: 0.0
        val tax = taxInput.text.toString().toDoubleOrNull() ?: 0.0
        val pension = pensionInput.text.toString().toDoubleOrNull() ?: 0.0
        val otherDeductions = otherDeductionsInput.text.toString().toDoubleOrNull() ?: 0.0

        val netIncome = grossSalary - (tax + pension + otherDeductions)
        netIncomeTextView.setText(netIncome.toString())
    }

    private fun exportUserData() {
        val userData = """
            Monthly Salary: ${monthlySalaryInput.text}
            Taxes: ${taxInput.text}
            Pension: ${pensionInput.text}
            Other Deductions: ${otherDeductionsInput.text}
            Net Income: ${netIncomeTextView.text}
        """.trimIndent()

        val fileName = "UserData.txt"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val fos = FileOutputStream(file)
            fos.write(userData.toByteArray())
            fos.close()
            Toast.makeText(this, "Data exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
