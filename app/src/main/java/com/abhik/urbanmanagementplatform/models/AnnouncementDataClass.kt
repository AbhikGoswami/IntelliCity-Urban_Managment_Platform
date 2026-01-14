package com.abhik.urbanmanagementplatform.models

data class Announcement(
    val title: String = "",
    val content: String = "",
    val status: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)