package com.example.randplayer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RandPlayerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupBouncyCastle()
    }

    private fun setupBouncyCastle() {
        // Android provides a stripped down BouncyCastle provider by default ("BC").
        // SMBJ and JCIFS-NG require full MD4 implementation for NTLM authentication which
        // was removed in newer Android versions. We remove the system provider and insert
        // the full BouncyCastle provider to fix the "no such algorithm: MD4" exception.
        val provider = java.security.Security.getProvider("BC")
        if (provider?.javaClass?.name != org.bouncycastle.jce.provider.BouncyCastleProvider::class.java.name) {
            java.security.Security.removeProvider("BC")
            java.security.Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 1)
        }
    }
}
