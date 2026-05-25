package com.example

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.DamageReport
import com.example.data.model.PontoReminder
import com.example.data.model.WorkDay
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PontoViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PontoApp()
            }
        }
    }
}

@Composable
fun PontoApp(viewModel: PontoViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    // Collect Room States
    val workDays by viewModel.workDays.collectAsStateWithLifecycle()
    val damageReports by viewModel.damageReports.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    // Request permissions launcher for Notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notificações ativadas com sucesso!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Aviso: Sem permissão de notificação as alertas não funcionarão.", Toast.LENGTH_LONG).show()
        }
    }

    // Trigger permission request on startup
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("app_scaffold"),
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.PunchClock, contentDescription = "Ponto") },
                    label = { Text("Ponto", style = MaterialTheme.typography.labelMedium) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.DirectionsBus, contentDescription = "Avarias") },
                    label = { Text("Avarias", style = MaterialTheme.typography.labelMedium) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = "Lembretes") },
                    label = { Text("Lembretes", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> WorkPointScreen(viewModel, workDays)
                1 -> DamagesScreen(viewModel, damageReports)
                2 -> RemindersScreen(viewModel, reminders)
            }
        }
    }
}

// --- TAB 1: WORK POINT SCREEN ---

@Composable
fun WorkPointScreen(viewModel: PontoViewModel, workDays: List<WorkDay>) {
    val context = LocalContext.current
    var inputPrefix by remember { mutableStateOf("") }
    var editWorkDayTarget by remember { mutableStateOf<WorkDay?>(null) }
    var showManualInputDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("work_point_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Olá, Fábio",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = viewModel.getTodayFormatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable {
                            Toast.makeText(context, "Fábio Anuvale (fabioanuvale@gmail.com)", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FA",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }

        // Today Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONTROLE DE PONTO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                        Icon(
                            imageVector = Icons.Default.PunchClock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Preencha o prefixo e registre seu ponto:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    // Prefix Input Field
                    OutlinedTextField(
                        value = inputPrefix,
                        onValueChange = { inputPrefix = it },
                        label = { Text("Prefixo do Microônibus (Ex: 4052)") },
                        modifier = Modifier.fillMaxWidth().testTag("prefix_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        leadingIcon = { Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid of Actions: Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.punchToday("entrada", inputPrefix)
                                Toast.makeText(context, "Entrada registrada!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(54.dp).testTag("btn_entrada"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Entrada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.punchToday("pausa", inputPrefix)
                                Toast.makeText(context, "Intervalo iniciado!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(54.dp).testTag("btn_pausa"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pausa", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid of Actions: Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.punchToday("retorno", inputPrefix)
                                Toast.makeText(context, "Retorno da pausa registrado!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(54.dp).testTag("btn_retorno"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retorno", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.punchToday("fim", inputPrefix)
                                Toast.makeText(context, "Expediente encerrado!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(54.dp).testTag("btn_fim"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fim", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sandy/Clay Option for Day-off (Folga)
                    Button(
                        onClick = {
                            viewModel.punchToday("folga")
                            Toast.makeText(context, "Registrado folga para hoje!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_folga_hoje"),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Weekend, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Opções de Folga", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Histórico de Pontos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showManualInputDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lançar Manual", fontSize = 12.sp)
                }
            }
        }

        if (workDays.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum ponto registrado ainda.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(workDays, key = { it.id }) { item ->
                WorkDayCard(
                    workDay = item,
                    onEditClick = { editWorkDayTarget = item },
                    onDeleteClick = { viewModel.deleteWorkDay(item) },
                    onShareClick = {
                        sharePontoOnWhatsApp(context, item)
                    }
                )
            }
        }
    }

    // Modal: Edit point register
    if (editWorkDayTarget != null) {
        WorkDayEditDialog(
            workDay = editWorkDayTarget!!,
            onDismiss = { editWorkDayTarget = null },
            onSave = { updated ->
                viewModel.saveWorkDay(updated)
                editWorkDayTarget = null
            }
        )
    }

    // Modal: Add point register manually
    if (showManualInputDialog) {
        WorkDayManualInputDialog(
            onDismiss = { showManualInputDialog = false },
            onSave = { newRecord ->
                viewModel.saveWorkDay(newRecord)
                showManualInputDialog = false
            }
        )
    }
}

@Composable
fun WorkDayCard(
    workDay: WorkDay,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Formatting date for visualization (e.g., 2026-05-25 to 25/05/2026)
                val visualDate = formatDateForDisplay(workDay.dateString)
                Column {
                    Text(
                        text = visualDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (workDay.prefix.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Prefixo: ${workDay.prefix}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartilhar",
                            tint = Color(0xFF25D366)
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (workDay.isFolga) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Weekend,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FOLGA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PunchTimeChip(label = "Entrada", time = workDay.entrada, color = MaterialTheme.colorScheme.primary)
                    PunchTimeChip(label = "Pausa", time = workDay.pausa, color = Color(0xFF8F6E50))
                    PunchTimeChip(label = "Retorno", time = workDay.retorno, color = Color(0xFF5A8F76))
                    PunchTimeChip(label = "Fim", time = workDay.fim, color = Color(0xFFB55D4C))
                }

                val workedHours = calculateWorkedHours(workDay)
                if (workedHours != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Trabalhado: $workedHours",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (workDay.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Obs: ${workDay.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun RowScope.PunchTimeChip(label: String, time: String?, color: Color) {
    Surface(
        modifier = Modifier.weight(1f),
        color = if (time != null) color.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (time != null) color else Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                time ?: "--:--",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (time != null) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.5f)
            )
        }
    }
}

// Dialog: Manual entry adding
@Composable
fun WorkDayManualInputDialog(
    onDismiss: () -> Unit,
    onSave: (WorkDay) -> Unit
) {
    val context = LocalContext.current
    var dateString by remember { mutableStateOf(getTodayDateRaw()) }
    var prefix by remember { mutableStateOf("") }
    var entrada by remember { mutableStateOf("") }
    var pausa by remember { mutableStateOf("") }
    var retorno by remember { mutableStateOf("") }
    var fim by remember { mutableStateOf("") }
    var isFolga by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lançar Ponto Manual") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date picker trigger button
                item {
                    Column {
                        Text("Data do Ponto", style = MaterialTheme.typography.labelMedium)
                        Button(
                            onClick = {
                                showDatePicker(context) { selectedDate ->
                                    dateString = selectedDate
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(formatDateForDisplay(dateString))
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("Prefixo do Microônibus") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isFolga, onCheckedChange = { isFolga = it })
                        Text("Este dia foi FOLGA")
                    }
                }

                if (!isFolga) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = entrada,
                                onValueChange = { entrada = formatTimeInput(it) },
                                label = { Text("Entrada") },
                                placeholder = { Text("08:00") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = pausa,
                                onValueChange = { pausa = formatTimeInput(it) },
                                label = { Text("Pausa") },
                                placeholder = { Text("12:00") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = retorno,
                                onValueChange = { retorno = formatTimeInput(it) },
                                label = { Text("Retorno") },
                                placeholder = { Text("13:00") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = fim,
                                onValueChange = { fim = formatTimeInput(it) },
                                label = { Text("Fim") },
                                placeholder = { Text("17:00") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observação") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanEntrada = entrada.trim().ifEmpty { null }
                    val cleanPausa = pausa.trim().ifEmpty { null }
                    val cleanRetorno = retorno.trim().ifEmpty { null }
                    val cleanFim = fim.trim().ifEmpty { null }

                    val record = WorkDay(
                        dateString = dateString,
                        prefix = prefix,
                        entrada = if (isFolga) null else cleanEntrada,
                        pausa = if (isFolga) null else cleanPausa,
                        retorno = if (isFolga) null else cleanRetorno,
                        fim = if (isFolga) null else cleanFim,
                        isFolga = isFolga,
                        notes = notes
                    )
                    onSave(record)
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Dialog: Editing existing entry
@Composable
fun WorkDayEditDialog(
    workDay: WorkDay,
    onDismiss: () -> Unit,
    onSave: (WorkDay) -> Unit
) {
    val context = LocalContext.current
    var dateString by remember { mutableStateOf(workDay.dateString) }
    var prefix by remember { mutableStateOf(workDay.prefix) }
    var entrada by remember { mutableStateOf(workDay.entrada ?: "") }
    var pausa by remember { mutableStateOf(workDay.pausa ?: "") }
    var retorno by remember { mutableStateOf(workDay.retorno ?: "") }
    var fim by remember { mutableStateOf(workDay.fim ?: "") }
    var isFolga by remember { mutableStateOf(workDay.isFolga) }
    var notes by remember { mutableStateOf(workDay.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Registro de Ponto") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column {
                        Text("Data do Ponto", style = MaterialTheme.typography.labelMedium)
                        Button(
                            onClick = {
                                showDatePicker(context) { selectedDate ->
                                    dateString = selectedDate
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(formatDateForDisplay(dateString))
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("Prefixo do Microônibus") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isFolga, onCheckedChange = { isFolga = it })
                        Text("Este dia foi FOLGA")
                    }
                }

                if (!isFolga) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = entrada,
                                onValueChange = { entrada = formatTimeInput(it) },
                                label = { Text("Entrada") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = pausa,
                                onValueChange = { pausa = formatTimeInput(it) },
                                label = { Text("Pausa") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = retorno,
                                onValueChange = { retorno = formatTimeInput(it) },
                                label = { Text("Retorno") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = fim,
                                onValueChange = { fim = formatTimeInput(it) },
                                label = { Text("Fim") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observação") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanEntrada = entrada.trim().ifEmpty { null }
                    val cleanPausa = pausa.trim().ifEmpty { null }
                    val cleanRetorno = retorno.trim().ifEmpty { null }
                    val cleanFim = fim.trim().ifEmpty { null }

                    val record = workDay.copy(
                        dateString = dateString,
                        prefix = prefix,
                        entrada = if (isFolga) null else cleanEntrada,
                        pausa = if (isFolga) null else cleanPausa,
                        retorno = if (isFolga) null else cleanRetorno,
                        fim = if (isFolga) null else cleanFim,
                        isFolga = isFolga,
                        notes = notes
                    )
                    onSave(record)
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// --- TAB 2: MINIBUS DAMAGES SCREEN ---

@Composable
fun DamagesScreen(viewModel: PontoViewModel, damageReports: List<DamageReport>) {
    val context = LocalContext.current
    var uploadPrefix by remember { mutableStateOf("") }
    var uploadDescription by remember { mutableStateOf("") }
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    var showReportCreation by remember { mutableStateOf(false) }

    // Launcher for standard Camera app
    val tempFile = remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempFile.value?.let { file ->
                pendingPhotoPath = file.absolutePath
            }
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = createImageFile(context)
            tempFile.value = file
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.pontoavaria.pwykt.fileprovider",
                file
            )
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Erro: Permissão de câmera é necessária para tirar fotos.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("damages_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vistoria de Avarias",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Registre qualquer arranhão, amassado ou avaria no microônibus com foto para evitar responsabilidade.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!showReportCreation) {
                        Button(
                            onClick = {
                                showReportCreation = true
                                pendingPhotoPath = null
                                uploadDescription = ""
                                uploadPrefix = ""
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_nova_avaria")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nova Foto de Avaria")
                        }
                    }
                }
            }
        }

        if (showReportCreation) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Novo Relatório de Avaria", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(onClick = { showReportCreation = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar")
                            }
                        }

                        // Photo container
                        if (pendingPhotoPath == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f))
                                    .clickable {
                                        val permissionCheck = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        )
                                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                            val file = createImageFile(context)
                                            tempFile.value = file
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "com.aistudio.pontoavaria.pwykt.fileprovider",
                                                file
                                            )
                                            cameraLauncher.launch(uri)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tirar Foto do Veículo", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = pendingPhotoPath,
                                    contentDescription = "Foto da Avaria",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clickable {
                                            pendingPhotoPath = null
                                        },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Excluir Foto",
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uploadPrefix,
                            onValueChange = { uploadPrefix = it },
                            label = { Text("Prefixo do Microônibus *") },
                            modifier = Modifier.fillMaxWidth().testTag("avaria_prefix_input"),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )

                        OutlinedTextField(
                            value = uploadDescription,
                            onValueChange = { uploadDescription = it },
                            label = { Text("Descrição da Avaria *") },
                            placeholder = { Text("Ex: Parachoque amassado traseiro esquerdo") },
                            modifier = Modifier.fillMaxWidth().testTag("avaria_desc_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showReportCreation = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }

                            Button(
                                onClick = {
                                    if (uploadPrefix.trim().isEmpty() || uploadDescription.trim().isEmpty()) {
                                        Toast.makeText(context, "Por favor, preencha o prefixo e a descrição.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (pendingPhotoPath == null) {
                                        Toast.makeText(context, "Por favor, tire uma foto do problema.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                    viewModel.saveDamageReport(
                                        prefix = uploadPrefix.trim(),
                                        photoPath = pendingPhotoPath!!,
                                        description = uploadDescription.trim(),
                                        dateString = todayStr
                                    )

                                    Toast.makeText(context, "Avaria salva com sucesso!", Toast.LENGTH_SHORT).show()
                                    showReportCreation = false
                                    pendingPhotoPath = null
                                    uploadDescription = ""
                                    uploadPrefix = ""
                                },
                                modifier = Modifier.weight(1.5f).testTag("btn_salvar_avaria")
                            ) {
                                Text("Salvar Avaria")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Cadastros Registrados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (damageReports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma avaria cadastrada.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(damageReports, key = { it.id }) { item ->
                DamageReportCard(
                    report = item,
                    onDeleteClick = { viewModel.deleteDamageReport(item) },
                    onShareClick = {
                        shareDamageReport(context, item)
                    }
                )
            }
        }
    }
}

@Composable
fun DamageReportCard(
    report: DamageReport,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo Thumbnail
            Surface(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color.Gray.copy(alpha = 0.2f)
            ) {
                AsyncImage(
                    model = report.photoPath,
                    contentDescription = "Foto Avaria",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Description column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PREFIXO: ${report.prefix}",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = report.dateString,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartilhar no WhatsApp",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 3: ALARMS & REMINDERS SCREEN ---

@Composable
fun RemindersScreen(viewModel: PontoViewModel, reminders: List<PontoReminder>) {
    val context = LocalContext.current
    var showAddCustomReminderDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reminders_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lembrete de Horários",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure avisos automáticos e não esqueça de bater o ponto. Ative a chave e clique sobre uma caixa para alterar a hora do despertador.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alarmes ativos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showAddCustomReminderDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar")
                }
            }
        }

        items(reminders, key = { it.id }) { item ->
            ReminderCard(
                reminder = item,
                onToggleChange = { checked ->
                    viewModel.toggleReminder(item, checked)
                },
                onCardClick = {
                    showTimePicker(context, item.hour, item.minute) { h, m ->
                        val updated = item.copy(hour = h, minute = m)
                        viewModel.saveReminder(updated)
                    }
                },
                onDeleteClick = {
                    viewModel.deleteReminder(item)
                }
            )
        }
    }

    if (showAddCustomReminderDialog) {
        var label by remember { mutableStateOf("") }
        var hour by remember { mutableStateOf(8) }
        var minute by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showAddCustomReminderDialog = false },
            title = { Text("Novo Lembrete de Horário") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Nome do Alarme (ex: Saída do Almoço)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            showTimePicker(context, hour, minute) { h, m ->
                                hour = h
                                minute = m
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(String.format("Definir Hora: %02d:%02d", hour, minute))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (label.trim().isEmpty()) {
                            Toast.makeText(context, "Por favor insira um nome", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val customReminder = PontoReminder(
                            label = label.trim(),
                            hour = hour,
                            minute = minute,
                            isEnabled = true
                        )
                        viewModel.saveReminder(customReminder)
                        showAddCustomReminderDialog = false
                    }
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomReminderDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ReminderCard(
    reminder: PontoReminder,
    onToggleChange: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.label,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (reminder.isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (reminder.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reminder.timeFormatted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (reminder.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggleChange
                )
                
                // Allow deleting custom alarms (we let them delete default alarms too if they want)
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir lembrete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// --- UTILITY METHODS ---

fun formatDateForDisplay(apiDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault())
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = parser.parse(apiDate)
        if (date != null) formatter.format(date) else apiDate
    } catch (e: Exception) {
        apiDate
    }
}

fun getTodayDateRaw(): String {
    return SimpleDateFormat("yyyy-MM-DD", Locale.getDefault()).format(Date())
}

fun formatTimeInput(raw: String): String {
    // Basic automatic formatting of HH:mm
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.length >= 4 -> "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
        digits.length == 3 -> "${digits.substring(0, 2)}:${digits.substring(2, 3)}"
        else -> digits
    }
}

fun showDatePicker(context: Context, onDateSet: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatter = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault())
            val dateCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            onDateSet(formatter.format(dateCal.time))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun showTimePicker(context: Context, initialHour: Int, initialMinute: Int, onTimeSet: (Int, Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSet(hourOfDay, minute)
        },
        initialHour,
        initialMinute,
        true
    ).show()
}

fun calculateWorkedHours(workDay: WorkDay): String? {
    if (workDay.isFolga || workDay.entrada == null || workDay.fim == null) return null
    return try {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val inTime = format.parse(workDay.entrada)!!
        val outTime = format.parse(workDay.fim)!!

        var diff = outTime.time - inTime.time

        if (workDay.pausa != null && workDay.retorno != null) {
            val breakStart = format.parse(workDay.pausa)!!
            val breakEnd = format.parse(workDay.retorno)!!
            val breakTotal = breakEnd.time - breakStart.time
            if (breakTotal > 0) {
                diff -= breakTotal
            }
        }

        if (diff < 0) return null

        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        String.format("%02dh %02dm", hours, minutes)
    } catch (e: Exception) {
        null
    }
}

fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("AVARIA_${timeStamp}_", ".jpg", storageDir)
}

// Share text details on Whatsapp
fun sharePontoOnWhatsApp(context: Context, workDay: WorkDay) {
    val visualDate = formatDateForDisplay(workDay.dateString)
    val totalHours = calculateWorkedHours(workDay) ?: "--"
    
    val text = StringBuilder().apply {
        append("📝 *REGISTRO DE PONTO*\n")
        append("📅 *Data:* $visualDate\n")
        if (workDay.prefix.isNotEmpty()) {
            append("🚐 *Microônibus:* ${workDay.prefix}\n")
        }
        if (workDay.isFolga) {
            append("🌴 *Status:* FOLGA / DESCANSO\n")
        } else {
            append("🕐 *Entrada:* ${workDay.entrada ?: "--:--"}\n")
            append("☕ *Pausa Almoço:* ${workDay.pausa ?: "--:--"}\n")
            append("🔄 *Retorno Almoço:* ${workDay.retorno ?: "--:--"}\n")
            append("🕕 *Fim Expediente:* ${workDay.fim ?: "--:--"}\n")
            append("⏱️ *Total Trabalhado:* $totalHours\n")
        }
        if (workDay.notes.isNotEmpty()) {
            append("📌 *Obs:* ${workDay.notes}\n")
        }
    }.toString()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    
    try {
        intent.setPackage("com.whatsapp")
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to standard app chooser if WhatsApp is not installed
        val chooser = Intent.createChooser(intent, "Compartilhar Registro")
        context.startActivity(chooser)
    }
}

// Share damage photo and text on WhatsApp
fun shareDamageReport(context: Context, report: DamageReport) {
    val text = StringBuilder().apply {
        append("⚠️ *RELATÓRIO DE AVARIA DO MICROÔNIBUS*\n")
        append("🚐 *Prefixo:* ${report.prefix}\n")
        append("📅 *Data:* ${report.dateString}\n")
        append("📝 *Descrição:* ${report.description}\n")
    }.toString()

    val file = File(report.photoPath)
    if (file.exists()) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "com.aistudio.pontoavaria.pwykt.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(intent, "Compartilhar Avaria")
            context.startActivity(chooser)
        }
    } else {
        // Fallback to text share only if photo file is lost
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Compartilhar Avaria (Apenas Texto)")
        context.startActivity(chooser)
    }
}
