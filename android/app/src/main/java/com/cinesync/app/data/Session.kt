package com.cinesync.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cinesync.app.BuildConfig

/** Holds the logged-in user's JWT + identity. Observable by Compose. */
object Session {
    private lateinit var prefs: SharedPreferences

    var token by mutableStateOf<String?>(null); private set
    var userId by mutableLongStateOf(-1L); private set
    var name by mutableStateOf(""); private set
    var email by mutableStateOf(""); private set
    var serverUrl by mutableStateOf(BuildConfig.BASE_URL); private set

    val loggedIn: Boolean get() = !token.isNullOrBlank()

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("cinesync_session", Context.MODE_PRIVATE)
        token = prefs.getString("token", null)
        userId = prefs.getLong("userId", -1L)
        name = prefs.getString("name", "") ?: ""
        email = prefs.getString("email", "") ?: ""
        val savedUrl = prefs.getString("server_url", null)
        val effectiveUrl = if (savedUrl.isNullOrBlank() || savedUrl.contains("trycloudflare.com") || savedUrl.contains("loca.lt")) {
            BuildConfig.BASE_URL
        } else {
            savedUrl
        }
        serverUrl = effectiveUrl
        Net.init(effectiveUrl)
    }

    fun save(r: AuthResponse) {
        token = r.token; userId = r.userId; name = r.name ?: ""; email = r.gmail ?: ""
        prefs.edit()
            .putString("token", token)
            .putLong("userId", userId)
            .putString("name", name)
            .putString("email", email)
            .apply()
    }

    fun updateServerUrl(newUrl: String) {
        Net.updateBaseUrl(newUrl)
        serverUrl = Net.baseUrl
        prefs.edit().putString("server_url", serverUrl).apply()
    }

    fun clear() {
        token = null; userId = -1L; name = ""; email = ""
        prefs.edit()
            .remove("token")
            .remove("userId")
            .remove("name")
            .remove("email")
            .apply()
    }
}
