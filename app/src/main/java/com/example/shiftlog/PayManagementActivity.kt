package com.example.shiftlog

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.*

class PayManagementActivity : AppCompatActivity() {

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
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_management)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database =
            FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app")

        // Link UI elements
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

        // Set up month picker
        monthPickerInput.setOnClickListener {
            showMonthPickerDialog()
        }

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
                                val salary =
                                    shiftSnapshot.child("salary").getValue(Double::class.java)
                                        ?: 0.0
                                totalSalary += salary
                            }
                        }
                    }
                    monthlySalaryInput.setText(totalSalary.toString())
                    // Automatically calculate the tax based on the updated salary
                    calculateTax(totalSalary)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(
                        this@PayManagementActivity,
                        "Error fetching shift data",
                        Toast.LENGTH_LONG
                    ).show()
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

    @SuppressLint("DefaultLocale")
    private fun createPdf() {
        val fileName = "UserData.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val writer = PdfWriter(file)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)
            document.add(Paragraph("Monthly Salary: ${String.format("%.3f", monthlySalaryInput.text.toString().toDouble())}"))
            document.add(Paragraph("Bonuses: ${String.format("%.3f", bonusesInput.text.toString().toDouble())}"))
            document.add(Paragraph("Deductions: ${String.format("%.3f", deductionsInput.text.toString().toDouble())}"))
            document.add(Paragraph("Taxes: ${String.format("%.3f", taxInput.text.toString().toDouble())}"))
            document.add(Paragraph("Pension: ${String.format("%.3f", pensionInput.text.toString().toDouble())}"))
            document.add(Paragraph("Other Deductions: ${String.format("%.3f", otherDeductionsInput.text.toString().toDouble())}"))
            document.add(Paragraph("Net Income: ${String.format("%.3f", netIncomeTextView.text.toString().toDouble())}"))
            document.close()

            shareFile(file)
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data to PDF: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun createExcel() {
        val fileName = "UserData.xlsx"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("User Data")

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Field")
            headerRow.createCell(1).setCellValue("Value")

            val dataRow = sheet.createRow(1)
            dataRow.createCell(0).setCellValue("Monthly Salary")
            dataRow.createCell(1).setCellValue(String.format("%.3f", monthlySalaryInput.text.toString().toDouble()))

            val bonusesRow = sheet.createRow(2)
            bonusesRow.createCell(0).setCellValue("Bonuses")
            bonusesRow.createCell(1).setCellValue(String.format("%.3f", bonusesInput.text.toString().toDouble()))

            val deductionsRow = sheet.createRow(3)
            deductionsRow.createCell(0).setCellValue("Deductions")
            deductionsRow.createCell(1).setCellValue(String.format("%.3f", deductionsInput.text.toString().toDouble()))

            val taxRow = sheet.createRow(4)
            taxRow.createCell(0).setCellValue("Taxes")
            taxRow.createCell(1).setCellValue(String.format("%.3f", taxInput.text.toString().toDouble()))

            val pensionRow = sheet.createRow(5)
            pensionRow.createCell(0).setCellValue("Pension")
            pensionRow.createCell(1).setCellValue(String.format("%.3f", pensionInput.text.toString().toDouble()))

            val otherRow = sheet.createRow(6)
            otherRow.createCell(0).setCellValue("Other Deductions")
            otherRow.createCell(1).setCellValue(String.format("%.3f", otherDeductionsInput.text.toString().toDouble()))

            val netIncomeRow = sheet.createRow(7)
            netIncomeRow.createCell(0).setCellValue("Net Income")
            netIncomeRow.createCell(1).setCellValue(String.format("%.3f", netIncomeTextView.text.toString().toDouble()))

            val fos = FileOutputStream(file)
            workbook.write(fos)
            fos.close()

            shareFile(file)
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data to Excel: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }


    private fun shareFile(file: File) {
        val uri =
            FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type =
            if (file.name.endsWith(".pdf")) "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share file via"))
    }
}