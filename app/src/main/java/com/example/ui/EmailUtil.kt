package com.example.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmailUtil {
    private const val SENDER_EMAIL = "nebochaptersukabumi16012026@gmail.com"
    private const val SENDER_PASS = "APP_PASSWORD_HERE" // Change this to real app password

    suspend fun sendOTP(recipientEmail: String, otp: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("EmailUtil", "Sending OTP $otp to $recipientEmail from $SENDER_EMAIL")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("EmailUtil", "Exception sending email: ${e.message}")
                false
            }
        }
    }
}
