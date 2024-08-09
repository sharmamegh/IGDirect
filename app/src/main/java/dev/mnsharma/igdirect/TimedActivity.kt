package dev.mnsharma.igdirect

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TimedActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    // Start a countdown timer for 2 minutes (120 seconds)
    private val timer: CountDownTimer = object : CountDownTimer(90000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            // Optional: You can update UI to show countdown if needed
        }

        override fun onFinish() {
            // Timer finished, start MainActivity
            startActivity(Intent(this@TimedActivity, MainActivity::class.java))
            finish() // Finish this activity so user cannot go back to it
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timed)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the WebView after setContentView
        webView = findViewById(R.id.webView)

        // Retrieve URL from Intent extras
        val url: String = intent.getStringExtra("TIMED_URL") ?: ""

        webView.webViewClient = WebViewClient()
        // Enable JavaScript (optional)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        // Start the countdown timer
        timer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel() // Cancel the timer to avoid memory leaks
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
}