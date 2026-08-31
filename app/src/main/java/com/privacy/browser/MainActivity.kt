package com.privacy.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.TypedValue
import android.view.DragEvent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebView.FindListener
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class Tab(val webView: WebView, var title: String = "تبويب جديد")
data class Shortcut(val title: String, val url: String)
data class Snippet(val title: String, val code: String)
data class DebugEntry(val time: String, val message: String)
data class PermLogEntry(val time: String, val host: String, val type: String, val allowed: Boolean)

class MainActivity : AppCompatActivity() {

private lateinit var homeScreen: FrameLayout  
private lateinit var browserContainer: FrameLayout  
private lateinit var webViewContainer: FrameLayout  
private lateinit var homeSearchBar: EditText  
private lateinit var urlDisplay: TextView  
private lateinit var urlFavicon: ImageView  
private lateinit var progressBar: ProgressBar  
private lateinit var swipeRefresh: SwipeRefreshLayout  
private lateinit var lockOverlay: FrameLayout  
private lateinit var fullscreenContainer: FrameLayout  
private lateinit var gridShortcuts: GridLayout  
private lateinit var shortcutsScroll: ScrollView  
private lateinit var tabsButton: Button  
private lateinit var bottomBar: LinearLayout  
private lateinit var protectionToggle: LinearLayout  
private var focusModeOn = false  

private lateinit var cyberContainer: FrameLayout  
private lateinit var cyberFab: ImageButton  
private lateinit var cyberPanel: FrameLayout  
private lateinit var cyberPanelTitle: TextView  
private lateinit var cyberFileManagerContainer: LinearLayout  
private lateinit var cyberPathLabel: TextView  
private lateinit var cyberFileListContainer: LinearLayout  
private lateinit var cyberActiveFileLabel: TextView  
private lateinit var cyberEditor: EditText  
private lateinit var cyberTerminalContainer: LinearLayout  
private lateinit var cyberTerminalOutput: TextView  
private lateinit var cyberPackagesContainer: LinearLayout  
private lateinit var cyberPackageListContainer: LinearLayout  
private lateinit var cyberSnippetsContainer: LinearLayout  
private lateinit var cyberSnippetListContainer: LinearLayout  
private lateinit var cyberInstallPackageButton: Button  
private lateinit var pyodideWebView: WebView  
private var cyberOpenPanel: String? = null  
private var pyodideReady = false  

private val terminalBuilder = SpannableStringBuilder()  
private val debugEntries = mutableListOf<DebugEntry>()  
private val permLogEntries = mutableListOf<PermLogEntry>()  

private lateinit var rootDir: File  
private lateinit var currentDir: File  
private var activeFile: File? = null  
private var lastRunStartTime: Long = 0L  

private val installedPackages = mutableListOf<String>()  
private val snippets = mutableListOf<Snippet>()  

private var customView: View? = null  
private var customViewCallback: WebChromeClient.CustomViewCallback? = null  
private var isLocked = true  
private var adBlockEnabled = true  
private var darkModeEnabled = false  
private var paranoiaModeOn = false  
private var lastClosedTabUrl: String? = null  
private var failedUnlockAttempts = 0  

private val tabs = mutableListOf<Tab>()  
private var currentTabIndex = -1  

private val shortcuts = mutableListOf<Shortcut>()  

private val blockedHosts = setOf(  
    "doubleclick.net", "googlesyndication.com", "googleadservices.com",  
    "google-analytics.com", "adservice.google.com", "googletagmanager.com",  
    "googletagservices.com", "ads.yahoo.com", "adnxs.com", "taboola.com",  
    "outbrain.com", "popads.net", "propellerads.com", "exoclick.com",  
    "juicyads.com", "adroll.com", "criteo.com", "connect.facebook.net",  
    "amazon-adsystem.com", "mgid.com", "revcontent.com", "media.net",  
    "pubmatic.com", "rubiconproject.com", "openx.net", "adsrvr.org",  
    "casalemedia.com", "smartadserver.com", "adform.net", "yieldmo.com",  
    "bidswitch.net", "contextweb.com", "indexexchange.com", "sharethrough.com",  
    "gumgum.com", "scorecardresearch.com", "quantserve.com", "hotjar.com",  
    "mixpanel.com", "segment.io", "appsflyer.com", "adjust.com",  
    "popcash.net", "adcash.com", "clickadu.com", "hilltopads.net",  
    "trafficjunky.net", "exosrv.com", "propellerclick.com"  
)  

private val manualBlacklist = mutableSetOf<String>()  
private val jsDisabledDomains = mutableSetOf<String>()  
private var pageBlockedCount = 0  

private val autoLockHandler = Handler(Looper.getMainLooper())  
private val autoLockRunnable = Runnable { lockNow() }  
private val autoLockDelayMs = 5 * 60 * 1000L  

private lateinit var sensorManager: SensorManager  
private var accelerometer: Sensor? = null  
private var lastAccel = floatArrayOf(0f, 0f, 0f)  
private var accelInitialized = false  
private val motionSensorListener = object : SensorEventListener {  
    override fun onSensorChanged(event: SensorEvent) {  
        if (isLocked) return  
        val x = event.values[0]  
        val y = event.values[1]  
        val z = event.values[2]  
        if (!accelInitialized) {  
            lastAccel = floatArrayOf(x, y, z)  
            accelInitialized = true  
            return  
        }  
        val delta = Math.abs(x - lastAccel[0]) + Math.abs(y - lastAccel[1]) + Math.abs(z - lastAccel[2])  
        lastAccel = floatArrayOf(x, y, z)  
        if (delta > 28f) {  
            lockNow()  
        }  
    }  

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}  
}  

private val importProjectLauncher = registerForActivityResult(  
    ActivityResultContracts.OpenDocument()  
) { uri: Uri? ->  
    if (uri != null) importProjectFromUri(uri)  
}  

override fun onCreate(savedInstanceState: Bundle?) {  
    super.onCreate(savedInstanceState)  
    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)  
    setContentView(R.layout.activity_main)  

    homeScreen = findViewById(R.id.homeScreen)  
    browserContainer = findViewById(R.id.browserContainer)  
    webViewContainer = findViewById(R.id.webViewContainer)  
    homeSearchBar = findViewById(R.id.homeSearchBar)  
    urlDisplay = findViewById(R.id.urlDisplay)  
    urlFavicon = findViewById(R.id.urlFavicon)  
    progressBar = findViewById(R.id.progressBar)  
    swipeRefresh = findViewById(R.id.swipeRefresh)  
    lockOverlay = findViewById(R.id.lockOverlay)  
    fullscreenContainer = findViewById(R.id.fullscreenContainer)  
    gridShortcuts = findViewById(R.id.gridShortcuts)  
    shortcutsScroll = findViewById(R.id.shortcutsScroll)  
    tabsButton = findViewById(R.id.tabsButton)  
    bottomBar = findViewById(R.id.bottomBar)  
    protectionToggle = findViewById(R.id.protectionToggle)  

    val focusModeButton: ImageButton = findViewById(R.id.focusModeButton)  
    val backButton: ImageButton = findViewById(R.id.backButton)  
    val homeButton: ImageButton = findViewById(R.id.homeButton)  
    val menuButton: ImageButton = findViewById(R.id.menuButton)  
    val menuButtonTop: ImageButton = findViewById(R.id.menuButtonTop)  
    val downloadsButton: ImageButton = findViewById(R.id.downloadsButton)  
    val unlockButton: Button = findViewById(R.id.unlockButton)  
    val protectionSwitch: Switch = findViewById(R.id.protectionSwitch)  
    val protectionLabel: TextView = findViewById(R.id.protectionLabel)  

    cyberContainer = findViewById(R.id.cyberContainer)  
    cyberFab = findViewById(R.id.cyberFab)  
    cyberPanel = findViewById(R.id.cyberPanel)  
    cyberPanelTitle = findViewById(R.id.cyberPanelTitle)  
    cyberFileManagerContainer = findViewById(R.id.cyberFileManagerContainer)  
    cyberPathLabel = findViewById(R.id.cyberPathLabel)  
    cyberFileListContainer = findViewById(R.id.cyberFileListContainer)  
    cyberActiveFileLabel = findViewById(R.id.cyberActiveFileLabel)  
    cyberEditor = findViewById(R.id.cyberEditor)  
    cyberTerminalContainer = findViewById(R.id.cyberTerminalContainer)  
    cyberTerminalOutput = findViewById(R.id.cyberTerminalOutput)  
    cyberPackagesContainer = findViewById(R.id.cyberPackagesContainer)  
    cyberPackageListContainer = findViewById(R.id.cyberPackageListContainer)  
    cyberSnippetsContainer = findViewById(R.id.cyberSnippetsContainer)  
    cyberSnippetListContainer = findViewById(R.id.cyberSnippetListContainer)  
    cyberInstallPackageButton = findViewById(R.id.cyberInstallPackageButton)  
    pyodideWebView = findViewById(R.id.pyodideWebView)  

    val cyberCloseButton: ImageButton = findViewById(R.id.cyberCloseButton)  
    val cyberMenuButton: ImageButton = findViewById(R.id.cyberMenuButton)  
    val cyberFilesButton: Button = findViewById(R.id.cyberFilesButton)  
    val cyberTerminalButton: Button = findViewById(R.id.cyberTerminalButton)  
    val cyberPackagesButton: Button = findViewById(R.id.cyberPackagesButton)  
    val cyberSnippetsButton: Button = findViewById(R.id.cyberSnippetsButton)  
    val cyberUpButton: Button = findViewById(R.id.cyberUpButton)  
    val cyberNewFileButton: Button = findViewById(R.id.cyberNewFileButton)  
    val cyberNewFolderButton: Button = findViewById(R.id.cyberNewFolderButton)  
    val cyberRunButton: Button = findViewById(R.id.cyberRunButton)  
    val cyberClearTerminalButton: Button = findViewById(R.id.cyberClearTerminalButton)  
    val cyberRunCommandButton: Button = findViewById(R.id.cyberRunCommandButton)  
    val cyberCommandInput: EditText = findViewById(R.id.cyberCommandInput)  
    val cyberPackageInput: EditText = findViewById(R.id.cyberPackageInput)  
    val cyberAddSnippetButton: Button = findViewById(R.id.cyberAddSnippetButton)  

    rootDir = File(filesDir, "cyber_projects")  
    if (!rootDir.exists()) rootDir.mkdirs()  
    currentDir = rootDir  

    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager  
    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)  

    loadManualBlacklist()  
    loadJsDisabledDomains()  
    setupPyodideWebView()  
    loadInstalledPackages()  
    loadSnippets()  
    restoreActiveFileIfAny()  
    restoreTabsState()  

    loadShortcuts()  
    rebuildShortcutsGrid()  

    focusModeButton.setOnClickListener { toggleFocusMode() }  
    backButton.setOnClickListener { onBackButtonPressed() }  
    homeButton.setOnClickListener { showHomeScreen() }  
    menuButton.setOnClickListener { showMainMenu(menuButton) }  
    menuButtonTop.setOnClickListener { showMainMenu(menuButtonTop) }  
    downloadsButton.setOnClickListener {  
        try {  
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))  
        } catch (e: Exception) {  
            Toast.makeText(this, "لا يمكن فتح التحميلات", Toast.LENGTH_SHORT).show()  
        }  
    }  
    tabsButton.setOnClickListener { showTabsDialog() }  
    unlockButton.setOnClickListener { showLockPrompt() }  

    protectionSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->  
        adBlockEnabled = isChecked  
        protectionLabel.text = if (isChecked) "الحماية مفعّلة" else "الحماية متوقفة"  
    }  

    swipeRefresh.setOnRefreshListener { currentWebView()?.reload() }  

    swipeRefresh.setOnChildScrollUpCallback(object : SwipeRefreshLayout.OnChildScrollUpCallback {  
        override fun canChildScrollUp(parent: SwipeRefreshLayout, child: View?): Boolean {  
            return currentWebView()?.canScrollVertically(-1) ?: false  
        }  
    })  

    homeSearchBar.setOnEditorActionListener { _: TextView, actionId: Int, event: KeyEvent? ->  
        if (actionId == EditorInfo.IME_ACTION_GO ||  
            (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)  
        ) {  
            val query = homeSearchBar.text.toString().trim()  
            if (query.isNotEmpty()) {  
                handleSearchBarInput(query)  
            }  
            true  
        } else {  
            false  
        }  
    }  

    cyberFab.setOnClickListener { openCyberMode() }  
    cyberFab.setOnLongClickListener {  
        panicWipeEverything()  
        true  
    }  
    cyberCloseButton.setOnClickListener { closeCyberMode() }  
    cyberMenuButton.setOnClickListener { showCyberExtraMenu(cyberMenuButton) }  
    cyberFilesButton.setOnClickListener { toggleCyberPanel("files", "📁 مدير الملفات") }  
    cyberTerminalButton.setOnClickListener { toggleCyberPanel("terminal", "⌨ الترمينال") }  
    cyberPackagesButton.setOnClickListener { toggleCyberPanel("packages", "📦 المكتبات") }  
    cyberSnippetsButton.setOnClickListener { toggleCyberPanel("snippets", "📚 مكتبة الأكواد") }  
    cyberUpButton.setOnClickListener { navigateUpDirectory() }  
    cyberNewFileButton.setOnClickListener { showNewFileDialog() }  
    cyberNewFolderButton.setOnClickListener { showNewFolderDialog() }  
    cyberRunButton.setOnClickListener { runActiveFileAsPython() }  
    cyberClearTerminalButton.setOnClickListener {  
        terminalBuilder.clear()  
        cyberTerminalOutput.text = ""  
    }  
    cyberRunCommandButton.setOnClickListener { runTerminalCommand(cyberCommandInput) }  
    cyberInstallPackageButton.setOnClickListener { installPackageFromInput(cyberPackageInput) }  
    cyberAddSnippetButton.setOnClickListener { showAddSnippetDialog() }  

    cyberCommandInput.setOnEditorActionListener { _: TextView, actionId: Int, event: KeyEvent? ->  
        if (actionId == EditorInfo.IME_ACTION_GO ||  
            (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)  
        ) {  
            runTerminalCommand(cyberCommandInput)  
            true  
        } else {  
            false  
        }  
    }  

    cyberEditor.setOnFocusChangeListener { _: View, hasFocus: Boolean ->  
        if (!hasFocus) saveActiveFileIfNeeded()  
    }  

    updateChromeVisibility()  
    checkRootStatus()  
}  

