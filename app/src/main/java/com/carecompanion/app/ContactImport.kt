package com.carecompanion.app

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactImport {
    @SuppressLint("Recycle")
    fun readNameAndPhone(context: Context, contactUri: Uri): Pair<String?, String?> {
        val resolver = context.contentResolver
        var name: String? = null
        var phone: String? = null
        resolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            ),
            null,
            null,
            null
        )?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                if (nameIdx >= 0) name = c.getString(nameIdx)?.trim()?.takeIf { it.isNotEmpty() }
                val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                if (idIdx >= 0) {
                    val id = c.getString(idIdx) ?: return@use
                    resolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )?.use { p ->
                        if (p.moveToFirst()) {
                            val numIdx = p.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIdx >= 0) {
                                phone = p.getString(numIdx)?.replace("\\s".toRegex(), "")?.trim()
                            }
                        }
                    }
                }
            }
        }
        return name to phone
    }
}
