package com.abhishek.inkora.domain.model

data class Folder(
    val id: Long = 0L,
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