override fun onResume() {  
    super.onResume()  
    accelerometer?.let {  
        sensorManager.registerListener(motionSensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)  
    }  
    if (!isLocked) resetAutoLockTimer()  
}  

override fun onPause() {  
    super.onPause()  
    sensorManager.unregisterListener(motionSensorListener)  
    autoLockHandler.removeCallbacks(autoLockRunnable)  
}  

override fun onUserInteraction() {  
    super.onUserInteraction()  
    if (!isLocked) resetAutoLockTimer()  
}  

private fun resetAutoLockTimer() {  
    autoLockHandler.removeCallbacks(autoLockRunnable)  
    autoLockHandler.postDelayed(autoLockRunnable, autoLockDelayMs)  
}  

private fun lockNow() {  
    isLocked = true  
    lockOverlay.visibility = View.VISIBLE  
    updateChromeVisibility()  
}  

// ---------- وضع التركيز ----------  

private fun toggleFocusMode() {  
    focusModeOn = !focusModeOn  
    val visibility = if (focusModeOn) View.GONE else View.VISIBLE  
    protectionToggle.visibility = visibility  
    shortcutsScroll.visibility = visibility  
    Toast.makeText(this, if (focusModeOn) "وضع التركيز مفعّل" else "وضع التركيز متوقف", Toast.LENGTH_SHORT).show()  
}  

// ---------- الآلة الحاسبة بشريط البحث ----------  

private fun handleSearchBarInput(query: String) {  
    val mathResult = tryEvaluateMath(query)  
    if (mathResult != null) {  
        AlertDialog.Builder(this)  
            .setTitle("النتيجة")  
            .setMessage("$query = $mathResult")  
            .setPositiveButton("إغلاق", null)  
            .show()  
        return  
    }  
    openUrlOrSearch(query)  
}  

private fun tryEvaluateMath(input: String): String? {  
    val cleaned = input.replace(" ", "")  
    if (!cleaned.matches(Regex("^[0-9+\\-*/().]+$"))) return null  
    if (!cleaned.any { it == '+' || it == '-' || it == '*' || it == '/' }) return null  
    return try {  
        val result = MathParser(cleaned).parse()  
        if (result == result.toLong().toDouble()) {  
            result.toLong().toString()  
        } else {  
            result.toString()  
        }  
    } catch (e: Exception) {  
        null  
    }  
}  

private class MathParser(private val expr: String) {  
    private var pos = 0  

    fun parse(): Double {  
        val result = parseExpression()  
        if (pos != expr.length) throw RuntimeException("Unexpected character")  
        return result  
    }  

    private fun parseExpression(): Double {  
        var value = parseTerm()  
        while (pos < expr.length && (expr[pos] == '+' || expr[pos] == '-')) {  
            val op = expr[pos]  
            pos++  
            val next = parseTerm()  
            value = if (op == '+') value + next else value - next  
        }  
        return value  
    }  

    private fun parseTerm(): Double {  
        var value = parseFactor()  
        while (pos < expr.length && (expr[pos] == '*' || expr[pos] == '/')) {  
            val op = expr[pos]  
            pos++  
            val next = parseFactor()  
            value = if (op == '*') value * next else value / next  
        }  
        return value  
    }  

    private fun parseFactor(): Double {  
        if (pos < expr.length && expr[pos] == '(') {  
            pos++  
            val value = parseExpression()  
            if (pos < expr.length && expr[pos] == ')') pos++  
            return value  
        }  
        if (pos < expr.length && expr[pos] == '-') {  
            pos++  
            return -parseFactor()  
        }  
        val start = pos  
        while (pos < expr.length && (expr[pos].isDigit() || expr[pos] == '.')) pos++  
        if (start == pos) throw RuntimeException("Expected number")  
        return expr.substring(start, pos).toDouble()  
    }  
}  

// ---------- القائمة السوداء اليدوية ----------  

private fun loadManualBlacklist() {  
    val prefs = getSharedPreferences("blacklist_prefs", MODE_PRIVATE)  
    val raw = prefs.getString("hosts", "") ?: ""  
    manualBlacklist.clear()  
    manualBlacklist.addAll(raw.split("\u0002").filter { it.isNotBlank() })  
}  

private fun saveManualBlacklist() {  
    val prefs = getSharedPreferences("blacklist_prefs", MODE_PRIVATE)  
    prefs.edit().putString("hosts", manualBlacklist.joinToString("\u0002")).apply()  
}  

private fun blockCurrentSitePermanently() {  
    val host = currentWebView()?.url?.let { Uri.parse(it).host } ?: return  
    manualBlacklist.add(host)  
    saveManualBlacklist()  
    Toast.makeText(this, "تم حظر $host نهائيًا", Toast.LENGTH_SHORT).show()  
    currentWebView()?.loadUrl("about:blank")  
}  

private fun showBlacklistDialog() {  
    if (manualBlacklist.isEmpty()) {  
        Toast.makeText(this, "القائمة السوداء فاضية", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val names = manualBlacklist.toTypedArray()  
    val checked = BooleanArray(names.size)  
    AlertDialog.Builder(this)  
        .setTitle("المواقع المحظورة نهائيًا")  
        .setMultiChoiceItems(names, checked) { _: DialogInterface, which: Int, isChecked: Boolean ->  
            checked[which] = isChecked  
        }  
        .setPositiveButton("حذف المحدد") { _: DialogInterface, _: Int ->  
            val toRemove = mutableListOf<String>()  
            for (i in checked.indices) if (checked[i]) toRemove.add(names[i])  
            manualBlacklist.removeAll(toRemove.toSet())  
            saveManualBlacklist()  
        }  
        .setNegativeButton("إغلاق", null)  
        .show()  
}  

// ---------- تعطيل JS لموقع معين ----------  

private fun loadJsDisabledDomains() {  
    val prefs = getSharedPreferences("js_prefs", MODE_PRIVATE)  
    val raw = prefs.getString("domains", "") ?: ""  
    jsDisabledDomains.clear()  
    jsDisabledDomains.addAll(raw.split("\u0002").filter { it.isNotBlank() })  
}  

private fun saveJsDisabledDomains() {  
    val prefs = getSharedPreferences("js_prefs", MODE_PRIVATE)  
    prefs.edit().putString("domains", jsDisabledDomains.joinToString("\u0002")).apply()  
}  

private fun toggleJsForCurrentSite() {  
    val webView = currentWebView() ?: return  
    val host = webView.url?.let { Uri.parse(it).host } ?: return  
    if (jsDisabledDomains.contains(host)) {  
        jsDisabledDomains.remove(host)  
        webView.settings.javaScriptEnabled = true  
        Toast.makeText(this, "تم تفعيل JS لهذا الموقع، جاري إعادة التحميل", Toast.LENGTH_SHORT).show()  
    } else {  
        jsDisabledDomains.add(host)  
        webView.settings.javaScriptEnabled = false  
        Toast.makeText(this, "تم تعطيل JS لهذا الموقع، جاري إعادة التحميل", Toast.LENGTH_SHORT).show()  
    }  
    saveJsDisabledDomains()  
    webView.reload()  
}  

// ---------- وضع بارانويا ----------  

private fun toggleParanoiaMode() {  
    paranoiaModeOn = !paranoiaModeOn  
    if (paranoiaModeOn) {  
        adBlockEnabled = true  
        CookieManager.getInstance().setAcceptCookie(false)  
        for (tab in tabs) {  
            tab.webView.settings.javaScriptEnabled = false  
            tab.webView.reload()  
        }  
        Toast.makeText(this, "😱 وضع بارانويا مفعّل: أقصى حماية ممكنة", Toast.LENGTH_LONG).show()  
    } else {  
        CookieManager.getInstance().setAcceptCookie(true)  
        for (tab in tabs) {  
            val host = tab.webView.url?.let { Uri.parse(it).host } ?: ""  
            tab.webView.settings.javaScriptEnabled = !jsDisabledDomains.contains(host)  
            tab.webView.reload()  
        }  
        Toast.makeText(this, "تم إيقاف وضع بارانويا", Toast.LENGTH_SHORT).show()  
    }  
}  

// ---------- إدارة الصلاحيات لكل موقع ----------  

private fun logPermission(host: String, type: String, allowed: Boolean) {  
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())  
    permLogEntries.add(0, PermLogEntry(time, host, type, allowed))  
    if (permLogEntries.size > 50) permLogEntries.removeAt(permLogEntries.size - 1)  
}  

private fun showPermissionLogDialog() {  
    if (permLogEntries.isEmpty()) {  
        Toast.makeText(this, "لا يوجد سجل صلاحيات بعد", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val layout = LinearLayout(this)  
    layout.orientation = LinearLayout.VERTICAL  
    layout.setPadding(dp(16), dp(8), dp(16), dp(8))  
    for (entry in permLogEntries) {  
        val row = TextView(this)  
        val status = if (entry.allowed) "✅ سُمح" else "❌ رُفض"  
        row.text = "[${entry.time}] ${entry.host} — ${entry.type} — $status"  
        row.textSize = 12f  
        row.setPadding(0, dp(6), 0, dp(6))  
        layout.addView(row)  
    }  
    val scroll = ScrollView(this)  
    scroll.addView(layout)  
    AlertDialog.Builder(this)  
        .setTitle("🕵️ سجل طلبات الصلاحيات")  
        .setView(scroll)  
        .setPositiveButton("إغلاق", null)  
        .show()  
}  

private fun requestSitePermission(host: String, type: String, onResult: (Boolean) -> Unit) {  
    AlertDialog.Builder(this)  
        .setTitle("طلب صلاحية")  
        .setMessage("الموقع \"$host\" يطلب صلاحية: $type")  
        .setPositiveButton("سماح") { _: DialogInterface, _: Int ->  
            logPermission(host, type, true)  
            onResult(true)  
        }  
        .setNegativeButton("رفض") { _: DialogInterface, _: Int ->  
            logPermission(host, type, false)  
            onResult(false)  
        }  
        .setCancelable(false)  
        .show()  
}  

// ---------- حفظ واستعادة التبويبات ----------  

private fun saveTabsState() {  
    val prefs = getSharedPreferences("tabs_prefs", MODE_PRIVATE)  
    val sb = StringBuilder()  
    for (i in tabs.indices) {  
        if (i > 0) sb.append("\u0002")  
        sb.append(tabs[i].webView.url ?: "")  
    }  
    prefs.edit()  
        .putString("urls", sb.toString())  
        .putInt("current", currentTabIndex)  
        .apply()  
}  

private fun restoreTabsState() {  
    val prefs = getSharedPreferences("tabs_prefs", MODE_PRIVATE)  
    val raw = prefs.getString("urls", "") ?: ""  
    if (raw.isBlank()) return  
    val urls = raw.split("\u0002").filter { it.isNotBlank() }  
    if (urls.isEmpty()) return  
    for (u in urls) {  
        val webView = WebView(this)  
        configureWebView(webView)  
        val tab = Tab(webView)  
        tabs.add(tab)  
        webView.loadUrl(u)  
    }  
    val savedIndex = prefs.getInt("current", 0)  
    currentTabIndex = if (savedIndex in tabs.indices) savedIndex else 0  
    updateTabsButton()  
}  

private fun clearSavedTabs() {  
    getSharedPreferences("tabs_prefs", MODE_PRIVATE).edit().clear().apply()  
}  

// ---------- حفظ واستعادة الملف النشط ----------  

private fun saveActiveFilePath() {  
    val prefs = getSharedPreferences("cyber_prefs", MODE_PRIVATE)  
    if (activeFile != null) {  
        prefs.edit().putString("active_path", activeFile!!.absolutePath).apply()  
    } else {  
        prefs.edit().remove("active_path").apply()  
    }  
}  

private fun restoreActiveFileIfAny() {  
    val prefs = getSharedPreferences("cyber_prefs", MODE_PRIVATE)  
    val path = prefs.getString("active_path", null) ?: return  
    val file = File(path)  
    if (file.exists()) {  
        activeFile = file  
        val content = try { file.readText() } catch (e: Exception) { "" }  
        cyberEditor.setText(content)  
        cyberEditor.isEnabled = true  
        cyberActiveFileLabel.text = "📄 ${file.name}"  
    } else {  
        prefs.edit().remove("active_path").apply()  
    }  
}  

// ---------- الأيقونات الحقيقية (Favicons) ----------  

private fun loadFaviconInto(imageView: ImageView, url: String, fallbackTitle: String) {  
    setFallbackLetterIcon(imageView, fallbackTitle)  
    try {  
        val host = Uri.parse(url).host ?: return  
        val faviconUrl = "https://www.google.com/s2/favicons?domain=$host&sz=64"  
        Thread {  
            try {  
                val input = URL(faviconUrl).openStream()  
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)  
                input.close()  
                if (bitmap != null) {  
                    runOnUiThread {  
                        imageView.setImageBitmap(bitmap)  
                    }  
                }  
            } catch (e: Exception) {  
                // يفضل الحرف الاحتياطي كما هو لو فشل التحميل  
            }  
        }.start()  
    } catch (e: Exception) {  
        // يفضل الحرف الاحتياطي كما هو  
    }  
}  

private fun setFallbackLetterIcon(imageView: ImageView, title: String) {  
    val size = dp(32)  
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)  
    val canvas = android.graphics.Canvas(bitmap)  
    val paint = android.graphics.Paint()  
    paint.color = android.graphics.Color.parseColor("#4A3A7A")  
    paint.isAntiAlias = true  
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)  
    paint.color = android.graphics.Color.WHITE  
    paint.textSize = size * 0.5f  
    paint.textAlign = android.graphics.Paint.Align.CENTER  
    val textY = size / 2f - (paint.descent() + paint.ascent()) / 2f  
    canvas.drawText(title.take(1).uppercase(), size / 2f, textY, paint)  
    imageView.setImageBitmap(bitmap)  
}  

