package com.nuvio.app.core.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * DNS for streaming clients on hostile networks (NetShare-style proxy-only
 * hotspots). OkHttp skips DNS for proxied requests entirely, so this only runs
 * for direct connections: it tries the system resolver first and, when the
 * network has no working DNS, falls back to DNS-over-HTTPS. Results keep the
 * IPv4-first ordering [IPv4FirstDns] provided.
 */
internal object ResilientDns : Dns {

    private const val DOH_URL = "https://dns.google/resolve?name=%s&type=%s"
    private const val CACHE_TTL_MS = 60_000L

    // The DoH client itself must never need DNS: dns.google is served by these
    // anycast IPs with a certificate valid for the dns.google hostname, so
    // OkHttp's hostname verification (done against the URL host) passes.
    private val dohBootstrapAddresses = listOf(
        InetAddress.getByName("8.8.8.8"),
        InetAddress.getByName("8.8.4.4"),
    )

    private val dohClient: OkHttpClient = OkHttpClient.Builder()
        .dns { hostname ->
            if (hostname.equals("dns.google", ignoreCase = true)) {
                dohBootstrapAddresses
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
        }
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .cache(null)
        .build()

    private data class CacheEntry(val addresses: List<InetAddress>, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override fun lookup(hostname: String): List<InetAddress> {
        val normalized = hostname.trim().removeSuffix(".")
        if (normalized.isBlank()) throw UnknownHostException("Empty hostname")

        val cached = cache[normalized]
        if (cached != null && cached.expiresAtMs > System.currentTimeMillis()) {
            return cached.addresses
        }
        if (cached != null) {
            cache.remove(normalized, cached)
        }

        return try {
            orderIpv4First(Dns.SYSTEM.lookup(normalized)).also { addresses ->
                cache[normalized] = CacheEntry(addresses, System.currentTimeMillis() + CACHE_TTL_MS)
            }
        } catch (primary: UnknownHostException) {
            val addresses = runCatching { lookupOverHttps(normalized) }.getOrNull().orEmpty()
            if (addresses.isEmpty()) throw primary
            addresses.also {
                cache[normalized] = CacheEntry(it, System.currentTimeMillis() + CACHE_TTL_MS)
            }
        }
    }

    private fun lookupOverHttps(hostname: String): List<InetAddress> = queryDoh(hostname)

    private val ipv4Literal = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    private fun queryDoh(hostname: String): List<InetAddress> {
        val request = Request.Builder()
            .url(DOH_URL.format(hostname, "A"))
            .header("Accept", "application/dns-json")
            .build()
        val body = dohClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            response.body?.string() ?: return emptyList()
        }

        val json = JSONObject(body)
        if (json.optInt("Status", -1) != 0) return emptyList()
        val answers: JSONArray = json.optJSONArray("Answer") ?: return emptyList()

        val addresses = mutableListOf<InetAddress>()
        for (index in 0 until answers.length()) {
            val answer = answers.optJSONObject(index) ?: continue
            if (answer.optInt("type", -1) != 1) continue // A records only (CNAME chains are pre-resolved)
            val data = answer.optString("data")
            // Only parse IPv4 literals: InetAddress.getByName(hostnames) would block on DNS.
            if (!ipv4Literal.matches(data)) continue
            runCatching { InetAddress.getByName(data) }
                .getOrNull()
                ?.let { addresses.add(it) }
        }
        return addresses
    }

    private fun orderIpv4First(addresses: List<InetAddress>): List<InetAddress> =
        addresses.sortedBy { if (it is Inet4Address) 0 else 1 }.ifEmpty { addresses }
}
