package com.example.simplechatapp

data class ChatMessage(
    private val text: String,
    private val name: String,
    private val photoUrl: String?
)