// ---------- بايثون (Pyodide) ----------  

@SuppressLint("SetJavaScriptEnabled")  
private fun setupPyodideWebView() {  
    pyodideWebView.settings.javaScriptEnabled = true  
    pyodideWebView.settings.domStorageEnabled = true  
    pyodideWebView.addJavascriptInterface(PyBridge(), "Android")  
    pyodideWebView.webViewClient = object : WebViewClient() {}  
    pyodideWebView.loadUrl("file:///android_asset/pyodide_runner.html")  
}  

private fun appendTerminal(text: String, colorHex: String) {  
    val start = terminalBuilder.length  
    terminalBuilder.append(text)  
    terminalBuilder.setSpan(  
        ForegroundColorSpan(android.graphics.Color.parseColor(colorHex)),  
        start,  
        terminalBuilder.length,  
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE  
    )  
    cyberTerminalOutput.text = terminalBuilder  
}  

private fun logDebugEntry(message: String) {  
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())  
    debugEntries.add(0, DebugEntry(time, message))  
    if (debugEntries.size > 50) {  
        debugEntries.removeAt(debugEntries.size - 1)  
    }  
}  

private inner class PyBridge {  
    @JavascriptInterface  
    fun onPyodideReady() {  
        runOnUiThread {  
            pyodideReady = true  
            appendTerminal("✅ بايثون جاهز\n", "#00FF41")  
            for (name in installedPackages.toList()) {  
                val encodedName = JSONObject.quote(name)  
                pyodideWebView.evaluateJavascript("installPackage($encodedName)", null)  
            }  
            val startupPath = getSharedPreferences("cyber_prefs", MODE_PRIVATE).getString("startup_script", null)  
            if (startupPath != null) {  
                val startupFile = File(startupPath)  
                if (startupFile.exists()) {  
                    val code = try { startupFile.readText() } catch (e: Exception) { "" }  
                    if (code.isNotBlank()) {  
                        appendTerminal("\n🚀 تشغيل تلقائي: ${startupFile.name}\n", "#00FF41")  
                        val encoded = JSONObject.quote(code)  
                        pyodideWebView.evaluateJavascript("runPythonCode($encoded)", null)  
                    }  
                }  
            }  
        }  
    }  

    @JavascriptInterface  
    fun onPyodideError(message: String) {  
        runOnUiThread {  
            appendTerminal("❌ فشل تجهيز بايثون: $message\n", "#FF6B6B")  
            logDebugEntry("فشل تجهيز بايثون: $message")  
        }  
    }  

    @JavascriptInterface  
    fun onPythonOutput(text: String) {  
        runOnUiThread {  
            appendTerminal(text, "#66FF8A")  
        }  
    }  

    @JavascriptInterface  
    fun onPythonDone() {  
        runOnUiThread {  
            val elapsed = if (lastRunStartTime > 0) System.currentTimeMillis() - lastRunStartTime else -1  
            if (elapsed >= 0) {  
                appendTerminal("\n✅ انتهى التنفيذ (${elapsed} مللي ثانية)\n", "#00FF41")  
            } else {  
                appendTerminal("\n✅ انتهى التنفيذ\n", "#00FF41")  
            }  
            lastRunStartTime = 0  
        }  
    }  

    @JavascriptInterface  
    fun onPythonError(message: String) {  
        runOnUiThread {  
            appendTerminal("\n❌ خطأ: $message\n", "#FF6B6B")  
            logDebugEntry(message)  
            lastRunStartTime = 0  
        }  
    }  

    @JavascriptInterface  
    fun onPackageInstalled(name: String) {  
        runOnUiThread {  
            if (!installedPackages.contains(name)) {  
                installedPackages.add(name)  
                saveInstalledPackages()  
                refreshPackageList()  
            }  
            cyberInstallPackageButton.isEnabled = true  
            cyberInstallPackageButton.text = "⬇ تثبيت"  
            Toast.makeText(this@MainActivity, "تم تثبيت $name بنجاح", Toast.LENGTH_SHORT).show()  
        }  
    }  

    @JavascriptInterface  
    fun onPackageError(name: String, message: String) {  
        runOnUiThread {  
            cyberInstallPackageButton.isEnabled = true  
            cyberInstallPackageButton.text = "⬇ تثبيت"  
            Toast.makeText(this@MainActivity, "فشل تثبيت $name: $message", Toast.LENGTH_LONG).show()  
            logDebugEntry("فشل تثبيت $name: $message")  
        }  
    }  
}  

private fun runActiveFileAsPython() {  
    val file = activeFile  
    if (file == null) {  
        Toast.makeText(this, "افتح ملف بايثون أولاً من قائمة الملفات", Toast.LENGTH_SHORT).show()  
        return  
    }  
    if (!pyodideReady) {  
        Toast.makeText(this, "بايثون لسا يتجهز، انتظر شوي", Toast.LENGTH_SHORT).show()  
        return  
    }  
    saveActiveFileIfNeeded()  
    val code = cyberEditor.text.toString()  
    appendTerminal("\n▶ تشغيل: ${file.name}\n", "#00FF41")  
    lastRunStartTime = System.currentTimeMillis()  
    val encodedCode = JSONObject.quote(code)  
    pyodideWebView.evaluateJavascript("runPythonCode($encodedCode)", null)  
}  

private fun runTerminalCommand(input: EditText) {  
    val command = input.text.toString().trim()  
    if (command.isEmpty()) return  
    if (!pyodideReady) {  
        Toast.makeText(this, "بايثون لسا يتجهز، انتظر شوي", Toast.LENGTH_SHORT).show()  
        return  
    }  
    appendTerminal("\n>>> $command\n", "#00FF41")  
    lastRunStartTime = System.currentTimeMillis()  
    val encodedCode = JSONObject.quote(command)  
    pyodideWebView.evaluateJavascript("runPythonCode($encodedCode)", null)  
    input.setText("")  
}  

// ---------- المكتبات ----------  

private fun installPackageFromInput(input: EditText) {  
    val name = input.text.toString().trim()  
    if (name.isEmpty()) return  
    if (!pyodideReady) {  
        Toast.makeText(this, "بايثون لسا يتجهز، انتظر شوي", Toast.LENGTH_SHORT).show()  
        return  
    }  
    cyberInstallPackageButton.isEnabled = false  
    cyberInstallPackageButton.text = "⏳ جاري التثبيت"  
    Toast.makeText(this, "جاري تثبيت $name...", Toast.LENGTH_SHORT).show()  
    val encodedName = JSONObject.quote(name)  
    pyodideWebView.evaluateJavascript("installPackage($encodedName)", null)  
    input.setText("")  
}  

private fun loadInstalledPackages() {  
    val prefs = getSharedPreferences("packages_prefs", MODE_PRIVATE)  
    val raw = prefs.getString("list", "") ?: ""  
    installedPackages.clear()  
    if (raw.isNotBlank()) {  
        installedPackages.addAll(raw.split("\u0002").filter { it.isNotBlank() })  
    }  
    refreshPackageList()  
}  

private fun saveInstalledPackages() {  
    val prefs = getSharedPreferences("packages_prefs", MODE_PRIVATE)  
    prefs.edit().putString("list", installedPackages.joinToString("\u0002")).apply()  
}  

private fun refreshPackageList() {  
    cyberPackageListContainer.removeAllViews()  
    if (installedPackages.isEmpty()) {  
        val emptyLabel = TextView(this)  
        emptyLabel.text = "لا توجد مكتبات مثبتة بعد"  
        emptyLabel.setTextColor(android.graphics.Color.parseColor("#3A6B45"))  
        emptyLabel.textSize = 12f  
        cyberPackageListContainer.addView(emptyLabel)  
        return  
    }  
    for (name in installedPackages) {  
        val row = LinearLayout(this)  
        row.orientation = LinearLayout.HORIZONTAL  
        row.gravity = android.view.Gravity.CENTER_VERTICAL  
        row.setPadding(dp(6), dp(8), dp(6), dp(8))  

        val label = TextView(this)  
        label.text = "📦 $name"  
        label.setTextColor(android.graphics.Color.parseColor("#66FF8A"))  
        label.textSize = 13f  
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)  
        label.layoutParams = lp  

        val remove = TextView(this)  
        remove.text = "✕"  
        remove.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))  
        remove.textSize = 14f  
        remove.setPadding(dp(12), 0, dp(4), 0)  
        remove.setOnClickListener {  
            installedPackages.remove(name)  
            saveInstalledPackages()  
            refreshPackageList()  
            Toast.makeText(this, "تمت الإزالة — لن تُحمّل بالمرة الجاية", Toast.LENGTH_SHORT).show()  
        }  

        row.addView(label)  
        row.addView(remove)  
        cyberPackageListContainer.addView(row)  
    }  
}  

// ---------- مكتبة الأكواد (Snippets) ----------  

private fun loadSnippets() {  
    val prefs = getSharedPreferences("snippets_prefs", MODE_PRIVATE)  
    val raw = prefs.getString("list", "") ?: ""  
    snippets.clear()  
    if (raw.isNotBlank()) {  
        val entries = raw.split("\u0003")  
        for (entry in entries) {  
            val parts = entry.split("\u0001")  
            if (parts.size == 2) snippets.add(Snippet(parts[0], parts[1]))  
        }  
    }  
    refreshSnippetList()  
}  

private fun saveSnippets() {  
    val prefs = getSharedPreferences("snippets_prefs", MODE_PRIVATE)  
    val sb = StringBuilder()  
    for (i in snippets.indices) {  
        if (i > 0) sb.append("\u0003")  
        sb.append(snippets[i].title)  
        sb.append("\u0001")  
        sb.append(snippets[i].code)  
    }  
    prefs.edit().putString("list", sb.toString()).apply()  
}  

private fun refreshSnippetList() {  
    cyberSnippetListContainer.removeAllViews()  
    if (snippets.isEmpty()) {  
        val emptyLabel = TextView(this)  
        emptyLabel.text = "لا توجد أكواد محفوظة بعد"  
        emptyLabel.setTextColor(android.graphics.Color.parseColor("#3A6B45"))  
        emptyLabel.textSize = 12f  
        cyberSnippetListContainer.addView(emptyLabel)  
        return  
    }  
    for (i in snippets.indices) {  
        val snippet = snippets[i]  
        val row = LinearLayout(this)  
        row.orientation = LinearLayout.HORIZONTAL  
        row.gravity = android.view.Gravity.CENTER_VERTICAL  
        row.setPadding(dp(6), dp(10), dp(6), dp(10))  

        val label = TextView(this)  
        label.text = "📄 ${snippet.title}"  
        label.setTextColor(android.graphics.Color.parseColor("#66FF8A"))  
        label.textSize = 13f  
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)  
        label.layoutParams = lp  

        val remove = TextView(this)  
        remove.text = "✕"  
        remove.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))  
        remove.textSize = 14f  
        remove.setPadding(dp(12), 0, dp(4), 0)  
        remove.setOnClickListener {  
            snippets.removeAt(i)  
            saveSnippets()  
            refreshSnippetList()  
        }  

        row.addView(label)  
        row.addView(remove)  

        row.setOnClickListener { insertSnippetIntoEditor(snippet) }  

        cyberSnippetListContainer.addView(row)  
    }  
}  

