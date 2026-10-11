package com.softwareparatodos.ladria

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Consulta solo el release público oficial. No instala APK en segundo plano. */
data class AppUpdate(val version: String, val releaseUrl: String)

object UpdateChecker {
    private const val API = "https://api.github.com/repos/maxijones012/LadrIA/releases/latest"
    private val versionPattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$")

    fun check(context: Context): AppUpdate? {
        val connection = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "LadrIA-Android-Updater")
            instanceFollowRedirects = false
        }
        try {
            val status = connection.responseCode
            if (status == 404) return null // Aún no hay releases públicos.
            if (status != 200) throw IllegalStateException("GitHub respondió $status")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null
            val version = json.optString("tag_name").removePrefix("v")
            val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
            if (!isNewer(version, current)) return null
            val releaseUrl = json.optString("html_url")
            require(releaseUrl.startsWith("https://github.com/maxijones012/LadrIA/releases/")) {
                "Origen de actualización desconocido"
            }
            return AppUpdate(version, releaseUrl)
        } finally {
            connection.disconnect()
        }
    }

    internal fun isNewer(remote: String, local: String): Boolean {
        val a = versionPattern.matchEntire(remote)?.destructured ?: return false
        val b = versionPattern.matchEntire(local)?.destructured ?: return false
        val aa = listOf(a.component1(), a.component2(), a.component3()).map { it.toIntOrNull() ?: return false }
        val bb = listOf(b.component1(), b.component2(), b.component3()).map { it.toIntOrNull() ?: return false }
        return (0..2).firstOrNull { aa[it] != bb[it] }?.let { aa[it] > bb[it] } ?: false
    }
}
