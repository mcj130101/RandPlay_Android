package com.example.randplayer.data.scanner

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredServer(
    val name: String,
    val host: String,
    val ip: String?
)

@Singleton
class SmbDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val SERVICE_TYPES = listOf("_smb._tcp.", "_microsoft-ds._tcp.")

    fun discoverServers(): Flow<List<DiscoveredServer>> = callbackFlow {
        val discoveredServices = mutableMapOf<String, DiscoveredServer>()

        fun createListener() = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Some devices might report serviceType with a trailing dot or without
                // NsdManager handles this but we should be careful
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        val server = DiscoveredServer(
                            name = resolvedInfo.serviceName,
                            host = resolvedInfo.host.hostName,
                            ip = resolvedInfo.host.hostAddress
                        )
                        discoveredServices[server.name] = server
                        trySend(discoveredServices.values.toList())
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                discoveredServices.remove(serviceInfo.serviceName)
                trySend(discoveredServices.values.toList())
            }

            override fun onDiscoveryStopped(regType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        val listeners = SERVICE_TYPES.map { type ->
            val listener = createListener()
            nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            listener
        }

        awaitClose {
            listeners.forEach { listener ->
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (e: Exception) {
                    // Ignore if already stopped
                }
            }
        }
    }
}
