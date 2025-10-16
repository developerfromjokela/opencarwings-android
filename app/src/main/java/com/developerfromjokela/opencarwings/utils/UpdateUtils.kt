package com.developerfromjokela.opencarwings.utils

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object UpdateUtils {
    suspend fun isAppUpToDate(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pkgInfo.versionName
            val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
            val currentTag = "v${versionName}(${versionCode})"

            val url =
                URL("https://api.github.com/repos/developerfromjokela/opencarwings-android/releases")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")

            conn.inputStream.bufferedReader().use { reader ->
                val releases = JSONArray(reader.readText())
                if (releases.length() == 0) return@withContext true
                val latestTag = releases.getJSONObject(0).getString("tag_name")

                return@withContext latestTag == currentTag
            }
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

}