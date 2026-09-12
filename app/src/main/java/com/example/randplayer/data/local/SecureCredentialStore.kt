package com.example.randplayer.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SmbCredential(
    val username: String,
    val password: String,
    val domain: String?
)

@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredential(
        credentialId: String,
        username: String,
        password: String,
        domain: String?
    ) {
        sharedPreferences.edit().apply {
            putString("${credentialId}_user", username)
            putString("${credentialId}_pass", password)
            putString("${credentialId}_domain", domain)
            apply()
        }
    }

    fun getCredential(credentialId: String): SmbCredential? {
        val username = sharedPreferences.getString("${credentialId}_user", null) ?: return null
        val password = sharedPreferences.getString("${credentialId}_pass", null) ?: return null
        val domain = sharedPreferences.getString("${credentialId}_domain", null)
        return SmbCredential(username, password, domain)
    }

    fun deleteCredential(credentialId: String) {
        sharedPreferences.edit().apply {
            remove("${credentialId}_user")
            remove("${credentialId}_pass")
            remove("${credentialId}_domain")
            apply()
        }
    }
}
