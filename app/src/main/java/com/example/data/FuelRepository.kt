package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FuelDailySummary(
    val fuelType: FuelType,
    val intakeLiters: Double,
    val dispenseLiters: Double,
    val transactionCount: Int
)

class FuelRepository(
    private val personnelDao: PersonnelDao,
    private val fuelTransactionDao: FuelTransactionDao,
    private val initialStockDao: InitialStockDao,
    private val userDao: UserDao
) {
    val allPersonnel: Flow<List<Personnel>> = personnelDao.getAllPersonnel()
    val allTransactions: Flow<List<FuelTransaction>> = fuelTransactionDao.getAllTransactions()
    val allInitialStocks: Flow<List<InitialStock>> = initialStockDao.getAllInitialStocks()
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    // Live Inventory Level (Calculated on-the-fly reactively, combined with database initial stocks)
    val fuelStockLevel: Flow<Map<FuelType, Double>> = combine(
        allTransactions,
        allInitialStocks
    ) { transactions, initialStocks ->
        val stock = mutableMapOf<FuelType, Double>()
        // Initialize with default or configured initial stock from database
        FuelType.entries.forEach { fType ->
            val matchingInit = initialStocks.firstOrNull { it.fuelType == fType.name }
            stock[fType] = matchingInit?.liters ?: 0.0
        }
        
        transactions.forEach { trans ->
            val fuelType = try {
                FuelType.valueOf(trans.fuelType)
            } catch (e: Exception) {
                null
            }
            if (fuelType != null) {
                val current = stock[fuelType] ?: 0.0
                if (trans.type == "INTAKE") {
                    stock[fuelType] = current + trans.liters
                } else if (trans.type == "DISPENSE") {
                    // Standard inventory depletion
                    stock[fuelType] = current - trans.liters
                }
            }
        }
        stock
    }

    // Helper to get daily statistics for a specific YYYY-MM-DD
    fun getDailySummary(dateString: String): Flow<List<FuelDailySummary>> {
        return allTransactions.map { transactions ->
            val dayTrans = transactions.filter { it.dateString == dateString }
            FuelType.entries.map { type ->
                val typeTrans = dayTrans.filter { it.fuelType == type.name }
                val intake = typeTrans.filter { it.type == "INTAKE" }.sumOf { it.liters }
                val dispense = typeTrans.filter { it.type == "DISPENSE" }.sumOf { it.liters }
                FuelDailySummary(
                    fuelType = type,
                    intakeLiters = intake,
                    dispenseLiters = dispense,
                    transactionCount = typeTrans.size
                )
            }
        }
    }

    // Insert new transaction
    suspend fun insertTransaction(transaction: FuelTransaction): Long {
        return fuelTransactionDao.insertTransaction(transaction)
    }

    // Delete transaction
    suspend fun deleteTransaction(transaction: FuelTransaction) {
        fuelTransactionDao.deleteTransaction(transaction)
    }

    // Add personnel
    suspend fun insertPersonnel(personnel: Personnel): Long {
        return personnelDao.insertPersonnel(personnel)
    }

    // Delete personnel
    suspend fun deletePersonnel(personnel: Personnel) {
        personnelDao.deletePersonnel(personnel)
    }

    // Helper helper to format current date to YYYY-MM-DD
    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getFormattedTodayThai(): String {
            val sdf = SimpleDateFormat("d MMMM yyyy", Locale("th", "TH"))
            return sdf.format(Date())
        }

        fun formatDateThai(dateString: String): String {
            return try {
                val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputSdf.parse(dateString) ?: Date()
                val outputSdf = SimpleDateFormat("d MMMM yyyy", Locale("th", "TH"))
                outputSdf.format(date)
            } catch (e: Exception) {
                dateString
            }
        }
    }

    // Seed Initial Data if database is currently empty
    suspend fun seedInitialDataIfEmpty() {
        val currentPersonnel = allPersonnel.first()
        val currentTransactions = allTransactions.first()
        
        if (currentPersonnel.isEmpty()) {
            val p1 = Personnel(name = "สมชาย ทองดี", department = "ฝ่ายขนส่งทางบก", phone = "081-234-5678")
            val p2 = Personnel(name = "กิตติพงษ์ แก้วมณี", department = "ฝ่ายบริการยานพาหนะ", phone = "089-876-5432")
            val p3 = Personnel(name = "สุชาดา รัตนวิจิตร", department = "ฝ่ายยุทธบริการทางอากาศ", phone = "082-345-6789")
            
            insertPersonnel(p1)
            insertPersonnel(p2)
            insertPersonnel(p3)
        }

        if (currentTransactions.isEmpty()) {
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // Format for today
            val todayDateStr = sdf.format(calendar.time)
            
            // Format for yesterday
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayDateStr = sdf.format(calendar.time)
            val yesterdayTime = calendar.timeInMillis
            
            // Revert back and format for today
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val todayTime = calendar.timeInMillis

            // 1. Initial Feed / Intake
            insertTransaction(
                FuelTransaction(
                    timestamp = yesterdayTime - 3600000,
                    dateString = yesterdayDateStr,
                    type = "INTAKE",
                    fuelType = FuelType.GASOHOL_95.name,
                    liters = 2000.0,
                    notes = "เติมปริมาณสำรองประจำเดือน"
                )
            )
            insertTransaction(
                FuelTransaction(
                    timestamp = yesterdayTime - 3600000 + 1000,
                    dateString = yesterdayDateStr,
                    type = "INTAKE",
                    fuelType = FuelType.GASOHOL_91.name,
                    liters = 1500.0,
                    notes = "เติมปริมาณสำรองประจำเดือน"
                )
            )
            insertTransaction(
                FuelTransaction(
                    timestamp = yesterdayTime - 3600000 + 2000,
                    dateString = yesterdayDateStr,
                    type = "INTAKE",
                    fuelType = FuelType.DIESEL.name,
                    liters = 5000.0,
                    notes = "รับน้ำมันดิสทริบิวเตอร์ ปั๊มหลัก"
                )
            )
            insertTransaction(
                FuelTransaction(
                    timestamp = yesterdayTime - 3600000 + 3000,
                    dateString = yesterdayDateStr,
                    type = "INTAKE",
                    fuelType = FuelType.JP_8.name,
                    liters = 10000.0,
                    notes = "คาร์โก้ทางอากาศเติมถังหลัก"
                )
            )

            // 2. Yesterday's Dispensation
            insertTransaction(
                FuelTransaction(
                    timestamp = yesterdayTime + 1800000,
                    dateString = yesterdayDateStr,
                    type = "DISPENSE",
                    fuelType = FuelType.GASOHOL_95.name,
                    liters = 120.0,
                    personName = "สมชาย ทองดี",
                    notes = "รถกระบะขนส่งทะเบียน ลร-4412"
                )
            )
            insertTransaction(
                FuelTransaction(
                    timestamp = yesterdayTime + 3600000,
                    dateString = yesterdayDateStr,
                    type = "DISPENSE",
                    fuelType = FuelType.DIESEL.name,
                    liters = 450.0,
                    personName = "กิตติพงษ์ แก้วมณี",
                    notes = "รถบัสรับส่งข้าราชการ ทะเบียน 3ก-9988"
                )
            )

            // 3. Today's Transactions
            insertTransaction(
                FuelTransaction(
                    timestamp = todayTime - 1800000,
                    dateString = todayDateStr,
                    type = "INTAKE",
                    fuelType = FuelType.GASOHOL_95.name,
                    liters = 500.0,
                    notes = "เติมถังด่วนพิเศษ"
                )
            )
            insertTransaction(
                FuelTransaction(
                    timestamp = todayTime,
                    dateString = todayDateStr,
                    type = "DISPENSE",
                    fuelType = FuelType.JP_8.name,
                    liters = 2500.0,
                    personName = "สุชาดา รัตนวิจิตร",
                    notes = "เติม ฮ.กู้ภัย รหัส H-122"
                )
            )
        }

        // Seed default initial stocks if empty
        val currentInitialStocks = allInitialStocks.first()
        if (currentInitialStocks.isEmpty()) {
            initialStockDao.insertInitialStock(InitialStock(FuelType.GASOHOL_95.name, 2000.0))
            initialStockDao.insertInitialStock(InitialStock(FuelType.GASOHOL_91.name, 1500.0))
            initialStockDao.insertInitialStock(InitialStock(FuelType.DIESEL.name, 5000.0))
            initialStockDao.insertInitialStock(InitialStock(FuelType.JP_8.name, 10000.0))
        }

        // Seed default users if empty (admin and test users)
        val currentUsers = allUsers.first()
        if (currentUsers.isEmpty()) {
            userDao.insertUser(User("admin", "admin", "ADMIN", true, "ผู้ดูแลระบบ (Admin)", "กองบังคับการ"))
            userDao.insertUser(User("user", "user", "USER", true, "เจ้าหน้าที่คลังน้ำมัน", "ฝ่ายซ่อมบำรุงและสนับสนุน"))
            userDao.insertUser(User("test", "test", "USER", false, "ผู้ลงทะเบียนใหม่ (รออนุมัติ)", "ฝ่ายยุทธบริการทางอากาศ"))
        }
    }

    suspend fun updateInitialStock(fuelType: String, liters: Double) {
        initialStockDao.insertInitialStock(InitialStock(fuelType, liters))
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }
}
