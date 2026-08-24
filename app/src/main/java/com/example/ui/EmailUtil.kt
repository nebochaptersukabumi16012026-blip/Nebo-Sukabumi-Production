package com.example.ui

import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmailUtil {
    private const val SENDER_EMAIL = "nebochaptersukabumi16012026@gmail.com"
    private const val SENDER_PASS = "APP_PASSWORD_HERE" // Change this to real app password

    suspend fun sendOTP(recipientEmail: String, otp: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (SENDER_PASS == "APP_PASSWORD_HERE") {
                android.util.Log.d("EmailUtil", "Mock sending OTP $otp to $recipientEmail from $SENDER_EMAIL")
                return@withContext true
            }
            try {
                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = "smtp.gmail.com"
                props["mail.smtp.port"] = "587"

                val session = Session.getInstance(props,
                    object : javax.mail.Authenticator() {
                        override fun getPasswordAuthentication(): PasswordAuthentication {
                            return PasswordAuthentication(SENDER_EMAIL, SENDER_PASS)
                        }
                    })

                val message = MimeMessage(session)
                message.setFrom(InternetAddress(SENDER_EMAIL))
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
                )
                message.subject = "Kode OTP Pemulihan Password - Nebo Sukabumi"
                message.setText("Halo,\n\nBerikut adalah kode OTP untuk mereset password akun Anda: $otp\n\nKode ini berlaku selama 5 menit.\n\nJangan berikan kode ini kepada siapapun.\n\nSalam,\nAdmin Nebo Sukabumi")

                Transport.send(message)
                true
            } catch (e: MessagingException) {
                e.printStackTrace()
                android.util.Log.e("EmailUtil", "Failed to send email: ${e.message}")
                false
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("EmailUtil", "Exception sending email: ${e.message}")
                false
            }
        }
    }
}
