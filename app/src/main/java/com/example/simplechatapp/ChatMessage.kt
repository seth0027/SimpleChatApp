package com.example.simplechatapp

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatMessage(
     val text: String?,
     val name: String,
     val photoUrl: String?
)
