package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.FinanceViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.reminder.BillReminderWorker.scheduleDailyBillCheck(applicationContext)

        val openTabExtra = intent.getIntExtra("OPEN_TAB", -1)

        setContent {
            MyApplicationTheme {
                val viewModel: FinanceViewModel by viewModels { FinanceViewModel.Factory(applicationContext) }
                val items by viewModel.allItems.collectAsState()
                val loanPayments by viewModel.allLoanPayments.collectAsState()
                val boxMovements by viewModel.allBoxMovements.collectAsState()
                val recurringBills by viewModel.allRecurringBills.collectAsState()
                val categoryBudgets by viewModel.allCategoryBudgets.collectAsState()
                val autoBackupTime by viewModel.autoBackupTime.collectAsState()
                val apartmentSubcategories by viewModel.apartmentSubcategories.collectAsState()

                var selectedTab by remember { mutableIntStateOf(if (openTabExtra in 0..3) openTabExtra else 0) }
                var showProfileSettings by remember { mutableStateOf(false) }
                var transactionsFilterToOpen by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Resumo") },
                                label = {
                                    Text(
                                        text = "Resumo",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Rounded.Savings, contentDescription = "Caixinhas") },
                                label = {
                                    Text(
                                        text = "Caixinhas",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = "Desafio 52S") },
                                label = {
                                    Text(
                                        text = "Desafio 52S",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Rounded.Handshake, contentDescription = "Empréstimos") },
                                label = {
                                    Text(
                                        text = "Empréstimos",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                items = items,
                                loanPayments = loanPayments,
                                recurringBills = recurringBills,
                                categoryBudgets = categoryBudgets,
                                onNavigateToTab = { selectedTab = it },
                                onOpenCategory = { filter -> transactionsFilterToOpen = filter },
                                onQuickAdd = {},
                                onDeleteItem = { viewModel.deleteItem(it) },
                                onProfileClick = { showProfileSettings = true },
                                onSaveRecurringBill = { viewModel.updateRecurringBill(it) },
                                onDeleteRecurringBill = { viewModel.deleteRecurringBill(it) },
                                onSaveCategoryBudget = { cat, lim -> viewModel.setCategoryBudget(cat, lim) },
                                onDeleteCategoryBudget = { viewModel.deleteCategoryBudget(it) }
                            )
                            1 -> BoxesScreen(
                                items = items,
                                boxMovements = boxMovements,
                                onAddItem = { viewModel.insertItem(it) },
                                onAddMovement = { boxId, amountCents, isDeposit, date, note, onSuccess, onError ->
                                    viewModel.addBoxMovement(boxId, amountCents, isDeposit, date, note, onSuccess, onError)
                                },
                                onDeleteMovement = { viewModel.deleteBoxMovement(it) },
                                onUpdateItem = { viewModel.updateItem(it) },
                                onDeleteItem = { viewModel.deleteItem(it) },
                                onProfileClick = { showProfileSettings = true }
                            )
                            2 -> ChallengeScreen(
                                items = items,
                                onAddItem = { viewModel.insertItem(it) },
                                onUpdateItem = { viewModel.updateItem(it) },
                                onStartChallenge = { viewModel.start52WeekChallenge(it) },
                                onResetChallenge = { viewModel.reset52WeekChallenge() },
                                onArchiveChallenge = { viewModel.archive52WeekChallenge() },
                                onProfileClick = { showProfileSettings = true }
                            )
                            3 -> LentAndBillsScreen(
                                items = items,
                                loanPayments = loanPayments,
                                onAddItem = { viewModel.insertItem(it) },
                                onUpdateItem = { viewModel.updateItem(it) },
                                onDeleteItem = { viewModel.deleteItem(it) },
                                onAddPayment = { loanId, amountCents, date, note, onSuccess, onError ->
                                    viewModel.addLoanPayment(loanId, amountCents, date, note, onSuccess, onError)
                                },
                                onDeletePayment = { viewModel.deleteLoanPayment(it) },
                                onSettleLoan = { loanId, date, note ->
                                    viewModel.settleLoan(loanId, date, note)
                                },
                                onReopenLoan = { loanId, removeLastPayment ->
                                    viewModel.reopenLoan(loanId, removeLastPayment)
                                },
                                onUpdatePrincipal = { loanId, newPrincipalCents, onSuccess, onError ->
                                    viewModel.updateLoanPrincipal(loanId, newPrincipalCents, onSuccess, onError)
                                },
                                onUpdateLoan = { loanId, title, amountCents, date, onSuccess, onError ->
                                    viewModel.updateLoan(loanId, title, amountCents, date, onSuccess, onError)
                                },
                                onProfileClick = { showProfileSettings = true }
                            )
                        }
                    }
                }

                if (transactionsFilterToOpen != null) {
                    Dialog(
                        onDismissRequest = { transactionsFilterToOpen = null },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                TransactionsScreen(
                                    items = items,
                                    apartmentSubcategories = apartmentSubcategories,
                                    onAddApartmentSubcategory = { viewModel.addApartmentSubcategory(it) },
                                    onUpdateApartmentSubcategory = { old, new -> viewModel.updateApartmentSubcategory(old, new) },
                                    onDeleteApartmentSubcategory = { viewModel.deleteApartmentSubcategory(it) },
                                    onResetApartmentSubcategories = { viewModel.resetApartmentSubcategories() },
                                    initialFilter = transactionsFilterToOpen ?: "TUDO",
                                    onAddItem = { viewModel.insertItem(it) },
                                    onUpdateItem = { viewModel.updateItem(it) },
                                    onDeleteItem = { viewModel.deleteItem(it) },
                                    onProfileClick = { showProfileSettings = true },
                                    onBack = { transactionsFilterToOpen = null }
                                )
                            }
                        }
                    }
                }

                if (showProfileSettings) {
                    Dialog(
                        onDismissRequest = { showProfileSettings = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { showProfileSettings = false }) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowBack,
                                            contentDescription = "Voltar",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Perfil e Segurança",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                BackupScreen(
                                    items = items,
                                    autoBackupTime = autoBackupTime,
                                    onRestoreAutoBackup = { onSuccess, onError ->
                                        viewModel.restoreFromAutoBackup(onSuccess, onError)
                                    },
                                    onManualExport = { password, file, onSuccess, onError ->
                                        viewModel.createBackup(password, file, onSuccess, onError)
                                    },
                                    onManualRestore = { password, file, onSuccess, onError ->
                                        viewModel.restoreBackup(password, file, onSuccess, onError)
                                    },
                                    onWipeAllData = {
                                        viewModel.wipeAllData()
                                        showProfileSettings = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
