package com.cimdriver.app.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.cimdriver.app.BuildConfig

data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = ""
)

object UpdateChecker {

    private const val VERSION_URL = "https://raw.githubusercontent.com/Vircos01/CIMDriver/main/android/version.json"

    suspend fun checkForUpdates(): UpdateInfo {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(VERSION_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    val remoteVersionCode = jsonObject.getInt("versionCode")
                    val remoteVersionName = jsonObject.getString("versionName")
                    val apkUrl = jsonObject.getString("apkUrl")
                    val notes = jsonObject.optString("releaseNotes", "")

                    val currentVersionCode = BuildConfig.VERSION_CODE

                    if (remoteVersionCode > currentVersionCode) {
                        return@withContext UpdateInfo(
                            isUpdateAvailable = true,
                            latestVersionCode = remoteVersionCode,
                            latestVersionName = remoteVersionName,
                            downloadUrl = apkUrl,
                            releaseNotes = notes
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Error checking for updates", e)
            }
            
            UpdateInfo(isUpdateAvailable = false)
        }
    }
}