private fun showAddSnippetDialog() {  
    val layout = LinearLayout(this)  
    layout.orientation = LinearLayout.VERTICAL  
    layout.setPadding(dp(20), dp(10), dp(20), dp(10))  

    val titleInput = EditText(this)  
    titleInput.hint = "عنوان الكود (مثال: قراءة ملف)"  

    val codeInput = EditText(this)  
    codeInput.hint = "الصق الكود هنا"  
    codeInput.minLines = 4  
    codeInput.gravity = android.view.Gravity.TOP  

    layout.addView(titleInput)  
    layout.addView(codeInput)  

    AlertDialog.Builder(this)  
        .setTitle("إضافة كود جديد")  
        .setView(layout)  
        .setPositiveButton("حفظ") { _: DialogInterface, _: Int ->  
            val title = titleInput.text.toString().trim()  
            val code = codeInput.text.toString()  
            if (title.isEmpty() || code.isBlank()) {  
                Toast.makeText(this, "الرجاء تعبئة الحقلين", Toast.LENGTH_SHORT).show()  
                return@setPositiveButton  
            }  
            snippets.add(Snippet(title, code))  
            saveSnippets()  
            refreshSnippetList()  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun insertSnippetIntoEditor(snippet: Snippet) {  
    if (activeFile == null) {  
        Toast.makeText(this, "افتح ملف أولاً من قائمة الملفات", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val current = cyberEditor.text.toString()  
    val newText = if (current.isEmpty()) snippet.code else "$current\n${snippet.code}"  
    cyberEditor.setText(newText)  
    cyberEditor.setSelection(cyberEditor.text.length)  
    cyberPanel.visibility = View.GONE  
    cyberOpenPanel = null  
    Toast.makeText(this, "تمت إضافة الكود", Toast.LENGTH_SHORT).show()  
}  

// ---------- أدوات المبرمج (Hash / Base64 / JSON) ----------  

private fun computeHash(text: String, algorithm: String): String {  
    return try {  
        val digest = MessageDigest.getInstance(algorithm)  
        val bytes = digest.digest(text.toByteArray())  
        val sb = StringBuilder()  
        for (b in bytes) sb.append(String.format("%02x", b))  
        sb.toString()  
    } catch (e: Exception) {  
        "خطأ"  
    }  
}  

private fun showHashToolDialog() {  
    val layout = LinearLayout(this)  
    layout.orientation = LinearLayout.VERTICAL  
    layout.setPadding(dp(20), dp(10), dp(20), dp(10))  

    val input = EditText(this)  
    input.hint = "النص المراد تشفيره"  
    input.minLines = 2  
    layout.addView(input)  

    val buttonsRow = LinearLayout(this)  
    buttonsRow.orientation = LinearLayout.HORIZONTAL  
    buttonsRow.setPadding(0, dp(10), 0, dp(10))  
    val md5Button = Button(this)  
    md5Button.text = "MD5"  
    val sha256Button = Button(this)  
    sha256Button.text = "SHA-256"  
    val p1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)  
    val p2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)  
    p1.marginEnd = dp(6)  
    md5Button.layoutParams = p1  
    sha256Button.layoutParams = p2  
    buttonsRow.addView(md5Button)  
    buttonsRow.addView(sha256Button)  
    layout.addView(buttonsRow)  

    val resultView = TextView(this)  
    resultView.textSize = 12f  
    resultView.setTextIsSelectable(true)  
    layout.addView(resultView)  

    md5Button.setOnClickListener {  
        resultView.text = "MD5:\n" + computeHash(input.text.toString(), "MD5")  
    }  
    sha256Button.setOnClickListener {  
        resultView.text = "SHA-256:\n" + computeHash(input.text.toString(), "SHA-256")  
    }  

    AlertDialog.Builder(this)  
        .setTitle("🔢 مولّد Hash")  
        .setView(layout)  
        .setPositiveButton("إغلاق", null)  
        .show()  
}  

private fun showBase64ToolDialog() {  
    val layout = LinearLayout(this)  
    layout.orientation = LinearLayout.VERTICAL  
    layout.setPadding(dp(20), dp(10), dp(20), dp(10))  

    val input = EditText(this)  
    input.hint = "النص"  
    input.minLines = 2  
    layout.addView(input)  

    val buttonsRow = LinearLayout(this)  
    buttonsRow.orientation = LinearLayout.HORIZONTAL  
    buttonsRow.setPadding(0, dp(10), 0, dp(10))  
    val encodeButton = Button(this)  
    encodeButton.text = "Encode"  
    val decodeButton = Button(this)  
    decodeButton.text = "Decode"  
    val p1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)  
    val p2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)  
    p1.marginEnd = dp(6)  
    encodeButton.layoutParams = p1  
    decodeButton.layoutParams = p2  
    buttonsRow.addView(encodeButton)  
    buttonsRow.addView(decodeButton)  
    layout.addView(buttonsRow)  

    val resultView = TextView(this)  
    resultView.textSize = 12f  
    resultView.setTextIsSelectable(true)  
    layout.addView(resultView)  

    encodeButton.setOnClickListener {  
        resultView.text = try {  
            Base64.encodeToString(input.text.toString().toByteArray(), Base64.NO_WRAP)  
        } catch (e: Exception) {  
            "خطأ"  
        }  
    }  
    decodeButton.setOnClickListener {  
        resultView.text = try {  
            String(Base64.decode(input.text.toString(), Base64.NO_WRAP))  
        } catch (e: Exception) {  
            "خطأ: نص Base64 غير صالح"  
        }  
    }  

    AlertDialog.Builder(this)  
        .setTitle("🔤 Base64")  
        .setView(layout)  
        .setPositiveButton("إغلاق", null)  
        .show()  
}  

private fun showJsonFormatterDialog() {  
    val layout = LinearLayout(this)  
    layout.orientation = LinearLayout.VERTICAL  
    layout.setPadding(dp(20), dp(10), dp(20), dp(10))  

    val input = EditText(this)  
    input.hint = "الصق JSON هنا"  
    input.minLines = 4  
    layout.addView(input)  

    val formatButton = Button(this)  
    formatButton.text = "تنسيق وفحص"  
    layout.addView(formatButton)  

    val resultView = TextView(this)  
    resultView.textSize = 12f  
    resultView.typeface = android.graphics.Typeface.MONOSPACE  
    resultView.setTextIsSelectable(true)  
    val resultScroll = ScrollView(this)  
    val resultScrollParams = LinearLayout.LayoutParams(  
        LinearLayout.LayoutParams.MATCH_PARENT,  
        dp(200)  
    )  
    resultScroll.layoutParams = resultScrollParams  
    resultScroll.addView(resultView)  
    layout.addView(resultScroll)  

    formatButton.setOnClickListener {  
        val raw = input.text.toString().trim()  
        try {  
            val formatted = if (raw.startsWith("[")) {  
                JSONArray(raw).toString(4)  
            } else {  
                JSONObject(raw).toString(4)  
            }  
            resultView.setTextColor(android.graphics.Color.parseColor("#66FF8A"))  
            resultView.text = formatted  
        } catch (e: Exception) {  
            resultView.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))  
            resultView.text = "❌ JSON غير صالح: ${e.message}"  
        }  
    }  

    AlertDialog.Builder(this)  
        .setTitle("📋 منسّق ومدقق JSON")  
        .setView(layout)  
        .setPositiveButton("إغلاق", null)  
        .show()  
}  

// ---------- القائمة الإضافية بالوضع السيبراني ----------  

private fun showCyberExtraMenu(anchor: View) {  
    val popup = PopupMenu(this, anchor)  
    popup.menu.add(0, 1, 0, "🐞 سجل الأخطاء")  
    popup.menu.add(0, 2, 0, "📸 حفظ لقطة من المشروع")  
    popup.menu.add(0, 3, 0, "🕘 استعادة لقطة سابقة")  
    popup.menu.add(0, 4, 0, "📤 تصدير المشروع (ZIP)")  
    popup.menu.add(0, 5, 0, "📥 استيراد مشروع (ZIP)")  
    popup.menu.add(0, 6, 0, "🐍 مرجع بايثون سريع")  
    popup.menu.add(0, 7, 0, "🚀 تعيين ملف تشغيل تلقائي")  
    popup.menu.add(0, 8, 0, "🔢 مولّد Hash")  
    popup.menu.add(0, 9, 0, "🔤 Base64")  
    popup.menu.add(0, 10, 0, "📋 منسّق JSON")  
    popup.setOnMenuItemClickListener { item: android.view.MenuItem ->  
        when (item.itemId) {  
            1 -> showDebugLogDialog()  
            2 -> saveSnapshot()  
            3 -> showSnapshotsDialog()  
            4 -> exportProject()  
            5 -> importProjectLauncher.launch(arrayOf("application/zip", "application/octet-stream"))  
            6 -> showPythonCheatSheet()  
            7 -> setStartupScript()  
            8 -> showHashToolDialog()  
            9 -> showBase64ToolDialog()  
            10 -> showJsonFormatterDialog()  
        }  
        true  
    }  
    popup.show()  
}  

private fun setStartupScript() {  
    val file = activeFile  
    if (file == null) {  
        Toast.makeText(this, "افتح ملف أولاً لتعيينه كملف بدء تشغيل", Toast.LENGTH_SHORT).show()  
        return  
    }  
    getSharedPreferences("cyber_prefs", MODE_PRIVATE).edit()  
        .putString("startup_script", file.absolutePath).apply()  
    Toast.makeText(this, "تم تعيين ${file.name} ليشتغل تلقائيًا", Toast.LENGTH_SHORT).show()  
}  

private fun showDebugLogDialog() {  
    if (debugEntries.isEmpty()) {  
        Toast.makeText(this, "لا توجد أخطاء مسجّلة", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val listLayout = LinearLayout(this)  
    listLayout.orientation = LinearLayout.VERTICAL  
    listLayout.setPadding(dp(16), dp(8), dp(16), dp(8))  
    for (entry in debugEntries) {  
        val row = TextView(this)  
        row.text = "[${entry.time}] ${entry.message}"  
        row.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))  
        row.textSize = 12f  
        row.setPadding(0, dp(6), 0, dp(6))  
        listLayout.addView(row)  
    }  
    val scroll = ScrollView(this)  
    scroll.addView(listLayout)  
    AlertDialog.Builder(this)  
        .setTitle("🐞 سجل الأخطاء")  
        .setView(scroll)  
        .setPositiveButton("إغلاق", null)  
        .setNegativeButton("مسح السجل") { _: DialogInterface, _: Int ->  
            debugEntries.clear()  
        }  
        .show()  
}  

private fun showPythonCheatSheet() {  
    val cheatSheetText = """  
        المتغيرات: x = 5  
        نص: s = "hello"  
        قائمة: lst = [1, 2, 3]  
        قاموس: d = {"key": "value"}  

        شرط:  
        if x > 0:  
            print("موجب")  
        elif x == 0:  
            print("صفر")  
        else:  
            print("سالب")  

        حلقة for:  
        for i in range(5):  
            print(i)  

        حلقة while:  
        while x > 0:  
            x -= 1  

        دالة:  
        def add(a, b):  
            return a + b  

        قراءة ملف:  
        with open("file.txt") as f:  
            content = f.read()  

        قاعدة بيانات SQLite (مدعومة تلقائيًا):  
        import sqlite3  
        conn = sqlite3.connect("data.db")  
        cur = conn.cursor()  
        cur.execute("CREATE TABLE t (id INT)")  
    """.trimIndent()  

    val textView = TextView(this)  
    textView.text = cheatSheetText  
    textView.textSize = 12f  
    textView.typeface = android.graphics.Typeface.MONOSPACE  
    textView.setPadding(dp(16), dp(16), dp(16), dp(16))  
    val scroll = ScrollView(this)  
    scroll.addView(textView)  

    AlertDialog.Builder(this)  
        .setTitle("🐍 مرجع بايثون سريع")  
        .setView(scroll)  
        .setPositiveButton("إغلاق", null)  
        .show()  
}  

// ---------- تشفير ملف بكلمة سر خاصة ----------  

private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKeySpec {  
    val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)  
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")  
    val keyBytes = factory.generateSecret(spec).encoded  
    return SecretKeySpec(keyBytes, "AES")  
}  

private fun encryptFileWithPassword(file: File, password: String) {  
    val plainBytes = file.readBytes()  
    val salt = ByteArray(16)  
    val iv = ByteArray(16)  
    SecureRandom().nextBytes(salt)  
    SecureRandom().nextBytes(iv)  
    val key = deriveKeyFromPassword(password, salt)  
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")  
    cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))  
    val encrypted = cipher.doFinal(plainBytes)  

    val output = ByteArrayOutputStream()  
    output.write(salt)  
    output.write(iv)  
    output.write(encrypted)  

    val lockedFile = File(file.parentFile, file.name + ".locked")  
    lockedFile.writeBytes(output.toByteArray())  
    file.delete()  
}  

private fun decryptFileWithPassword(lockedFile: File, password: String): Boolean {  
    return try {  
        val allBytes = lockedFile.readBytes()  
        val salt = allBytes.copyOfRange(0, 16)  
        val iv = allBytes.copyOfRange(16, 32)  
        val cipherBytes = allBytes.copyOfRange(32, allBytes.size)  
        val key = deriveKeyFromPassword(password, salt)  
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")  
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))  
        val decrypted = cipher.doFinal(cipherBytes)  

        val originalName = lockedFile.name.removeSuffix(".locked")  
        val originalFile = File(lockedFile.parentFile, originalName)  
        originalFile.writeBytes(decrypted)  
        lockedFile.delete()  
        true  
    } catch (e: Exception) {  
        false  
    }  
}  

