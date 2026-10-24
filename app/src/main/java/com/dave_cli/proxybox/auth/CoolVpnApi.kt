package com.dave_cli.proxybox.auth

import android.content.Context
import android.provider.Settings
import java.util.UUID
import com.dave_cli.proxybox.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** COOL-VPN authentication gateway client. */
class CoolVpnApi(context: Context) {

    private val appContext = context.applicationContext

    private val prefs = appContext.getSharedPreferences(
        PREFS,
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun deviceHwid(): String {
        val stored = prefs.getString(KEY_HWID, null)

        if (!stored.isNullOrBlank()) {
            return stored
        }

        val androidId = runCatching {
            Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }.getOrNull()?.trim().orEmpty()

        val hwid = if (androidId.isNotBlank()) {
            "android-$androidId"
        } else {
            "coolvpn-${UUID.randomUUID()}"
        }

        prefs.edit()
            .putString(KEY_HWID, hwid)
            .apply()

        return hwid
    }

    data class Result(
        val ok: Boolean,
        val username: String? = null,
        val subscription: String? = null,
        val expiresAt: String? = null,
        val error: String? = null,
        val devicesUsed: Int? = null,
        val devicesLimit: Int? = null
    )

    fun login(
        username: String,
        password: String
    ): Result {

        val payload = gson.toJson(
            mapOf(
                "username" to username,
                "password" to password,
                "hwid" to deviceHwid()
            )
        )

        return request(
            path = "/v1/login",
            method = "POST",
            body = payload,
            token = null
        )
    }

    /**
     * Pobiera aktualną konfigurację użytkownika z mastera.
     *
     * Aktualizuje:
     * - listę serwerów
     * - adresy IP
     * - konfiguracje VLESS
     * - datę wygaśnięcia
     */
    fun refresh(): Result {

        val token = prefs.getString(KEY_TOKEN, null)

            ?: return Result(
                ok = false,
                error = "Brak sesji"
            )

        val result = request(
            path = "/v1/config",
            method = "GET",
            body = null,
            token = token,
            hwid = deviceHwid()
        )

        if (result.ok) {

            prefs.edit()
                .putString(
                    KEY_EXPIRES_AT,
                    result.expiresAt.orEmpty()
                )
                .apply()

            if (result.devicesUsed != null) {
                prefs.edit()
                    .putInt(
                        KEY_DEVICES_USED,
                        result.devicesUsed
                    )
                    .apply()
            }

            if (result.devicesLimit != null) {
                prefs.edit()
                    .putInt(
                        KEY_DEVICES_LIMIT,
                        result.devicesLimit
                    )
                    .apply()
            }
        }

        return result
    }

    fun saveSession(
        username: String,
        token: String
    ) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    fun hasSession(): Boolean {
        return !prefs
            .getString(KEY_TOKEN, null)
            .isNullOrBlank()
    }

    fun username(): String {
        return prefs.getString(
            KEY_USERNAME,
            ""
        ) ?: ""
    }

    fun subscriptionExpiry(): String? {
        return prefs.getString(
            KEY_EXPIRES_AT,
            null
        )
    }

    fun devicesUsed(): Int {
        return prefs.getInt(KEY_DEVICES_USED, 0)
    }

    fun devicesLimit(): Int {
        return prefs.getInt(KEY_DEVICES_LIMIT, 0)
    }

    fun subscriptionExpiryLabel(): String {

        val raw = subscriptionExpiry()
            .orEmpty()
            .trim()

        if (raw.isEmpty()) {
            return "DOŻYWOTNIO"
        }

        return formatExpiry(raw)
    }

    private fun formatExpiry(raw: String): String {

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        for (pattern in patterns) {

            try {

                val parser = java.text.SimpleDateFormat(
                    pattern,
                    java.util.Locale.US
                )

                parser.isLenient = false

                val date = parser.parse(raw)

                if (date != null) {

                    return java.text.SimpleDateFormat(
                        "dd.MM.yyyy",
                        java.util.Locale.getDefault()
                    ).format(date)
                }

            } catch (_: Exception) {
            }
        }

        return raw
    }

    private fun request(
        path: String,
        method: String,
        body: String?,
        token: String?,
        hwid: String? = null
    ): Result {

        return try {

            val url =
                BuildConfig.COOLVPN_API_BASE_URL
                    .trimEnd('/') + path

            val builder = Request.Builder()
                .url(url)

            if (!token.isNullOrBlank()) {

                builder.header(
                    "Authorization",
                    "Bearer $token"
                )
            }

            if (!hwid.isNullOrBlank()) {

                builder.header(
                    "X-Hwid",
                    hwid
                )
            }

            if (method == "POST") {

                builder.post(
                    (body ?: "{}")
                        .toRequestBody(
                            "application/json; charset=utf-8"
                                .toMediaType()
                        )
                )

            } else {

                builder.get()
            }

            client
                .newCall(builder.build())
                .execute()
                .use { response ->

                    val text =
                        response.body
                            ?.string()
                            .orEmpty()

                    val obj =
                        runCatching {
                            gson.fromJson(
                                text,
                                JsonObject::class.java
                            )
                        }.getOrNull()

                    if (!response.isSuccessful || obj == null) {

                        return Result(
                            ok = false,
                            error =
                                obj
                                    ?.get("error")
                                    ?.asString
                                    ?: "Błąd serwera HTTP ${response.code}"
                        )
                    }

                    Result(
                        ok = true,
                        username =
                            obj.get("username")
                                ?.asString,

                        subscription =
                            obj.get("subscription")
                                ?.asString,

                        expiresAt =
                            obj.get("expiresAt")
                                ?.asString,

                        error = null,

                        devicesUsed =
                            obj.getAsJsonObject("hwidStatus")
                                ?.get("registered")
                                ?.asInt,

                        devicesLimit =
                            obj.getAsJsonObject("hwidStatus")
                                ?.get("limit")
                                ?.asInt
                    )
                }

        } catch (e: Exception) {

            Result(
                ok = false,
                error =
                    e.message
                        ?: "Nie można połączyć z serwerem COOL-VPN"
            )
        }
    }

    fun loginAndSave(
        username: String,
        password: String
    ): Result {

        return try {

            val payload = gson.toJson(
                mapOf(
                    "username" to username,
                    "password" to password,
                    "hwid" to deviceHwid()
                )
            )

            val url =
                BuildConfig.COOLVPN_API_BASE_URL
                    .trimEnd('/') + "/v1/login"

            val request =
                Request.Builder()
                    .url(url)
                    .post(
                        payload.toRequestBody(
                            "application/json; charset=utf-8"
                                .toMediaType()
                        )
                    )
                    .build()

            client
                .newCall(request)
                .execute()
                .use { response ->

                    val text =
                        response.body
                            ?.string()
                            .orEmpty()

                    val obj =
                        runCatching {
                            gson.fromJson(
                                text,
                                JsonObject::class.java
                            )
                        }.getOrNull()

                    if (!response.isSuccessful || obj == null) {

                        return Result(
                            false,
                            error =
                                obj
                                    ?.get("error")
                                    ?.asString
                                    ?: "Błąd logowania (${response.code})"
                        )
                    }

                    val token =
                        obj.get("token")
                            ?.asString
                            ?: return Result(
                                false,
                                error =
                                    "Serwer nie zwrócił sesji"
                            )

                    val user =
                        obj.get("username")
                            ?.asString
                            ?: username

                    saveSession(
                        user,
                        token
                    )

                    prefs.edit()
                        .putString(
                            KEY_EXPIRES_AT,
                            obj.get("expiresAt")
                                ?.asString
                                .orEmpty()
                        )
                        .apply()

                    val loginResult = Result(
            ok = true,
            username = user,
            subscription = obj.get("subscription")
                ?.asString,
            expiresAt = obj.get("expiresAt")
                ?.asString,
            error = null,
            devicesUsed = obj.getAsJsonObject("hwidStatus")
                ?.get("registered")
                ?.asInt,
            devicesLimit = obj.getAsJsonObject("hwidStatus")
                ?.get("limit")
                ?.asInt
        )

        if (loginResult.devicesUsed != null) {
            prefs.edit()
                .putInt(KEY_DEVICES_USED, loginResult.devicesUsed)
                .apply()
        }

        if (loginResult.devicesLimit != null) {
            prefs.edit()
                .putInt(KEY_DEVICES_LIMIT, loginResult.devicesLimit)
                .apply()
        }

        loginResult

    } catch (e: Exception) {

            Result(
                false,
                error =
                    e.message
                        ?: "Nie można połączyć z serwerem COOL-VPN"
            )
        }
    }

    companion object {
        private const val KEY_DEVICES_USED = "devices_used"
        private const val KEY_DEVICES_LIMIT = "devices_limit"


        private const val PREFS = "coolvpn_auth"

        private const val KEY_TOKEN = "token"

        private const val KEY_USERNAME = "username"

        private const val KEY_EXPIRES_AT = "expiresAt"

        private const val KEY_HWID = "hwid"
    }
}
