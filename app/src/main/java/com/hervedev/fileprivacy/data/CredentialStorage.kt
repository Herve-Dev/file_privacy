package com.hervedev.fileprivacy.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

@Suppress("DEPRECATION")
class CredentialStorage(context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_credentials_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePassword(connectionId: Long, password: String, type: String = "smb") {
        sharedPreferences.edit {
            putString(key(connectionId, type), password)
        }
    }

    fun getPassword(connectionId: Long, type: String = "smb"): String? {
        return sharedPreferences.getString(key(connectionId, type), null)
    }

    fun deletePassword(connectionId: Long, type: String = "smb") {
        sharedPreferences.edit {
            remove(key(connectionId, type))
        }
    }

    private fun key(connectionId: Long, type: String = "smb"): String = "${type}_password_$connectionId"
}
