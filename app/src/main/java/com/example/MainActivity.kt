package com.example
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.testTag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

@Composable
fun MainScreen(rootNavController: androidx.navigation.NavController, viewModel: CommunityViewModel) {
    val navController = rememberNavController()
    val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
    
    val requireNewPassword by viewModel.requireNewPassword.collectAsStateWithLifecycle()
    if (requireNewPassword) {
        var passwordBaru by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }
        val context = androidx.compose.ui.platform.LocalContext.current
        
        AlertDialog(
            onDismissRequest = { /* Do not allow dismiss */ },
            title = {
                Text(
                    "Buat Password Baru",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Demi keamanan akun, silakan buat password baru.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = passwordBaru,
                        onValueChange = { passwordBaru = it },
                        label = { Text("Password Baru") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("new_password_input"),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Password Baru") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_new_password_input"),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordBaru.isBlank() || confirmPassword.isBlank()) {
                            android.widget.Toast.makeText(context, "Password tidak boleh kosong!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (passwordBaru != confirmPassword) {
                            android.widget.Toast.makeText(context, "Password baru dan konfirmasi tidak cocok!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        viewModel.completeResetRequest(passwordBaru) { success, msg ->
                            isSaving = false
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_new_password_button"),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = androidx.compose.ui.graphics.Color.White)
                    } else {
                        Text("Simpan")
                    }
                }
            }
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var showGuestLogoutDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    if (showGuestLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showGuestLogoutDialog = false },
            title = { Text("Konfirmasi") },
            text = { Text("Apakah Anda yakin ingin keluar?") },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestLogoutDialog = false
                        viewModel.logout()
                        rootNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Konfirmasi") },
            text = { Text("Apakah Anda ingin keluar dari aplikasi?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        (rootNavController.context as? android.app.Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Tidak")
                }
            }
        )
    }

    val rootNavBackStackEntry by rootNavController.currentBackStackEntryAsState()
    val isMainActive = rootNavBackStackEntry?.destination?.route == "main"

    BackHandler(enabled = isMainActive) {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            showExitDialog = true
        }
    }
    
    val items = listOfNotNull(
        Triple("dashboard", "Dashboard", Icons.Default.Home),
        Triple("anggota_list", "Anggota", Icons.Default.Person),
        Triple("uang_kas", "Pembayaran", Icons.Default.List),
        if (userRole != "GUEST") {
            Triple("laporan", "Laporan", Icons.Default.Info)
        } else null,
        if (userRole == "BENDAHARA" || userRole == "ADMIN" || viewModel.loggedInUserRole.value == "DEVELOPER") {
            Triple("pengaturan", "Pengaturan", Icons.Default.Settings)
        } else if (userRole == "GUEST" || userRole == "ANGGOTA") {
            Triple("logout_guest", "Keluar", Icons.Default.ExitToApp)
        } else null
    )

    Scaffold(
        bottomBar = {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = androidx.compose.ui.graphics.Color.Black,
                shadowElevation = 8.dp
            ) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    items.forEach { (route, title, icon) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                        NavigationBarItem(
                            icon = { 
                                Icon(icon, contentDescription = title) 
                            },
                            label = { 
                                androidx.compose.animation.AnimatedVisibility(visible = selected) {
                                    Text(title, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            selected = selected,
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                unselectedIconColor = androidx.compose.ui.graphics.Color.White,
                                selectedTextColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                unselectedTextColor = androidx.compose.ui.graphics.Color.White,
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFF2196F3).copy(alpha = 0.2f)
                            ),
                            onClick = {
                                if (route == "logout_guest") {
                                    showGuestLogoutDialog = true
                                } else if (currentDestination?.route != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable("dashboard") {
                DashboardScreen(navController = rootNavController, viewModel = viewModel)
            }
            
            composable("anggota_list") {
                AnggotaListScreen(navController = rootNavController, viewModel = viewModel)
            }
            
            composable("uang_kas") {
                UangKasScreen(navController = rootNavController, viewModel = viewModel)
            }
            
            composable("laporan") {
                LaporanScreen(navController = rootNavController, viewModel = viewModel)
            }
            
            composable("pengaturan") {
                if (userRole != "BENDAHARA" && userRole != "ADMIN" && userRole != "DEVELOPER") {
                    LaunchedEffect(Unit) {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                } else {
                    SettingsScreen(navController = rootNavController, viewModel = viewModel)
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CommunityViewModel = viewModel()

            MyApplicationTheme(darkTheme = true) {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("splash") {
                        SplashScreen(navController = navController, viewModel = viewModel)
                    }
                    
                    composable("login") {
                        LoginScreen(navController = navController, viewModel = viewModel)
                    }
                    
                    composable("forgot_password") {
                        ForgotPasswordScreen(navController = navController, viewModel = viewModel)
                    }
                    
                    composable("reset_password_requests") {
                        ResetPasswordRequestsScreen(navController = navController, viewModel = viewModel)
                    }
                    
                    composable(
                        "main",
                        deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "https://nebosukabumi.net/" }, androidx.navigation.navDeepLink { uriPattern = "https://www.nebosukabumi.net/" })
                    ) {
                        MainScreen(rootNavController = navController, viewModel = viewModel)
                    }
                    
                    composable("daftar_hadir") {
                        DaftarHadirScreen(navController = navController, viewModel = viewModel)
                    }

                    composable("catatan_bebas") {
                        CatatanScreen(navController = navController, viewModel = viewModel)
                    }
                    
                    composable(
                        route = "anggota_form?id={id}",
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                        )
                    ) { backStackEntry ->
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole != "BENDAHARA" && userRole != "ADMIN" && userRole != "DEVELOPER") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            val memberId = backStackEntry.arguments?.getInt("id") ?: -1
                            AnggotaFormScreen(
                                navController = navController,
                                viewModel = viewModel,
                                memberId = memberId
                            )
                        }
                    }
                    
                    composable(
                        route = "anggota_detail?id={id}",
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                        )
                    ) { backStackEntry ->
                        val memberId = backStackEntry.arguments?.getInt("id") ?: -1
                        AnggotaDetailScreen(
                            navController = navController,
                            viewModel = viewModel,
                            memberId = memberId
                        )
                    }
                    
                    composable("customization") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole != "ADMIN" && userRole != "DEVELOPER") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            CustomizationScreen(navController = navController, viewModel = viewModel)
                        }
                    }

                    composable("audit_log") {
                        if (viewModel.loggedInUserRole.value == "DEVELOPER") {
                            AuditLogScreen(navController = navController)
                        } else {
                            navController.popBackStack()
                        }
                    }
                    composable("login_log") {
                        if (viewModel.loggedInUserRole.value == "DEVELOPER") {
                            LoginLogScreen(navController = navController)
                        } else {
                            navController.popBackStack()
                        }
                    }
                    composable("developer_panel") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole != "DEVELOPER") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            DeveloperPanelScreen(navController = navController, viewModel = viewModel)
                        }
                    }
                    
                    composable(
                        route = "pembayaran_form?anggotaId={anggotaId}&jenis={jenis}",
                        arguments = listOf(
                            navArgument("anggotaId") {
                                type = NavType.IntType
                                defaultValue = -1
                            },
                            navArgument("jenis") {
                                type = NavType.StringType
                                defaultValue = "KAS"
                            }
                        )
                    ) { backStackEntry ->
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole != "BENDAHARA" && userRole != "ADMIN" && userRole != "DEVELOPER") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            val anggotaId = backStackEntry.arguments?.getInt("anggotaId") ?: -1
                            val jenis = backStackEntry.arguments?.getString("jenis") ?: "KAS"
                            PembayaranFormScreen(
                                navController = navController,
                                viewModel = viewModel,
                                anggotaId = anggotaId,
                                jenisPembayaran = jenis
                            )
                        }
                    }
                    
                    composable("identitas_komunitas") {
                        com.example.ui.IdentitasKomunitasScreen(navController, viewModel)
                    }
                    composable("pengeluaran_kas") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole != "BENDAHARA" && userRole != "ADMIN" && userRole != "DEVELOPER") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            PengeluaranScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }

                    composable("anggota_list_full") {
                        AnggotaListScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }

                    composable("anniversary_summary") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole == "GUEST" || userRole == "ANGGOTA") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            AnniversarySummaryScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }

                    composable("cicilan_summary") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole == "GUEST" || userRole == "ANGGOTA") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            CicilanSummaryScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }

                    composable("sisa_cicilan_only") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole == "GUEST" || userRole == "ANGGOTA") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            SisaCicilanOnlyScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }

                    composable("kas_keliling") {
                        KasKelilingScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }

                    composable(
                        route = "kas_keliling_form?id={id}",
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                        )
                    ) { backStackEntry ->
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole != "BENDAHARA" && userRole != "ADMIN" && userRole != "DEVELOPER") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            KasKelilingFormScreen(
                                navController = navController,
                                viewModel = viewModel,
                                kasKelilingId = id
                            )
                        }
                    }
                    
                    composable(
                        route = "pengeluaran_form?id={id}",
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                        )
                    ) { backStackEntry ->
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole != "BENDAHARA" && userRole != "ADMIN" && userRole != "DEVELOPER") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            PengeluaranFormScreen(
                                navController = navController,
                                viewModel = viewModel,
                                pengeluaranId = id
                            )
                        }
                    }

                    composable("detail_anggota") {
                        DetailAnggotaScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }

                    composable("detail_uang_kas") {
                        DetailUangKasScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }

                    composable("detail_iuran_aniv") {
                        DetailIuranAnivScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }

                    composable("detail_sisa_cicilan") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole == "GUEST" || userRole == "ANGGOTA") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            DetailSisaCicilanScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }

                    composable("detail_belum_kas") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole == "GUEST" || userRole == "ANGGOTA") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            DetailBelumKasScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }

                    composable("detail_belum_aniv") {
                        val userRole by viewModel.loggedInUserRole.collectAsStateWithLifecycle()
                        if (userRole == "GUEST" || userRole == "ANGGOTA") {
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } else {
                            DetailBelumAnivScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }

                    composable("detail_total_pengeluaran") {
                        DetailTotalPengeluaranScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }

                    composable("detail_saldo_kas") {
                        DetailSaldoKasScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
