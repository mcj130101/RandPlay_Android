package com.mcj130101.randplayer.data.scanner

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
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
    private val resolveMutex = Mutex()

    fun discoverServers(): Flow<List<DiscoveredServer>> = callbackFlow {
        val discoveredServices = mutableMapOf<String, DiscoveredServer>()

        fun emitList() {
            trySend(discoveredServices.values.toList())
        }

        // 1. Start Subnet Port 445 Scanner (Fast IP Probe)
        val subnetJob = launch(Dispatchers.IO) {
            val subnetPrefix = getLocalSubnetPrefix() ?: return@launch
            coroutineScope {
                (1..254).map { i ->
                    async {
                        val ip = "$subnetPrefix.$i"
                        if (isSmbPortOpen(ip, 445, 600)) {
                            val hostName = try {
                                InetAddress.getByName(ip).hostName
                            } catch (_: Exception) {
                                ip
                            }
                            val name = if (hostName != ip) "$hostName ($ip)" else "SMB Server ($ip)"
                            val server = DiscoveredServer(name = name, host = ip, ip = ip)
                            synchronized(discoveredServices) {
                                discoveredServices[ip] = server
                            }
                            emitList()
                        }
                    }
                }.awaitAll()
            }
        }

        // 2. Start mDNS DNS-SD Scanner (Sequenced Resolution)
        fun createListener() = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                launch(Dispatchers.IO) {
                    resolveMutex.withLock {
                        try {
                            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                                override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                    val ipStr = resolvedInfo.host?.hostAddress
                                    val server = DiscoveredServer(
                                        name = resolvedInfo.serviceName,
                                        host = resolvedInfo.host?.hostName ?: ipStr ?: resolvedInfo.serviceName,
                                        ip = ipStr
                                    )
                                    val key = ipStr ?: server.name
                                    synchronized(discoveredServices) {
                                        discoveredServices[key] = server
                                    }
                                    emitList()
                                }
                            })
                        } catch (_: Exception) {}
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val key = serviceInfo.host?.hostAddress ?: serviceInfo.serviceName
                synchronized(discoveredServices) {
                    discoveredServices.remove(key)
                }
                emitList()
            }

            override fun onDiscoveryStopped(regType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        val listeners = SERVICE_TYPES.map { type ->
            val listener = createListener()
            try {
                nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (_: Exception) {}
            listener
        }

        awaitClose {
            subnetJob.cancel()
            listeners.forEach { listener ->
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (_: Exception) {}
            }
        }
    }

    private fun isSmbPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalSubnetPrefix(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            val lastDot = host.lastIndexOf('.')
                            if (lastDot > 0) {
                                return host.substring(0, lastDot)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
