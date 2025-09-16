package com.abhik.urbanmanagementplatform.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.abhik.urbanmanagementplatform.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class GrievanceFormFragment : Fragment() {

    private lateinit var departmentName: String
    private var imageUri: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var previewImageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var submitButton: Button

    // Modern way to handle activity results
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            previewImageView.setImageURI(imageUri)
            previewImageView.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            departmentName = it.getString(ARG_DEPT_NAME) ?: "Unknown"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.grievanceformlayout, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        view.findViewById<TextView>(R.id.tv_department_title).text = "File Grievance: $departmentName"
        titleEditText = view.findViewById(R.id.et_grievance_title)
        descriptionEditText = view.findViewById(R.id.et_grievance_description)
        previewImageView = view.findViewById(R.id.iv_preview)
        progressBar = view.findViewById(R.id.progress_bar)
        submitButton = view.findViewById(R.id.btn_submit_grievance)
        val uploadButton = view.findViewById<Button>(R.id.btn_upload_photo)

        uploadButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(galleryIntent)
        }

        submitButton.setOnClickListener {
            submitGrievance()
        }

        return view
    }

    private fun submitGrievance() {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Title and description cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        if (imageUri != null) {
            // If there's an image, upload it first
            //val fileName = "grievances/${UUID.randomUUID()}.jpg"

            val folderName = departmentName.replace(" ", "_").lowercase(Locale.getDefault())
            val fileName = "grievances/$folderName/${UUID.randomUUID()}.jpg"

            val storageRef = storage.reference.child(fileName)
            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveGrievanceToFirestore(title, description, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
        } else {
            // If no image, save directly to Firestore
            saveGrievanceToFirestore(title, description, null)
        }
    }

    private fun saveGrievanceToFirestore(title: String, description: String, imageUrl: String?) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "You must be logged in to submit a grievance.", Toast.LENGTH_LONG).show()
            setLoading(false)
            return
        }

        val grievance = hashMapOf(
            "userId" to userId,
            "department" to departmentName,
            "title" to title,
            "description" to description,
            "imageUrl" to imageUrl,
            "timestamp" to Date(),
            "status" to "Submitted" // Initial status
        )

        db.collection("grievances")
            .add(grievance)
            .addOnSuccessListener {
                Toast.makeText(context, "Grievance submitted successfully!", Toast.LENGTH_SHORT).show()
                setLoading(false)
                // Go back to the home screen
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to submit: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            submitButton.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            submitButton.isEnabled = true
        }
    }

    companion object {
        private const val ARG_DEPT_NAME = "department_name"

        @JvmStatic
        fun newInstance(departmentName: String) =
            GrievanceFormFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DEPT_NAME, departmentName)
                }
            }
    }
}
