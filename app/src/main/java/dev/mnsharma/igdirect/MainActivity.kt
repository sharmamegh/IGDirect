package dev.mnsharma.igdirect

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {
    private var webViewBundle: Bundle? = null
    private lateinit var webView: WebView
    private lateinit var loadingAnimation: LottieAnimationView

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

        intent?.let {
            val action = it.action
            val data = it.data

            if (Intent.ACTION_VIEW == action && data != null) {
                val url = data.toString()

                // Call startTimedActivity to process the intent
                startTimedActivity(this, url)
            }
        }

        loadingAnimation = findViewById(R.id.loadingAnimation)
        webView = findViewById(R.id.webView)
        webViewScreen(
            loadingAnimation = loadingAnimation,
            webView = webView,
            saveWebViewState = { state -> webViewBundle = state },
            restoreWebViewState = { webViewBundle },
            this
        )

        checkPermissions()

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()  // Go back in WebView history
            } else {
                // If WebView can't go back, let the system handle the back press
                onBackPressedDispatcher.onBackPressed()
            }
        }
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

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // Check for permissions based on SDK version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // Always call super

//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//            val deniedPermissions = permissions.filterIndexed { index, _ ->
//                grantResults[index] != PackageManager.PERMISSION_GRANTED
//            }
//            if (deniedPermissions.isNotEmpty()) {
//                Toast.makeText(this, "Permissions denied: $deniedPermissions", Toast.LENGTH_LONG).show()
//            } else {
//                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun webViewScreen(loadingAnimation: LottieAnimationView ,webView: WebView, saveWebViewState: (Bundle) -> Unit, restoreWebViewState: () -> Bundle?, activity: MainActivity) {
    WebView.setWebContentsDebuggingEnabled(true)

    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        cacheMode = WebSettings.LOAD_DEFAULT
        loadsImagesAutomatically = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }
    webView.setNetworkAvailable(true)  // Make sure network status is properly handled

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
        "about:blank",
        "file:///android_res/raw/error_page.html"
    )

    webView.webViewClient = object : WebViewClient() {

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
            super.onReceivedError(view, request, error)
        }

        private var lastProcessedUrl: String? = null

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            // Ensure the request object is not null and get the URL
            request?.let {
//                val url = it.url.toString()

                // Check if the URL is the error page
//                if (!url.contains("file:///android_res/raw/error_page.html")) {
//                    isErrorPage = false
//                }

                // Optional: Log the intercepted URL for debugging
                //Log.d("Intercepted URL", url)
            }

            // Return null to allow WebView to handle the request normally
            return null

        // Instagram updates webpage content dynamically using AJAX navigation & route definition
            /*
            # https://www.instagram.com/ajax/navigation/
            # https://ww.instagram.com/ajax/route-definition/
            # https://www.instagram.com/ajax/bulk-route-definitions/
            */
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            loadingAnimation.visibility = View.VISIBLE
            webView.visibility = View.GONE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            loadingAnimation.postDelayed({
                loadingAnimation.cancelAnimation()
                webView.visibility = View.VISIBLE
            }, 2650)

            Log.d("URL Loaded: ", "$url")
            if (url != null) {
                if (!allowedUrls.contains(url) && lastProcessedUrl != url) {
                    lastProcessedUrl = url // Update the last processed URL
                    Log.d("DEBUG LOG", "URL: $url, LastProcessed: $lastProcessedUrl")
                    Log.v("DEBUG LOG", "This message should only be displayed once.")
                    startTimedActivity(webView.context, url)
                    view?.loadUrl("https://instagram.com/direct")
                    if (url == "https://instagram.com/direct") {
                        webView.clearHistory()
                    }
                }
            } else {
                val nullErrorContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Oops! No URL Found</title>
                        <style>
                            body {
                                font-family: Arial, sans-serif;
                                text-align: center;
                                margin: 0;
                                padding: 0;
                                background-color: #f4f4f4;
                            }
                            .container {
                                display: flex;
                                flex-direction: column;
                                justify-content: center;
                                align-items: center;
                                height: 100vh;
                                padding: 20px;
                            }
                            h1 {
                                color: #ff6b6b;
                                margin-bottom: 10px;
                            }
                            p {
                                color: #555;
                                font-size: 16px;
                                margin-bottom: 20px;
                            }
                            .button {
                                text-decoration: none;
                                color: white;
                                background-color: #007bff;
                                padding: 10px 20px;
                                border-radius: 5px;
                                font-size: 16px;
                                font-weight: bold;
                                box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
                                transition: background-color 0.3s;
                            }
                            .button:hover {
                                background-color: #0056b3;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <h1>Oops! URL Not Found</h1>
                            <p>It seems something went wrong and we couldn't find the link you were looking for.</p>
                            <p>No worries, you can reconnect to the content you love!</p>
                            <a href="javascript:window.location.href='https://instagram.com/direct';" class="button">Go Back</a>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                webView.loadData(nullErrorContent, "text/html", "UTF-8")
                view?.post { webView.clearHistory() }
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
        flags = Intent.FLAG_ACTIVITY_NEW_TASK  // Starts a new task for TimedActivity
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