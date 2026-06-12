package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.viewmodel.FuelViewModel
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTrackerScreen(
    viewModel: FuelViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab = remember { mutableStateOf(0) } // 0 = Inventory, 1 = History/Report, 2 = Personnel
    
    val fuelStock by viewModel.fuelStockLevel.collectAsStateWithLifecycle()
    val selectedDateTransactions by viewModel.selectedDateTransactions.collectAsStateWithLifecycle()
    val dailySummaries by viewModel.dailySummaryList.collectAsStateWithLifecycle()
    val personnelList by viewModel.personnelList.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var showIntakeDialog by remember { mutableStateOf(false) }
    var showDispenseDialog by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    
    // For warnings
    var snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    if (currentUser == null) {
        LoginRegisterScreen(viewModel = viewModel, snackbarHostState = snackbarHostState)
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Fuel Tracker Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ระบบควบคุม สป.3 มทบ.44",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                },
                actions = {
                    if (currentUser != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = currentUser?.name ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentUser?.role == "ADMIN") 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = when (currentUser?.role) {
                                        "ADMIN" -> "ADMIN"
                                        "OPERATOR" -> "OPERATOR"
                                        else -> "USER"
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            IconButton(
                                onClick = { 
                                    viewModel.logout() 
                                    currentTab.value = 0
                                },
                                modifier = Modifier.size(36.dp).testTag("logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "ออกจากระบบ",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab.value == 0,
                    onClick = { currentTab.value = 0 },
                    icon = { 
                        Icon(
                            imageVector = when (currentUser?.role) {
                                "USER" -> Icons.Default.LocalGasStation
                                "OPERATOR" -> Icons.Default.LocalGasStation
                                else -> Icons.Default.Home
                            }, 
                            contentDescription = when (currentUser?.role) {
                                "USER" -> "จ่ายน้ำมัน"
                                "OPERATOR" -> "น้ำมันคงเหลือสังกัด"
                                else -> "คลังน้ำมัน"
                            }
                        ) 
                    },
                    label = { 
                        Text(
                            text = when (currentUser?.role) {
                                "USER" -> "จ่ายน้ำมัน"
                                "OPERATOR" -> "น้ำมันของหน่วย"
                                else -> "คลังน้ำมัน"
                            }, 
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    modifier = Modifier.testTag("tab_inventory")
                )
                NavigationBarItem(
                    selected = currentTab.value == 1,
                    onClick = { currentTab.value = 1 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "รายงานประจำวัน") },
                    label = { Text(if (currentUser?.role == "OPERATOR") "รายงานประจำหน่วย" else "รายงานประจำวัน", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("tab_reports")
                )
                if (currentUser?.role == "ADMIN") {
                    NavigationBarItem(
                        selected = currentTab.value == 2,
                        onClick = { currentTab.value = 2 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "หน่วย/บุคคล") },
                        label = { Text("หน่วย/บุคคล", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("tab_personnel")
                    )
                }
                if (currentUser?.role == "ADMIN") {
                    NavigationBarItem(
                        selected = currentTab.value == 3,
                        onClick = { currentTab.value = 3 },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "จัดการระบบ") },
                        label = { Text("จัดการระบบ", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("tab_admin")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentUser?.role != "OPERATOR") {
                // Contextual FAB depending on active tab
                when (currentTab.value) {
                    0 -> {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                        ) {
                            // Receiving fuel
                            if (currentUser?.role != "USER") {
                                FloatingActionButton(
                                    onClick = { showIntakeDialog = true },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("fab_intake")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "รับน้ำมันเข้า")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("รับเข้าคลัง")
                                    }
                                }
                            }

                            // Dispensing fuel
                            FloatingActionButton(
                                onClick = { showDispenseDialog = true },
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.testTag("fab_dispense")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocalGasStation, contentDescription = "เบิกจ่ายน้ำมัน")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("เบิกจ่าย")
                                }
                            }
                        }
                    }
                    2 -> {
                        FloatingActionButton(
                            onClick = { showAddPersonDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_add_person")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "เลือกหน่วย/บุคคล")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("เลือกหน่วย/บุคคล")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab.value) {
                0 -> {
                    if (currentUser?.role == "OPERATOR") {
                        OperatorInventoryDashboard(
                            currentUser = currentUser!!,
                            department = currentUser?.department ?: "",
                            allTransactions = allTransactions,
                            personnelList = personnelList,
                            fuelStock = fuelStock,
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState
                        )
                    } else if (currentUser?.role == "USER") {
                        UserDispenseDashboard(
                            currentUser = currentUser!!,
                            personnelList = personnelList,
                            fuelStock = fuelStock,
                            allTransactions = allTransactions,
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState
                        )
                    } else {
                        InventoryDashboard(
                            fuelStock = fuelStock,
                            personnelList = personnelList,
                            onIntakeClick = { showIntakeDialog = true },
                            onDispenseClick = { showDispenseDialog = true },
                            onViewPersonnelTab = { currentTab.value = 2 }
                        )
                    }
                }
                1 -> {
                    if (currentUser?.role == "OPERATOR") {
                        OperatorDailyReportView(
                            department = currentUser?.department ?: "",
                            selectedDate = selectedDate,
                            allTransactions = allTransactions,
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState
                        )
                    } else {
                        DailyReportView(
                            selectedDate = selectedDate,
                            transactions = selectedDateTransactions,
                            summaries = dailySummaries,
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState
                        )
                    }
                }
                2 -> if (currentUser?.role == "ADMIN") {
                    PersonnelView(
                        personnelList = personnelList,
                        onDeletePerson = { viewModel.removePersonnel(it) },
                        allTransactions = viewModel.allTransactions.collectAsStateWithLifecycle().value
                    )
                }
                3 -> if (currentUser?.role == "ADMIN") {
                    AdminDashboardView(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showIntakeDialog) {
        IntakeDialog(
            onDismiss = { showIntakeDialog = false },
            onSubmit = { type, amount, notes ->
                viewModel.receiveFuel(type, amount, notes)
                showIntakeDialog = false
            }
        )
    }

    if (showDispenseDialog) {
        DispenseDialog(
            personnelList = personnelList,
            fuelStock = fuelStock,
            onDismiss = { showDispenseDialog = false },
            onSubmit = { type, amount, personName, notes ->
                val success = viewModel.dispenseFuel(type, amount, personName, notes)
                if (success) {
                    showDispenseDialog = false
                }
                success
            }
        )
    }

    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onSubmit = { name, bld, phone ->
                viewModel.addPersonnel(name, bld, phone)
                showAddPersonDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------
// TAB 1: INVENTORY DASHBOARD - HIGH DENSITY THEME
// -----------------------------------------------------------------
@Composable
fun InventoryDashboard(
    fuelStock: Map<FuelType, Double>,
    personnelList: List<Personnel>,
    onIntakeClick: () -> Unit,
    onDispenseClick: () -> Unit,
    onViewPersonnelTab: () -> Unit
) {
    val df = DecimalFormat("#,##0.0")
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inventory_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Aggregated Total Reserve Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ปริมาณน้ำมันสำรองในคลังรวมทั้งหมด",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val totalLiters = fuelStock.values.sum()
                    Text(
                        text = "${df.format(totalLiters)} ลิตร",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "สถานะคลังน้ำมันหลักพร้อมทำงานในสภาวะหนาแน่น",
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // On-Duty Personnel Panel (Interactive & Adaptive High Density style)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "หน่วย/บุคคลผู้รับบริการ (On-Duty)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ดูทั้งหมด",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onViewPersonnelTab() }
                        )
                    }

                    if (personnelList.isEmpty()) {
                        Text(
                            text = "ยังไม่มีรายชื่อหน่วยหรือบุคคลลงทะเบียนในระบบ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(personnelList) { person ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { onViewPersonnelTab() }
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                                            .border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = person.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = person.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Detailed Fuel Types (Gauges Removed)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "สถานะแยกตามประเภทน้ำมัน",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 2-Column High Density Grid of Stocks without Gauges
        val chunkedFuelTypes = FuelType.entries.toList().chunked(2)
        items(chunkedFuelTypes) { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPair.forEach { fuelType ->
                    val liters = fuelStock[fuelType] ?: 0.0
                    // Define standard capacity logic for presentation (e.g. 5,000 to 15,000 Liters Max)
                    val maxCapacity = when(fuelType) {
                        FuelType.GASOHOL_95 -> 5000.0
                        FuelType.GASOHOL_91 -> 5000.0
                        FuelType.DIESEL -> 10000.0
                        FuelType.JP_8 -> 15000.0
                    }
                    val fillRatio = (liters / maxCapacity).toFloat().coerceIn(0f, 1f)
                    val themeColor = Color(android.graphics.Color.parseColor(fuelType.colorHex))
                    val isLowStock = fillRatio < 0.2f

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        // Classic Compact Linear Progress Bar View
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fuelType.name.replace("_", " "),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = when(fuelType) {
                                        FuelType.GASOHOL_95, FuelType.GASOHOL_91 -> Icons.Default.LocalGasStation
                                        FuelType.DIESEL -> Icons.Default.AirportShuttle
                                        FuelType.JP_8 -> Icons.Default.Flight
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            val displayLiters = if (liters >= 1000.0) {
                                "${df.format(liters / 1000.0)}k ลิตร"
                            } else {
                                "${df.format(liters)} ลิตร"
                            }
                            
                            Text(
                                text = displayLiters,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fillRatio)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isLowStock) MaterialTheme.colorScheme.error else themeColor)
                                )
                            }

                            Text(
                                text = "${(fillRatio * 100).toInt()}% Capacity",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (rowPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// COMPONENT: HIGH DENSITY RADIAL DIAL GAUGE
// -----------------------------------------------------------------
@Composable
fun CircularGauge(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 22f
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Background arc track (260-degree sweep from 140 degrees)
            drawArc(
                color = color.copy(alpha = 0.12f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            // Foreground active progress arc (dynamic 260-degree sweep)
            drawArc(
                color = color,
                startAngle = 140f,
                sweepAngle = 260f * progress,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = "CAPACITY",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// -----------------------------------------------------------------
// TAB 2: DAILY REPORT VIEW
// -----------------------------------------------------------------
fun generateCsvString(
    selectedDate: String,
    transactions: List<FuelTransaction>,
    summaries: List<FuelDailySummary>,
    viewModel: FuelViewModel
): String {
    val fuelStock = viewModel.fuelStockLevel.value
    val initialStocks = viewModel.allInitialStocks.value
    val currentUser = viewModel.currentUser.value

    val csvBuilder = StringBuilder()
    // Title block (Thai and English)
    csvBuilder.append("รายงานคลังน้ำมันและรายการเดินบัญชีประจำวัน (Daily Fuel Inventory and Consumption Report)\n")
    csvBuilder.append("ประจำวันที่ (Date),${selectedDate}\n")
    csvBuilder.append("ผู้พิมพ์รายงาน (Exporter),\"${currentUser?.name ?: "Unknown"} (@${currentUser?.username ?: "unknown"})\"\n")
    csvBuilder.append("เวลาที่ส่งออก (Export Time),${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
    csvBuilder.append("\n")

    // Section 1: Daily flow totals
    csvBuilder.append("สรุปยอดการไหลเวียนน้ำมันประจำวัน (Daily Fuel Circulation Summary)\n")
    csvBuilder.append("ประเภทน้ำมัน (Fuel Type),รับเข้าคลังรวม (Intake Liters),เบิกจ่ายออกจากคลังรวม (Dispense Liters),จำนวนรายการธุรกรรม (Transaction Count)\n")
    summaries.forEach { summary ->
        csvBuilder.append("${summary.fuelType.displayName},${summary.intakeLiters},${summary.dispenseLiters},${summary.transactionCount}\n")
    }
    val totalIntake = summaries.sumOf { it.intakeLiters }
    val totalDispense = summaries.sumOf { it.dispenseLiters }
    val totalCount = summaries.sumOf { it.transactionCount }
    csvBuilder.append("รวมทั้งหมด (Grand Total),${totalIntake},${totalDispense},${totalCount}\n")
    csvBuilder.append("\n")

    // Section 2: Real-time inventory status
    csvBuilder.append("สถานะระดับคลังน้ำมันสำรองปัจจุบัน (Current Live Inventory Status)\n")
    csvBuilder.append("ประเภทน้ำมัน (Fuel Type),ปริมาณน้ำมันคงคลังขั้นต้น (Baseline Start Liters),ยอดปริมาณการไหลเวียนสะสมสุทธิ (Cumulative Net Flow),ปริมาณคงคลังน้ำมันปัจจุบัน (Current Net Inventory Liters)\n")
    FuelType.entries.forEach { fType ->
        val baseline = initialStocks.firstOrNull { it.fuelType == fType.name }?.liters ?: 0.0
        val liveStock = fuelStock[fType] ?: 0.0
        val cumulativeDelta = liveStock - baseline
        csvBuilder.append("${fType.displayName},${baseline},${cumulativeDelta},${liveStock}\n")
    }
    csvBuilder.append("\n")

    // Section 3: Individual transactions log
    csvBuilder.append("บันทึกประวัติการเดินบัญชีน้ำมันรายรายการประจำวัน (Daily Transaction Details Log)\n")
    csvBuilder.append("ลำดับ (No.),ประเภทธุรกรรม (Transaction Type),ชนิดน้ำมัน (Fuel Type),ปริมาณน้ำมัน (Amount Liters),ผู้ดำเนินงาน/รับบริการ (Authorized Person),บันทึกตรวจสอบ (Notes)\n")
    if (transactions.isEmpty()) {
        csvBuilder.append("ไม่มีประวัติธุรกรรมสำหรับวันนี้ (No transaction records for this day)\n")
    } else {
        transactions.forEachIndexed { idx, trans ->
            val typeLabel = if (trans.type == "INTAKE") "รับเข้าคลัง (INTAKE)" else "เบิกจ่ายออกจากคลัง (DISPENSE)"
            val typeEnum = try { FuelType.valueOf(trans.fuelType) } catch (_: Exception) { null }
            val fTypeName = typeEnum?.displayName ?: trans.fuelType
            val personName = trans.personName ?: "-"
            val notes = trans.notes?.replace("\"", "\"\"")?.replace(",", " ")?.replace("\n", " ") ?: "-" // sanitize CSV delimiters
            csvBuilder.append("${idx + 1},${typeLabel},${fTypeName},${trans.liters},\"${personName}\",\"${notes}\"\n")
        }
    }
    return csvBuilder.toString()
}

@Composable
fun DailyReportView(
    selectedDate: String,
    transactions: List<FuelTransaction>,
    summaries: List<FuelDailySummary>,
    viewModel: FuelViewModel,
    snackbarHostState: SnackbarHostState
) {
    val df = DecimalFormat("#,##0.0")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State to hold compiled CSV string for the exporter
    val csvToExport = remember { mutableStateOf("") }
    
    // Setup standard Android storage document launcher for type CSV
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    java.io.OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        // Add UTF-8 BOM byte sequence so MS Excel doesn't garble Thai Unicode characters
                        writer.write('\uFEFF'.code)
                        writer.write(csvToExport.value)
                    }
                }
                scope.launch {
                    snackbarHostState.showSnackbar("💾 ส่งออกและบันทึกรายงาน CSV สำเร็จ!")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("❌ บันทึกล้มเหลว: ${e.localizedMessage}")
                }
            }
        }
    }
    
    // Date navigation calendar
    val calendar = remember { Calendar.getInstance() }
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Sync calendar with selected date
    LaunchedEffect(selectedDate) {
        try {
            val date = sdfDb.parse(selectedDate)
            if (date != null) {
                calendar.time = date
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_section")
    ) {
        // Date Switcher Header
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        viewModel.changeSelectedDate(sdfDb.format(calendar.time))
                    },
                    modifier = Modifier.testTag("prev_date_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "วันก่อนหน้า")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = viewModel.getSelectedDateThai(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        themeColor = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "รายงานสถิตประจำวัน",
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        viewModel.changeSelectedDate(sdfDb.format(calendar.time))
                    },
                    modifier = Modifier.testTag("next_date_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "วันถัดไป")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CSV Export Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ส่งออกรายงานประจำวัน (CSV)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "บันทึกสรุปบัญชีและรายการคงเหลือจริงระดับคลังเพื่อเก็บบันทึกถาวร",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                lineHeight = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val csvText = generateCsvString(
                                    selectedDate = selectedDate,
                                    transactions = transactions,
                                    summaries = summaries,
                                    viewModel = viewModel
                                )
                                csvToExport.value = csvText
                                csvLauncher.launch("fuel_report_${selectedDate}.csv")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "ส่งออกรายงาน", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ส่งออก CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Aggregated Summary for the selected date
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "สรุปยอดน้ำมัน เข้า-ออกประจำวัน",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val totalDayIntake = summaries.sumOf { it.intakeLiters }
                        val totalDayDispense = summaries.sumOf { it.dispenseLiters }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowDownward, 
                                        contentDescription = "เข้า", 
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("รับเข้าคลังรวม", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "${df.format(totalDayIntake)} ล.",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                            
                            Box(modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                .align(Alignment.CenterVertically)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowUpward, 
                                        contentDescription = "ออก", 
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("เบิกจ่ายรวม", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "${df.format(totalDayDispense)} ล.",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA580C)
                                )
                            }
                        }
                    }
                }
            }

            // Summaries list by fuel type
            item {
                Text(
                    text = "แยกตามรายชนิดน้ำมัน",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(summaries) { summary ->
                val color = Color(android.graphics.Color.parseColor(summary.fuelType.colorHex))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = summary.fuelType.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "เข้า: +${df.format(summary.intakeLiters)} ล.", 
                                fontSize = 12.sp, 
                                color = if (summary.intakeLiters > 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = if (summary.intakeLiters > 0) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "ออก: -${df.format(summary.dispenseLiters)} ล.", 
                                fontSize = 12.sp, 
                                color = if (summary.dispenseLiters > 0) Color(0xFFEA580C) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = if (summary.dispenseLiters > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Today's transaction Logs
            item {
                Text(
                    text = "รายการเดินบัญชีน้ำมัน (${transactions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "ไม่มีข้อมูล",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ไม่มีประวัติการเบิกจ่ายหรือรับเข้าในวันที่เลือก",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { trans ->
                    val fuelTypeEnum = try {
                        FuelType.valueOf(trans.fuelType)
                    } catch (_: Exception) {
                        FuelType.GASOHOL_95
                    }
                    val color = Color(android.graphics.Color.parseColor(fuelTypeEnum.colorHex))
                    val isIntake = trans.type == "INTAKE"
                    
                    val timeSdf = SimpleDateFormat("HH:mm น.", Locale.getDefault())
                    val timeFormatted = timeSdf.format(Date(trans.timestamp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left badge (Intake / Dispense)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isIntake) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIntake) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = if (isIntake) "รับเข้า" else "จ่ายออก",
                                    tint = if (isIntake) Color(0xFF15803D) else Color(0xFFC2410C),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isIntake) "รับเข้าคลัง" else "เบิกจ่ายออก",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isIntake) Color(0xFF15803D) else Color(0xFFC2410C)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(color.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = fuelTypeEnum.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                val personStr = trans.personName?.let { "ผู้รับ: $it" } ?: "บันทึกคลัง"
                                Text(
                                    text = personStr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                if (!trans.notes.isNullOrBlank()) {
                                    Text(
                                        text = "บันทึก: ${trans.notes}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Text(
                                    text = "เวลา: $timeFormatted",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            // Right: Liters amount & Action delete
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${if (isIntake) "+" else "-"}${df.format(trans.liters)} ล.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isIntake) Color(0xFF16A34A) else Color(0xFFEA580C)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                IconButton(
                                    onClick = { viewModel.deleteTransaction(trans) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "ลบประวัติ",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 3: PERSONNEL VIEW
// -----------------------------------------------------------------
@Composable
fun PersonnelView(
    personnelList: List<Personnel>,
    onDeletePerson: (Personnel) -> Unit,
    allTransactions: List<FuelTransaction>
) {
    val df = DecimalFormat("#,##0.0")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("personnel_section"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "หน่วย/บุคคล",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ทะเบียนข้อมูลหน่วย/บุคคลผู้รับบริการ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "บันทึกรายชื่อหน่วยงานหรือรายชื่อบุคคลเพื่อความสะดวกในการเข้าเบิก",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "บัญชีหน่วย/บุคคล (${personnelList.size} รายการ)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (personnelList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PersonOutline,
                                contentDescription = "ไม่มีข้อมูล",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "ไม่มีรายชื่อหน่วยหรือบุคคลในระบบในขณะนี้",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "แตะปุ่มด้านล่างขวาเพื่อเพิ่ม/เลือกหน่วย/บุคคล",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        } else {
            items(personnelList, key = { it.id }) { person ->
                // Calculate historical fuel consumption for this person
                val drawnTransactions = allTransactions.filter { 
                    it.type == "DISPENSE" && it.personName == person.name 
                }
                val totalDrawnLiters = drawnTransactions.sumOf { it.liters }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Icon Circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Details String
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = person.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            
                            if (person.department.isNotBlank()) {
                                Text(
                                    text = "หน่วยงาน: ${person.department}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (person.phone.isNotBlank()) {
                                Text(
                                    text = "โทร: ${person.phone}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Visual stats label
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = "เบิกจ่าย",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "เบิกไปแล้วรวม: ${df.format(totalDrawnLiters)} ลิตร (${drawnTransactions.size} ครั้ง)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Options Drawer or Trigger Action delete
                        IconButton(
                            onClick = { onDeletePerson(person) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "ลบข้อมูลบุคคล",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// MODIFIED TEXT WRAPPER FOR THAI
// -----------------------------------------------------------------
@Composable
fun Text(
    text: String,
    fontWeight: FontWeight? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    themeColor: Color? = null,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    androidx.compose.material3.Text(
        text = text,
        fontWeight = fontWeight,
        fontSize = fontSize,
        color = themeColor ?: Color.Unspecified,
        modifier = modifier,
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}

// -----------------------------------------------------------------
// DIALOG COMPONENT: ADD INTAKE (รับเข้าคลัง)
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeDialog(
    onDismiss: () -> Unit,
    onSubmit: (FuelType, Double, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(FuelType.GASOHOL_95) }
    var litersStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📥 บันทึกรับน้ำมันเข้าคลัง",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        themeColor = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "ปิด")
                    }
                }

                // Fuel Type Selection Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ชนิดน้ำมัน") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        FuelType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Liters Amount Field (Double)
                OutlinedTextField(
                    value = litersStr,
                    onValueChange = { 
                        litersStr = it
                        errorMsg = null 
                    },
                    label = { Text("ปริมาณ (ลิตร)") },
                    placeholder = { Text("เช่น 1000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("intake_amount_input"),
                    singleLine = true
                )

                // Memo Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("บันทึกช่วยจำ (เช่น เลขที่ใบส่งของ)") },
                    placeholder = { Text("ไม่ระบุได้") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        themeColor = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ยกเลิก")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        modifier = Modifier.testTag("submit_intake_button"),
                        onClick = {
                            val amount = litersStr.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                errorMsg = "กรุณากรอกตัวเลขจำนวนลิตรกว่า 0"
                            } else {
                                onSubmit(selectedType, amount, notes)
                            }
                        }
                    ) {
                        Text("บันทึกสำเร็จ")
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// DIALOG COMPONENT: DISPENSE OIL (เบิกจ่ายน้ำมัน)
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispenseDialog(
    personnelList: List<Personnel>,
    fuelStock: Map<FuelType, Double>,
    onDismiss: () -> Unit,
    onSubmit: (FuelType, Double, String, String) -> Boolean
) {
    var selectedType by remember { mutableStateOf(FuelType.GASOHOL_95) }
    var litersStr by remember { mutableStateOf("") }
    
    // Personnel selection autocomplete selection
    var personNameSelected by remember { mutableStateOf("") }
    var expandedPersonDropdown by remember { mutableStateOf(false) }
    var expandedTypeDropdown by remember { mutableStateOf(false) }
    
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val currentStock = fuelStock[selectedType] ?: 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⛽ ทำการเบิกจ่ายน้ำมัน",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        themeColor = MaterialTheme.colorScheme.tertiary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "ปิด")
                    }
                }

                // Display dynamic stock helper info
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalGasStation, 
                            contentDescription = "สต็อกปัจจุบัน", 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "คงเหลือในคลัง (${selectedType.displayName}): ${DecimalFormat("#,##0.0").format(currentStock)} ลิตร",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Fuel Type Selector Dropdown Menu
                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("เลือกชนิดน้ำมันที่เบิก") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        FuelType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Personnel Selector Autocomplete / Text Input
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = personNameSelected,
                        onValueChange = { 
                            personNameSelected = it
                            expandedPersonDropdown = true 
                            errorMsg = null
                        },
                        label = { Text("ผู้เบิกพลังงาน (เลือกหน่วย/บุคคล)") },
                        placeholder = { Text("พิมพ์หรือเลือกหน่วย/บุคคลจากรายการ") },
                        trailingIcon = { 
                            IconButton(onClick = { expandedPersonDropdown = !expandedPersonDropdown }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "เปิดรายชื่อ")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dispense_person_input"),
                        singleLine = true
                    )
                    
                    // Filter personnel based on text input helper
                    val filteredPersonnel = personnelList.filter { 
                        it.name.contains(personNameSelected, ignoreCase = true) 
                    }
                    
                    if (expandedPersonDropdown && (filteredPersonnel.isNotEmpty() || personnelList.isNotEmpty())) {
                        DropdownMenu(
                            expanded = expandedPersonDropdown,
                            onDismissRequest = { expandedPersonDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            val displayList = if (personNameSelected.isEmpty()) personnelList else filteredPersonnel
                            displayList.forEach { personnel ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(personnel.name, fontWeight = FontWeight.Bold)
                                            if (personnel.department.isNotBlank()) {
                                                Text(personnel.department, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        personNameSelected = personnel.name
                                        expandedPersonDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Liters Amount Field (Double)
                OutlinedTextField(
                    value = litersStr,
                    onValueChange = { 
                        litersStr = it
                        errorMsg = null 
                    },
                    label = { Text("จำนวนเบิก (ลิตร)") },
                    placeholder = { Text("เช่น 45.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dispense_amount_input"),
                    singleLine = true
                )

                // Memo Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("หมายเหตุ/ทะเบียนยานพาหนะ") },
                    placeholder = { Text("เช่น รถส่วนกลาง กข-1234") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        themeColor = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ยกเลิก")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        modifier = Modifier.testTag("submit_dispense_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        onClick = {
                            val amount = litersStr.toDoubleOrNull()
                            if (personNameSelected.isBlank()) {
                                errorMsg = "กรุณากรอกชื่อผู้เบิกน้ำมัน"
                            } else if (amount == null || amount <= 0.0) {
                                errorMsg = "กรุณากรอกจำนวนเบิกเป็นตัวเลขที่ถูกต้อง"
                            } else if (amount > currentStock) {
                                errorMsg = "ไม่สามารถเบิกได้เนื่องจากมีน้ำมันคงคลังไม่เพียงพอ"
                            } else {
                                val success = onSubmit(selectedType, amount, personNameSelected, notes)
                                if (!success) {
                                    errorMsg = "เกิดข้อผิดพลาดในการบันทึกหรือน้ำมันไม่พอคิววิเคราะห์"
                                }
                            }
                        }
                    ) {
                        Text("บันทึกการเบิก")
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// DIALOG COMPONENT: ADD PERSONNEL (เพิ่มข้อมูลบุคลากร)
// -----------------------------------------------------------------
@Composable
fun AddPersonDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "👤 เพิ่มข้อมูลหน่วย/บุคคลใหม่",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        themeColor = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "ปิด")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        errorMsg = null 
                    },
                    label = { Text("ชื่อหน่วย / ชื่อบุคคล") },
                    placeholder = { Text("พิมพ์ชื่อหน่วยงาน คลังย่อย หรือชื่อจริง-นามสกุล") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("person_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("แผนก / ฝ่าย / สังกัด / หน่วยงานหลัก") },
                    placeholder = { Text("เช่น กองร้อยฝ่ายสนับสนุน, แผนกบัญชี") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("เบอร์โทรศัพท์ติดต่อ (ถ้ามี)") },
                    placeholder = { Text("เช่น 08x-xxxxxxx หรือเบอร์โทรภายใน") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        themeColor = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ยกเลิก")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        modifier = Modifier.testTag("submit_person_button"),
                        onClick = {
                            if (name.isBlank()) {
                                errorMsg = "กรุณากรอกชื่อหน่วยหรือชื่อจริงของบุคคล"
                            } else {
                                onSubmit(name, department, phone)
                            }
                        }
                    ) {
                        Text("บันทึกข้อมูล")
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// AUTHENTICATION COMPONENT: LOGIN & SIGN REGISTER (เข้าสู่ระบบ / สมัครสมาชิก)
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    viewModel: FuelViewModel,
    snackbarHostState: SnackbarHostState
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("USER") } // "USER" or "ADMIN"
    var selectedDepartment by remember { mutableStateOf("") }
    var departmentDropdownExpanded by remember { mutableStateOf(false) }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            item {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape)
                        .padding(14.dp)
                )
            }
            
            item {
                Text(
                    text = "ระบบควบคุม สป.3 มทบ.44",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "กรุณาลงชื่อใช้งานเพื่อควบคุมคลังคงเหลือ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section Switcher Tab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { 
                                        isSignUpMode = false 
                                        errorMsg = null
                                        successMsg = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "เข้าสู่ระบบ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { 
                                        isSignUpMode = true 
                                        errorMsg = null
                                        successMsg = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "สมัครบัญชีใหม่",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))
                        
                        Text(
                            text = if (isSignUpMode) "กรอกข้อมูลสมัครใช้ระบบ" else "กรอกเอกสิทธิ์ล็อกอิน",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; errorMsg = null },
                            label = { Text("ชื่อผู้ใช้งาน (Username)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username")
                        )
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = null },
                            label = { Text("รหัสผ่าน (Password)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        
                        if (!isSignUpMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { rememberMe = !rememberMe }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    modifier = Modifier.testTag("remember_me_checkbox")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "จดจำการเข้าระบบในเครื่องนี้",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("ชื่อและนามสกุลจริง") },
                                placeholder = { Text("เช่น สมชาย รักเรียน") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.PermIdentity, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_name")
                            )

                            // Choose Unit / Department Selection
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedDepartment,
                                    onValueChange = { selectedDepartment = it },
                                    label = { Text("เลือกสังกัด / หน่วยงาน (Unit)") },
                                    placeholder = { Text("แตะเพื่อเลือก หรือพิมพ์ชื่อสังกัดใหม่") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Business, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { departmentDropdownExpanded = !departmentDropdownExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "เลือกหน่วย")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_department_input")
                                )
                                DropdownMenu(
                                    expanded = departmentDropdownExpanded,
                                    onDismissRequest = { departmentDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    val depts = listOf(
                                        "กองบังคับการ",
                                        "ฝ่ายขนส่งทางบก",
                                        "ฝ่ายบริการยานพาหนะ",
                                        "ฝ่ายยุทธบริการทางอากาศ",
                                        "ฝ่ายซ่อมบำรุงและสนับสนุน",
                                        "ฝ่ายคลังพัสดุและเชื้อเพลิง",
                                        "กองร้อยบริการพิเศษ",
                                        "หน่วยเฉพาะกิจ"
                                    )
                                    depts.forEach { dept ->
                                        DropdownMenuItem(
                                            text = { Text(dept, fontSize = 13.sp) },
                                            onClick = {
                                                selectedDepartment = dept
                                                departmentDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                "เลือกบทบาทสิทธิ์ (Role)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = selectedRole == "USER",
                                    onClick = { selectedRole = "USER" },
                                    label = { Text("เจ้าหน้าที่เบิกจ่าย (USER)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.1f)
                                )
                                FilterChip(
                                    selected = selectedRole == "OPERATOR",
                                    onClick = { selectedRole = "OPERATOR" },
                                    label = { Text("เจ้าหน้าที่ปฏิบัติ (OPERATOR)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.2f)
                                )
                                FilterChip(
                                    selected = selectedRole == "ADMIN",
                                    onClick = { selectedRole = "ADMIN" },
                                    label = { Text("แอดมิน (ADMIN)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                            
                            Text(
                                "⚠️ หมายเหตุ: การสร้างบัญชีใหม่จะต้องรอให้ ผู้ดูแลระบบ (ADMIN) ออนแอร์เพื่ออนุมัติสิทธิ์การป้อนระบบก่อนจึงจะล็อกอินเข้าสู่หน้าควบคุมได้",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp
                            )
                        }
                        
                        if (errorMsg != null) {
                            Text(
                                text = errorMsg ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (successMsg != null) {
                            Text(
                                text = successMsg ?: "",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (isSignUpMode) {
                                    viewModel.registerUser(
                                        username = username,
                                        pwhash = password,
                                        name = fullName,
                                        role = selectedRole,
                                        department = selectedDepartment,
                                        onResult = { result ->
                                            when(result) {
                                                FuelViewModel.RegisterResult.Success -> {
                                                    successMsg = "สมัครบัญชีเรียบร้อย! กรุณาใช้บัญชี Admin ล็อกอินเข้ามาอนุมัติสิทธิ์ใช้งาน"
                                                    errorMsg = null
                                                    username = ""
                                                    password = ""
                                                    fullName = ""
                                                    selectedDepartment = ""
                                                }
                                                FuelViewModel.RegisterResult.UsernameExists -> {
                                                    errorMsg = "❌ ชื่อผู้ใช้งานนี้ได้รับการสมัครแล้ว"
                                                    successMsg = null
                                                }
                                                FuelViewModel.RegisterResult.Error -> {
                                                    errorMsg = "❌ กรุณากรอกฟิลด์ข้อมูลระบุตัวตน ชื่อ-สกุล และเลือกสังกัด/หน่วยงานให้ครบถ้วน"
                                                    successMsg = null
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.loginUser(
                                        username = username,
                                        pwhash = password,
                                        rememberMe = rememberMe,
                                        onResult = { result ->
                                            when (result) {
                                                FuelViewModel.LoginResult.Success -> {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("ลงชื่อเข้าใช้งานสำเร็จ ยินดีช่วยเหลือ!")
                                                    }
                                                }
                                                FuelViewModel.LoginResult.UserNotFound -> {
                                                    errorMsg = "❌ บัญชีไม่ถูกต้อง หรือพิมพ์รหัสชื่อผิดพลาด"
                                                }
                                                FuelViewModel.LoginResult.IncorrectPassword -> {
                                                    errorMsg = "❌ รหัสผ่านคัดลอกไม่ตรงเฉลย"
                                                }
                                                FuelViewModel.LoginResult.NotApproved -> {
                                                    errorMsg = "⚠️ บัญชีของคุณยังไม่ได้รับการอนุญาตอนุมัติจากแอดมินระบบหลัก"
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag(if (isSignUpMode) "signup_button" else "login_button")
                        ) {
                            Text(
                                text = if (isSignUpMode) "ยืนยันลงทะเบียน" else "เข้าสู่ระบบคลังน้ำมัน",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// PANEL: SYSTEM ADMINISTRATION (แผงควบคุมแอดมิน & อนุมัติผู้ใช้)
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardView(
    viewModel: FuelViewModel,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val initialStocks by viewModel.allInitialStocks.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    
    // Form fields for initial stock edits
    var g95Str by remember { mutableStateOf("") }
    var g91Str by remember { mutableStateOf("") }
    var dieselStr by remember { mutableStateOf("") }
    var jp8Str by remember { mutableStateOf("") }

    // Form fields for unit quota allocation setup
    var selectedUnitDept by remember { mutableStateOf("") }
    var unitDeptDropdownExpanded by remember { mutableStateOf(false) }
    var selectedUnitFuelType by remember { mutableStateOf(FuelType.GASOHOL_95) }
    var unitFuelTypeDropdownExpanded by remember { mutableStateOf(false) }
    var unitLitersStr by remember { mutableStateOf("") }
    var unitNotes by remember { mutableStateOf("") }

    // Predefined departments for selection
    val depts = listOf(
        "กองบังคับการ",
        "ฝ่ายขนส่งทางบก",
        "ฝ่ายบริการยานพาหนะ",
        "ฝ่ายยุทธบริการทางอากาศ",
        "ฝ่ายซ่อมบำรุงและสนับสนุน",
        "ฝ่ายคลังพัสดุและเชื้อเพลิง",
        "กองร้อยบริการพิเศษ",
        "หน่วยเฉพาะกิจ"
    )

    // Calculate quota for each department dynamically from transactions
    val departmentQuotas = remember(allTransactions) {
        val map = mutableMapOf<String, MutableMap<FuelType, Triple<Double, Double, Double>>>()
        
        // Initialize map for pre-defined departments
        depts.forEach { dept ->
            val deptMap = mutableMapOf<FuelType, Triple<Double, Double, Double>>()
            FuelType.entries.forEach { fType ->
                deptMap[fType] = Triple(0.0, 0.0, 0.0) // Pair(allocated, dispensed, remaining)
            }
            map[dept] = deptMap
        }

        // Process existing transactions
        allTransactions.forEach { trans ->
            val transDept = trans.department.trim()
            if (transDept.isNotEmpty()) {
                val fType = try {
                    FuelType.valueOf(trans.fuelType)
                } catch (e: Exception) {
                    null
                }
                if (fType != null) {
                    val deptMap = map.getOrPut(transDept) {
                        val newMap = mutableMapOf<FuelType, Triple<Double, Double, Double>>()
                        FuelType.entries.forEach { newMap[it] = Triple(0.0, 0.0, 0.0) }
                        newMap
                    }
                    val currentVal = deptMap[fType] ?: Triple(0.0, 0.0, 0.0)
                    if (trans.type == "INTAKE") {
                        val newAllo = currentVal.first + trans.liters
                        deptMap[fType] = Triple(newAllo, currentVal.second, newAllo - currentVal.second)
                    } else if (trans.type == "DISPENSE") {
                        val newDisp = currentVal.second + trans.liters
                        deptMap[fType] = Triple(currentVal.first, newDisp, currentVal.first - newDisp)
                    }
                }
            }
        }
        map
    }
    
    // Load initial stock fields from state once ready
    LaunchedEffect(initialStocks) {
        if (initialStocks.isNotEmpty()) {
            initialStocks.forEach { stock ->
                when(stock.fuelType) {
                    FuelType.GASOHOL_95.name -> if (g95Str.isEmpty()) g95Str = stock.liters.toInt().toString()
                    FuelType.GASOHOL_91.name -> if (g91Str.isEmpty()) g91Str = stock.liters.toInt().toString()
                    FuelType.DIESEL.name -> if (dieselStr.isEmpty()) dieselStr = stock.liters.toInt().toString()
                    FuelType.JP_8.name -> if (jp8Str.isEmpty()) jp8Str = stock.liters.toInt().toString()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ระบบผู้ดูแลคลังน้ำมันสูงสุด (ADMIN)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "สิทธิ์กำหนดคลังน้ำมันคงคลังขั้นต้น และตอบรับสิทธิ์อนุมัติให้ล็อกอินบัญชีใหม่",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 1: Edit Initial Fuel Stocks
        item {
            Text(
                text = "⚡ แก้ไขข้อมูลจำนวนน้ำมันคงคลังขั้นต้น (Settings)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "แก้ไข baseline ปริมาณระดับคลังสำรองเริ่มต้นก่อนการประมวลผลธุรกรรมเข้า-ออก",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = g95Str,
                        onValueChange = { g95Str = it },
                        label = { Text("คลังสำรองขั้นต้น Gasohol 95 (ลิตร)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_start_stock_g95")
                    )

                    OutlinedTextField(
                        value = g91Str,
                        onValueChange = { g91Str = it },
                        label = { Text("คลังสำรองขั้นต้น Gasohol 91 (ลิตร)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dieselStr,
                        onValueChange = { dieselStr = it },
                        label = { Text("คลังสำรองขั้นต้น Diesel (ลิตร)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = jp8Str,
                        onValueChange = { jp8Str = it },
                        label = { Text("คลังสำรองขั้นต้น JP-8 (ลิตร)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val g95 = g95Str.toDoubleOrNull() ?: 0.0
                            val g91 = g91Str.toDoubleOrNull() ?: 0.0
                            val diesel = dieselStr.toDoubleOrNull() ?: 0.0
                            val jp8 = jp8Str.toDoubleOrNull() ?: 0.0

                            viewModel.saveInitialStock(FuelType.GASOHOL_95, g95)
                            viewModel.saveInitialStock(FuelType.GASOHOL_91, g91)
                            viewModel.saveInitialStock(FuelType.DIESEL, diesel)
                            viewModel.saveInitialStock(FuelType.JP_8, jp8)

                            scope.launch {
                                snackbarHostState.showSnackbar("บันทึกคลังคงเหลือเริ่มต้นสำเร็จแล้ว! ระดับน้ำมันที่มาตรวัดด้านหน้า ได้รับการอัปเดตแบบเรียลไทม์")
                            }
                        },
                        modifier = Modifier.align(Alignment.End).testTag("save_start_stock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("บันทึกปริมาณเริ่มต้น")
                    }
                }
            }
        }

        // --- SECTION: UNIT FUEL ALLOCATION & QUOTA (ระบบโควตาและนำเข้าน้ำมันรายหน่วย) ---
        item {
            Text(
                text = "📦 ระบบตรวจสอบโควตาน้ำมันคงเหลือและนำเข้าน้ำมันรายหน่วย (Unit Fuel Quota)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚡ แบบฟอร์มนำเข้าน้ำมันและจัดสรรโควตาให้หน่วยงาน (Unit Allocation Form)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Department Selection Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedUnitDept,
                            onValueChange = { 
                                selectedUnitDept = it
                                unitDeptDropdownExpanded = true
                            },
                            label = { Text("เลือกหรือระบุสังกัด / หน่วยงาน (Unit)") },
                            placeholder = { Text("แตะเพื่อเลือก หรือระบุชื่อของหน่วย") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Business, null) },
                            trailingIcon = {
                                IconButton(onClick = { unitDeptDropdownExpanded = !unitDeptDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "เลือกหน่วย")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("alloc_unit_input")
                        )
                        DropdownMenu(
                            expanded = unitDeptDropdownExpanded,
                            onDismissRequest = { unitDeptDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            depts.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept, fontSize = 13.sp) },
                                    onClick = {
                                        selectedUnitDept = dept
                                        unitDeptDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Fuel Type Chips / Filter Selection
                    Text(
                        text = "ชนิดน้ำมันที่ต้องการนำเข้าเติมโควตา",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FuelType.entries.forEach { fType ->
                            val isSelected = selectedUnitFuelType == fType
                            val color = Color(android.graphics.Color.parseColor(fType.colorHex))
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedUnitFuelType = fType },
                                label = { Text(fType.displayName, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color,
                                    selectedLeadingIconColor = color
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Input Liters Double Amount
                    OutlinedTextField(
                        value = unitLitersStr,
                        onValueChange = { unitLitersStr = it },
                        label = { Text("ปริมาณน้ำมันนำเข้า (ลิตร)") },
                        placeholder = { Text("เช่น 2500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("alloc_liters_input")
                    )

                    // Memo input
                    OutlinedTextField(
                        value = unitNotes,
                        onValueChange = { unitNotes = it },
                        label = { Text("หมายเหตุ / เลขที่ใบสั่งนำเข้า") },
                        placeholder = { Text("เช่น คลัง ทบ. แจกจ่าย ประจำปี 2569") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val liters = unitLitersStr.toDoubleOrNull() ?: 0.0
                            if (selectedUnitDept.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("❌ กรุณาเลือกหรือระบุสังกัด / หน่วยงาน")
                                }
                                return@Button
                            }
                            if (liters <= 0.0) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("❌ กรุณากรอกจำนวนลิตรน้ำมันที่มีค่าเป็นบวก")
                                }
                                return@Button
                            }

                            // Call new ViewModel API to insert INTAKE with department
                            viewModel.receiveFuelForUnit(
                                fuelType = selectedUnitFuelType,
                                amount = liters,
                                department = selectedUnitDept,
                                notes = unitNotes.trim().ifEmpty { "นำเข้าน้ำมันจัดสรรสำหรับหน่วย" }
                            )

                            scope.launch {
                                snackbarHostState.showSnackbar("📥 บันทึกนำเข้าน้ำมัน ${selectedUnitFuelType.displayName} ให้หน่วยงาน $selectedUnitDept อัปเดตปริมาณสำเร็จแล้ว!")
                            }

                            // Reset local fields
                            unitLitersStr = ""
                            unitNotes = ""
                        },
                        modifier = Modifier.align(Alignment.End).testTag("confirm_unit_alloc_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("นำเข้าน้ำมันให้หน่วย")
                    }
                }
            }
        }

        // Ledger Panel displaying all Unit allocations, dispenses and remaining balances
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📊 ตารางแสดงบัญชีโควตาน้ำมันคงเหลือสะสมรายหน่วยงาน (Ledger Balances)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    val activeDepts = departmentQuotas.keys.sorted()
                    if (activeDepts.isEmpty()) {
                        Text(
                            text = "ยังไม่มีประวัติการบันทึกจัดสรรน้ำมันรายหน่วย",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        activeDepts.forEach { deptName ->
                            val fuelMap = departmentQuotas[deptName] ?: emptyMap()
                            
                            // Check if this department has values
                            val hasActivity = fuelMap.values.any { it.first > 0.0 || it.second > 0.0 }
                            
                            if (hasActivity) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Business,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = deptName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // Render allocations of G95, G91, Diesel, JP8
                                        FuelType.entries.forEach { fType ->
                                            val stats = fuelMap[fType] ?: Triple(0.0, 0.0, 0.0)
                                            val allocated = stats.first
                                            val dispensed = stats.second
                                            val remaining = stats.third

                                            if (allocated > 0.0 || dispensed > 0.0) {
                                                val fColor = Color(android.graphics.Color.parseColor(fType.colorHex))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(fColor, CircleShape)
                                                        )
                                                        Text(
                                                            text = fType.displayName,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "โควตาคงเหลือ: ${DecimalFormat("#,##0.0").format(remaining)} ลิตร",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (remaining <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "นำเข้าสะสม: ${DecimalFormat("#,##0.0").format(allocated)} | เบิกใช้แล้ว: ${DecimalFormat("#,##0.0").format(dispensed)} ลิตร",
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: User Approval & Account Management Dashboard
        item {
            Text(
                text = "👥 จัดการตอบรับและอนุมัติบัญชีสมาชิกล็อกอินเข้าใช้งาน",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (users.isEmpty()) {
            item {
                Text(
                    "ไม่มีบัญชีในระบบ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(users) { u ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = u.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (u.role == "ADMIN") MaterialTheme.colorScheme.primaryContainer else if (u.role == "OPERATOR") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer /*
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.secondaryContainer
                                    */),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = u.role,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "@${u.username}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (u.department.isNotBlank()) {
                                Text(
                                    text = "สังกัด: ${u.department}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (u.isApproved) Color(0xFF4CAF50) else Color(0xFFFF9800), CircleShape)
                                )
                                Text(
                                    text = if (u.isApproved) "สถานะ: อนุมัติใช้งานแล้ว" else "สถานะ: รอดำเนินการอนุมัติ (Pending)",
                                    fontSize = 9.sp,
                                    color = if (u.isApproved) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!u.isApproved) {
                                Button(
                                    onClick = { viewModel.approveUser(u.username) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(30.dp).testTag("approve_user_${u.username}")
                                ) {
                                    Text("อนุมัติใช้งาน", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            
                            // Delete button (Prevent deleting the active logged in admin "admin")
                            if (u.username != "admin") {
                                IconButton(
                                    onClick = { viewModel.deleteUser(u.username) },
                                    modifier = Modifier.size(30.dp).testTag("delete_user_${u.username}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "ลบ",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OperatorInventoryDashboard(
    currentUser: User,
    department: String,
    allTransactions: List<FuelTransaction>,
    personnelList: List<Personnel>,
    fuelStock: Map<FuelType, Double>,
    viewModel: FuelViewModel,
    snackbarHostState: SnackbarHostState
) {
    val df = DecimalFormat("#,##0.0")
    val scope = rememberCoroutineScope()
    
    var selectedType by remember { mutableStateOf(FuelType.GASOHOL_95) }
    var litersStr by remember { mutableStateOf("") }
    var personNameSelected by remember { mutableStateOf("") }
    var vehicleInfo by remember { mutableStateOf("") }
    var expandedPersonDropdown by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Calculate stats
    val unitFuelStats = remember(allTransactions, department) {
        val stats = mutableMapOf<FuelType, Triple<Double, Double, Double>>()
        FuelType.entries.forEach { fType ->
            stats[fType] = Triple(0.0, 0.0, 0.0)
        }
        allTransactions.forEach { trans ->
            if (trans.department.trim().equals(department.trim(), ignoreCase = true)) {
                val fType = try {
                    FuelType.valueOf(trans.fuelType)
                } catch (e: Exception) {
                    null
                }
                if (fType != null) {
                    val current = stats[fType] ?: Triple(0.0, 0.0, 0.0)
                    if (trans.type == "INTAKE") {
                        val newAllo = current.first + trans.liters
                        stats[fType] = Triple(newAllo, current.second, newAllo - current.second)
                    } else if (trans.type == "DISPENSE") {
                        val newDisp = current.second + trans.liters
                        stats[fType] = Triple(current.first, newDisp, current.first - newDisp)
                    }
                }
            }
        }
        stats
    }

    val currentUnitRemaining = unitFuelStats[selectedType]?.third ?: 0.0

    // Generate smart contextual alerts (ระบบแจ้งเตือน)
    val alerts = remember(unitFuelStats, allTransactions, department) {
        val list = mutableListOf<String>()
        // 1. Low Fuel warning
        unitFuelStats.forEach { (fType, triple) ->
            val allocated = triple.first
            val remaining = triple.third
            if (allocated > 0.0 && remaining < 500.0) {
                list.add("🚨 แฟ้มแจ้งเตือนควบคุม: น้ำมัน ${fType.displayName} คงเหลือต่ำวิกฤต! (คงเหลือเพียง ${df.format(remaining)} ลิตร) กรุณาทำเรื่องประสานแอดมิน เพื่อนำเข้าน้ำมันจัดสรรเพิ่ม")
            } else if (allocated > 0.0 && remaining < 1000.0) {
                list.add("⚠️ ระวัง: น้ำมัน ${fType.displayName} คงเหลือระดับปลอดภัยต่ำ (คงเหลือ ${df.format(remaining)} ลิตร)")
            }
        }
        
        // 2. Recent allocations / transactions alerts for department
        val recentAllo = allTransactions
            .filter { it.department.trim().equals(department.trim(), ignoreCase = true) && it.type == "INTAKE" }
            .sortedByDescending { it.timestamp }
            .take(2)
        recentAllo.forEach { trans ->
            val fType = try { FuelType.valueOf(trans.fuelType).displayName } catch(e:Exception) { trans.fuelType }
            list.add("📥 ได้รับการจัดสรรเพิ่มเติม: +${df.format(trans.liters)} ลิตร ของน้ำมัน $fType (หมายเหตุ: ${trans.notes ?: "-"})")
        }
        
        // 3. No allocations yet
        val totalAllocated = unitFuelStats.values.sumOf { it.first }
        if (totalAllocated == 0.0) {
            list.add("ℹ️ ฝ่ายธุรการแจ้งเตือน: สังกัดของท่านยังไม่ได้รับจัดสรรน้ำมันเข้าคลังหน่วย กรุณาแจ้งแอดมินเพื่อทำการจัดสรรเชื้อเพลิงเริ่มต้น")
        }
        
        list
    }

    // Get all requests of our unit (Pending and Confirmed Dispenses)
    val myUnitRequests = remember(allTransactions, department) {
        allTransactions.filter { 
            (it.type == "PENDING_DISPENSE" || it.type == "DISPENSE") && 
            it.department.trim().equals(department.trim(), ignoreCase = true)
        }.sortedByDescending { it.timestamp }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("operator_inventory_dashboard")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Unit Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "สังกัดหน่วยงาน: $department",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ผู้ปฏิบัติการหน่วย (OPERATOR) คุณ ${currentUser.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Alert Area Widget (ระบบแจ้งเตือน)
        if (alerts.isNotEmpty()) {
            item {
                Text(
                    text = "🔔 ระบบแจ้งเตือนประจำแผนก (Department Alerts)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (alerts.any { it.contains("🚨") }) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        alerts.forEach { alert ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = alert,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (alert.contains("🚨")) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ✍️ FORM TO DEFINE DISPENSE REQUEST
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "✍️ ส่งคำขอเบิกจ่ายน้ำมันประจำหน่วย (Dispense Request Form)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "กรอกฟอร์มเพื่อส่งแจ้งเตือนให้เจ้าหน้าที่ตัดคลังกลาง (USER) ตรวจสอบและดำเนินการจ่ายออกจริง",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Error Message Display
                    if (errorMsg != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // 1. CHOOSE FUEL TYPE
                    Text(
                        text = "1. เลือกชนิดน้ำมัน (Fuel Type)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FuelType.entries.chunked(2).forEach { rowTypes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTypes.forEach { fType ->
                                    val isSelected = selectedType == fType
                                    val fColor = Color(android.graphics.Color.parseColor(fType.colorHex))
                                    val balVal = unitFuelStats[fType]?.third ?: 0.0

                                    Card(
                                        onClick = { 
                                            selectedType = fType
                                            errorMsg = null
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) fColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) fColor else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(fColor, CircleShape)
                                                )
                                                Text(
                                                    text = fType.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) fColor else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = "โควตาคงเหลือ: ${df.format(balVal)} ลิตร",
                                                fontSize = 11.sp,
                                                color = if (balVal < 100.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. PERSONNEL FIELD (หน่วยงานฟิลเตอร์)
                    Text(
                        text = "2. บุคคลผู้รับน้ำมัน / คนขับรถหลัก (Person / Driver)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = personNameSelected,
                            onValueChange = {
                                personNameSelected = it
                                expandedPersonDropdown = true
                                errorMsg = null
                            },
                            label = { Text("ชื่อ-นามสกุล ของคนขับ/ผู้รับน้ำมัน") },
                            placeholder = { Text("พิมพ์เพื่อค้นหาบุคคลหรือระบุเอง") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { expandedPersonDropdown = !expandedPersonDropdown }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "ลูกศรเลือก"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("operator_request_person_input"),
                            singleLine = true
                        )

                        // Filter personnel of this department
                        val filteredPersonnel = personnelList.filter {
                            it.name.contains(personNameSelected, ignoreCase = true) &&
                            (it.department.trim().equals(department.trim(), ignoreCase = true) || it.department.isBlank())
                        }

                        if (expandedPersonDropdown && (filteredPersonnel.isNotEmpty() || personnelList.isNotEmpty())) {
                            DropdownMenu(
                                expanded = expandedPersonDropdown,
                                onDismissRequest = { expandedPersonDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                val displayList = if (personNameSelected.isEmpty()) {
                                    personnelList.filter { it.department.trim().equals(department.trim(), ignoreCase = true) }
                                } else {
                                    filteredPersonnel
                                }
                                displayList.forEach { personnel ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text(personnel.name, fontWeight = FontWeight.Bold)
                                                if (personnel.department.isNotBlank()) {
                                                    Text(personnel.department, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        },
                                        onClick = {
                                            personNameSelected = personnel.name
                                            expandedPersonDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. VEHICLE FIELD
                    Text(
                        text = "3. รายละเอียดและทะเบียนยานพาหนะ (Vehicle Details)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = vehicleInfo,
                        onValueChange = { 
                            vehicleInfo = it
                            errorMsg = null
                        },
                        label = { Text("ทะเบียนรถ / ยานพาหนะที่รับการเติม") },
                        placeholder = { Text("เช่น รถกู้ชีพหน่วย กข-4444, รถดับเพลิงหลัก") },
                        leadingIcon = { Icon(Icons.Default.AirportShuttle, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("รถหลวง", "รถดับเพลิง", "รถพยาบาล", "รถสนับสนุน").forEach { sug ->
                            SuggestionChip(
                                onClick = { vehicleInfo = sug },
                                label = { Text(sug, fontSize = 10.sp) }
                            )
                        }
                    }

                    // 4. LITERS FIELD
                    Text(
                        text = "4. ปริมาณต้องการเติม (Liters Suggested)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = litersStr,
                        onValueChange = { 
                            litersStr = it
                            errorMsg = null
                        },
                        label = { Text("ปริมาณเสนอขอเบิก (ลิตร)") },
                        leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) },
                        suffix = { Text("ลิตร") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(20, 50, 80, 100).forEach { amount ->
                            TextButton(
                                onClick = { 
                                    val currentVal = litersStr.toDoubleOrNull() ?: 0.0
                                    litersStr = (currentVal + amount).toString()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Text("+$amount ลิตร", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 5. NOTES
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("จุดที่หมาย / ภารกิจเบิก (เช่น ออกลาดตระเวน)") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // SUBMIT REQUEST BUTTON
                    Button(
                        onClick = {
                            val amount = litersStr.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                errorMsg = "❌ กรุณากรอกปริมาณลิตรที่ต้องการเบิกให้ถูกต้อง"
                                return@Button
                            }
                            if (amount > currentUnitRemaining) {
                                errorMsg = "❌ ยอดโควตาของสังกัดมีเพียง ${df.format(currentUnitRemaining)} ลิตร ไม่พอกับยอดที่จะสั่งจ่าย!"
                                return@Button
                            }
                            if (personNameSelected.isBlank()) {
                                errorMsg = "❌ กรุณากรอกคนขับหรือผู้มาติดต่อรับน้ำมัน"
                                return@Button
                            }
                            if (vehicleInfo.isBlank()) {
                                errorMsg = "❌ กรุณาระบุรายละเอียดรถหรือเลขครุภัณฑ์ยานพาหนะ"
                                return@Button
                            }

                            val payloadNotes = "ยานพาหนะ: ${vehicleInfo.trim()} (ภารกิจ: ${notes.trim().ifBlank { "ไม่ระบุ" }}) [สั่งจ่ายโดยกำลังพล OPERATOR: ${currentUser.name}]"
                            
                            viewModel.requestDispenseFuel(
                                fuelType = selectedType,
                                amount = amount,
                                personName = personNameSelected.trim(),
                                notes = payloadNotes,
                                department = department
                            )

                            scope.launch {
                                snackbarHostState.showSnackbar("🔔 บันทึกใบเบิกสั่งจ่าย ${df.format(amount)} ลิตร เรียบร้อย! รอผู้สั่งจ่าย (USER) อนุมัติ...")
                            }

                            // reset form fields
                            litersStr = ""
                            personNameSelected = ""
                            vehicleInfo = ""
                            notes = ""
                            errorMsg = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_operator_dispense_request"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ส่งคำขอสั่งจ่าย (ส่งแจ้งเตือนระบบรอดำเนินการ)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Grid showing fuel balances (น้ำมันคงเหลือของหน่วย)
        item {
            Text(
                text = "📊 สถิติโควตาน้ำมันสังกัดและสถานะ (Unit Remaining Fuel Stock)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FuelType.entries.forEach { fType ->
                    val stats = unitFuelStats[fType] ?: Triple(0.0, 0.0, 0.0)
                    val allocated = stats.first
                    val dispensed = stats.second
                    val remaining = stats.third
                    
                    val percentRemain = if (allocated > 0.0) (remaining / allocated) else 0.0
                    val isLow = allocated > 0.0 && remaining < 500.0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp, 
                            if (isLow) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Fuel title header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val fColor = Color(android.graphics.Color.parseColor(fType.colorHex))
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(fColor, CircleShape)
                                    )
                                    Text(
                                        text = fType.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                if (allocated > 0.0) {
                                    Text(
                                        text = "${df.format(percentRemain * 100)}% คงเหลือในหน่วย",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "ไม่มีโควตาจัดสรร",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Large liters amount display
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "โควตาคงเหลือพร้อมจ่าย",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "${df.format(remaining)} ลิตร",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "จัดสรรรวม: ${df.format(allocated)} ลิตร",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "เบิกจ่ายสำเร็จ: ${df.format(dispensed)} ลิตร",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom usage bar graph
                            if (allocated > 0.0) {
                                LinearProgressIndicator(
                                    progress = { percentRemain.toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isLow) MaterialTheme.colorScheme.error else Color(android.graphics.Color.parseColor(fType.colorHex)),
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section header for Recent Dispensing Requests & History of this unit
        item {
            Text(
                text = "📜 สถานะรายงานเบิกจ่ายและใบเบิกของหน่วยงาน (Unit Requests & Logs)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (myUnitRequests.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ยังไม่มีประวัติการส่งคำขอจ่ายน้ำมันจากหน่วยงานนี้",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(myUnitRequests, key = { it.id }) { trans ->
                val fuelTypeEnum = try {
                    FuelType.valueOf(trans.fuelType)
                } catch (_: Exception) {
                    FuelType.GASOHOL_95
                }
                val fColor = Color(android.graphics.Color.parseColor(fuelTypeEnum.colorHex))
                val timeSdf = SimpleDateFormat("yyyy-MM-dd HH:mm น.", Locale.getDefault())
                val timeFormatted = timeSdf.format(Date(trans.timestamp))
                val isPending = trans.type == "PENDING_DISPENSE"

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPending) 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    border = if (isPending) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
                    elevation = CardDefaults.cardElevation(if (isPending) 0.dp else 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(fColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = fuelTypeEnum.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = fColor
                                        )
                                    }

                                    // Badge Status
                                    if (isPending) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "รอผู้สั่งจ่ายอนุมัติ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE8F5E9))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "เติมสั่งจ่ายแล้ว",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "เมื่อ: $timeFormatted",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            Text(
                                text = "${df.format(trans.liters)} ลิตร",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = if (isPending) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "👤 ผู้เบิกเติม: ${trans.personName ?: "ไม่ระบุ"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (!trans.notes.isNullOrBlank()) {
                                Text(
                                    text = trans.notes,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        if (isPending) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.deleteTransaction(trans)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("🗑️ ยกเลิกคำขอสั่งจ่ายน้ำมันสำเร็จ")
                                        }
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ยกเลิกคำขอ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OperatorDailyReportView(
    department: String,
    selectedDate: String,
    allTransactions: List<FuelTransaction>,
    viewModel: FuelViewModel,
    snackbarHostState: SnackbarHostState
) {
    val df = DecimalFormat("#,##0.0")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State to hold compiled CSV string for the exporter
    val csvToExport = remember { mutableStateOf("") }
    
    // Setup standard Android storage document launcher for type CSV
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    java.io.OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write('\uFEFF'.code)
                        writer.write(csvToExport.value)
                    }
                }
                scope.launch {
                    snackbarHostState.showSnackbar("💾 ส่งออกและบันทึกรายงานประจำหน่วย CSV สำเร็จ!")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("❌ บันทึกล้มเหลว: ${e.localizedMessage}")
                }
            }
        }
    }
    
    // Date navigation calendar
    val calendar = remember { Calendar.getInstance() }
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Sync calendar with selected date
    LaunchedEffect(selectedDate) {
        try {
            val date = sdfDb.parse(selectedDate)
            if (date != null) {
                calendar.time = date
            }
        } catch (_: Exception) {}
    }

    // Filter transactions for this unit and date
    val filteredTransactions = remember(allTransactions, selectedDate, department) {
        allTransactions.filter { 
            it.dateString == selectedDate && 
            it.department.trim().equals(department.trim(), ignoreCase = true)
        }
    }

    // Calculate aggregated summaries for this unit on this date
    val unitDailySummaries = remember(filteredTransactions) {
        FuelType.entries.map { fType ->
            val typeTrans = filteredTransactions.filter { it.fuelType == fType.name }
            val intake = typeTrans.filter { it.type == "INTAKE" }.sumOf { it.liters }
            val dispense = typeTrans.filter { it.type == "DISPENSE" }.sumOf { it.liters }
            FuelDailySummary(fType, intake, dispense, typeTrans.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("operator_reports_section")
    ) {
        // Date Switcher Header
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        viewModel.changeSelectedDate(sdfDb.format(calendar.time))
                    },
                    modifier = Modifier.testTag("prev_date_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "วันก่อนหน้า")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = viewModel.getSelectedDateThai(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "รายงานสถิตประจำหน่วยงาน: $department",
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        viewModel.changeSelectedDate(sdfDb.format(calendar.time))
                    },
                    modifier = Modifier.testTag("next_date_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "วันถัดไป")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CSV Export Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ส่งออกรายงานประจำหน่วย (CSV)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "บันทึกข้อมูลโควตานำเข้าและประวัติใช้น้ำมันรายวันของฝ่าย",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                lineHeight = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val csvText = generateCsvString(
                                    selectedDate = selectedDate,
                                    transactions = filteredTransactions,
                                    summaries = unitDailySummaries,
                                    viewModel = viewModel
                                )
                                csvToExport.value = csvText
                                csvLauncher.launch("fuel_report_${department}_${selectedDate}.csv")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "ส่งออกรายงาน", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ส่งออก CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Aggregated Summary for the selected date
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "สรุปยอดจัดสรร-เบิกใช้ ประจำหน่วยงาน",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val totalDayIntake = unitDailySummaries.sumOf { it.intakeLiters }
                        val totalDayDispense = unitDailySummaries.sumOf { it.dispenseLiters }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowDownward, 
                                        contentDescription = "เข้า", 
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ได้รับจัดสรรโควตา", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "${df.format(totalDayIntake)} ล.",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                            
                            Box(modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                .align(Alignment.CenterVertically)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowUpward, 
                                        contentDescription = "ออก", 
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("แผนกเบิกใช้ไปรวม", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "${df.format(totalDayDispense)} ล.",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA580C)
                                )
                            }
                        }
                    }
                }
            }

            // Summaries list by fuel type
            item {
                Text(
                    text = "แยกตามรายชนิดน้ำมันของหน่วย",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(unitDailySummaries) { summary ->
                val color = Color(android.graphics.Color.parseColor(summary.fuelType.colorHex))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = summary.fuelType.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "จัดสรรเข้า: +${df.format(summary.intakeLiters)} ล.", 
                                fontSize = 12.sp, 
                                color = if (summary.intakeLiters > 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = if (summary.intakeLiters > 0) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "แผนกใช้ไป: -${df.format(summary.dispenseLiters)} ล.", 
                                fontSize = 12.sp, 
                                color = if (summary.dispenseLiters > 0) Color(0xFFEA580C) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = if (summary.dispenseLiters > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Today's transaction Logs
            item {
                Text(
                    text = "ประวัติการใช้น้ำมันสังกัดหน่วยงาน (${filteredTransactions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "ไม่มีข้อมูล",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ไม่มีประวัติการเบิกจ่ายหรือโควตารับในวันที่เลือก",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { trans ->
                    val fuelTypeEnum = try {
                        FuelType.valueOf(trans.fuelType)
                    } catch (_: Exception) {
                        FuelType.GASOHOL_95
                    }
                    val color = Color(android.graphics.Color.parseColor(fuelTypeEnum.colorHex))
                    val isIntake = trans.type == "INTAKE"
                    
                    val timeSdf = SimpleDateFormat("HH:mm น.", Locale.getDefault())
                    val timeFormatted = timeSdf.format(Date(trans.timestamp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left badge (Intake / Dispense)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isIntake) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIntake) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = if (isIntake) "รับเข้า" else "จ่ายออก",
                                    tint = if (isIntake) Color(0xFF15803D) else Color(0xFFC2410C),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isIntake) "โควตารับเข้า" else "สังกัดเบิกจ่าย",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isIntake) Color(0xFF15803D) else Color(0xFFC2410C)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(color.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = fuelTypeEnum.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                val amountText = if (isIntake) "+${df.format(trans.liters)} ลิตร" else "-${df.format(trans.liters)} ลิตร"
                                Text(
                                    text = "จำนวน: $amountText",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isIntake) Color(0xFF15803D) else Color(0xFFC2410C)
                                )

                                val personStr = trans.personName?.let { "ผู้ทำรายการ: $it" } ?: "บันทึกคลัง"
                                Text(
                                    text = personStr,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (!trans.notes.isNullOrBlank()) {
                                    Text(
                                        text = "บันทึก: ${trans.notes}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Text(
                                    text = "เวลา: $timeFormatted",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserDispenseDashboard(
    currentUser: User,
    personnelList: List<Personnel>,
    fuelStock: Map<FuelType, Double>,
    allTransactions: List<FuelTransaction>,
    viewModel: FuelViewModel,
    snackbarHostState: SnackbarHostState
) {
    val df = DecimalFormat("#,##0.0")
    val scope = rememberCoroutineScope()
    
    var selectedType by remember { mutableStateOf(FuelType.GASOHOL_95) }
    var litersStr by remember { mutableStateOf("") }
    var personNameSelected by remember { mutableStateOf("") }
    var vehicleInfo by remember { mutableStateOf("") }
    var expandedPersonDropdown by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val currentStock = fuelStock[selectedType] ?: 0.0

    // Filter transactions to show pending requests
    val pendingDispensations = remember(allTransactions) {
        allTransactions.filter { it.type == "PENDING_DISPENSE" }
            .sortedByDescending { it.timestamp }
    }

    // Filter transactions to show current user's department's recent dispense logs (today and past) so they have immediate status
    val myRecentDispensing = remember(allTransactions, currentUser.department) {
        allTransactions.filter { 
            it.type == "DISPENSE" && 
            (it.department.trim().equals(currentUser.department.trim(), ignoreCase = true) || currentUser.department.isBlank())
        }.sortedByDescending { it.timestamp }.take(10)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_dispense_dashboard")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Card for USER Refueling Officer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "สวัสดีคุณ ${currentUser.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "สังกัด: ${currentUser.department.ifEmpty { "ผู้เชี่ยวชาญสั่งจ่าย" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ผู้สั่งจ่าย (USER)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- NEW PENDING DISPENSE NOTIFICATIONS SECTION ---
        if (pendingDispensations.isNotEmpty()) {
            item {
                Text(
                    text = "🔔 รายการสั่งจ่ายน้ำมันรอดำเนินการ (${pendingDispensations.size} รายการ)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            items(pendingDispensations, key = { "pending_${it.id}" }) { trans ->
                val fuelTypeEnum = try {
                    FuelType.valueOf(trans.fuelType)
                } catch (_: Exception) {
                    FuelType.GASOHOL_95
                }
                val fColor = Color(android.graphics.Color.parseColor(fuelTypeEnum.colorHex))
                val timeSdf = SimpleDateFormat("HH:mm น.", Locale.getDefault())
                val timeFormatted = timeSdf.format(Date(trans.timestamp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(fColor, CircleShape)
                                )
                                Text(
                                    text = fuelTypeEnum.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${df.format(trans.liters)} ลิตร",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

                        Text(
                            text = "🏢 แจ้งเบิกสังกัด: ${trans.department}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "👤 ผู้ขับขี่/ผู้รับน้ำมัน: ${trans.personName ?: "ไม่ระบุ"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!trans.notes.isNullOrBlank()) {
                            Text(
                                text = "📝 ข้อมูลเสนอการเบิก: ${trans.notes}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "⏰ ส่งเสนอเข้าระบบเมื่อ: $timeFormatted",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Reject Button
                            OutlinedButton(
                                onClick = {
                                    viewModel.deleteTransaction(trans)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("❌ ปฏิเสธการสั่งจ่ายสป.3 เรียบร้อยแล้ว")
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ปฏิเสธคำขอ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Confirm Button
                            Button(
                                onClick = {
                                    val success = viewModel.confirmPendingDispense(trans, currentUser.name)
                                    scope.launch {
                                        if (success) {
                                            snackbarHostState.showSnackbar("✅ อนุมัติสั่งจ่ายและตัดยอดคลังกลาง ${df.format(trans.liters)} ลิตร สำเร็จ!")
                                        } else {
                                            snackbarHostState.showSnackbar("❌ เติมออกล้มเหลว! น้ำมันในคลังกลางไม่เพียงพอ")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("อนุมัติสั่งจ่ายจริง", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // The Formulation Card ("ทำรายการสั่งจ่ายน้ำมัน")
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "✍️ ทำรายการสั่งจ่ายน้ำมัน (Fuel Dispensation Form)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Error Message Display
                    if (errorMsg != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // 1. CHOOSE FUEL TYPE (ชนิดน้ำมัน)
                    Text(
                        text = "1. เลือกชนิดน้ำมัน (Fuel Type)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Display grid or list of available fuel types
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FuelType.entries.chunked(2).forEach { rowTypes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTypes.forEach { fType ->
                                    val isSelected = selectedType == fType
                                    val fColor = Color(android.graphics.Color.parseColor(fType.colorHex))
                                    val stockVal = fuelStock[fType] ?: 0.0

                                    Card(
                                        onClick = { 
                                            selectedType = fType
                                            errorMsg = null
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) fColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) fColor else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(fColor, CircleShape)
                                                )
                                                Text(
                                                    text = fType.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) fColor else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = "คงเหลือ: ${df.format(stockVal)} ลิตร",
                                                fontSize = 11.sp,
                                                color = if (stockVal < 300.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. PERSONNEL FIELD (บุคคลผู้รับน้ำมัน)
                    Text(
                        text = "2. บุคคลผู้รับน้ำมัน / ผู้ขับชี่ (Person / Driver)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = personNameSelected,
                            onValueChange = {
                                personNameSelected = it
                                expandedPersonDropdown = true
                                errorMsg = null
                            },
                            label = { Text("ชื่อ-นามสกุล ผู้รับน้ำมัน") },
                            placeholder = { Text("พิมพ์เพื่อค้นหาบุคคลหรือป้อนข้อมูลใหม่") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { expandedPersonDropdown = !expandedPersonDropdown }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "ลูกศรเลือก"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("operator_behalf_person_input"),
                            singleLine = true
                        )

                        // Filter personnel based on text input
                        val filteredPersonnel = personnelList.filter {
                            it.name.contains(personNameSelected, ignoreCase = true)
                        }

                        if (expandedPersonDropdown && (filteredPersonnel.isNotEmpty() || personnelList.isNotEmpty())) {
                            DropdownMenu(
                                expanded = expandedPersonDropdown,
                                onDismissRequest = { expandedPersonDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                val displayList = if (personNameSelected.isEmpty()) personnelList else filteredPersonnel
                                displayList.forEach { personnel ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(personnel.name, fontWeight = FontWeight.Bold)
                                                if (personnel.department.isNotBlank()) {
                                                    Text(personnel.department, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        },
                                        onClick = {
                                            personNameSelected = personnel.name
                                            expandedPersonDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. VEHICLE FIELD (ยานพาหนะ)
                    Text(
                        text = "3. รายละเอียดและทะเบียนยานพาหนะ (Vehicle Details)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = vehicleInfo,
                        onValueChange = { 
                            vehicleInfo = it
                            errorMsg = null
                        },
                        label = { Text("ทะเบียนรถ / รหัสยานพาหนะ") },
                        placeholder = { Text("เช่น กข-1234 กทม., รถดับเพลิงหน่วย") },
                        leadingIcon = { Icon(Icons.Default.AirportShuttle, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Quick select Suggestions for Vehicles to make it super fast to fill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("รถดับเพลิง", "รถพยาบาล", "รถกู้ภัย", "รถหลวง").forEach { sug ->
                            SuggestionChip(
                                onClick = { vehicleInfo = sug },
                                label = { Text(sug, fontSize = 10.sp) }
                            )
                        }
                    }

                    // 4. LITERS FIELD (ปริมาณน้ำมัน)
                    Text(
                        text = "4. ปริมาณน้ำมันที่สั่งจ่าย (Liters)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = litersStr,
                        onValueChange = { 
                            litersStr = it
                            errorMsg = null
                        },
                        label = { Text("ปริมาณ (ลิตร)") },
                        leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) },
                        suffix = { Text("ลิตร") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Fast adjustment row of buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 20, 50, 100).forEach { amount ->
                            TextButton(
                                onClick = { 
                                    val currentVal = litersStr.toDoubleOrNull() ?: 0.0
                                    litersStr = (currentVal + amount).toString()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Text("+$amount ลิตร", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 5. ADDITIONAL REMARKS / NOTES
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("หมายเหตุเพิ่มเติม / วัตถุประสงค์ (ถ้ามี)") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // CONFIRM BUTTON
                    Button(
                        onClick = {
                            val amount = litersStr.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                errorMsg = "❌ กรุณากรอกปริมาณน้ำมันที่ถูกต้อง (ตัวเลขมากกว่า 0)"
                                return@Button
                            }
                            if (amount > currentStock) {
                                errorMsg = "❌ ปริมาณน้ำมันในคลังของ ${selectedType.displayName} ไม่เพียงพอ! (ในคลังมีเพียง ${df.format(currentStock)} ลิตร)"
                                return@Button
                            }
                            if (personNameSelected.isBlank()) {
                                errorMsg = "❌ กรุณากรอกชื่อผู้รับเครื่องยนต์/บุคคลขับขี่"
                                return@Button
                            }
                            if (vehicleInfo.isBlank()) {
                                errorMsg = "❌ กรุณากรอกรายละเอียดทะเบียน/ชนิดของยานพาหนะ"
                                return@Button
                            }

                            // Build final notes payload capturing vehicle profile & notes
                            val deptOfPerson = personnelList.firstOrNull { it.name.trim().equals(personNameSelected.trim(), ignoreCase = true) }?.department ?: currentUser.department
                            val notesPayload = "ยานพาหนะ: ${vehicleInfo.trim()} [${notes.trim().ifBlank { "เบิกจ่ายปกติ" }}] (สั่งจ่ายโดย: ${currentUser.name})"

                            // Call ViewModel dispenseFuel
                            val success = viewModel.dispenseFuel(
                                fuelType = selectedType,
                                amount = amount,
                                personName = personNameSelected.trim(),
                                notes = notesPayload
                            )

                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("✅ บันทึกคำสั่งจ่ายปริมาณน้ำมัน ${df.format(amount)} ลิตร เรียบร้อยแล้ว!")
                                }
                                // Reset inputs except fuel type
                                litersStr = ""
                                personNameSelected = ""
                                vehicleInfo = ""
                                notes = ""
                                errorMsg = null
                            } else {
                                errorMsg = "❌ การเบิกจ่ายล้มเหลว กรุณาตรวจสอบยอดเงินหรือสถานะพนักงาน"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_dispense_order"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ยืนยันสั่งจ่ายน้ำมันเข้าระบบ", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section header for Recent Dispensing History
        item {
            Text(
                text = "📜 ประวัติการสั่งจ่ายน้ำมันสังกัดที่ดูแล (My Recent Refuel Orders)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (myRecentDispensing.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ยังไม่มีประวัติการสั่งจ่ายน้ำมันลงทะเบียนตามสังกัดนี้",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(myRecentDispensing, key = { it.id }) { trans ->
                val fuelTypeEnum = try {
                    FuelType.valueOf(trans.fuelType)
                } catch (_: Exception) {
                    FuelType.GASOHOL_95
                }
                val fColor = Color(android.graphics.Color.parseColor(fuelTypeEnum.colorHex))
                val timeSdf = SimpleDateFormat("HH:mm น.", Locale.getDefault())
                val timeFormatted = timeSdf.format(Date(trans.timestamp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "สั่งจ่ายเชื้อเพลิง",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(fColor.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = fuelTypeEnum.displayName,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = fColor
                                        )
                                    }
                                }
                                Text(
                                    text = timeFormatted,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            Text(
                                text = "จำนวน: -${df.format(trans.liters)} ลิตร",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )

                            Text(
                                text = "ผู้เบิก: ${trans.personName ?: "ไม่ระบุ"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (!trans.notes.isNullOrBlank()) {
                                Text(
                                    text = trans.notes,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


