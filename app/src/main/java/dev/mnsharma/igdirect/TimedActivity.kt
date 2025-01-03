package dev.mnsharma.igdirect

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class TimedActivity : AppCompatActivity() {
    private lateinit var distractionView: WebView
    private lateinit var countdownText: TextView

    // Start a countdown timer for 1 minute (60 seconds)
    private val timer: CountDownTimer = object : CountDownTimer(60000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val secondsRemaining = millisUntilFinished / 1000
            countdownText.text = String.format(Locale.getDefault(), "%d", secondsRemaining)
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

        countdownText = findViewById(R.id.countdownText)
        distractionView = findViewById(R.id.webView)

        val url: String = intent.getStringExtra("TIMED_URL") ?: ""

        distractionView.webViewClient = WebViewClient()

        distractionView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        distractionView.loadUrl(url)
        // Start the countdown timer
        timer.start()

        onBackPressedDispatcher.addCallback(this) {
            if (distractionView.canGoBack()) {
                distractionView.goBack()  // Go back in WebView history
            } else {
                // If WebView can't go back, clear webView history and finish the activity
                distractionView.clearHistory()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel() // Cancel the timer to avoid memory leaks
        // Dispose of the WebView to avoid potential memory leaks
        distractionView.destroy()
    }
}