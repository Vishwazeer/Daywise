package com.example.daywise

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Enable remote debugging in Chrome DevTools (chrome://inspect)
    WebView.setWebContentsDebuggingEnabled(true)

    setContent {
      WebViewScreen()
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen() {
  val useDevServer = true // Set to true for HMR Live Sync, false for offline build

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context ->
      WebView(context).apply {
        // Transparent background so web CSS controls color
        setBackgroundColor(Color.TRANSPARENT)

        // Simple pass-through client
        webViewClient = WebViewClient()

        // Capture JS console logs in logcat
        webChromeClient = object : WebChromeClient() {
          override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            Log.d("WebViewConsole", "${consoleMessage?.message()} [${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}]")
            return true
          }
        }

        // Configure WebView settings
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          databaseEnabled = true
          allowFileAccess = true
          allowContentAccess = true
          mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
          useWideViewPort = true
          loadWithOverviewMode = true
          builtInZoomControls = false
          displayZoomControls = false
          setSupportZoom(false)
          cacheMode = if (useDevServer) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        }

        if (useDevServer) {
          // Load from PC Vite dev server over local Wi-Fi
          loadUrl("http://10.112.100.39:5173/")
        } else {
          // Read bundled single-file HTML from assets
          val html = context.assets.open("index.html").bufferedReader().use { it.readText() }
          loadDataWithBaseURL(
            "https://appassets.androidplatform.net/",
            html,
            "text/html",
            "UTF-8",
            null
          )
        }
      }
    }
  )
}
