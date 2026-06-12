package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.FuelRepository
import com.example.ui.screens.FuelTrackerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FuelViewModel

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Database & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = FuelRepository(
            personnelDao = database.personnelDao(),
            fuelTransactionDao = database.fuelTransactionDao(),
            initialStockDao = database.initialStockDao(),
            userDao = database.userDao()
        )
        
        val sharedPreferences = getSharedPreferences("fuel_tracker_prefs", MODE_PRIVATE)
        
        // Initialize ViewModel using Factory
        val viewModel: FuelViewModel by viewModels {
            FuelViewModel.Factory(repository, sharedPreferences)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    FuelTrackerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
