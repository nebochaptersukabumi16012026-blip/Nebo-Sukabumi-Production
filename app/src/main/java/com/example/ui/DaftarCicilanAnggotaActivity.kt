package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.ui.theme.MyApplicationTheme

class DaftarCicilanAnggotaActivity : ComponentActivity() {
    private val viewModel: CommunityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "daftar_cicilan_anggota") {
                        composable("daftar_cicilan_anggota") {
                            DaftarCicilanAnggotaScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
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
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            AnggotaDetailScreen(
                                navController = navController,
                                viewModel = viewModel,
                                memberId = id
                            )
                        }
                    }
                }
            }
        }
    }
}
