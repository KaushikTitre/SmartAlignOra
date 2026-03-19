package com.example.smartalignoraapplication.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

// ─────────────────────────────────────────────────────────────────────────────
// EmailService — SMTP वापरून auto email send करतो
//
// ✅ User ला manually send करायची गरज नाही
// ✅ Sender Gmail (app चा) → Receiver (user चा emergency contact)
// ✅ Gmail SMTP वापरतो — port 587
//
// SETUP:
// 1. एक Gmail account बनवा (sender साठी) — example: smartalignora.alert@gmail.com
// 2. Gmail → Settings → Security → 2-Step Verification ON करा
// 3. Gmail → App Passwords → generate करा (16 digit password)
// 4. खाली SENDER_EMAIL आणि SENDER_APP_PASSWORD set करा
// ─────────────────────────────────────────────────────────────────────────────
object EmailService {

    // ✅ हे तुमच्या sender Gmail ने replace करा
    private const val SENDER_EMAIL        = "atharvaswankhade@gmail.com"
    private const val SENDER_APP_PASSWORD = "kqmo rsff yylp nlrm"  // Gmail App Password

    // ─────────────────────────────────────────────────────────────────────────
    // Auto send fall alert email
    // Background thread वर run होतो — UI block होत नाही
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun sendFallAlert(
        toEmail:   String,   // receiver — user ने settings मध्ये दिलेला
        time:      String,
        latitude:  String,
        longitude: String,
        mapsLink:  String,
        fallCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Gmail SMTP properties
            val props = Properties().apply {
                put("mail.smtp.host",            "smtp.gmail.com")
                put("mail.smtp.port",            "587")
                put("mail.smtp.auth",            "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.ssl.trust",       "smtp.gmail.com")
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout",           "10000")
            }

            // Authenticator — sender credentials
            val auth = object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD)
                }
            }

            val session = Session.getInstance(props, auth)

            // Email message
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL, "SmartAlignOra Alert"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "🚨 FALL DETECTED — SmartAlignOra Emergency Alert"

                // HTML email body — professional दिसतो
                setContent(
                    buildEmailHtml(
                        toEmail    = toEmail,
                        time       = time,
                        latitude   = latitude,
                        longitude  = longitude,
                        mapsLink   = mapsLink,
                        fallCount  = fallCount
                    ),
                    "text/html; charset=utf-8"
                )
            }

            // ✅ Send करतो
            Transport.send(message)
            Log.d("EmailService", "✅ Fall alert email sent to $toEmail")
            true

        } catch (e: Exception) {
            Log.e("EmailService", "❌ Email send failed: ${e.message}")
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML email template — professional look
    // ─────────────────────────────────────────────────────────────────────────
    private fun buildEmailHtml(
        toEmail:   String,
        time:      String,
        latitude:  String,
        longitude: String,
        mapsLink:  String,
        fallCount: Int
    ): String {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px;">
            
              <div style="max-width: 600px; margin: 0 auto; background: white; 
                          border-radius: 12px; overflow: hidden; 
                          box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
            
                <!-- Header -->
                <div style="background: linear-gradient(135deg, #DC2626, #991B1B); 
                            padding: 30px 24px; text-align: center;">
                  <h1 style="color: white; margin: 0; font-size: 24px;">🚨 FALL DETECTED</h1>
                  <p style="color: #FCA5A5; margin: 8px 0 0 0; font-size: 14px;">
                    SmartAlignOra Emergency Alert
                  </p>
                </div>
            
                <!-- Body -->
                <div style="padding: 24px;">
            
                  <p style="color: #374151; font-size: 15px; line-height: 1.6;">
                    A <strong>fall has been detected</strong> by the SmartAlignOra wearable device.
                    This alert was triggered after <strong>$fallCount consecutive fall detections</strong>.
                  </p>
            
                  <!-- Info cards -->
                  <div style="background: #FEF2F2; border-left: 4px solid #DC2626; 
                              padding: 16px; border-radius: 8px; margin: 16px 0;">
                    <table style="width: 100%; border-collapse: collapse;">
                      <tr>
                        <td style="padding: 6px 0; color: #6B7280; font-size: 13px; width: 120px;">
                          📅 Time
                        </td>
                        <td style="padding: 6px 0; color: #111827; font-size: 13px; font-weight: bold;">
                          $time
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 6px 0; color: #6B7280; font-size: 13px;">
                          📍 Latitude
                        </td>
                        <td style="padding: 6px 0; color: #111827; font-size: 13px; font-weight: bold;">
                          $latitude
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 6px 0; color: #6B7280; font-size: 13px;">
                          📍 Longitude
                        </td>
                        <td style="padding: 6px 0; color: #111827; font-size: 13px; font-weight: bold;">
                          $longitude
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 6px 0; color: #6B7280; font-size: 13px;">
                          🔁 Fall Count
                        </td>
                        <td style="padding: 6px 0; color: #DC2626; font-size: 13px; font-weight: bold;">
                          $fallCount consecutive detections
                        </td>
                      </tr>
                    </table>
                  </div>
            
                  <!-- Map button -->
                  <div style="text-align: center; margin: 20px 0;">
                    <a href="$mapsLink"
                       style="background: #6B21A8; color: white; padding: 12px 28px; 
                              text-decoration: none; border-radius: 8px; font-weight: bold;
                              font-size: 14px; display: inline-block;">
                      📍 View Location on Map
                    </a>
                  </div>
            
                  <p style="color: #EF4444; font-size: 14px; font-weight: bold; text-align: center;">
                    Please check on the user immediately!
                  </p>
            
                </div>
            
                <!-- Footer -->
                <div style="background: #F9FAFB; padding: 16px 24px; text-align: center;
                            border-top: 1px solid #E5E7EB;">
                  <p style="color: #9CA3AF; font-size: 12px; margin: 0;">
                    Sent by SmartAlignOra · Emergency Alert System
                  </p>
                  <p style="color: #9CA3AF; font-size: 11px; margin: 4px 0 0 0;">
                    This is an automated alert. Do not reply to this email.
                  </p>
                </div>
            
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}