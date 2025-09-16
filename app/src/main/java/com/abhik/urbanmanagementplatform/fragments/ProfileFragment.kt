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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var nameEditText: EditText
    private lateinit var editSaveButton: Button
    private lateinit var progressBar: ProgressBar

    private var imageUri: Uri? = null
    private var isEditMode = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            profileImageView.setImageURI(imageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profilefragmentlayout, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI
        profileImageView = view.findViewById(R.id.iv_profile_picture)
        nameTextView = view.findViewById(R.id.tv_user_name)
        emailTextView = view.findViewById(R.id.tv_user_email)
        nameEditText = view.findViewById(R.id.et_user_name)
        editSaveButton = view.findViewById(R.id.btn_edit_save_profile)
        progressBar = view.findViewById(R.id.profile_progress_bar)
        val changePhotoButton = view.findViewById<TextView>(R.id.tv_change_photo)

        loadUserProfile()

        editSaveButton.setOnClickListener {
            toggleEditMode()
        }

        changePhotoButton.setOnClickListener {
            openGallery()
        }

        return view
    }

    private fun loadUserProfile() {
        setLoading(true)
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: ""
                        val email = document.getString("email") ?: ""
                        val profileImageUrl = document.getString("profileImageUrl")

                        nameTextView.text = name
                        nameEditText.setText(name)
                        emailTextView.text = email

                        if (!profileImageUrl.isNullOrEmpty() && context != null) {
                            Glide.with(this).load(profileImageUrl).into(profileImageView)
                        }
                    }
                    setLoading(false)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                }
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        if (isEditMode) {
            // Switch to Edit Mode
            nameTextView.visibility = View.GONE
            nameEditText.visibility = View.VISIBLE
            editSaveButton.text = "Save Changes"
        } else {
            // Switch to View Mode (and save data)
            saveProfileChanges()
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    private fun saveProfileChanges() {
        val newName = nameEditText.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        if (imageUri != null) {
            // If a new image was selected, upload it first
            uploadProfilePicture(newName)
        } else {
            // If only the name was changed
            updateUserData(newName, null)
        }
    }

    private fun uploadProfilePicture(name: String) {
        val userId = auth.currentUser?.uid ?: return
        val fileName = "profile_pictures/$userId.jpg"
        val storageRef = storage.reference.child(fileName)

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateUserData(name, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
    }

    private fun updateUserData(name: String, imageUrl: String?) {
        val userId = auth.currentUser?.uid ?: return
        val userUpdates = mutableMapOf<String, Any>("name" to name)

        if (imageUrl != null) {
            userUpdates["profileImageUrl"] = imageUrl
        }

        db.collection("users").document(userId).update(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                setLoading(false)
                // Revert UI back to view mode
                nameTextView.text = name
                nameTextView.visibility = View.VISIBLE
                nameEditText.visibility = View.GONE
                editSaveButton.text = "Edit Profile"
                imageUri = null // Reset image URI
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
    }


    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}

