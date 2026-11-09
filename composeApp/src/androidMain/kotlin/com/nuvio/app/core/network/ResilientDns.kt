package com.nuvio.app.core.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * IPv4-first DNS with a DNS-over-HTTPS fallback.
 *
 * OkHttp only consults [Dns] for DIRECT connections — requests selected for the
 * system HTTP proxy skip DNS entirely. So on a proxy-only hotspot this class is
 * normally never hit. When a connection does go direct and the network's local
 * DNS is dead (NetShare hotspots, where Android Private DNS / DoT:853 cannot
 * run), the system lookup would previously block for many seconds and fail.
 * Falling back to DoH resolvers addressed by IP literal keeps those direct
 * connections working with zero local DNS. On healthy networks nothing changes:
 * the system lookup succeeds on the first try.
 */
class ResilientDns : Dns {

    private val delegate = IPv4FirstDns()

    private val dohFallback: List<Dns> by lazy {
        val bootstrap = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
        listOf("1.1.1.1", "8.8.8.8").map { resolverIp ->
            DnsOverHttps.Builder()
                .client(bootstrap)
                // IP-literal URL: no bootstrap DNS needed, and both resolvers ship
                // TLS certificates valid for their bare IPs.
                .url("https://$resolverIp/dns-query".toHttpUrl())
                .build()
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        try {
            return delegate.lookup(hostname)
        } catch (systemFailure: Exception) {
            dohFallback.forEach { resolver ->
                try {
                    return resolver.lookup(hostname)
                } catch (_: Exception) {
                    // try the next resolver
                }
            }
            throw systemFailure
        }
    }
}
