package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FuelViewModel(
    private val repository: FuelRepository,
    private val sharedPrefs: android.content.SharedPreferences? = null
) : ViewModel() {

    // Initial seeding & load remembered session
    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            
            // Check if remember login is enabled, and auto-authenticate the saved user
            sharedPrefs?.let { prefs ->
                val rememberEnabled = prefs.getBoolean("remember_session", false)
                if (rememberEnabled) {
                    val savedUsername = prefs.getString("saved_username", null)
                    if (!savedUsername.isNullOrBlank()) {
                        val user = repository.getUserByUsername(savedUsername)
                        if (user != null && user.isApproved) {
                            _currentUser.value = user
                        }
                    }
                }
            }
        }
    }

    // Live Personnel List
    val personnelList: StateFlow<List<Personnel>> = repository.allPersonnel
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Live Fuel Inventory Levels
    val fuelStockLevel: StateFlow<Map<FuelType, Double>> = repository.fuelStockLevel
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FuelType.entries.associateWith { 0.0 }
        )

    // Users state
    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Initial stock configurations
    val allInitialStocks: StateFlow<List<InitialStock>> = repository.allInitialStocks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    sealed interface LoginResult {
        object Success : LoginResult
        object UserNotFound : LoginResult
        object IncorrectPassword : LoginResult
        object NotApproved : LoginResult
    }

    sealed interface RegisterResult {
        object Success : RegisterResult
        object UsernameExists : RegisterResult
        object Error : RegisterResult
    }

    fun loginUser(username: String, pwhash: String, rememberMe: Boolean, onResult: (LoginResult) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username.trim())
            if (user == null) {
                onResult(LoginResult.UserNotFound)
            } else if (user.passwordHash != pwhash) {
                onResult(LoginResult.IncorrectPassword)
            } else if (!user.isApproved) {
                onResult(LoginResult.NotApproved)
            } else {
                _currentUser.value = user
                
                // Save session preference
                sharedPrefs?.edit()?.apply {
                    putBoolean("remember_session", rememberMe)
                    if (rememberMe) {
                        putString("saved_username", user.username)
                    } else {
                        remove("saved_username")
                    }
                    apply()
                }
                
                onResult(LoginResult.Success)
            }
        }
    }

    fun loginUser(username: String, pwhash: String, onResult: (LoginResult) -> Unit) {
        loginUser(username, pwhash, rememberMe = false, onResult = onResult)
    }

    fun registerUser(username: String, pwhash: String, name: String, role: String, department: String = "", onResult: (RegisterResult) -> Unit) {
        if (username.isBlank() || pwhash.isBlank() || name.isBlank() || department.isBlank()) {
            onResult(RegisterResult.Error)
            return
        }
        viewModelScope.launch {
            val existing = repository.getUserByUsername(username.trim())
            if (existing != null) {
                onResult(RegisterResult.UsernameExists)
            } else {
                val newUser = User(
                    username = username.trim(),
                    passwordHash = pwhash,
                    role = role,
                    isApproved = false, // Needs admin approval to log in
                    name = name.trim(),
                    department = department.trim()
                )
                repository.insertUser(newUser)
                onResult(RegisterResult.Success)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        sharedPrefs?.edit()?.apply {
            putBoolean("remember_session", false)
            remove("saved_username")
            apply()
        }
    }

    fun approveUser(username: String) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            if (user != null) {
                repository.updateUser(user.copy(isApproved = true))
            }
        }
    }

    fun deleteUser(username: String) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            if (user != null) {
                repository.deleteUser(user)
            }
        }
    }

    fun saveInitialStock(fuelType: FuelType, amount: Double) {
        viewModelScope.launch {
            repository.updateInitialStock(fuelType.name, amount)
        }
    }

    // Selected Date for reporting (Default today "YYYY-MM-DD")
    private val _selectedDate = MutableStateFlow(FuelRepository.getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Daily transactions filtered list
    val allTransactions: StateFlow<List<FuelTransaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Transactions of the selected date
    val selectedDateTransactions: StateFlow<List<FuelTransaction>> = combine(allTransactions, _selectedDate) { transactions, date ->
        transactions.filter { it.dateString == date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Daily aggregated summary (Intake, Dispense, count per fuel type) for the selected date
    @OptIn(ExperimentalCoroutinesApi::class)
    val dailySummaryList: StateFlow<List<FuelDailySummary>> = _selectedDate
        .flatMapLatest { date ->
            repository.getDailySummary(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FuelType.entries.map { FuelDailySummary(it, 0.0, 0.0, 0) }
        )

    // Actions
    fun changeSelectedDate(dateString: String) {
        _selectedDate.value = dateString
    }

    // Receive fuel into system (Intake)
    fun receiveFuel(fuelType: FuelType, amount: Double, notes: String) {
        viewModelScope.launch {
            val transaction = FuelTransaction(
                dateString = FuelRepository.getCurrentDateString(),
                type = "INTAKE",
                fuelType = fuelType.name,
                liters = amount,
                notes = notes.trim().ifEmpty { "รับน้ำมันเข้าคลังปกติ" }
            )
            repository.insertTransaction(transaction)
        }
    }

    // Dispense fuel to a person
    fun dispenseFuel(fuelType: FuelType, amount: Double, personName: String, notes: String): Boolean {
        // Prevent dispensing more than currently in stock (simple check)
        val currentStock = fuelStockLevel.value[fuelType] ?: 0.0
        if (currentStock < amount) {
            return false // Insufficient stock
        }
        
        // Lookup the person's department/unit
        val dept = personnelList.value.firstOrNull { it.name.trim() == personName.trim() }?.department ?: ""
        
        viewModelScope.launch {
            val transaction = FuelTransaction(
                dateString = FuelRepository.getCurrentDateString(),
                type = "DISPENSE",
                fuelType = fuelType.name,
                liters = amount,
                personName = personName,
                notes = notes.trim().ifEmpty { "จ่ายออกยานพาหนะ" },
                department = dept
            )
            repository.insertTransaction(transaction)
        }
        return true
    }

    // Request dispense fuel (Operator places a pending order)
    fun requestDispenseFuel(fuelType: FuelType, amount: Double, personName: String, notes: String, department: String): Boolean {
        viewModelScope.launch {
            val transaction = FuelTransaction(
                dateString = FuelRepository.getCurrentDateString(),
                type = "PENDING_DISPENSE", // Special status
                fuelType = fuelType.name,
                liters = amount,
                personName = personName,
                notes = notes.trim().ifEmpty { "ใบสั่งจ่ายรอการดำเนินการ" },
                department = department
            )
            repository.insertTransaction(transaction)
        }
        return true
    }

    // Confirm a pending dispense request and execute it
    fun confirmPendingDispense(transaction: FuelTransaction, dispenserName: String): Boolean {
        val fuelType = try {
            FuelType.valueOf(transaction.fuelType)
        } catch (e: Exception) {
            return false
        }
        
        val currentStock = fuelStockLevel.value[fuelType] ?: 0.0
        if (currentStock < transaction.liters) {
            return false // Insufficient central stock at execution time
        }

        viewModelScope.launch {
            val updatedNotes = if (transaction.notes?.contains("จ่ายโดย") == true) {
                transaction.notes
            } else {
                "${transaction.notes ?: ""} (อนุมัติจ่ายโดย: $dispenserName)"
            }
            
            val confirmedTransaction = transaction.copy(
                type = "DISPENSE",
                timestamp = System.currentTimeMillis(), // update timestamp to actual dispatch time
                notes = updatedNotes
            )
            repository.insertTransaction(confirmedTransaction)
        }
        return true
    }

    // Intake/Allocate fuel to a specific unit (Department)
    fun receiveFuelForUnit(fuelType: FuelType, amount: Double, department: String, notes: String) {
        viewModelScope.launch {
            val transaction = FuelTransaction(
                dateString = FuelRepository.getCurrentDateString(),
                type = "INTAKE",
                fuelType = fuelType.name,
                liters = amount,
                notes = notes.trim().ifEmpty { "นำเข้าน้ำมันให้หน่วย" },
                department = department.trim()
            )
            repository.insertTransaction(transaction)
        }
    }

    // Delete transaction log
    fun deleteTransaction(transaction: FuelTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Personnel Actions
    fun addPersonnel(name: String, department: String, phone: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val p = Personnel(
                name = name.trim(),
                department = department.trim(),
                phone = phone.trim()
            )
            repository.insertPersonnel(p)
        }
    }

    fun removePersonnel(personnel: Personnel) {
        viewModelScope.launch {
            repository.deletePersonnel(personnel)
        }
    }

    // Formatted Thai version of selected date
    fun getSelectedDateThai(): String {
        return FuelRepository.formatDateThai(_selectedDate.value)
    }

    // Simple factory class for ViewModel
    class Factory(
        private val repository: FuelRepository,
        private val sharedPrefs: android.content.SharedPreferences? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FuelViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FuelViewModel(repository, sharedPrefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