private fun showEncryptFileDialog(file: File) {  
    val layout = LinearLayout(this)  
    layout.orientation = LinearLayout.VERTICAL  
    layout.setPadding(dp(20), dp(10), dp(20), dp(10))  
    val pass1 = EditText(this)  
    pass1.hint = "كلمة السر"  
    pass1.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD  
    val pass2 = EditText(this)  
    pass2.hint = "تأكيد كلمة السر"  
    pass2.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD  
    layout.addView(pass1)  
    layout.addView(pass2)  

    AlertDialog.Builder(this)  
        .setTitle("🔒 تشفير ${file.name}")  
        .setView(layout)  
        .setPositiveButton("تشفير") { _: DialogInterface, _: Int ->  
            val p1 = pass1.text.toString()  
            val p2 = pass2.text.toString()  
            if (p1.isEmpty() || p1 != p2) {  
                Toast.makeText(this, "كلمتا السر غير متطابقتين", Toast.LENGTH_SHORT).show()  
                return@setPositiveButton  
            }  
            if (activeFile?.absolutePath == file.absolutePath) {  
                activeFile = null  
                cyberEditor.setText("")  
                cyberEditor.isEnabled = false  
                cyberActiveFileLabel.text = "لا يوجد ملف مفتوح"  
                saveActiveFilePath()  
            }  
            encryptFileWithPassword(file, p1)  
            refreshFileList()  
            Toast.makeText(this, "تم التشفير", Toast.LENGTH_SHORT).show()  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun showDecryptFileDialog(file: File) {  
    val input = EditText(this)  
    input.hint = "كلمة السر"  
    input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD  
    AlertDialog.Builder(this)  
        .setTitle("🔓 فك تشفير ${file.name}")  
        .setView(input)  
        .setPositiveButton("فك التشفير") { _: DialogInterface, _: Int ->  
            val password = input.text.toString()  
            val success = decryptFileWithPassword(file, password)  
            if (success) {  
                Toast.makeText(this, "تم فك التشفير بنجاح", Toast.LENGTH_SHORT).show()  
                refreshFileList()  
            } else {  
                Toast.makeText(this, "كلمة السر غير صحيحة", Toast.LENGTH_SHORT).show()  
            }  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

// ---------- لقطات الحفظ (Snapshots) ----------  

private fun saveSnapshot() {  
    try {  
        val snapshotsDir = File(filesDir, "cyber_snapshots")  
        if (!snapshotsDir.exists()) snapshotsDir.mkdirs()  
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())  
        val target = File(snapshotsDir, timestamp)  
        target.mkdirs()  
        copyDirectoryRecursive(rootDir, target)  
        Toast.makeText(this, "تم حفظ اللقطة: $timestamp", Toast.LENGTH_SHORT).show()  
    } catch (e: Exception) {  
        Toast.makeText(this, "فشل حفظ اللقطة", Toast.LENGTH_SHORT).show()  
    }  
}  

private fun showSnapshotsDialog() {  
    val snapshotsDir = File(filesDir, "cyber_snapshots")  
    val snapshots = snapshotsDir.listFiles()?.filter { it.isDirectory }?.sortedDescending() ?: emptyList()  
    if (snapshots.isEmpty()) {  
        Toast.makeText(this, "لا توجد لقطات محفوظة", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val names = snapshots.map { it.name }.toTypedArray()  
    AlertDialog.Builder(this)  
        .setTitle("🕘 اختر لقطة للاستعادة")  
        .setItems(names) { _: DialogInterface, which: Int ->  
            confirmRestoreSnapshot(snapshots[which])  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun confirmRestoreSnapshot(snapshotDir: File) {  
    AlertDialog.Builder(this)  
        .setTitle("استعادة لقطة")  
        .setMessage("هذا بيستبدل كل ملفاتك الحالية بمحتوى اللقطة. متأكد؟")  
        .setPositiveButton("استعادة") { _: DialogInterface, _: Int ->  
            try {  
                rootDir.deleteRecursively()  
                rootDir.mkdirs()  
                copyDirectoryRecursive(snapshotDir, rootDir)  
                currentDir = rootDir  
                activeFile = null  
                cyberEditor.setText("")  
                cyberEditor.isEnabled = false  
                cyberActiveFileLabel.text = "لا يوجد ملف مفتوح"  
                saveActiveFilePath()  
                Toast.makeText(this, "تمت الاستعادة بنجاح", Toast.LENGTH_SHORT).show()  
            } catch (e: Exception) {  
                Toast.makeText(this, "فشلت الاستعادة", Toast.LENGTH_SHORT).show()  
            }  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun copyDirectoryRecursive(source: File, target: File) {  
    if (source.isDirectory) {  
        if (!target.exists()) target.mkdirs()  
        val children = source.listFiles() ?: return  
        for (child in children) {  
            copyDirectoryRecursive(child, File(target, child.name))  
        }  
    } else {  
        source.copyTo(target, overwrite = true)  
    }  
}  

// ---------- تصدير / استيراد المشروع (ZIP) ----------  

private fun exportProject() {  
    val input = EditText(this)  
    input.hint = "كلمة سر للحماية (اختياري، اتركه فاضي بدون تشفير)"  
    input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD  
    AlertDialog.Builder(this)  
        .setTitle("📤 تصدير المشروع")  
        .setView(input)  
        .setPositiveButton("تصدير") { _: DialogInterface, _: Int ->  
            doExportProject(input.text.toString())  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun doExportProject(password: String) {  
    try {  
        val exportsDir = File(getExternalFilesDir(null), "exports")  
        if (!exportsDir.exists()) exportsDir.mkdirs()  
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())  

        val zipBytesOutput = ByteArrayOutputStream()  
        ZipOutputStream(BufferedOutputStream(zipBytesOutput)).use { zos: ZipOutputStream ->  
            zipDirectoryContents(rootDir, rootDir, zos)  
        }  
        val zipBytes = zipBytesOutput.toByteArray()  

        if (password.isBlank()) {  
            val zipFile = File(exportsDir, "project_$timestamp.zip")  
            zipFile.writeBytes(zipBytes)  
            Toast.makeText(this, "تم التصدير: ${zipFile.absolutePath}", Toast.LENGTH_LONG).show()  
        } else {  
            val salt = ByteArray(16)  
            val iv = ByteArray(16)  
            SecureRandom().nextBytes(salt)  
            SecureRandom().nextBytes(iv)  
            val key = deriveKeyFromPassword(password, salt)  
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")  
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))  
            val encrypted = cipher.doFinal(zipBytes)  

            val output = ByteArrayOutputStream()  
            output.write(salt)  
            output.write(iv)  
            output.write(encrypted)  

            val encFile = File(exportsDir, "project_$timestamp.zip.enc")  
            encFile.writeBytes(output.toByteArray())  
            Toast.makeText(this, "تم التصدير المشفّر: ${encFile.absolutePath}", Toast.LENGTH_LONG).show()  
        }  
    } catch (e: Exception) {  
        Toast.makeText(this, "فشل التصدير", Toast.LENGTH_SHORT).show()  
    }  
}  

private fun zipDirectoryContents(baseDir: File, current: File, zos: ZipOutputStream) {  
    val files = current.listFiles() ?: return  
    for (file in files) {  
        val relativePath = file.absolutePath.removePrefix(baseDir.absolutePath + File.separator)  
        if (file.isDirectory) {  
            zipDirectoryContents(baseDir, file, zos)  
        } else {  
            val entry = ZipEntry(relativePath)  
            zos.putNextEntry(entry)  
            BufferedInputStream(FileInputStream(file)).use { input: BufferedInputStream ->  
                input.copyTo(zos)  
            }  
            zos.closeEntry()  
        }  
    }  
}  

private fun importProjectFromUri(uri: Uri) {  
    val path = uri.path ?: ""  
    if (path.endsWith(".enc")) {  
        val input = EditText(this)  
        input.hint = "كلمة السر"  
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD  
        AlertDialog.Builder(this)  
            .setTitle("استيراد مشروع مشفّر")  
            .setView(input)  
            .setPositiveButton("استيراد") { _: DialogInterface, _: Int ->  
                importEncryptedProject(uri, input.text.toString())  
            }  
            .setNegativeButton("إلغاء", null)  
            .show()  
    } else {  
        importPlainProject(uri)  
    }  
}  

private fun importPlainProject(uri: Uri) {  
    try {  
        val inputStream = contentResolver.openInputStream(uri) ?: return  
        unzipStreamToRoot(BufferedInputStream(inputStream))  
        Toast.makeText(this, "تم استيراد المشروع بنجاح", Toast.LENGTH_SHORT).show()  
    } catch (e: Exception) {  
        Toast.makeText(this, "فشل الاستيراد", Toast.LENGTH_SHORT).show()  
    }  
}  

private fun importEncryptedProject(uri: Uri, password: String) {  
    try {  
        val inputStream = contentResolver.openInputStream(uri) ?: return  
        val allBytes = inputStream.readBytes()  
        inputStream.close()  
        val salt = allBytes.copyOfRange(0, 16)  
        val iv = allBytes.copyOfRange(16, 32)  
        val cipherBytes = allBytes.copyOfRange(32, allBytes.size)  
        val key = deriveKeyFromPassword(password, salt)  
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")  
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))  
        val zipBytes = cipher.doFinal(cipherBytes)  
        unzipStreamToRoot(BufferedInputStream(ByteArrayInputStream(zipBytes)))  
        Toast.makeText(this, "تم استيراد المشروع بنجاح", Toast.LENGTH_SHORT).show()  
    } catch (e: Exception) {  
        Toast.makeText(this, "فشل الاستيراد — تأكد من كلمة السر", Toast.LENGTH_SHORT).show()  
    }  
}  

private fun unzipStreamToRoot(inputStream: BufferedInputStream) {  
    ZipInputStream(inputStream).use { zis: ZipInputStream ->  
        var entry: ZipEntry? = zis.nextEntry  
        while (entry != null) {  
            val outFile = File(rootDir, entry.name)  
            if (entry.isDirectory) {  
                outFile.mkdirs()  
            } else {  
                outFile.parentFile?.mkdirs()  
                BufferedOutputStream(FileOutputStream(outFile)).use { out: BufferedOutputStream ->  
                    zis.copyTo(out)  
                }  
            }  
            zis.closeEntry()  
            entry = zis.nextEntry  
        }  
    }  
    currentDir = rootDir  
}  

// ---------- القفل ----------  

override fun onStart() {  
    super.onStart()  
    if (isLocked) showLockPrompt()  
}  

private fun showLockPrompt() {  
    val biometricManager = BiometricManager.from(this)  
    val canAuth = biometricManager.canAuthenticate(  
        BiometricManager.Authenticators.BIOMETRIC_WEAK or  
            BiometricManager.Authenticators.DEVICE_CREDENTIAL  
    )  
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {  
        isLocked = false  
        lockOverlay.visibility = View.GONE  
        updateChromeVisibility()  
        resetAutoLockTimer()  
        return  
    }  
    val executor = ContextCompat.getMainExecutor(this)  
    val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {  
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {  
            isLocked = false  
            failedUnlockAttempts = 0  
            lockOverlay.visibility = View.GONE  
            updateChromeVisibility()  
            resetAutoLockTimer()  
        }  

        override fun onAuthenticationFailed() {  
            failedUnlockAttempts++  
            if (failedUnlockAttempts >= 5) {  
                panicWipeEverything()  
            }  
        }  
    })  
    val promptInfo = BiometricPrompt.PromptInfo.Builder()  
        .setTitle("فتح المتصفح الخاص")  
        .setAllowedAuthenticators(  
            BiometricManager.Authenticators.BIOMETRIC_WEAK or  
                BiometricManager.Authenticators.DEVICE_CREDENTIAL  
        )  
        .build()  
    prompt.authenticate(promptInfo)  
}  

private fun emergencyWipeAndClose() {  
    wipeEverything()  
    clearSavedTabs()  
    isLocked = true  
    finishAffinity()  
}  

// ---------- الحذف الآمن ----------  

private fun secureDeleteFile(file: File) {  
    try {  
        val length = file.length()  
        if (length > 0) {  
            val randomBytes = ByteArray(length.toInt())  
            SecureRandom().nextBytes(randomBytes)  
            file.writeBytes(randomBytes)  
        }  
    } catch (e: Exception) {  
        // تجاهل، بنكمل الحذف حتى لو فشلت الكتابة العشوائية  
    }  
    file.delete()  
}  

private fun secureWipeDirectory(dir: File) {  
    val entries = dir.listFiles() ?: return  
    for (entry in entries) {  
        if (entry.isDirectory) {  
            secureWipeDirectory(entry)  
            entry.delete()  
        } else {  
            secureDeleteFile(entry)  
        }  
    }  
}  

// ---------- محو شامل فوري (Panic Wipe) ----------  

private fun panicWipeEverything() {  
    wipeEverything()  
    secureWipeDirectory(rootDir)  
    secureWipeDirectory(File(filesDir, "cyber_snapshots"))  
    clearSavedTabs()  
    getSharedPreferences("cyber_prefs", MODE_PRIVATE).edit().clear().apply()  
    getSharedPreferences("shortcuts_prefs", MODE_PRIVATE).edit().clear().apply()  
    getSharedPreferences("snippets_prefs", MODE_PRIVATE).edit().clear().apply()  
    getSharedPreferences("packages_prefs", MODE_PRIVATE).edit().clear().apply()  
    getSharedPreferences("blacklist_prefs", MODE_PRIVATE).edit().clear().apply()  
    getSharedPreferences("js_prefs", MODE_PRIVATE).edit().clear().apply()  
    getSharedPreferences("stats_prefs", MODE_PRIVATE).edit().clear().apply()  
    isLocked = true  
    finishAffinity()  
}  

// ---------- كشف الجهاز المروّق (Root) ----------  

private fun isDeviceRooted(): Boolean {  
    val paths = arrayOf(  
        "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",  
        "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",  
        "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"  
    )  
    for (path in paths) {  
        if (File(path).exists()) return true  
    }  
    val buildTags = android.os.Build.TAGS  
    if (buildTags != null && buildTags.contains("test-keys")) return true  
    return false  
}  

private fun checkRootStatus() {  
    if (isDeviceRooted()) {  
        AlertDialog.Builder(this)  
            .setTitle("⚠️ تنبيه أمني")  
            .setMessage("يبدو إن جهازك يحتوي صلاحيات Root، وهذا يقلل من ضمانات الحماية اللي يوفرها التطبيق. تصفح بحذر.")  
            .setPositiveButton("فهمت", null)  
            .show()  
    }  
}  

// ---------- وضع التمويه (تغيير الأيقونة) ----------  

private fun toggleDisguiseMode() {  
    val pm = packageManager  
    val realComponent = ComponentName(this, MainActivity::class.java)  
    val aliasComponent = ComponentName(this, "com.privacy.browser.DisguiseAlias")  
    val isDisguised = getSharedPreferences("security_prefs", MODE_PRIVATE).getBoolean("disguised", false)  

    if (!isDisguised) {  
        pm.setComponentEnabledSetting(realComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)  
        pm.setComponentEnabledSetting(aliasComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)  
        getSharedPreferences("security_prefs", MODE_PRIVATE).edit().putBoolean("disguised", true).apply()  
        Toast.makeText(this, "تم التفعيل! التطبيق الآن يظهر كـ\"Calculator\" بالشاشة الرئيسية", Toast.LENGTH_LONG).show()  
    } else {  
        pm.setComponentEnabledSetting(aliasComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)  
        pm.setComponentEnabledSetting(realComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)  
        getSharedPreferences("security_prefs", MODE_PRIVATE).edit().putBoolean("disguised", false).apply()  
        Toast.makeText(this, "تم إلغاء التمويه، الأيقونة رجعت طبيعية", Toast.LENGTH_LONG).show()  
    }  
}  

private fun showDisguiseConfirmation() {  
    val isDisguised = getSharedPreferences("security_prefs", MODE_PRIVATE).getBoolean("disguised", false)  
    val message = if (isDisguised) {  
        "هل تبي ترجع الأيقونة الطبيعية؟"  
    } else {  
        "التطبيق راح يختفي من مكانه الحالي ويظهر بأيقونة \"Calculator\" بدله. لازم تدور عليه بالاسم الجديد بعدها. متأكد؟"  
    }  
    AlertDialog.Builder(this)  
        .setTitle("🎭 وضع التمويه")  
        .setMessage(message)  
        .setPositiveButton("تأكيد") { _: DialogInterface, _: Int -> toggleDisguiseMode() }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

// ---------- الوضع السيبراني: التنقل العام ----------  

private fun openCyberMode() {  
    cyberContainer.visibility = View.VISIBLE  
    updateChromeVisibility()  
}  

private fun closeCyberMode() {  
    saveActiveFileIfNeeded()  
    cyberContainer.visibility = View.GONE  
    cyberPanel.visibility = View.GONE  
    cyberOpenPanel = null  
    updateChromeVisibility()  
}  

private fun toggleCyberPanel(panelKey: String, title: String) {  
    if (cyberOpenPanel == panelKey) {  
        cyberPanel.visibility = View.GONE  
        cyberOpenPanel = null  
        return  
    }  
    cyberPanelTitle.text = title  
    cyberFileManagerContainer.visibility = View.GONE  
    cyberTerminalContainer.visibility = View.GONE  
    cyberPackagesContainer.visibility = View.GONE  
    cyberSnippetsContainer.visibility = View.GONE  

    when (panelKey) {  
        "files" -> {  
            cyberFileManagerContainer.visibility = View.VISIBLE  
            refreshFileList()  
        }  
        "terminal" -> {  
            cyberTerminalContainer.visibility = View.VISIBLE  
        }  
        "packages" -> {  
            cyberPackagesContainer.visibility = View.VISIBLE  
            refreshPackageList()  
        }  
        "snippets" -> {  
            cyberSnippetsContainer.visibility = View.VISIBLE  
            refreshSnippetList()  
        }  
    }  
    cyberPanel.visibility = View.VISIBLE  
    cyberOpenPanel = panelKey  
}  

// ---------- الوضع السيبراني: مدير الملفات ----------  

private fun refreshFileList() {  
    cyberFileListContainer.removeAllViews()  

    val relativePath = currentDir.absolutePath.removePrefix(rootDir.absolutePath)  
    cyberPathLabel.text = if (relativePath.isBlank()) "/" else relativePath  

    val entries = currentDir.listFiles()  
    if (entries != null) {  
        val folders = entries.filter { it.isDirectory }.sortedBy { it.name }  
        val files = entries.filter { it.isFile }.sortedBy { it.name }  
        for (folder in folders) cyberFileListContainer.addView(createFileRow(folder, true))  
        for (file in files) cyberFileListContainer.addView(createFileRow(file, false))  
    }  

    if (entries == null || entries.isEmpty()) {  
        val emptyLabel = TextView(this)  
        emptyLabel.text = "لا توجد ملفات هنا"  
        emptyLabel.setTextColor(android.graphics.Color.parseColor("#3A6B45"))  
        emptyLabel.textSize = 12f  
        emptyLabel.setPadding(0, dp(10), 0, 0)  
        cyberFileListContainer.addView(emptyLabel)  
    }  
}  

private fun createFileRow(entry: File, isDir: Boolean): View {  
    val row = LinearLayout(this)  
    row.orientation = LinearLayout.HORIZONTAL  
    row.gravity = android.view.Gravity.CENTER_VERTICAL  
    row.setPadding(dp(6), dp(10), dp(6), dp(10))  

    val icon = TextView(this)  
    icon.text = if (isDir) "📁" else "📄"  
    icon.textSize = 16f  
    icon.setPadding(0, 0, dp(10), 0)  

    val name = TextView(this)  
    name.text = entry.name  
    name.setTextColor(android.graphics.Color.parseColor("#66FF8A"))  
    name.textSize = 13f  
    val nameParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)  
    name.layoutParams = nameParams  

    row.addView(icon)  
    row.addView(name)  

    if (!isDir) {  
        val lockBtn = TextView(this)  
        lockBtn.text = if (entry.name.endsWith(".locked")) "🔓" else "🔒"  
        lockBtn.textSize = 14f  
        lockBtn.setPadding(dp(8), 0, dp(4), 0)  
        lockBtn.setOnClickListener {  
            if (entry.name.endsWith(".locked")) {  
                showDecryptFileDialog(entry)  
            } else {  
                showEncryptFileDialog(entry)  
            }  
        }  
        row.addView(lockBtn)  
    }  

    val delete = TextView(this)  
    delete.text = "✕"  
    delete.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))  
    delete.textSize = 14f  
    delete.setPadding(dp(12), 0, dp(4), 0)  
    delete.setOnClickListener { confirmDeleteEntry(entry) }  
    row.addView(delete)  

    row.setOnClickListener {  
        if (isDir) {  
            currentDir = entry  
            refreshFileList()  
        } else if (!entry.name.endsWith(".locked")) {  
            openFileInEditor(entry)  
        } else {  
            Toast.makeText(this, "الملف مشفّر، اضغط 🔓 لفك التشفير أولاً", Toast.LENGTH_SHORT).show()  
        }  
    }  

    return row  
}  

private fun confirmDeleteEntry(entry: File) {  
    AlertDialog.Builder(this)  
        .setTitle("حذف")  
        .setMessage("هل تريد حذف \"${entry.name}\"؟ (سيُحذف بشكل آمن)")  
        .setPositiveButton("حذف") { _: DialogInterface, _: Int ->  
            if (activeFile != null && activeFile?.absolutePath == entry.absolutePath) {  
                activeFile = null  
                saveActiveFilePath()  
                cyberEditor.setText("")  
                cyberEditor.isEnabled = false  
                cyberActiveFileLabel.text = "لا يوجد ملف مفتوح"  
            }  
            if (entry.isDirectory) {  
                secureWipeDirectory(entry)  
                entry.delete()  
            } else {  
                secureDeleteFile(entry)  
            }  
            refreshFileList()  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun navigateUpDirectory() {  
    if (currentDir.absolutePath != rootDir.absolutePath) {  
        val parent = currentDir.parentFile  
        if (parent != null) {  
            currentDir = parent  
            refreshFileList()  
        }  
    }  
}  

private fun showNewFileDialog() {  
    val input = EditText(this)  
    input.hint = "اسم الملف (مثال: main.py)"  
    AlertDialog.Builder(this)  
        .setTitle("ملف جديد")  
        .setView(input)  
        .setPositiveButton("إنشاء") { _: DialogInterface, _: Int ->  
            val name = input.text.toString().trim()  
            if (name.isNotEmpty()) {  
                val newFile = File(currentDir, name)  
                if (!newFile.exists()) {  
                    newFile.createNewFile()  
                    refreshFileList()  
                } else {  
                    Toast.makeText(this, "الملف موجود مسبقًا", Toast.LENGTH_SHORT).show()  
                }  
            }  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun showNewFolderDialog() {  
    val input = EditText(this)  
    input.hint = "اسم المجلد"  
    AlertDialog.Builder(this)  
        .setTitle("مجلد جديد")  
        .setView(input)  
        .setPositiveButton("إنشاء") { _: DialogInterface, _: Int ->  
            val name = input.text.toString().trim()  
            if (name.isNotEmpty()) {  
                val newFolder = File(currentDir, name)  
                if (!newFolder.exists()) {  
                    newFolder.mkdirs()  
                    refreshFileList()  
                } else {  
                    Toast.makeText(this, "المجلد موجود مسبقًا", Toast.LENGTH_SHORT).show()  
                }  
            }  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun openFileInEditor(file: File) {  
    saveActiveFileIfNeeded()  
    activeFile = file  
    saveActiveFilePath()  
    val content = try {  
        file.readText()  
    } catch (e: Exception) {  
        ""  
    }  
    cyberEditor.setText(content)  
    cyberEditor.isEnabled = true  
    cyberActiveFileLabel.text = "📄 ${file.name}"  
    cyberPanel.visibility = View.GONE  
    cyberOpenPanel = null  
}  

private fun saveActiveFileIfNeeded() {  
    val file = activeFile  
    if (file != null) {  
        try {  
            file.writeText(cyberEditor.text.toString())  
        } catch (e: Exception) {  
            // تجاهل بصمت لو فشل الحفظ  
        }  
    }  
}  

// ---------- التنقل بين الشاشتين ----------  

private fun showHomeScreen() {  
    homeScreen.visibility = View.VISIBLE  
    browserContainer.visibility = View.GONE  
    updateChromeVisibility()  
}  

private fun showBrowserScreen() {  
    homeScreen.visibility = View.GONE  
    browserContainer.visibility = View.VISIBLE  
    updateChromeVisibility()  
}  

private fun onBackButtonPressed() {  
    if (cyberContainer.visibility == View.VISIBLE) {  
        closeCyberMode()  
        return  
    }  
    val wv = currentWebView()  
    when {  
        customView != null -> wv?.webChromeClient?.onHideCustomView()  
        browserContainer.visibility == View.VISIBLE && wv != null && wv.canGoBack() -> wv.goBack()  
        browserContainer.visibility == View.VISIBLE -> showHomeScreen()  
        else -> { /* على الشاشة الرئيسية، ما نسوي شيء */ }  
    }  
}  

private fun openUrlOrSearch(input: String) {  
    val looksLikeUrl = input.contains(".") && !input.contains(" ")  
    val finalUrl = if (looksLikeUrl) {  
        if (!input.startsWith("http")) "https://$input" else input  
    } else {  
        "https://www.google.com/search?q=" + input.replace(" ", "+")  
    }  
    if (tabs.isEmpty()) {  
        createNewTab(finalUrl)  
    } else {  
        currentWebView()?.loadUrl(finalUrl)  
        switchToTab(currentTabIndex)  
    }  
    showBrowserScreen()  
}  

// ---------- التبويبات ----------  

private fun currentWebView(): WebView? {  
    return tabs.getOrNull(currentTabIndex)?.webView  
}  

@SuppressLint("SetJavaScriptEnabled")  
private fun createNewTab(url: String) {  
    if (tabs.size >= 8) {  
        Toast.makeText(this, "عندك تبويبات كثيرة، ممكن يتأثر الأداء", Toast.LENGTH_SHORT).show()  
    }  
    val webView = WebView(this)  
    configureWebView(webView)  
    val tab = Tab(webView)  
    tabs.add(tab)  
    currentTabIndex = tabs.size - 1  
    webView.loadUrl(url)  
    switchToTab(currentTabIndex)  
    updateTabsButton()  
    saveTabsState()  
}  

private fun switchToTab(index: Int) {  
    if (index !in tabs.indices) return  
    currentTabIndex = index  
    webViewContainer.removeAllViews()  
    webViewContainer.addView(  
        tabs[index].webView,  
        ViewGroup.LayoutParams.MATCH_PARENT,  
        ViewGroup.LayoutParams.MATCH_PARENT  
    )  
    val url = tabs[index].webView.url ?: ""  
    urlDisplay.text = url  
    updateUrlFavicon(url)  
    updateTabsButton()  
    saveTabsState()  
}  

private fun updateUrlFavicon(url: String) {  
    if (url.isBlank()) {  
        urlFavicon.visibility = View.GONE  
        return  
    }  
    urlFavicon.visibility = View.VISIBLE  
    loadFaviconInto(urlFavicon, url, "?")  
}  

private fun closeTab(index: Int) {  
    if (index !in tabs.indices) return  
    lastClosedTabUrl = tabs[index].webView.url  
    tabs[index].webView.destroy()  
    tabs.removeAt(index)  
    if (tabs.isEmpty()) {  
        currentTabIndex = -1  
        showHomeScreen()  
    } else {  
        currentTabIndex = (index - 1).coerceAtLeast(0)  
        switchToTab(currentTabIndex)  
    }  
    updateTabsButton()  
    saveTabsState()  
}  

private fun reopenLastClosedTab() {  
    val url = lastClosedTabUrl  
    if (url == null || url.isBlank()) {  
        Toast.makeText(this, "لا يوجد تبويب لإعادة فتحه", Toast.LENGTH_SHORT).show()  
        return  
    }  
    createNewTab(url)  
    lastClosedTabUrl = null  
}  

private fun updateTabsButton() {  
    tabsButton.text = tabs.size.toString()  
}  

private fun showTabsDialog() {  
    if (tabs.isEmpty()) {  
        Toast.makeText(this, "لا توجد تبويبات مفتوحة", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val listLayout = LinearLayout(this)  
    listLayout.orientation = LinearLayout.VERTICAL  

    val dialog = AlertDialog.Builder(this)  
        .setTitle("التبويبات المفتوحة")  
        .setView(buildTabsListView(listLayout))  
        .setNeutralButton("+ تبويب جديد") { _: DialogInterface, _: Int ->  
            createNewTab("https://www.google.com")  
            showBrowserScreen()  
        }  
        .setPositiveButton("إغلاق", null)  
        .create()  
    dialog.show()  
}  

private fun buildTabsListView(container: LinearLayout): ScrollView {  
    for (i in tabs.indices) {  
        val tab = tabs[i]  
        val row = LinearLayout(this)  
        row.orientation = LinearLayout.HORIZONTAL  
        row.gravity = android.view.Gravity.CENTER_VERTICAL  
        row.setPadding(dp(10), dp(10), dp(10), dp(10))  

        val icon = ImageView(this)  
        val iconParams = LinearLayout.LayoutParams(dp(24), dp(24))  
        iconParams.marginEnd = dp(10)  
        icon.layoutParams = iconParams  
        loadFaviconInto(icon, tab.webView.url ?: "", tab.title)  

        val label = TextView(this)  
        label.text = tab.title.ifBlank { "تبويب ${i + 1}" }  
        label.textSize = 14f  
        val labelParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)  
        label.layoutParams = labelParams  

        row.addView(icon)  
        row.addView(label)  

        row.setOnClickListener {  
            switchToTab(i)  
            showBrowserScreen()  
        }  

        container.addView(row)  
    }  
    val scroll = ScrollView(this)  
    scroll.addView(container)  
    return scroll  
}  

// ---------- إعداد WebView ----------  

@SuppressLint("SetJavaScriptEnabled")  
private fun configureWebView(webView: WebView) {  
    val settings: WebSettings = webView.settings  
    settings.javaScriptEnabled = true  
    settings.domStorageEnabled = true  
    settings.databaseEnabled = true  
    settings.mediaPlaybackRequiresUserGesture = false  
    settings.setSupportZoom(true)  
    settings.builtInZoomControls = true  
    settings.displayZoomControls = false  
    settings.loadWithOverviewMode = true  
    settings.useWideViewPort = true  
    settings.safeBrowsingEnabled = true  
    settings.javaScriptCanOpenWindowsAutomatically = false  
    settings.setSupportMultipleWindows(false)  
    settings.allowFileAccess = false  
    settings.allowContentAccess = false  
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW  

    val savedZoom = getSharedPreferences("prefs_general", MODE_PRIVATE).getInt("text_zoom", 100)  
    settings.textZoom = savedZoom  

    CookieManager.getInstance().setAcceptCookie(true)  
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)  

    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-A265F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"  

    webView.setDownloadListener(  
        object : android.webkit.DownloadListener {  
            override fun onDownloadStart(  
                url: String,  
                userAgent: String,  
                contentDisposition: String,  
                mimeType: String,  
                contentLength: Long  
            ) {  
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)  
                val dangerousExtensions = setOf(".apk", ".exe", ".bat", ".sh", ".dex")  
                val isDangerous = dangerousExtensions.any { fileName.lowercase().endsWith(it) }  

                if (isDangerous && adBlockEnabled) {  
                    AlertDialog.Builder(this@MainActivity)  
                        .setTitle("⚠️ تحذير أمني")  
                        .setMessage("هذا الملف قد يكون خطيرًا (${fileName}). تأكد إن المصدر موثوق قبل المتابعة.")  
                        .setPositiveButton("تحميل على مسؤوليتي") { _: DialogInterface, _: Int ->  
                            startDownload(url, userAgent, contentDisposition, mimeType, fileName)  
                        }  
                        .setNegativeButton("إلغاء", null)  
                        .show()  
                } else {  
                    startDownload(url, userAgent, contentDisposition, mimeType, fileName)  
                }  
            }  
        }  
    )  

    webView.webViewClient = object : WebViewClient() {  
        override fun shouldOverrideUrlLoading(  
            view: WebView?,  
            request: WebResourceRequest?  
        ): Boolean {  
            val host = request?.url?.host ?: return false  
            if (manualBlacklist.contains(host)) {  
                view?.loadData(  
                    "<html><body style='background:#0B0714;color:#FF6B6B;font-family:sans-serif;padding:40px;text-align:center;'><h2>⛔ هذا الموقع محظور</h2></body></html>",  
                    "text/html", "UTF-8"  
                )  
                return true  
            }  
            return false  
        }  

        override fun shouldInterceptRequest(  
            view: WebView?,  
            request: WebResourceRequest?  
        ): WebResourceResponse? {  
            if (!adBlockEnabled) return super.shouldInterceptRequest(view, request)  
            val host = request?.url?.host ?: return super.shouldInterceptRequest(view, request)  
            var isBlocked = false  
            for (blocked in blockedHosts) {  
                if (host.endsWith(blocked)) {  
                    isBlocked = true  
                    break  
                }  
            }  
            if (isBlocked) {  
                incrementBlockedCount()  
                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))  
            }  
            return super.shouldInterceptRequest(view, request)  
        }  

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {  
            super.onPageStarted(view, url, favicon)  
            if (view == currentWebView()) {  
                progressBar.visibility = View.VISIBLE  
                pageBlockedCount = 0  
            }  
            if (url != null) {  
                val uri = Uri.parse(url)  
                if (uri.scheme == "http") {  
                    Toast.makeText(this@MainActivity, "⚠️ هذا الموقع غير آمن (HTTP)", Toast.LENGTH_SHORT).show()  
                }  
                val host = uri.host ?: ""  
                view?.settings?.javaScriptEnabled = !jsDisabledDomains.contains(host)  
            }  
        }  

        override fun onPageFinished(view: WebView?, url: String?) {  
            super.onPageFinished(view, url)  
            if (view == currentWebView()) {  
                progressBar.visibility = View.GONE  
                swipeRefresh.isRefreshing = false  
                urlDisplay.text = url ?: ""  
                updateUrlFavicon(url ?: "")  
            }  
            saveTabsState()  
        }  

        override fun onReceivedSslError(  
            view: WebView?,  
            handler: SslErrorHandler?,  
            error: android.net.http.SslError?  
        ) {  
            handler?.cancel()  
            Toast.makeText(this@MainActivity, "⛔ شهادة الموقع غير موثوقة، تم الإيقاف", Toast.LENGTH_LONG).show()  
        }  
    }  

    webView.webChromeClient = object : WebChromeClient() {  
        override fun onProgressChanged(view: WebView?, newProgress: Int) {  
            if (view == currentWebView()) progressBar.progress = newProgress  
        }  

        override fun onReceivedTitle(view: WebView?, title: String?) {  
            val tab = tabs.find { it.webView == view }  
            if (tab != null && !title.isNullOrBlank()) {  
                tab.title = title  
            }  
        }  

        override fun onJsAlert(  
            view: WebView?,  
            url: String?,  
            message: String?,  
            result: JsResult?  
        ): Boolean {  
            result?.cancel()  
            return true  
        }  

        override fun onPermissionRequest(request: PermissionRequest?) {  
            if (request == null) return  
            val host = Uri.parse(request.origin.toString()).host ?: request.origin.toString()  
            requestSitePermission(host, "كاميرا/ميكروفون") { allowed: Boolean ->  
                if (allowed) {  
                    request.grant(request.resources)  
                } else {  
                    request.deny()  
                }  
            }  
        }  

        override fun onGeolocationPermissionsShowPrompt(  
            origin: String?,  
            callback: GeolocationPermissions.Callback?  
        ) {  
            if (origin == null || callback == null) return  
            val host = Uri.parse(origin).host ?: origin  
            requestSitePermission(host, "الموقع الجغرافي") { allowed: Boolean ->  
                callback.invoke(origin, allowed, false)  
            }  
        }  

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {  
            if (customView != null) {  
                callback?.onCustomViewHidden()  
                return  
            }  
            customView = view  
            customViewCallback = callback  
            fullscreenContainer.addView(  
                view,  
                ViewGroup.LayoutParams.MATCH_PARENT,  
                ViewGroup.LayoutParams.MATCH_PARENT  
            )  
            fullscreenContainer.visibility = View.VISIBLE  
            browserContainer.visibility = View.GONE  
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)  
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE  
            updateChromeVisibility()  
        }  

        override fun onHideCustomView() {  
            fullscreenContainer.visibility = View.GONE  
            fullscreenContainer.removeView(customView)  
            customView = null  
            customViewCallback?.onCustomViewHidden()  
            browserContainer.visibility = View.VISIBLE  
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)  
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED  
            updateChromeVisibility()  
        }  
    }  

    webView.setFindListener(object : FindListener {  
        override fun onFindResultReceived(  
            activeMatchOrdinal: Int,  
            numberOfMatches: Int,  
            isDoneCounting: Boolean  
        ) {  
            // النتائج تظهر تلقائيًا بتظليل WebView  
        }  
    })  
}  

private fun incrementBlockedCount() {  
    pageBlockedCount++  
    val prefs = getSharedPreferences("stats_prefs", MODE_PRIVATE)  
    val current = prefs.getInt("blocked_count", 0)  
    prefs.edit().putInt("blocked_count", current + 1).apply()  
}  

private fun showPrivacyStats() {  
    val prefs = getSharedPreferences("stats_prefs", MODE_PRIVATE)  
    val count = prefs.getInt("blocked_count", 0)  
    AlertDialog.Builder(this)  
        .setTitle("📊 إحصائيات الحماية")  
        .setMessage("تم حظر $count طلب إعلان أو تتبع منذ تثبيت التطبيق.\n\nبهذه الصفحة تحديدًا: $pageBlockedCount محاولة تتبع.")  
        .setPositiveButton("إغلاق", null)  
        .show()  
}  

private fun startDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String, fileName: String) {  
    try {  
        val request = DownloadManager.Request(Uri.parse(url))  
        request.setMimeType(mimeType)  
        request.addRequestHeader("User-Agent", userAgent)  
        request.setTitle(fileName)  
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)  
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)  
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager  
        dm.enqueue(request)  
        Toast.makeText(this, "بدأ تحميل: $fileName", Toast.LENGTH_SHORT).show()  
    } catch (e: Exception) {  
        Toast.makeText(this, "فشل التحميل", Toast.LENGTH_SHORT).show()  
    }  
}  

// ---------- بحث داخل الصفحة ----------  

private fun showFindInPageDialog() {  
    val webView = currentWebView()  
    if (webView == null) {  
        Toast.makeText(this, "افتح صفحة أولاً", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val input = EditText(this)  
    input.hint = "كلمة البحث"  

    val dialog = AlertDialog.Builder(this)  
        .setTitle("🔍 بحث بالصفحة")  
        .setView(input)  
        .setPositiveButton("بحث") { _: DialogInterface, _: Int ->  
            val query = input.text.toString()  
            if (query.isNotBlank()) {  
                webView.findAllAsync(query)  
            }  
        }  
        .setNeutralButton("التالي") { _: DialogInterface, _: Int ->  
            webView.findNext(true)  
        }  
        .setNegativeButton("إغلاق") { _: DialogInterface, _: Int ->  
            webView.clearMatches()  
        }  
        .create()  
    dialog.show()  
}  

// ---------- وضع القراءة الأساسي ----------  

private fun activateReaderMode() {  
    val webView = currentWebView() ?: return  
    val js = """  
        (function() {  
            var article = document.querySelector('article') || document.querySelector('main') || document.body;  
            var text = article.innerText;  
            document.body.innerHTML = '<div style="padding:20px;font-size:18px;line-height:1.6;white-space:pre-wrap;">' + text + '</div>';  
        })();  
    """.trimIndent()  
    webView.evaluateJavascript(js, null)  
    Toast.makeText(this, "تم تفعيل وضع القراءة الأساسي", Toast.LENGTH_SHORT).show()  
}  

// ---------- القائمة ----------  

private fun showMainMenu(anchor: View) {  
    val popup = PopupMenu(this, anchor)  
    popup.menu.add(0, 1, 0, "📌 إضافة الصفحة الحالية للاختصارات")  
    popup.menu.add(0, 2, 0, if (adBlockEnabled) "🚫 حظر الإعلانات: مفعّل" else "🚫 حظر الإعلانات: متوقف")  
    popup.menu.add(0, 3, 0, if (darkModeEnabled) "🌙 الوضع الليلي: مفعّل" else "☀️ الوضع الليلي: متوقف")  
    popup.menu.add(0, 4, 0, "🔗 نسخ الرابط")  
    popup.menu.add(0, 5, 0, "📤 مشاركة الرابط")  
    popup.menu.add(0, 6, 0, "🔍 بحث بالصفحة")  
    popup.menu.add(0, 7, 0, "🔤 تكبير الخط")  
    popup.menu.add(0, 8, 0, "🔡 تصغير الخط")  
    popup.menu.add(0, 9, 0, "📖 وضع القراءة")  
    popup.menu.add(0, 10, 0, "↩️ إعادة فتح آخر تبويب مسكرته")  
    popup.menu.add(0, 11, 0, "📊 إحصائيات الحماية")  
    popup.menu.add(0, 12, 0, "🚫 حظر هذا الموقع نهائيًا")  
    popup.menu.add(0, 13, 0, "📋 عرض المواقع المحظورة")  
    popup.menu.add(0, 14, 0, "🔧 تبديل حالة JS لهذا الموقع")  
    popup.menu.add(0, 15, 0, "🕵️ سجل طلبات الصلاحيات")  
    popup.menu.add(0, 16, 0, if (paranoiaModeOn) "😱 إيقاف وضع بارانويا" else "😱 تفعيل وضع بارانويا")  
    popup.menu.add(0, 17, 0, "🎭 وضع التمويه (تغيير الأيقونة)")  
    popup.menu.add(0, 18, 0, "🆘 محو شامل فوري (طوارئ قصوى)")  
    popup.menu.add(0, 19, 0, "🚨 مسح فوري وإغلاق")  
    popup.setOnMenuItemClickListener { item: android.view.MenuItem ->  
        when (item.itemId) {  
            1 -> addCurrentPageAsShortcut()  
            2 -> {  
                adBlockEnabled = !adBlockEnabled  
                val msg = if (adBlockEnabled) "تم تفعيل حظر الإعلانات" else "تم إيقاف حظر الإعلانات"  
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()  
            }  
            3 -> {  
                darkModeEnabled = !darkModeEnabled  
                val msg = if (darkModeEnabled) "تم تفعيل الوضع الليلي" else "تم إيقاف الوضع الليلي"  
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()  
            }  
            4 -> copyCurrentLink()  
            5 -> shareCurrentLink()  
            6 -> showFindInPageDialog()  
            7 -> adjustTextZoom(10)  
            8 -> adjustTextZoom(-10)  
            9 -> activateReaderMode()  
            10 -> reopenLastClosedTab()  
            11 -> showPrivacyStats()  
            12 -> blockCurrentSitePermanently()  
            13 -> showBlacklistDialog()  
            14 -> toggleJsForCurrentSite()  
            15 -> showPermissionLogDialog()  
            16 -> toggleParanoiaMode()  
            17 -> showDisguiseConfirmation()  
            18 -> {  
                AlertDialog.Builder(this)  
                    .setTitle("🆘 محو شامل فوري")  
                    .setMessage("هذا يمسح كل شي نهائيًا: ملفاتك، أكوادك، إعداداتك، بدون رجعة. متأكد؟")  
                    .setPositiveButton("امسح كل شي") { _: DialogInterface, _: Int -> panicWipeEverything() }  
                    .setNegativeButton("إلغاء", null)  
                    .show()  
            }  
            19 -> emergencyWipeAndClose()  
        }  
        true  
    }  
    popup.show()  
}  

private fun copyCurrentLink() {  
    val url = currentWebView()?.url  
    if (url.isNullOrBlank()) {  
        Toast.makeText(this, "لا يوجد رابط حاليًا", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager  
    clipboard.setPrimaryClip(ClipData.newPlainText("link", url))  
    Toast.makeText(this, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()  
}  

private fun shareCurrentLink() {  
    val url = currentWebView()?.url  
    if (url.isNullOrBlank()) {  
        Toast.makeText(this, "لا يوجد رابط حاليًا", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val intent = Intent(Intent.ACTION_SEND)  
    intent.type = "text/plain"  
    intent.putExtra(Intent.EXTRA_TEXT, url)  
    startActivity(Intent.createChooser(intent, "مشاركة الرابط"))  
}  

private fun adjustTextZoom(delta: Int) {  
    val webView = currentWebView()  
    if (webView == null) {  
        Toast.makeText(this, "افتح صفحة أولاً", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val newZoom = (webView.settings.textZoom + delta).coerceIn(50, 200)  
    webView.settings.textZoom = newZoom  
    getSharedPreferences("prefs_general", MODE_PRIVATE).edit().putInt("text_zoom", newZoom).apply()  
    Toast.makeText(this, "حجم الخط: $newZoom%", Toast.LENGTH_SHORT).show()  
}  

// ---------- الاختصارات (Speed Dial) ----------  

private fun loadShortcuts() {  
    val prefs = getSharedPreferences("shortcuts_prefs", MODE_PRIVATE)  
    val raw = prefs.getString("list", "") ?: ""  
    shortcuts.clear()  
    if (raw.isNotBlank()) {  
        val entries = raw.split("\u0002")  
        for (entry in entries) {  
            val parts = entry.split("\u0001")  
            if (parts.size == 2) shortcuts.add(Shortcut(parts[0], parts[1]))  
        }  
    }  
    if (shortcuts.isEmpty()) {  
        shortcuts.add(Shortcut("Google", "https://www.google.com"))  
    }  
}  

private fun saveShortcuts() {  
    val prefs = getSharedPreferences("shortcuts_prefs", MODE_PRIVATE)  
    val sb = StringBuilder()  
    for (i in shortcuts.indices) {  
        if (i > 0) sb.append("\u0002")  
        sb.append(shortcuts[i].title)  
        sb.append("\u0001")  
        sb.append(shortcuts[i].url)  
    }  
    prefs.edit().putString("list", sb.toString()).apply()  
}  

private fun dp(value: Int): Int {  
    return TypedValue.applyDimension(  
        TypedValue.COMPLEX_UNIT_DIP,  
        value.toFloat(),  
        resources.displayMetrics  
    ).toInt()  
}  

private fun rebuildShortcutsGrid() {  
    gridShortcuts.removeAllViews()  

    for (i in shortcuts.indices) {  
        val tile = createShortcutTile(i, shortcuts[i])  
        val lp = GridLayout.LayoutParams()  
        lp.width = dp(72)  
        lp.height = dp(90)  
        lp.setMargins(dp(8), dp(8), dp(8), dp(8))  
        gridShortcuts.addView(tile, lp)  
    }  

    val addTile = createAddTile()  
    val addLp = GridLayout.LayoutParams()  
    addLp.width = dp(72)  
    addLp.height = dp(90)  
    addLp.setMargins(dp(8), dp(8), dp(8), dp(8))  
    gridShortcuts.addView(addTile, addLp)  

    gridShortcuts.setOnDragListener(object : View.OnDragListener {  
        override fun onDrag(v: View?, event: DragEvent): Boolean {  
            when (event.action) {  
                DragEvent.ACTION_DRAG_STARTED -> return true  
                DragEvent.ACTION_DROP -> {  
                    val clipData: ClipData? = event.clipData  
                    if (clipData == null || clipData.itemCount == 0) return true  
                    val sourceIndexText = clipData.getItemAt(0).text  
                    if (sourceIndexText == null) return true  
                    val sourceIndex = sourceIndexText.toString().toIntOrNull() ?: return true  

                    var targetIndex = -1  
                    for (childIndex in 0 until gridShortcuts.childCount) {  
                        val child = gridShortcuts.getChildAt(childIndex)  
                        val tag = child.tag  
                        if (tag is Int && tag != sourceIndex) {  
                            if (event.x >= child.left && event.x <= child.right &&  
                                event.y >= child.top && event.y <= child.bottom  
                            ) {  
                                targetIndex = tag  
                                break  
                            }  
                        }  
                    }  

                    if (targetIndex != -1 && sourceIndex in shortcuts.indices && targetIndex in shortcuts.indices) {  
                        val moved = shortcuts.removeAt(sourceIndex)  
                        shortcuts.add(targetIndex, moved)  
                        saveShortcuts()  
                        rebuildShortcutsGrid()  
                    }  
                    return true  
                }  
                else -> return true  
            }  
        }  
    })  
}  

private fun createShortcutTile(index: Int, shortcut: Shortcut): View {  
    val container = FrameLayout(this)  
    container.tag = index  
    container.setBackgroundResource(R.drawable.bg_tile_v2)  

    val inner = LinearLayout(this)  
    inner.orientation = LinearLayout.VERTICAL  
    inner.gravity = android.view.Gravity.CENTER  

    val icon = ImageView(this)  
    val iconParams = LinearLayout.LayoutParams(dp(32), dp(32))  
    icon.layoutParams = iconParams  
    loadFaviconInto(icon, shortcut.url, shortcut.title)  

    val label = TextView(this)  
    label.text = shortcut.title.take(10)  
    label.textSize = 10f  
    label.setTextColor(android.graphics.Color.WHITE)  
    label.gravity = android.view.Gravity.CENTER  
    label.setPadding(0, dp(6), 0, 0)  

    inner.addView(icon)  
    inner.addView(label)  

    val removeBtn = TextView(this)  
    removeBtn.text = "✕"  
    removeBtn.textSize = 12f  
    removeBtn.setTextColor(android.graphics.Color.WHITE)  
    val removeParams = FrameLayout.LayoutParams(  
        FrameLayout.LayoutParams.WRAP_CONTENT,  
        FrameLayout.LayoutParams.WRAP_CONTENT  
    )  
    removeParams.gravity = android.view.Gravity.TOP or android.view.Gravity.END  
    removeBtn.layoutParams = removeParams  
    removeBtn.setPadding(dp(4), dp(2), dp(4), dp(2))  
    removeBtn.setOnClickListener {  
        if (index in shortcuts.indices) {  
            shortcuts.removeAt(index)  
            saveShortcuts()  
            rebuildShortcutsGrid()  
        }  
    }  

    container.addView(  
        inner,  
        FrameLayout.LayoutParams(  
            FrameLayout.LayoutParams.MATCH_PARENT,  
            FrameLayout.LayoutParams.MATCH_PARENT  
        )  
    )  
    container.addView(removeBtn)  

    container.setOnClickListener {  
        openUrlOrSearch(shortcut.url)  
    }  

    container.setOnLongClickListener { view: View ->  
        val clipData = ClipData.newPlainText("index", index.toString())  
        val shadow = View.DragShadowBuilder(view)  
        view.startDragAndDrop(clipData, shadow, null, 0)  
        true  
    }  

    return container  
}  

private fun createAddTile(): View {  
    val container = FrameLayout(this)  
    container.setBackgroundResource(R.drawable.bg_tile_v2)  

    val label = TextView(this)  
    label.text = "+"  
    label.textSize = 28f  
    label.setTextColor(android.graphics.Color.WHITE)  
    label.gravity = android.view.Gravity.CENTER  

    container.addView(  
        label,  
        FrameLayout.LayoutParams(  
            FrameLayout.LayoutParams.MATCH_PARENT,  
            FrameLayout.LayoutParams.MATCH_PARENT  
        )  
    )  

    container.setOnClickListener { showAddShortcutDialog() }  
    return container  
}  

private fun showAddShortcutDialog() {  
    val layout = LinearLayout(this)  
    layout.orientation = LinearLayout.VERTICAL  
    layout.setPadding(dp(20), dp(10), dp(20), dp(10))  

    val titleInput = EditText(this)  
    titleInput.hint = "اسم الاختصار"  

    val urlInput = EditText(this)  
    urlInput.hint = "الرابط (مثال: example.com)"  

    layout.addView(titleInput)  
    layout.addView(urlInput)  

    AlertDialog.Builder(this)  
        .setTitle("إضافة اختصار جديد")  
        .setView(layout)  
        .setPositiveButton("إضافة") { _: DialogInterface, _: Int ->  
            val title = titleInput.text.toString().trim()  
            var url = urlInput.text.toString().trim()  
            if (title.isEmpty() || url.isEmpty()) {  
                Toast.makeText(this, "الرجاء تعبئة الحقلين", Toast.LENGTH_SHORT).show()  
                return@setPositiveButton  
            }  
            if (!url.startsWith("http")) url = "https://$url"  
            shortcuts.add(Shortcut(title, url))  
            saveShortcuts()  
            rebuildShortcutsGrid()  
        }  
        .setNegativeButton("إلغاء", null)  
        .show()  
}  

private fun addCurrentPageAsShortcut() {  
    val webView = currentWebView()  
    if (webView == null) {  
        Toast.makeText(this, "لا يوجد تبويب مفتوح", Toast.LENGTH_SHORT).show()  
        return  
    }  
    val url = webView.url ?: return  
    val title = tabs.getOrNull(currentTabIndex)?.title?.ifBlank { url } ?: url  
    shortcuts.add(Shortcut(title.take(15), url))  
    saveShortcuts()  
    rebuildShortcutsGrid()  
    Toast.makeText(this, "تمت الإضافة للاختصارات", Toast.LENGTH_SHORT).show()  
}  

// ---------- المسح ----------  

private fun wipeEverything() {  
    for (tab in tabs) {  
        tab.webView.clearHistory()  
        tab.webView.clearCache(true)  
        tab.webView.clearFormData()  
    }  
    CookieManager.getInstance().removeAllCookies(null)  
    CookieManager.getInstance().flush()  
    WebStorage.getInstance().deleteAllData()  
}  

// ---------- إدارة ظهور الأزرار العائمة ----------  

private fun updateChromeVisibility() {  
    val shouldHide = isLocked || customView != null || cyberContainer.visibility == View.VISIBLE  
    val visibility = if (shouldHide) View.GONE else View.VISIBLE  
    bottomBar.visibility = visibility  
    cyberFab.visibility = visibility  
}  

override fun onDestroy() {  
    saveTabsState()  
    saveActiveFileIfNeeded()  
    wipeEverything()  
    for (tab in tabs) tab.webView.destroy()  
    super.onDestroy()  
}  

@Deprecated("Deprecated in Java")  
override fun onBackPressed() {  
    onBackButtonPressed()  
}

}
