package com.abhik.urbanmanagementplatform.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import java.util.Date

// This data class represents a single grievance record from Firestore.
data class Grievance(
    val id: String = "", // Document ID
    val title: String = "",
    val department: String = "",
    val description: String = "",
    val status: String = "Submitted",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp(Date()), // Use Firebase's Timestamp for easier querying


    val location: Map<String, Double>? = null
)

//package com.abhik.urbanmanagementplatform.models
//
//import com.google.firebase.firestore.Exclude
//import java.util.Date
//
//// Make sure your class definition matches all the fields in Firestore
//data class Grievance(
//    // These properties likely already exist
//    val title: String? = null,
//    val description: String? = null,
//    val department: String? = null,
//    val imageUrl: String? = null,
//    val timestamp: Date? = null,
//    val status: String? = null,
//
//    // <<< ADD THESE TWO MISSING PROPERTIES >>>
//    val userId: String? = null,
//    val location: Map<String, Double>? = null // Firestore's nested map becomes a Map<>
//) {
//    // This part from your fragment logic: .copy(id = document.id)
//    // implies you are setting the ID separately, so this @Exclude is good practice.
//    @get:Exclude var id: String = ""
//}
