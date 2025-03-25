package com.attentive.example2.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(@PrimaryKey val email: String, val password: String, val signedIn: Boolean = false)
