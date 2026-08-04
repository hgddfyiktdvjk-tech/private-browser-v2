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
import android.util.TypedValue
import android.view.DragEvent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream

data class Tab(val webView: WebView, var title: String = "تبويب جديد")
data class Shortcut(val title: String, val url: String)

class MainActivity : AppCompatActivity() {

    private lateinit var cyberContainer: FrameLayout
    private lateinit var cyberFab: ImageButton
    private lateinit var cyberPanel: FrameLayout
    private lateinit var cyberPanelTitle: TextView
    private lateinit var cyberPanelContent: TextView
    private var cyberOpenPanel: String? = null
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

        val backButton: ImageButton = findViewById(R.id.backButton)
        val homeButton: ImageButton = findViewById(R.id.homeButton)
        val menuButton: ImageButton = findViewById(R.id.menuButton)
        val menuButtonTop: ImageButton = findViewById(R.id.menuButtonTop)
        val downloadsButton: ImageButton = findViewById(R.id.downloadsButton)
        val unlockButton: Button = findViewById(R.id.unlockButton)
        cyberContainer = findViewById(R.id.cyberContainer)
        cyberFab = findViewById(R.id.cyberFab)
        cyberPanel = findViewById(R.id.cyberPanel)
        cyberPanelTitle = findViewById(R.id.cyberPanelTitle)
        cyberPanelContent = findViewById(R.id.cyberPanelContent)

        val cyberCloseButton: ImageButton = findViewById(R.id.cyberCloseButton)
        val cyberFilesButton: Button = findViewById(R.id.cyberFilesButton)
        val cyberTerminalButton: Button = findViewById(R.id.cyberTerminalButton)
        val cyberPackagesButton: Button = findViewById(R.id.cyberPackagesButton)
        val cyberSnippetsButton: Button = findViewById(R.id.cyberSnippetsButton)

        cyberFab.setOnClickListener { openCyberMode() }
        cyberCloseButton.setOnClickListener { closeCyberMode() }
        cyberFilesButton.setOnClickListener { toggleCyberPanel("files", "📁 مدير الملفات") }
        cyberTerminalButton.setOnClickListener { toggleCyberPanel("terminal", "⌨ الترمينال") }
        cyberPackagesButton.setOnClickListener { toggleCyberPanel("packages", "📦 المكتبات") }
        cyberSnippetsButton.setOnClickListener { toggleCyberPanel("snippets", "📚 مكتبة الأكواد") }
        
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

        swipeRefresh.setOnRefreshListener { currentWebView()?.reload() }

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
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isLocked = false
                lockOverlay.visibility = View.GONE
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
        isLocked = true
        finishAffinity()
    }

    // ---------- الوضع السيبراني ----------

    private fun openCyberMode() {
        cyberContainer.visibility = View.VISIBLE
    }

    private fun closeCyberMode() {
        cyberContainer.visibility = View.GONE
        cyberPanel.visibility = View.GONE
        cyberOpenPanel = null
    }

    private fun toggleCyberPanel(panelKey: String, title: String) {
        if (cyberOpenPanel == panelKey) {
            cyberPanel.visibility = View.GONE
            cyberOpenPanel = null
        } else {
            cyberPanelTitle.text = title
            cyberPanelContent.text = "قريبًا..."
            cyberPanel.visibility = View.VISIBLE
            cyberOpenPanel = panelKey
        }
    }
    
    // ---------- التنقل بين الشاشتين ----------

    private fun showHomeScreen() {
        homeScreen.visibility = View.VISIBLE
        browserContainer.visibility = View.GONE
    }

    private fun showBrowserScreen() {
        homeScreen.visibility = View.GONE
        browserContainer.visibility = View.VISIBLE
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
        val webView = WebView(this)
        configureWebView(webView)
        val tab = Tab(webView)
        tabs.add(tab)
        currentTabIndex = tabs.size - 1
        webView.loadUrl(url)
        switchToTab(currentTabIndex)
        updateTabsButton()
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

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

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
                    try {
                        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                        val request = DownloadManager.Request(Uri.parse(url))
                        request.setMimeType(mimeType)
                        request.addRequestHeader("User-Agent", userAgent)
                        request.setTitle(fileName)
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                        dm.enqueue(request)
                        Toast.makeText(this@MainActivity, "بدأ تحميل: $fileName", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "فشل التحميل", Toast.LENGTH_SHORT).show()
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
            }

            override fun onHideCustomView() {
                fullscreenContainer.visibility = View.GONE
                fullscreenContainer.removeView(customView)
                customView = null
                customViewCallback?.onCustomViewHidden()
                browserContainer.visibility = View.VISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
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
        container.setBackgroundResource(R.drawable.bg_tile)

        val inner = LinearLayout(this)
        inner.orientation = LinearLayout.VERTICAL
        inner.gravity = android.view.Gravity.CENTER

        val icon = TextView(this)
        icon.text = shortcut.title.take(1).uppercase()
        icon.textSize = 22f
        icon.setTextColor(android.graphics.Color.WHITE)
        icon.gravity = android.view.Gravity.CENTER

        val label = TextView(this)
        label.text = shortcut.title.take(10)
        label.textSize = 10f
        label.setTextColor(android.graphics.Color.WHITE)
        label.gravity = android.view.Gravity.CENTER
        label.setPadding(0, dp(4), 0, 0)

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
        container.setBackgroundResource(R.drawable.bg_tile)

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
        wipeEverything()
        for (tab in tabs) tab.webView.destroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        onBackButtonPressed()
    }
}
