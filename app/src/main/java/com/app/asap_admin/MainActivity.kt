package com.app.asap_admin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.asap_admin.LoginActivity
import com.app.asap_admin.R
import com.app.asap_admin.PDF
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private val REQUEST_PICK_PDF = 1
    private lateinit var etYear: EditText
    private lateinit var etFilename: EditText
    private lateinit var btnUploadPdf: Button
    private lateinit var tvOrigName: TextView
    private lateinit var btnChoosePdf: ImageButton
    private lateinit var btnX: ImageButton
    private lateinit var spinnerDepartment: Spinner
    private lateinit var spinnerSemester: Spinner
    private lateinit var dateRangePickerDialog: Dialog
    private lateinit var storageRef: StorageReference
    private var selectedFileUri: Uri? = null
    private var mDatabase: DatabaseReference? = null
    private var userSpecifiedFilename: String? = null
    private var filePath: String? = null
    private var userid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        FirebaseApp.initializeApp(this)

        setupFirebaseReferences()
        setupUIElements()
        setupSpinners()
        requestStoragePermission()
        handleIntentActions(intent)

        btnChoosePdf.setOnClickListener { openPdfSelector() }
        btnX.setOnClickListener { clearSelectedFile() }
        btnUploadPdf.setOnClickListener { validateAndUploadFile() }
    }

    private fun setupFirebaseReferences() {
        val storage = FirebaseStorage.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference
        storageRef = storage.reference.child("chennai")
    }

    private fun setupUIElements() {
        etYear = findViewById(R.id.idETVYear)
        etFilename = findViewById(R.id.idETVFilename)
        btnChoosePdf = findViewById(R.id.idBTNPdfUpload)
        btnUploadPdf = findViewById(R.id.idBTNUpload)
        btnX = findViewById(R.id.idBTNClosePdf)
        tvOrigName = findViewById(R.id.idTVOrigName)
        spinnerSemester = findViewById(R.id.idSpinnerSemester)
        spinnerDepartment = findViewById(R.id.idSpinnerDepartment)

        // Set the title with username if available
        retrieveUsernameFromSharedPreferences()?.let {
            supportActionBar?.title = "Hi, $it"
        }
    }

    private fun setupSpinners() {
        val semesterOptions = arrayOf("SELECT", "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8")
        val departmentOptions = arrayOf("SELECT", "CSE", "CYS", "AIE", "ECE", "CCE", "MEE", "ARE")

        spinnerSemester.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, semesterOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerDepartment.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, departmentOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    private fun handleIntentActions(intent: Intent) {
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type == "application/pdf") {
            handleSharedPdf(intent)
        }
    }

    private fun openPdfSelector() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, REQUEST_PICK_PDF)
    }

    private fun clearSelectedFile() {
        selectedFileUri = null
        tvOrigName.text = ""
        btnChoosePdf.visibility = View.VISIBLE
        btnX.visibility = View.GONE
        tvOrigName.visibility = View.GONE
        btnUploadPdf.setBackgroundColor(ContextCompat.getColor(this, R.color.original))

    }

    private fun validateAndUploadFile() {
        val semester = spinnerSemester.selectedItem.toString()
        val department = spinnerDepartment.selectedItem.toString()
        val year = etYear.text.toString()
        userSpecifiedFilename = etFilename.text.toString()

        if (semester == "SELECT" || department == "SELECT" || year.isEmpty() || userSpecifiedFilename.isNullOrEmpty()) {
            Toast.makeText(this, "Please fill in all fields with valid data.", Toast.LENGTH_SHORT).show()
            return
        }

        filePath = "$semester/$department/$year/"
        selectedFileUri?.let { uploadFileToFirebaseStorage(it) }
            ?: Toast.makeText(this, "Please choose a PDF file.", Toast.LENGTH_SHORT).show()
    }

    private fun uploadFileToFirebaseStorage(fileUri: Uri) {
        val pdfRef = storageRef.child("$filePath$userSpecifiedFilename")
        pdfRef.putFile(fileUri).addOnSuccessListener {
            pdfRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val pdf = PDF(userid, filePath, userSpecifiedFilename, downloadUri.toString(), currentTime)
                mDatabase?.child("Uploaded pdf")?.push()?.setValue(pdf)
                Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show()
                resetUI()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun retrieveUsernameFromSharedPreferences(): String? {
        val preferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        return preferences.getString("name", "")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_PDF && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                selectedFileUri = it
                btnChoosePdf.visibility = View.GONE
                btnX.visibility = View.VISIBLE
                tvOrigName.visibility = View.VISIBLE
                tvOrigName.text = getOriginalFileName(it)
                btnUploadPdf.setBackgroundColor(ContextCompat.getColor(this, R.color.green))

                etFilename.text.clear()
            }
        }
    }

    private fun getOriginalFileName(fileUri: Uri): String? {
        contentResolver.query(fileUri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
            }
        }
        return null
    }

    private fun handleSharedPdf(intent: Intent) {
        selectedFileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        selectedFileUri?.let {
            btnChoosePdf.visibility = View.GONE
            btnX.visibility = View.VISIBLE
            tvOrigName.visibility = View.VISIBLE
            tvOrigName.text = getOriginalFileName(it)
            btnUploadPdf.setBackgroundColor(ContextCompat.getColor(this, R.color.original))
            etFilename.text.clear()
        }
    }

    private fun resetUI() {
        spinnerDepartment.setSelection(0)
        spinnerSemester.setSelection(0)
        etYear.text.clear()
        etFilename.text.clear()
        clearSelectedFile()
    }
}
