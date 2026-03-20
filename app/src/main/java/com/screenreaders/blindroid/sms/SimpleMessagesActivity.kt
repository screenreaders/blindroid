package com.screenreaders.blindroid.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.screenreaders.blindroid.R

class SimpleMessagesActivity : AppCompatActivity() {
    private lateinit var tabInbox: Button
    private lateinit var tabCompose: Button
    private lateinit var inboxContainer: LinearLayout
    private lateinit var composeContainer: LinearLayout
    private lateinit var inboxList: RecyclerView
    private lateinit var composeNumber: EditText
    private lateinit var composeBody: EditText
    private lateinit var sendButton: Button

    private val adapter = SimpleMessageAdapter(::onMessageClick)
    private var messages: List<MessageEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_messages)

        tabInbox = findViewById(R.id.tabInbox)
        tabCompose = findViewById(R.id.tabCompose)
        inboxContainer = findViewById(R.id.inboxContainer)
        composeContainer = findViewById(R.id.composeContainer)
        inboxList = findViewById(R.id.inboxList)
        composeNumber = findViewById(R.id.composeNumber)
        composeBody = findViewById(R.id.composeBody)
        sendButton = findViewById(R.id.composeSend)

        inboxList.layoutManager = LinearLayoutManager(this)
        inboxList.adapter = adapter

        tabInbox.setOnClickListener { switchTab(Tab.INBOX) }
        tabCompose.setOnClickListener { switchTab(Tab.COMPOSE) }
        sendButton.setOnClickListener { sendMessage() }

        switchTab(Tab.INBOX)
        loadInbox()
    }

    private fun switchTab(tab: Tab) {
        inboxContainer.visibility = if (tab == Tab.INBOX) LinearLayout.VISIBLE else LinearLayout.GONE
        composeContainer.visibility = if (tab == Tab.COMPOSE) LinearLayout.VISIBLE else LinearLayout.GONE
        tabInbox.isEnabled = tab != Tab.INBOX
        tabCompose.isEnabled = tab != Tab.COMPOSE
    }

    private fun loadInbox() {
        if (!hasPermission(Manifest.permission.READ_SMS)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), REQ_READ_SMS)
            return
        }
        Thread {
            val list = mutableListOf<MessageEntry>()
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.Inbox.ADDRESS,
                    Telephony.Sms.Inbox.BODY,
                    Telephony.Sms.Inbox.DATE
                ),
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC LIMIT 50"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(0) ?: ""
                    val body = it.getString(1) ?: ""
                    val date = it.getLong(2)
                    list.add(MessageEntry(address, body, date))
                }
            }
            runOnUiThread {
                messages = list
                adapter.submit(list)
            }
        }.start()
    }

    private fun sendMessage() {
        val number = composeNumber.text?.toString()?.trim().orEmpty()
        val body = composeBody.text?.toString()?.trim().orEmpty()
        if (number.isBlank() || body.isBlank()) {
            Toast.makeText(this, R.string.simple_messages_missing, Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQ_SEND_SMS)
            return
        }
        try {
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(body)
            manager.sendMultipartTextMessage(number, null, parts, null, null)
            Toast.makeText(this, R.string.simple_messages_sent, Toast.LENGTH_SHORT).show()
            composeBody.setText("")
        } catch (_: Exception) {
            Toast.makeText(this, R.string.simple_messages_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onMessageClick(message: MessageEntry) {
        composeNumber.setText(message.address)
        switchTab(Tab.COMPOSE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_READ_SMS) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                loadInbox()
            } else {
                Toast.makeText(this, R.string.simple_messages_permission_read, Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQ_SEND_SMS) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                sendMessage()
            } else {
                Toast.makeText(this, R.string.simple_messages_permission_send, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    enum class Tab { INBOX, COMPOSE }

    companion object {
        private const val REQ_READ_SMS = 1301
        private const val REQ_SEND_SMS = 1302
    }
}

data class MessageEntry(
    val address: String,
    val body: String,
    val date: Long
) {
    fun format(context: android.content.Context): String {
        val preview = if (body.length > 80) body.substring(0, 80) + "…" else body
        val whenText = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale("pl", "PL"))
            .format(java.util.Date(date))
        return "${preview}\n$whenText"
    }
}
