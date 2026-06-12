package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonnelDao {
    @Query("SELECT * FROM personnel ORDER BY name ASC")
    fun getAllPersonnel(): Flow<List<Personnel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonnel(personnel: Personnel): Long

    @Update
    suspend fun updatePersonnel(personnel: Personnel)

    @Delete
    suspend fun deletePersonnel(personnel: Personnel)
}

@Dao
interface FuelTransactionDao {
    @Query("SELECT * FROM fuel_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<FuelTransaction>>

    @Query("SELECT * FROM fuel_transactions WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getTransactionsByDate(dateString: String): Flow<List<FuelTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FuelTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: FuelTransaction)
}

@Dao
interface InitialStockDao {
    @Query("SELECT * FROM initial_stocks")
    fun getAllInitialStocks(): Flow<List<InitialStock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInitialStock(stock: InitialStock)

    @Query("SELECT * FROM initial_stocks WHERE fuelType = :fuelType")
    suspend fun getInitialStockByFuelType(fuelType: String): InitialStock?
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}
