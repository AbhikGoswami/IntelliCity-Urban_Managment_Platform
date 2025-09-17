package com.abhik.urbanmanagementplatform.models

import com.google.firebase.Timestamp
import java.util.Date

// This data class represents a single grievance record from Firestore.
data class Grievance(
    val id: String = "", // Document ID
    val title: String = "",
    val department: String = "",
    val description: String = "",
    val status: String = "Submitted",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp(Date()) // Use Firebase's Timestamp for easier querying
)
