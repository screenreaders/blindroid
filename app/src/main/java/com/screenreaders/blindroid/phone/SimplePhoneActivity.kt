package com.screenreaders.blindroid.phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.screenreaders.blindroid.R
import java.text.SimpleDateFormat
import java.util.Locale

class SimplePhoneActivity : AppCompatActivity() {
    private lateinit var tabDialer: Button
    private lateinit var tabContacts: Button
    private lateinit var tabRecents: Button
    private lateinit var dialerContainer: LinearLayout
    private lateinit var contactsContainer: LinearLayout
    private lateinit var recentsContainer: LinearLayout
    private lateinit var numberView: TextView
    private lateinit var contactsSearch: EditText
    private lateinit var contactsList: RecyclerView
    private lateinit var recentsList: RecyclerView

    private val contactsAdapter = SimpleContactAdapter(::onContactClick)
    private val recentsAdapter = SimpleRecentAdapter(::onRecentClick)
    private var contacts: List<PhoneContact> = emptyList()
    private var recents: List<RecentCall> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_phone)

        tabDialer = findViewById(R.id.tabDialer)
        tabContacts = findViewById(R.id.tabContacts)
        tabRecents = findViewById(R.id.tabRecents)
        dialerContainer = findViewById(R.id.dialerContainer)
        contactsContainer = findViewById(R.id.contactsContainer)
        recentsContainer = findViewById(R.id.recentsContainer)
        numberView = findViewById(R.id.dialerNumber)
        contactsSearch = findViewById(R.id.contactsSearch)
        contactsList = findViewById(R.id.contactsList)
        recentsList = findViewById(R.id.recentsList)

        contactsList.layoutManager = LinearLayoutManager(this)
        contactsList.adapter = contactsAdapter
        recentsList.layoutManager = LinearLayoutManager(this)
        recentsList.adapter = recentsAdapter

        setupDialerPad()
        tabDialer.setOnClickListener { switchTab(Tab.DIALER) }
        tabContacts.setOnClickListener { switchTab(Tab.CONTACTS) }
        tabRecents.setOnClickListener { switchTab(Tab.RECENTS) }

        contactsSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterContacts()
            }
        })

        switchTab(Tab.DIALER)
        loadContacts()
        loadRecents()
    }

    private fun setupDialerPad() {
        val pad = findViewById<android.widget.GridLayout>(R.id.dialerPad)
        val digits = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "*", "0", "#"
        )
        digits.forEach { digit ->
            val button = Button(this).apply {
                text = digit
                textSize = 24f
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(6, 6, 6, 6)
                }
                setOnClickListener { appendDigit(digit) }
            }
            pad.addView(button)
        }
        findViewById<Button>(R.id.dialerBackspace).setOnClickListener { removeLastDigit() }
        findViewById<Button>(R.id.dialerClear).setOnClickListener { numberView.text = "" }
        findViewById<Button>(R.id.dialerCall).setOnClickListener { placeCall(numberView.text.toString()) }
    }

    private fun appendDigit(digit: String) {
        numberView.text = numberView.text.toString() + digit
    }

    private fun removeLastDigit() {
        val current = numberView.text.toString()
        if (current.isNotEmpty()) {
            numberView.text = current.dropLast(1)
        }
    }

    private fun placeCall(number: String) {
        if (number.isBlank()) return
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, R.string.simple_phone_call_failed, Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQ_CALL)
        }
    }

    private fun switchTab(tab: Tab) {
        dialerContainer.visibility = if (tab == Tab.DIALER) LinearLayout.VISIBLE else LinearLayout.GONE
        contactsContainer.visibility = if (tab == Tab.CONTACTS) LinearLayout.VISIBLE else LinearLayout.GONE
        recentsContainer.visibility = if (tab == Tab.RECENTS) LinearLayout.VISIBLE else LinearLayout.GONE
        tabDialer.isEnabled = tab != Tab.DIALER
        tabContacts.isEnabled = tab != Tab.CONTACTS
        tabRecents.isEnabled = tab != Tab.RECENTS
    }

    private fun loadContacts() {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
            return
        }
        Thread {
            val list = mutableListOf<PhoneContact>()
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0) ?: ""
                    val number = it.getString(1) ?: ""
                    if (number.isNotBlank()) {
                        list.add(PhoneContact(name, number))
                    }
                }
            }
            runOnUiThread {
                contacts = list
                filterContacts()
            }
        }.start()
    }

    private fun filterContacts() {
        val query = contactsSearch.text?.toString()?.trim()?.lowercase().orEmpty()
        val filtered = if (query.isBlank()) {
            contacts
        } else {
            contacts.filter { it.name.lowercase().contains(query) || it.number.contains(query) }
        }
        contactsAdapter.submit(filtered)
    }

    private fun loadRecents() {
        if (!hasPermission(Manifest.permission.READ_CALL_LOG)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALL_LOG), REQ_CALL_LOG)
            return
        }
        Thread {
            val list = mutableListOf<RecentCall>()
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 50"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0) ?: ""
                    val number = it.getString(1) ?: ""
                    val date = it.getLong(2)
                    val type = it.getInt(3)
                    list.add(RecentCall(name, number, date, type))
                }
            }
            runOnUiThread {
                recents = list
                recentsAdapter.submit(list)
            }
        }.start()
    }

    private fun onContactClick(contact: PhoneContact) {
        numberView.text = contact.number
        switchTab(Tab.DIALER)
    }

    private fun onRecentClick(recent: RecentCall) {
        numberView.text = recent.number
        switchTab(Tab.DIALER)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CONTACTS) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                loadContacts()
            } else {
                Toast.makeText(this, R.string.simple_phone_permission_contacts, Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQ_CALL_LOG) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                loadRecents()
            } else {
                Toast.makeText(this, R.string.simple_phone_permission_calls, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    enum class Tab { DIALER, CONTACTS, RECENTS }

    companion object {
        private const val REQ_CALL = 1201
        private const val REQ_CONTACTS = 1202
        private const val REQ_CALL_LOG = 1203
    }
}

data class PhoneContact(val name: String, val number: String)

data class RecentCall(val name: String, val number: String, val time: Long, val type: Int) {
    fun format(context: android.content.Context): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.forLanguageTag("pl-PL")).format(java.util.Date(time))
        val typeText = when (type) {
            CallLog.Calls.INCOMING_TYPE -> context.getString(R.string.simple_phone_call_incoming)
            CallLog.Calls.OUTGOING_TYPE -> context.getString(R.string.simple_phone_call_outgoing)
            CallLog.Calls.MISSED_TYPE -> context.getString(R.string.simple_phone_call_missed)
            CallLog.Calls.REJECTED_TYPE -> context.getString(R.string.simple_phone_call_rejected)
            else -> context.getString(R.string.simple_phone_call_other)
        }
        return "$typeText • $date"
    }
}
