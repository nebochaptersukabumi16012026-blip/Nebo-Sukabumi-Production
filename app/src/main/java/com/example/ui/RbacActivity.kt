package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.MainActivity
import com.example.R

class RbacActivity : AppCompatActivity() {

    private var currentRole: String = "member"
    private var isVerified: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. PROTEKSI HAK AKSES (RBAC Gate)
        // Halaman ini HANYA boleh diakses oleh user dengan role: "ADMIN", "BENDAHARA", atau "PENGURUS"
        val activeRole = SessionManager.getRole(this).trim().uppercase()
        val allowedRoles = listOf("ADMIN", "BENDAHARA", "PENGURUS")

        if (activeRole !in allowedRoles) {
            Toast.makeText(
                this,
                "Akses Terbatas untuk Pengurus & Admin!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        setContentView(R.layout.activity_rbac)

        // Support ActionBar Back Button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Simulasi RBAC"

        val tvRole = findViewById<TextView>(R.id.tvRole)
        val tvStatusVerifikasi = findViewById<TextView>(R.id.tvStatusVerifikasi)
        val btnRoleMember = findViewById<Button>(R.id.btnRoleMember)
        val btnRolePengurus = findViewById<Button>(R.id.btnRolePengurus)
        val btnRoleAdmin = findViewById<Button>(R.id.btnRoleAdmin)
        val btnTogglePending = findViewById<Button>(R.id.btnTogglePending)
        val btnInputKas = findViewById<Button>(R.id.btnInputKas)
        val btnKelolaAnggota = findViewById<Button>(R.id.btnKelolaAnggota)
        val btnLaporan = findViewById<Button>(R.id.btnLaporan)

        fun updateUI() {
            tvRole.text = "Role: ${currentRole.uppercase()}"
            if (isVerified) {
                tvStatusVerifikasi.text = "Status Verifikasi: VERIFIED"
                tvStatusVerifikasi.setTextColor(0xFF10B981.toInt())
            } else {
                tvStatusVerifikasi.text = "Status Verifikasi: PENDING"
                tvStatusVerifikasi.setTextColor(0xFFEF4444.toInt())
            }

            btnInputKas.visibility = if (isVerified && (currentRole == "admin" || currentRole == "pengurus")) View.VISIBLE else View.GONE
            btnKelolaAnggota.visibility = if (isVerified && currentRole == "admin") View.VISIBLE else View.GONE
            btnLaporan.visibility = View.VISIBLE

            if (!isVerified) {
                AlertDialog.Builder(this)
                    .setTitle("Akses Terbatas")
                    .setMessage("Akun Anda belum diverifikasi oleh pengurus")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        btnRoleMember.setOnClickListener {
            currentRole = "member"
            isVerified = true
            updateUI()
        }

        btnRolePengurus.setOnClickListener {
            currentRole = "pengurus"
            isVerified = true
            updateUI()
        }

        btnRoleAdmin.setOnClickListener {
            currentRole = "admin"
            isVerified = true
            updateUI()
        }

        btnTogglePending.setOnClickListener {
            isVerified = !isVerified
            updateUI()
        }

        // =========================================================================
        // Tombol Kembali ke Dashboard Utama
        // =========================================================================
        val btnKembali = findViewById<Button>(R.id.btnKembaliKeDashboard)
        btnKembali?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        updateUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
