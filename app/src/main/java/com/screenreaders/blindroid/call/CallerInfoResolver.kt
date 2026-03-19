package com.screenreaders.blindroid.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telecom.Call
import android.text.TextUtils
import androidx.core.content.ContextCompat

object CallerInfoResolver {
    fun displayName(context: Context, call: Call): String {
        val handle = call.details.handle
        val number = handle?.schemeSpecificPart
        if (!number.isNullOrBlank()) {
            val name = lookupContactName(context, number)
            if (!name.isNullOrBlank()) return name
            return number
        }
        val nameFromNetwork = call.details.callerDisplayName
        if (!nameFromNetwork.isNullOrBlank()) return nameFromNetwork
        return context.getString(com.screenreaders.blindroid.R.string.unknown_caller)
    }

    private fun lookupContactName(context: Context, number: String): String? {
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        )
        if (permission != PackageManager.PERMISSION_GRANTED) return null

        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(number)
            .build()

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!TextUtils.isEmpty(name)) return name
            }
        }
        return null
    }
}
