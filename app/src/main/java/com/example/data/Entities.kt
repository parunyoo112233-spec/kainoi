package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personnel")
data class Personnel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val department: String = "",
    val phone: String = ""
)

@Entity(tableName = "fuel_transactions")
data class FuelTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // format "YYYY-MM-DD" matching local date for daily reporting
    val type: String, // "INTAKE" (รับเข้า) or "DISPENSE" (จ่ายออก)
    val fuelType: String, // String representation of FuelType enum name
    val liters: Double,
    val personName: String? = null, // Who drew or authorized it
    val notes: String? = null, // e.g., vehicle plate, purpose
    val department: String = "" // Under what unit/department this belongs to (empty for central stock)
)

@Entity(tableName = "initial_stocks")
data class InitialStock(
    @PrimaryKey val fuelType: String, // FuelType enum name (GASOHOL_95, etc.)
    val liters: Double
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val passwordHash: String, // plain password for local offline usage
    val role: String, // "USER" or "ADMIN"
    val isApproved: Boolean = false,
    val name: String = "",
    val department: String = ""
)
