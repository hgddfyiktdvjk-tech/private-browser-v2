package com.privacy.browser

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.DragEvent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL

data class Tab(val webView: WebView, var title: String = "تبويب جديد")
data class Shortcut(val title: String, val url: String)
data class Snippet(val title: String, val code: String)

class MainActivity : AppCompatActivity() {

    private lateinit var homeScreen: FrameLayout
    private lateinit var browserContainer: FrameLayout
    private lateinit var webViewContainer: FrameLayout
    private lateinit var homeSearchBar: EditText
    private lateinit var urlDisplay: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var lockOverlay: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var gridShortcuts: GridLayout
    private lateinit var tabsButton: Button
    private lateinit var bottomBar: LinearLayout

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

    private lateinit var rootDir: File
    private lateinit var currentDir: File
    private var activeFile: File? = null

    private val installedPackages = mutableListOf<String>()
    private val snippets = mutableListOf<Snippet>()

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var isLocked = true
    private var adBlockEnabled = true
    private var darkModeEnabled = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        homeScreen = findViewById(R.id.homeScreen)
        browserContainer = findViewById(R.id.browserContainer)
        webViewContainer = findViewById(R.id.webViewContainer)
        homeSearchBar = findViewById(R.id.homeSearchBar)
        urlDisplay = findViewById(R.id.urlDisplay)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        lockOverlay = findViewById(R.id.lockOverlay)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        gridShortcuts = findViewById(R.id.gridShortcuts)
        tabsButton = findViewById(R.id.tabsButton)
        bottomBar = findViewById(R.id.bottomBar)

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

        setupPyodideWebView()
        loadInstalledPackages()
        loadSnippets()
        restoreActiveFileIfAny()
        restoreTabsState()

        loadShortcuts()
        rebuildShortcutsGrid()

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
                    openUrlOrSearch(query)
                }
                true
            } else {
                false
            }
        }

        cyberFab.setOnClickListener { openCyberMode() }
        cyberCloseButton.setOnClickListener { closeCyberMode() }
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
    }

    // ---------- إدارة ظهور الأزرار العائمة (الإصلاح الجديد) ----------

    private fun updateChromeVisibility() {
        val shouldHide = isLocked || customView != null || cyberContainer.visibility == View.VISIBLE
        val visibility = if (shouldHide) View.GONE else View.VISIBLE
        bottomBar.visibility = visibility
        cyberFab.visibility = visibility
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

    private inner class PyBridge {
        @JavascriptInterface
        fun onPyodideReady() {
            runOnUiThread {
                pyodideReady = true
                appendTerminal("✅ بايثون جاهز\n", "#00FF41")
            }
        }

        @JavascriptInterface
        fun onPyodideError(message: String) {
            runOnUiThread {
                appendTerminal("❌ فشل تجهيز بايثون: $message\n", "#FF6B6B")
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
                appendTerminal("\n✅ انتهى التنفيذ\n", "#00FF41")
            }
        }

        @JavascriptInterface
        fun onPythonError(message: String) {
            runOnUiThread {
                appendTerminal("\n❌ خطأ: $message\n", "#FF6B6B")
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
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isLocked = false
                lockOverlay.visibility = View.GONE
                updateChromeVisibility()
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

        val delete = TextView(this)
        delete.text = "✕"
        delete.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
        delete.textSize = 14f
        delete.setPadding(dp(12), 0, dp(4), 0)
        delete.setOnClickListener { confirmDeleteEntry(entry) }

        row.addView(icon)
        row.addView(name)
        row.addView(delete)

        row.setOnClickListener {
            if (isDir) {
                currentDir = entry
                refreshFileList()
            } else {
                openFileInEditor(entry)
            }
        }

        return row
    }

    private fun confirmDeleteEntry(entry: File) {
        AlertDialog.Builder(this)
            .setTitle("حذف")
            .setMessage("هل تريد حذف \"${entry.name}\"؟")
            .setPositiveButton("حذف") { _: DialogInterface, _: Int ->
                if (activeFile != null && activeFile?.absolutePath == entry.absolutePath) {
                    activeFile = null
                    saveActiveFilePath()
                    cyberEditor.setText("")
                    cyberEditor.isEnabled = false
                    cyberActiveFileLabel.text = "لا يوجد ملف مفتوح"
                }
                entry.deleteRecursively()
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
        urlDisplay.text = tabs[index].webView.url ?: ""
        updateTabsButton()
        saveTabsState()
    }

    private fun closeTab(index: Int) {
        if (index !in tabs.indices) return
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

    private fun updateTabsButton() {
        tabsButton.text = tabs.size.toString()
    }

    private fun showTabsDialog() {
        if (tabs.isEmpty()) {
            Toast.makeText(this, "لا توجد تبويبات مفتوحة", Toast.LENGTH_SHORT).show()
            return
        }
        val titles = arrayOfNulls<String>(tabs.size)
        for (i in tabs.indices) titles[i] = tabs[i].title.ifBlank { "تبويب ${i + 1}" }

        AlertDialog.Builder(this)
            .setTitle("التبويبات المفتوحة")
            .setItems(titles) { _: DialogInterface, which: Int ->
                switchToTab(which)
                showBrowserScreen()
            }
            .setNeutralButton("+ تبويب جديد") { _: DialogInterface, _: Int ->
                createNewTab("https://www.google.com")
            }
            .setPositiveButton("إغلاق", null)
            .show()
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

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-A265F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

        applyDarkMode(webView)

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
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (view == currentWebView()) progressBar.visibility = View.VISIBLE
                if (url != null) {
                    val uri = Uri.parse(url)
                    if (uri.scheme == "http") {
                        Toast.makeText(this@MainActivity, "⚠️ هذا الموقع غير آمن (HTTP)", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view == currentWebView()) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    urlDisplay.text = url ?: ""
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

    private fun applyDarkMode(webView: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            // WebSettingsCompat.setAlgorithmicDarkening(webView.settings, darkModeEnabled)
        }
    }

    // ---------- القائمة ----------

    private fun showMainMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "📌 إضافة الصفحة الحالية للاختصارات")
        popup.menu.add(0, 2, 0, if (adBlockEnabled) "🚫 حظر الإعلانات: مفعّل" else "🚫 حظر الإعلانات: متوقف")
        popup.menu.add(0, 3, 0, if (darkModeEnabled) "🌙 الوضع الليلي: مفعّل" else "☀️ الوضع الليلي: متوقف")
        popup.menu.add(0, 4, 0, "🚨 مسح فوري وإغلاق")
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
                    for (tab in tabs) applyDarkMode(tab.webView)
                    val msg = if (darkModeEnabled) "تم تفعيل الوضع الليلي" else "تم إيقاف الوضع الليلي"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
                4 -> emergencyWipeAndClose()
            }
            true
        }
        popup.show()
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
