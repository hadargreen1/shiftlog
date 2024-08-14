package com.example.shiftlog

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream


class PayManagementActivity : AppCompatActivity() {

    private lateinit var monthlySalaryInput: EditText
    private lateinit var taxInput: EditText
    private lateinit var pensionInput: EditText
    private lateinit var otherDeductionsInput: EditText
    private lateinit var netIncomeTextView: EditText
    private lateinit var calculateButton: Button
    private lateinit var exportPdfButton: Button
    private lateinit var exportExcelButton: Button

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
        exportPdfButton = findViewById(R.id.exportPdfButton)
        exportExcelButton = findViewById(R.id.exportExcelButton)

        // Fetch current hourly wage and update monthly salary input
        fetchHourlyWage()

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

    private fun exportUserData(format: String) {
        when (format) {
            "PDF" -> createPdf()
            "Excel" -> createExcel()
        }
    }

    private fun createPdf() {
        val fileName = "UserData.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val writer = PdfWriter(file)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)
            document.add(Paragraph("Monthly Salary: ${monthlySalaryInput.text}"))
            document.add(Paragraph("Taxes: ${taxInput.text}"))
            document.add(Paragraph("Pension: ${pensionInput.text}"))
            document.add(Paragraph("Other Deductions: ${otherDeductionsInput.text}"))
            document.add(Paragraph("Net Income: ${netIncomeTextView.text}"))
            document.close()

            shareFile(file)
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data to PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

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
            dataRow.createCell(1).setCellValue(monthlySalaryInput.text.toString())

            val taxRow = sheet.createRow(2)
            taxRow.createCell(0).setCellValue("Taxes")
            taxRow.createCell(1).setCellValue(taxInput.text.toString())

            val pensionRow = sheet.createRow(3)
            pensionRow.createCell(0).setCellValue("Pension")
            pensionRow.createCell(1).setCellValue(pensionInput.text.toString())

            val otherRow = sheet.createRow(4)
            otherRow.createCell(0).setCellValue("Other Deductions")
            otherRow.createCell(1).setCellValue(otherDeductionsInput.text.toString())

            val netIncomeRow = sheet.createRow(5)
            netIncomeRow.createCell(0).setCellValue("Net Income")
            netIncomeRow.createCell(1).setCellValue(netIncomeTextView.text.toString())

            val fos = FileOutputStream(file)
            workbook.write(fos)
            fos.close()

            shareFile(file)
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data to Excel: ${e.message}", Toast.LENGTH_LONG).show()
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

}
