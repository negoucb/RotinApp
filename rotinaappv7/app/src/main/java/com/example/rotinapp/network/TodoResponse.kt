package com.example.rotinapp.network

data class TodoResponse(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean
)