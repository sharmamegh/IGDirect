package dev.mnsharma.igdirect

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var webViewBundle: Bundle? = null
    private lateinit var webView: WebView

    var filePathCallback: ValueCallback<Array<Uri>>? = null
    val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            filePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webViewScreen(
            webView = webView,
            saveWebViewState = { state -> webViewBundle = state },
            restoreWebViewState = { webViewBundle },
            this
        )

        checkPermissions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dispose of the WebView to avoid potential memory leaks
        webView.destroy()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission was denied
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun webViewScreen(webView: WebView, saveWebViewState: (Bundle) -> Unit, restoreWebViewState: () -> Bundle?, activity: MainActivity) {
    WebView.setWebContentsDebuggingEnabled(true)

    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        cacheMode = WebSettings.LOAD_DEFAULT
        loadsImagesAutomatically = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }

    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }

    val allowedUrls = setOf(
        "https://www.instagram.com/accounts/login/?next=https://www.instagram.com/direct/?__coig_login=1",
        "https://www.instagram.com/accounts/login/?next=https%3A%2F%2Fwww.instagram.com%2Fdirect%2F%3F__coig_login%3D1",
        "https://www.instagram.com/accounts/onetap/?next=https%3A%2F%2Fwww.instagram.com%2Fdirect%2F%3F__coig_login%3D1",
        "https://www.instagram.com/accounts/onetap/?next=%2Fdirect%2Finbox%2F",
        "https://www.instagram.com/direct",
        "https://www.instagram.com/direct/",
        "https://www.instagram.com/direct/?__coig_login=1",
        "https://instagram.com/direct",
        "https://instagram.com/direct/",
        "https://instagram.com/accounts/login/",
        "https://www.instagram.com/accounts/login/",
        "https://www.instagram.com/accounts/login/two_factor?next=%2F",
        "about:blank"
    )

    webView.webViewClient = object : WebViewClient() {

        /*override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
                // Intercept the request and modify the response
                if (request != null) {
                    val url = request.url.toString()
                    Log.d("Received URL: ", url)
                }
            return null

        // Instagram updates webpage content dynamically using AJAX navigation & route definition
            /*
            # https://www.instagram.com/ajax/navigation/
            # https://ww.instagram.com/ajax/route-definition/
            # https://www.instagram.com/ajax/bulk-route-definitions/
            */
        }*/

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d("Received URL: ", "$url")
            if (url != null) {
                if (!allowedUrls.contains(url)) {
                    startTimedActivity(webView.context, url) // is being invoked twice?
                    view?.loadUrl("https://instagram.com/direct")
                }
            } else {
                webView.loadData(
                    "<html><body><h1>URL is null</h1></body></html>",
                    "text/html",
                    "UTF-8"
                )
            }
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            activity.filePathCallback = filePathCallback

            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            }

            val chooserIntent = Intent.createChooser(intent, "File Chooser")
            activity.fileChooserLauncher.launch(chooserIntent)
            return true
        }
    }

    // Restore the WebView state if available
    val state = restoreWebViewState()
    if (state != null) {
        webView.restoreState(state)
    } else {
        // Load the initial URL
        webView.loadUrl("https://instagram.com/direct")
    }

    // Save the WebView state when the activity is destroyed
    (webView.context as? Activity)?.application?.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks() {
        override fun onActivityDestroyed(activity: Activity) {
            if (activity == webView.context) {
                val bundle = Bundle()
                webView.saveState(bundle)
                saveWebViewState(bundle)
            }
        }
    })
}

private fun startTimedActivity(context: Context, url: String) {
    val intent = Intent(context, TimedActivity::class.java).apply {
        putExtra("TIMED_URL", url)
    }
    context.startActivity(intent)
}

abstract class SimpleActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}