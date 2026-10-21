package com.dave_cli.proxybox.auth

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
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
    private lateinit var loginButton: TextView
    private lateinit var progress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var username: EditText
    private lateinit var password: EditText

    private val bg = Color.rgb(10, 10, 20)
    private val card = Color.rgb(24, 24, 43)
    private val fieldBg = Color.rgb(18, 18, 34)
    private val violet = Color.rgb(125, 92, 255)
    private val violet2 = Color.rgb(96, 70, 230)
    private val textPrimary = Color.WHITE
    private val textSecondary = Color.rgb(175, 175, 198)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = CoolVpnApi(this)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showLogin()
        if (api.hasSession()) refreshExistingSession()
    }

    private fun showLogin() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(22))
        }

        val topSpace = View(this)
        root.addView(topSpace, LinearLayout.LayoutParams(1, 0, 0.12f))

        val logo = TextView(this).apply {
            text = "C"
            gravity = Gravity.CENTER
            textSize = 34f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = gradient(violet, violet2, 100f)
            elevation = dp(8).toFloat()
        }
        root.addView(logo, LinearLayout.LayoutParams(dp(78), dp(78)).apply { bottomMargin = dp(18) })

        val title = TextView(this).apply {
            text = "COOL-VPN"
            setTextColor(textPrimary)
            textSize = 30f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.03f
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "Bezpieczne połączenie. Zaloguj się, aby rozpocząć."
            setTextColor(textSecondary)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(8), dp(20), dp(22))
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
            background = rounded(card, 26f)
            elevation = dp(8).toFloat()
        }
        root.addView(panel, LinearLayout.LayoutParams(-1, -2))

        val accountLabel = TextView(this).apply {
            text = "DANE LOGOWANIA"
            setTextColor(Color.rgb(155, 135, 255))
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.12f
        }
        panel.addView(accountLabel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        username = field("Login", false)
        panel.addView(username, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(12) })

        val passwordContainer = FrameLayout(this)
        password = field("Hasło", true).apply {
            setPadding(dp(16), 0, dp(78), 0)
        }
        passwordContainer.addView(password, FrameLayout.LayoutParams(-1, dp(56)))

        val toggle = TextView(this).apply {
            text = "Pokaż"
            setTextColor(Color.rgb(167, 143, 255))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(14), 0)
            setOnClickListener {
                val visible = password.transformationMethod is HideReturnsTransformationMethod
                password.transformationMethod = if (visible) PasswordTransformationMethod.getInstance()
                else HideReturnsTransformationMethod.getInstance()
                text = if (visible) "Pokaż" else "Ukryj"
                password.setSelection(password.text.length)
            }
        }
        passwordContainer.addView(toggle, FrameLayout.LayoutParams(dp(68), dp(56), Gravity.END))
        panel.addView(passwordContainer, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(18) })

        loginButton = TextView(this).apply {
            text = "ZALOGUJ SIĘ"
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            background = gradient(violet, violet2, 16f)
            isClickable = true
            isFocusable = true
            setOnClickListener { doLogin() }
        }
        panel.addView(loginButton, LinearLayout.LayoutParams(-1, dp(54)))

        progress = ProgressBar(this).apply {
            visibility = View.GONE
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        }
        panel.addView(progress, LinearLayout.LayoutParams(dp(28), dp(28)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(14)
        })

        errorText = TextView(this).apply {
            setTextColor(Color.rgb(255, 150, 150))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(54, 25, 39), 14f)
            visibility = View.GONE
        }
        panel.addView(errorText, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        val footer = TextView(this).apply {
            text = "COOL-VPN  •  Bezpieczny dostęp do Internetu"
            setTextColor(Color.rgb(105, 105, 130))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, 0)
        }
        root.addView(footer, LinearLayout.LayoutParams(-1, -2))

        val bottomSpace = View(this)
        root.addView(bottomSpace, LinearLayout.LayoutParams(1, 0, 0.12f))

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun field(hint: String, secret: Boolean): EditText = EditText(this).apply {
        this.hint = hint
        setTextColor(textPrimary)
        setHintTextColor(Color.rgb(115, 115, 140))
        textSize = 16f
        setSingleLine(true)
        setPadding(dp(16), 0, dp(16), 0)
        background = roundedWithStroke(fieldBg, Color.rgb(52, 52, 78), 16f, dp(1))
        inputType = if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else InputType.TYPE_CLASS_TEXT
        if (secret) transformationMethod = PasswordTransformationMethod.getInstance()
        isFocusableInTouchMode = true
    }

    private fun doLogin() {
        val u = username.text.toString().trim()
        val p = password.text.toString()
        if (u.isBlank() || p.isBlank()) {
            showError("Wpisz login i hasło")
            return
        }
        hideKeyboard()
        setBusy(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = api.loginAndSave(u, p)
            if (result.ok && !result.subscription.isNullOrBlank()) {
                val imported = importSubscription(result.subscription!!)
                withContext(Dispatchers.Main) {
                    if (imported) openApp()
                    else {
                        api.clearSession()
                        setBusy(false)
                        showError("Nie udało się pobrać konfiguracji VPN")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    setBusy(false)
                    showError(result.error ?: getString(R.string.coolvpn_login_error))
                }
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
                    if (imported) openApp()
                    else {
                        api.clearSession()
                        setBusy(false)
                        showError("Sesja wygasła. Zaloguj się ponownie.")
                    }
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
        loginButton.alpha = if (busy) 0.55f else 1f
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        if (busy) errorText.visibility = View.GONE
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun roundedWithStroke(color: Int, strokeColor: Int, radius: Float, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(stroke, strokeColor)
        }

    private fun gradient(start: Int, end: Int, radius: Float): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun isTV(): Boolean =
        (getSystemService(UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}
