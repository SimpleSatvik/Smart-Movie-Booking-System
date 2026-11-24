package com.cinesync.app.data

import com.cinesync.app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object Net {
    /** Small in-memory cache so the seat screen can show movie/theatre info for a show. */
    val showCache = mutableMapOf<Long, Show>()

    var baseUrl: String = BuildConfig.BASE_URL
        private set

    @Volatile
    private var currentApi: CineApi? = null

    val api: CineApi
        get() = currentApi ?: synchronized(this) {
            currentApi ?: buildApi(baseUrl).also { currentApi = it }
        }

    fun init(url: String?) {
        updateBaseUrl(url?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL)
    }

    fun updateBaseUrl(rawUrl: String) {
        var clean = rawUrl.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        baseUrl = clean
        synchronized(this) {
            currentApi = buildApi(clean)
        }
    }

    private fun buildApi(url: String): CineApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val t = Session.token
                val req = if (t != null) chain.request().newBuilder().header("Authorization", "Bearer $t").build() else chain.request()
                chain.proceed(req)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CineApi::class.java)
    }

    suspend fun testConnection(testUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            var clean = testUrl.trim()
            if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                clean = "http://$clean"
            }
            if (!clean.endsWith("/")) {
                clean = "$clean/"
            }
            val testClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("${clean}api/movies")
                .get()
                .build()
            testClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Connected successfully! (HTTP ${response.code})")
                } else {
                    Result.failure(Exception("Server returned HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** The backend returns {timestamp,status,error,message} for most failures (mostly 400, 409 for booking races). */
fun errorMessage(e: Throwable): String = when (e) {
    is HttpException -> {
        if (e.code() in listOf(521, 522, 530)) {
            "Tunnel offline (${e.code()}). Tap the Server link on Sign In to use your local Wi-Fi IP."
        } else {
            val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val msg = raw?.let { runCatching { Gson().fromJson(it, ApiError::class.java)?.message }.getOrNull() }
            msg?.takeIf { it.isNotBlank() } ?: "Request failed (${e.code()})"
        }
    }
    is IOException -> "Can't reach the cinema network. Check your Wi-Fi or server IP."
    else -> e.message ?: "Something went wrong"
}
