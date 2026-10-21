package com.dave_cli.proxybox.auth

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.dave_cli.proxybox.R
import com.dave_cli.proxybox.data.db.AppDatabase
import com.dave_cli.proxybox.data.repository.ProfileRepository
import com.dave_cli.proxybox.ui.main.MainActivity
import com.dave_cli.proxybox.ui.tv.TvMainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    private lateinit var api: CoolVpnApi
    private lateinit var loginButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var username: EditText
    private lateinit var password: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = CoolVpnApi(this)
        window.statusBarColor = Color.rgb(15,15,26)
        window.navigationBarColor = Color.rgb(15,15,26)
        showLogin()
        if (api.hasSession()) refreshExistingSession()
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 32, 48, 32)
            setBackgroundColor(Color.rgb(15,15,26))
        }
        val title = TextView(this).apply {
            text = getString(R.string.coolvpn_login_title)
            setTextColor(Color.WHITE); textSize = 34f; gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = getString(R.string.coolvpn_login_subtitle)
            setTextColor(Color.rgb(170,170,190)); textSize = 16f; gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        }
        username = field(getString(R.string.coolvpn_username), false)
        password = field(getString(R.string.coolvpn_password), true)
        loginButton = Button(this).apply {
            text = getString(R.string.coolvpn_login)
            isAllCaps = false
            setOnClickListener { doLogin() }
        }
        progress = ProgressBar(this).apply { visibility = View.GONE }
        errorText = TextView(this).apply {
            setTextColor(Color.rgb(255,110,110)); textSize = 14f; gravity = Gravity.CENTER
            visibility = View.GONE; setPadding(0,16,0,0)
        }
        listOf(title, subtitle, username, password, loginButton, progress, errorText).forEach {
            root.addView(it, LinearLayout.LayoutParams(-1, if (it == title) 55 else LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                if (it == username || it == password) setMargins(0,0,0,12)
                if (it == loginButton) setMargins(0,12,0,0)
            })
        }
        setContentView(root)
        username.requestFocus()
    }

    private fun field(hint: String, secret: Boolean): EditText = EditText(this).apply {
        this.hint = hint
        setTextColor(Color.WHITE); setHintTextColor(Color.rgb(130,130,150)); textSize = 17f
        setSingleLine(true); setPadding(24, 12, 24, 12)
        if (secret) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        else inputType = android.text.InputType.TYPE_CLASS_TEXT
        if (isTV()) isFocusableInTouchMode = true
    }

    private fun doLogin() {
        val u = username.text.toString().trim()
        val p = password.text.toString()
        if (u.isBlank() || p.isBlank()) { showError("Wpisz login i hasło"); return }
        setBusy(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = api.loginAndSave(u, p)
            if (result.ok && !result.subscription.isNullOrBlank()) {
                val imported = importSubscription(result.subscription!!)
                withContext(Dispatchers.Main) {
                    if (imported) openApp() else { api.clearSession(); setBusy(false); showError("Nie udało się pobrać konfiguracji VPN") }
                }
            } else {
                withContext(Dispatchers.Main) { setBusy(false); showError(result.error ?: getString(R.string.coolvpn_login_error)) }
            }
        }
    }

    private fun refreshExistingSession() {
        setBusy(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = api.refresh()
            if (result.ok && !result.subscription.isNullOrBlank()) {
                val imported = importSubscription(result.subscription!!)
                withContext(Dispatchers.Main) {
                    if (imported) openApp() else { api.clearSession(); setBusy(false); showError("Sesja wygasła. Zaloguj się ponownie.") }
                }
            } else {
                api.clearSession()
                withContext(Dispatchers.Main) { setBusy(false) }
            }
        }
    }

    private suspend fun importSubscription(raw: String): Boolean = withContext(Dispatchers.IO) {
        val repo = ProfileRepository(AppDatabase.getInstance(applicationContext))
        repo.importCoolVpnSubscription(raw)
    }

    private fun openApp() {
        val target = if (isTV()) TvMainActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun setBusy(busy: Boolean) {
        loginButton.isEnabled = !busy
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        if (busy) errorText.visibility = View.GONE
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isTV(): Boolean =
        (getSystemService(UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}
