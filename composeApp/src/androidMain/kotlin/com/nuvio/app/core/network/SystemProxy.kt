package com.nuvio.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import java.net.URI

/**
 * Reads the Android system HTTP proxy (the per-network proxy configured in
 * Wi-Fi / hotspot settings). NetShare-style proxy-only hotspots serve traffic
 * exclusively through that proxy and have no local DNS: every OkHttp client
 * already honors the proxy, but libmpv ignores it and resolves DNS itself,
 * which blocks forever on such networks. [mpvProxyForUrl] hands mpv the same
 * proxy OkHttp already uses; loopback, LAN and system-bypassed targets stay
 * direct.
 */
internal object SystemProxy {

    fun isActive(context: Context): Boolean = currentProxyUrl(context) != null

    fun currentProxyUrl(context: Context): String? {
        val fromLinkProperties = linkPropertiesProxy(context)
        if (fromLinkProperties != null) return fromLinkProperties

        val host = System.getProperty("http.proxyHost")?.trim()?.takeIf { it.isNotBlank() }
            ?: return null
        val port = System.getProperty("http.proxyPort")?.trim()?.takeIf { it.isNotBlank() }
            ?: return null
        val portNumber = port.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        return "http://$host:$portNumber"
    }

    /**
     * Proxy URL to hand to mpv for [url], or null when the target should stay
     * direct: no system proxy configured, loopback/LAN/link-local destinations,
     * the proxy host itself, or hosts the system excludes from proxying.
     */
    fun mpvProxyForUrl(context: Context, url: String): String? {
        val proxy = currentProxyUrl(context) ?: return null
        val host = runCatching { URI(url).host }.getOrNull()
            ?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: return null

        if (isDirectTarget(host)) return null
        if (isInExclusionList(context, host)) return null
        if (host == proxyHostOrNull()) return null
        return proxy
    }

    private fun linkPropertiesProxy(context: Context): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val httpProxy = runCatching {
            connectivityManager?.activeNetwork?.let { network ->
                connectivityManager.getLinkProperties(network)?.httpProxy
            }
        }.getOrNull()
        val host = httpProxy?.host?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val port = httpProxy.port.takeIf { it != 0 } ?: return null
        return "http://$host:$port"
    }

    private fun proxyHostOrNull(): String? =
        System.getProperty("http.proxyHost")?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

    private fun isInExclusionList(context: Context, host: String): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val exclusions = runCatching {
            connectivityManager?.activeNetwork?.let { network ->
                connectivityManager.getLinkProperties(network)?.httpProxy?.exclusionList?.toList()
            }
        }.getOrNull() ?: emptyList()
        if (exclusions.isEmpty()) return false

        return exclusions.any { pattern ->
            val normalized = pattern.trim().lowercase().removePrefix(".")
            when {
                normalized.isBlank() -> false
                normalized == host -> true
                normalized.startsWith("*") -> host.endsWith(normalized.removePrefix("*").removePrefix("."))
                else -> host == normalized || host.endsWith(".$normalized")
            }
        }
    }

    private fun isDirectTarget(host: String): Boolean {
        if (host == "localhost" || host == "::1" || host.endsWith(".localhost")) return true
        if (host.startsWith("[")) return false // IPv6 literal: leave the decision to the system
        if (host.startsWith("127.")) return true
        if (host.startsWith("10.")) return true
        if (host.startsWith("192.168.")) return true
        if (host.startsWith("169.254.")) return true
        if (host.startsWith("fd")) return true // IPv6 unique-local (fc00::/7)
        if (isPrivate172(host)) return true
        return false
    }

    /** 172.16.0.0/12 — 172.16.x.x through 172.31.x.x. */
    private fun isPrivate172(host: String): Boolean {
        if (!host.startsWith("172.")) return false
        val secondOctet = host.removePrefix("172.").substringBefore('.').toIntOrNull() ?: return false
        return secondOctet in 16..31
    }
}
