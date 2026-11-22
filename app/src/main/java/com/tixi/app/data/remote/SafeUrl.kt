package com.tixi.app.data.remote

import java.net.URI
import java.util.Locale

/**
 * 下载地址安全闸门：MinerU 返回的 full_zip_url 来自服务端，必须防 SSRF / 本地文件读取。
 * 纯 JVM 实现（只依赖 java.net.URI），可直接单测。
 */
object SafeUrl {

    private val blockedHostSuffixes = listOf(
        ".local", ".localhost", ".localdomain", ".internal", ".intranet",
        ".lan", ".home", ".corp", ".home.arpa",
    )

    fun isAllowed(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        // 控制字符 / 空白一律拒绝，避免 URL 解析歧义
        if (trimmed.any { it.isWhitespace() || it.code < 0x20 }) return false

        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false

        // 1) 仅 https
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        // 2) 拒绝 userinfo（https://user:pass@host 之类）
        if (uri.userInfo != null) return false
        // 3) 仅允许标准端口（443 或不写端口）
        val port = uri.port
        if (port != -1 && port != 443) return false

        val host = uri.host?.let { normalizeHost(it) } ?: return false
        if (host.isEmpty()) return false

        // 4) 拒绝环回 / 内网 / 链路本地
        if (isBlockedLiteral(host)) return false
        // 5) 拒绝内网域名后缀与单标签主机名
        if (blockedHostSuffixes.any { host.endsWith(it) }) return false
        if (!host.contains('.') && !isIpv6(host)) return false

        return true
    }

    private fun normalizeHost(host: String): String {
        var h = host.trim().lowercase(Locale.ROOT)
        if (h.startsWith("[") && h.endsWith("]")) h = h.substring(1, h.length - 1)
        return h.trimEnd('.')
    }

    private fun isIpv6(host: String): Boolean = host.contains(':')

    private fun isBlockedLiteral(host: String): Boolean {
        if (host == "localhost") return true
        if (isIpv6(host)) {
            return host == "::1" ||
                host == "0:0:0:0:0:0:0:1" ||
                host == "::" ||
                host.startsWith("fe80:") || // 链路本地 fe80::/10
                host.startsWith("fc") ||     // 唯一本地 fc00::/7
                host.startsWith("fd")
        }

        val octets = host.split('.')
        if (octets.size == 4 && octets.all { it.isNotEmpty() && it.all { c -> c.isDigit() } }) {
            val nums = octets.map { it.toIntOrNull() ?: return true }
            if (nums.any { it !in 0..255 }) return true
            val a = nums[0]
            val b = nums[1]
            return when {
                a == 127 -> true              // 环回
                a == 10 -> true               // A 类私有
                a == 0 -> true                // 0.0.0.0/8
                a == 172 && b in 16..31 -> true // B 类私有
                a == 192 && b == 168 -> true  // C 类私有
                a == 169 && b == 254 -> true  // 链路本地（含云元数据 169.254.169.254）
                a == 100 && b in 64..127 -> true // CGNAT
                else -> false
            }
        }
        // 纯数字主机（如 2130706433）一律拒绝
        return isIpv6(host).not() && host.all { it.isDigit() || it == '.' }
    }
}
