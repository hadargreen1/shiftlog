package com.example.shiftlog

import BaseActivity
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PayManagementActivity : BaseActivity() {

    private lateinit var monthPickerInput: EditText
    private lateinit var monthlySalaryInput: EditText
    private lateinit var deductionsInput: EditText
    private lateinit var bonusesInput: EditText
    private lateinit var taxInput: EditText
    private lateinit var pensionInput: EditText
    private lateinit var otherDeductionsInput: EditText
    private lateinit var netIncomeTextView: EditText
    private lateinit var calculateButton: Button
    private lateinit var exportPdfButton: Button
    private lateinit var exportExcelButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var hourlyWage: Double = 0.0 // To store the hourly wage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_management)

        // Setup toolbar and drawer
        setupToolbarAndDrawer(R.id.toolbar, R.id.drawer_layout, R.id.nav_view)

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app")

        // Link UI elements
        initializeUI()

        // Fetch the hourly wage from Realtime Database
        fetchHourlyWage()

        // Set up month picker
        monthPickerInput.setOnClickListener {
            showMonthPickerDialog()
        }

        // Set onClick listeners
        calculateButton.setOnClickListener {
            calculateNetIncome()
        }
        exportPdfButton.setOnClickListener {
            exportUserData("PDF")
        }
        exportExcelButton.setOnClickListener {
            exportUserData("Excel")
        }
    }

    private fun initializeUI() {
        monthPickerInput = findViewById(R.id.monthPickerInput)
        monthlySalaryInput = findViewById(R.id.monthlySalaryInput)
        deductionsInput = findViewById(R.id.deductionsInput)
        bonusesInput = findViewById(R.id.bonusesInput)
        taxInput = findViewById(R.id.taxInput)
        pensionInput = findViewById(R.id.pensionInput)
        otherDeductionsInput = findViewById(R.id.otherDeductionsInput)
        netIncomeTextView = findViewById(R.id.netIncomeTextView)
        calculateButton = findViewById(R.id.calculateButton)
        exportPdfButton = findViewById(R.id.exportPdfButton)
        exportExcelButton = findViewById(R.id.exportExcelButton)
    }

    private fun fetchHourlyWage() {
        val user = auth.currentUser?.uid
        if (user != null) {
            val userRef = database.reference.child("users").child(user).child("hourlyWage")
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    hourlyWage = dataSnapshot.getValue(Double::class.java) ?: 0.0
                    Toast.makeText(this@PayManagementActivity, "Hourly Wage: $$hourlyWage", Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    showError("Error fetching hourly wage")
                }
            })
        }
    }

    private fun showMonthPickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, _ ->
                val formattedMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    .format(GregorianCalendar(selectedYear, selectedMonth, 1).time)
                monthPickerInput.setText(formattedMonth)
                calculateMonthlySalary(formattedMonth)
            }, year, month, 1
        )

        datePickerDialog.show()
    }

    private fun calculateMonthlySalary(month: String) {
        val user = auth.currentUser?.uid

        if (user != null) {
            val userShiftsRef = database.reference.child("users").child(user).child("shifts")
            userShiftsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalSalary = 0.0
                    for (dateSnapshot in dataSnapshot.children) {
                        if (dateSnapshot.key?.startsWith(month) == true) {
                            for (shiftSnapshot in dateSnapshot.children) {
                                val shiftDuration = shiftSnapshot.child("duration").getValue(Double::class.java) ?: 0.0
                                val shiftSalary = shiftDuration * hourlyWage
                                totalSalary += shiftSalary
                            }
                        }
                    }
                    monthlySalaryInput.setText(totalSalary.toString())
                    calculateTax(totalSalary)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    showError("Error fetching shift data")
                }
            })
        }
    }

    @SuppressLint("DefaultLocale")
    private fun calculateTax(income: Double) {
        val tax = when {
            income <= 7010 -> income * 0.1
            income <= 10060 -> 701 + (income - 7010) * 0.14
            income <= 16150 -> 1255 + (income - 10060) * 0.2
            income <= 22440 -> 2483 + (income - 16150) * 0.31
            income <= 46690 -> 4407 + (income - 22440) * 0.35
            income <= 60130 -> 13225 + (income - 46690) * 0.47
            else -> 19849 + (income - 60130) * 0.5
        }
        taxInput.setText(String.format("%.3f", tax))
    }

    @SuppressLint("DefaultLocale")
    private fun calculateNetIncome() {
        val grossSalary = monthlySalaryInput.text.toString().toDoubleOrNull() ?: 0.0
        val deductions = deductionsInput.text.toString().toDoubleOrNull() ?: 0.0
        val bonuses = bonusesInput.text.toString().toDoubleOrNull() ?: 0.0

        val totalSalary = grossSalary + bonuses - deductions
        calculateTax(totalSalary)
        val tax = taxInput.text.toString().toDoubleOrNull() ?: 0.0
        val pension = calculatePension(totalSalary)
        val otherDeductions = calculateInsurance(totalSalary)

        pensionInput.setText(String.format("%.3f", pension))
        otherDeductionsInput.setText(String.format("%.3f", otherDeductions))
        taxInput.setText(String.format("%.3f", tax))

        val netIncome = totalSalary - (tax + pension + otherDeductions)
        netIncomeTextView.setText(String.format("%.3f", netIncome))
    }

    private fun calculatePension(grossSalary: Double): Double {
        return grossSalary * 0.06 // 6% employee contribution
    }

    private fun calculateInsurance(grossSalary: Double): Double {
        val healthInsurance = grossSalary * 0.05 // 5% health insurance
        val socialSecurity = if (grossSalary <= 7522) {
            grossSalary * 0.0355
        } else if (grossSalary <= 49030) {
            7522 * 0.0355 + (grossSalary - 7522) * 0.146
        } else {
            7522 * 0.0355 + (49030 - 7522) * 0.146
        }
        return healthInsurance + socialSecurity
    }

    private fun exportUserData(format: String) {
        when (format) {
            "PDF" -> createPdf()
            "Excel" -> createExcel()
        }
    }

    private fun createPdf() {
        val userId = auth.currentUser?.uid ?: return
        val userFirestoreRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        // Fetch user data from Firestore
        userFirestoreRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val userFullName = document.getString("fullName") ?: "Unknown Name"
                val userEmail = document.getString("email") ?: "Unknown Email"
                val hourlyWage = document.getDouble("hourlyWage") ?: 0.0

                // Fetch total worked hours for the month from Realtime Database
                val month = monthPickerInput.text.toString()
                val dbRef = database.reference.child("users").child(userId).child("shifts")
                dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var totalWorkedHours = 0.0
                        for (dateSnapshot in dataSnapshot.children) {
                            if (dateSnapshot.key?.startsWith(month) == true) {
                                for (shiftSnapshot in dateSnapshot.children) {
                                    val duration = shiftSnapshot.child("duration").getValue(Double::class.java) ?: 0.0
                                    totalWorkedHours += duration
                                }
                            }
                        }
                        generatePdf(userFullName, userEmail, hourlyWage, totalWorkedHours)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        showError("Error fetching shift data: ${databaseError.message}")
                    }
                })
            } else {
                showError("User data not found")
            }
        }.addOnFailureListener { exception ->
            showError("Error fetching user data: ${exception.message}")
        }
    }




    @SuppressLint("DefaultLocale")
    private fun generatePdf(userFullName: String, userEmail: String, hourlyWage: Double, totalWorkedHours: Double) {
        val month = monthPickerInput.text.toString()
        val fileName = "UserData_${month}.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val writer = PdfWriter(file)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)

            // Header Section
            document.add(Paragraph("Pay Stub for $month").setBold())
            document.add(Paragraph("Name: $userFullName"))
            document.add(Paragraph("Email: $userEmail"))
            document.add(Paragraph("Hourly Wage: $${String.format("%.2f", hourlyWage)}"))
            document.add(Paragraph("Total Worked Hours: ${String.format("%.2f", totalWorkedHours)}"))
            document.add(Paragraph("Date Generated: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"))
            document.add(Paragraph("\n"))

            // Create a table with 2 columns
            val table = Table(floatArrayOf(3f, 3f)).useAllAvailableWidth()

            // Add table headers
            table.addHeaderCell(Cell().add(Paragraph("Description").setBold()))
            table.addHeaderCell(Cell().add(Paragraph("Amount").setBold()))

            // Add table rows with payment details
            addTableRow(table, "Monthly Salary", monthlySalaryInput.text.toString())
            addTableRow(table, "Bonuses", bonusesInput.text.toString())
            addTableRow(table, "Deductions", deductionsInput.text.toString())
            addTableRow(table, "Taxes", taxInput.text.toString())
            addTableRow(table, "Pension", pensionInput.text.toString())
            addTableRow(table, "Other Deductions", otherDeductionsInput.text.toString())
            addTableRow(table, "Net Income", netIncomeTextView.text.toString(), true)

            // Add the table to the document
            document.add(table)

            document.close()

            shareFile(file)
        } catch (e: Exception) {
            showError("Error exporting data to PDF: ${e.message}")
        }
    }

    private fun addTableRow(table: Table, description: String, amount: String, isBold: Boolean = false) {
        table.addCell(description)
        table.addCell(Cell().add(Paragraph(amount).apply { if (isBold) setBold() }))
    }

    @SuppressLint("DefaultLocale")
    private fun createExcel() {
        val fileName = "UserData.xlsx"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("User Data")

            val headers = listOf("Field", "Value")
            val data = listOf(
                "Monthly Salary" to monthlySalaryInput.text.toString(),
                "Bonuses" to bonusesInput.text.toString(),
                "Deductions" to deductionsInput.text.toString(),
                "Taxes" to taxInput.text.toString(),
                "Pension" to pensionInput.text.toString(),
                "Other Deductions" to otherDeductionsInput.text.toString(),
                "Net Income" to netIncomeTextView.text.toString()
            )

            // Add header row
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header -> headerRow.createCell(index).setCellValue(header) }

            // Add data rows
            data.forEachIndexed { rowIndex, pair ->
                val row = sheet.createRow(rowIndex + 1)
                row.createCell(0).setCellValue(pair.first)
                row.createCell(1).setCellValue(pair.second)
            }

            val fos = FileOutputStream(file)
            workbook.write(fos)
            fos.close()

            shareFile(file)
        } catch (e: Exception) {
            showError("Error exporting data to Excel: ${e.message}")
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = if (file.name.endsWith(".pdf")) "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share file via"))
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
