package com.example.ui

import android.os.Bundle
import android.view.View
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
import com.example.R
import com.example.ui.theme.MyApplicationTheme

class DetailAnggotaActivity : ComponentActivity() {
    private val viewModel: CommunityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Ambil ID/NRA user yang sedang login dari SessionManager / SharedPreferences (loggedInNra)
        val loggedInNra = SessionManager.getUserNra(this).trim()
        val loggedInId = SessionManager.getUserId(this)

        // 2. Ambil ID/NRA dari anggota yang diklik (targetNra / targetId)
        val targetId = intent?.getIntExtra("id", -1) ?: -1
        val targetNra = (intent?.getStringExtra("nra") ?: "").trim()

        // 3. Ambil Role user yang sedang login (userRole)
        val userRole = SessionManager.getRole(this).trim().uppercase()

        // 4. Lakukan pengecekan kondisi hak akses (Privasi Data Cicilan):
        val isSelf = (loggedInNra.isNotBlank() && targetNra.isNotBlank() && loggedInNra.equals(targetNra, ignoreCase = true)) ||
                (loggedInId != -1 && targetId != -1 && loggedInId == targetId)
        val isPrivileged = userRole == "ADMIN" || userRole == "BENDAHARA" || userRole == "PENGURUS" || userRole == "DEVELOPER"

        val canViewCicilan = isSelf || isPrivileged

        // Pengecekan kondisi jika menggunakan layout XML (CardView Visibility):
        val cardDataCicilan = findViewById<View?>(R.id.cardDataCicilan)
        val cardRiwayatCicilan = findViewById<View?>(R.id.cardRiwayatCicilan)
        if (cardDataCicilan != null && cardRiwayatCicilan != null) {
            if (loggedInNra == targetNra || userRole == "ADMIN" || userRole == "BENDAHARA" || userRole == "PENGURUS" || userRole == "DEVELOPER") {
                cardDataCicilan.visibility = View.VISIBLE
                cardRiwayatCicilan.visibility = View.VISIBLE
            } else {
                cardDataCicilan.visibility = View.GONE
                cardRiwayatCicilan.visibility = View.GONE
            }
        }

        // Tampilkan halaman detail anggota Jetpack Compose dengan aturan privasi data cicilan
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "detail_anggota") {
                        composable("detail_anggota") {
                            AnggotaDetailScreen(
                                navController = navController,
                                viewModel = viewModel,
                                memberId = targetId,
                                canViewCicilanOverride = canViewCicilan
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper method publik untuk mengatur visibilitas Card data cicilan
     * sesuai aturan RBAC dan Privasi Data
     */
    fun setupCicilanPrivacy(
        cardDataCicilan: View?,
        cardRiwayatCicilan: View?,
        loggedInNra: String,
        targetNra: String,
        userRole: String
    ) {
        val roleUpper = userRole.trim().uppercase()
        if (loggedInNra == targetNra || roleUpper == "ADMIN" || roleUpper == "BENDAHARA" || roleUpper == "PENGURUS" || roleUpper == "DEVELOPER") {
            cardDataCicilan?.visibility = View.VISIBLE
            cardRiwayatCicilan?.visibility = View.VISIBLE
        } else {
            cardDataCicilan?.visibility = View.GONE
            cardRiwayatCicilan?.visibility = View.GONE
        }
    }
}
