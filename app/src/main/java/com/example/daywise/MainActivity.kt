package com.example.daywise

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.os.Build
import java.util.Calendar

class MainActivity : ComponentActivity() {

  // Camera: store base64 if WebView reloads before we can inject it
  private var pendingCameraBase64: String? = null
  private var webViewInstance: WebView? = null
  private var cameraPhotoUri: Uri? = null

  // Gallery still uses filePathCallback
  private var filePathCallback: android.webkit.ValueCallback<Array<Uri>>? = null

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted -> Log.d("DaywisePerm", if (granted) "Camera granted" else "Camera denied") }

  private val requestPermissionsLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    permissions.entries.forEach {
      Log.d("DaywisePerm", "${it.key} = ${it.value}")
    }
  }

  // ── Camera launcher: compress → base64 → evaluateJavascript ──────────────
  private val cameraLauncher = registerForActivityResult(
    ActivityResultContracts.TakePicture()
  ) { success ->
    val uri = cameraPhotoUri
    cameraPhotoUri = null
    if (success && uri != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        val base64 = compressUriToBase64(uri)
        withContext(Dispatchers.Main) {
          if (base64 != null) {
            val wv = webViewInstance
            if (wv != null) {
              wv.evaluateJavascript("window.onCameraImageReady('$base64')", null)
            } else {
              pendingCameraBase64 = base64
            }
          } else {
            webViewInstance?.evaluateJavascript(
              "window.onCameraError('Failed to process image. Try again.')", null
            )
          }
        }
      }
    } else {
      webViewInstance?.evaluateJavascript("window.onCameraError('Photo cancelled.')", null)
    }
  }

  // ── Gallery launcher ──────────────────────────────────────────────────────
  private val fileChooserLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
    if (uris != null && uris.isNotEmpty()) {
      processAndSendUris(uris)
    } else {
      filePathCallback?.onReceiveValue(null)
      filePathCallback = null
    }
  }

  // ── AndroidBridge: called from JS via window.DaywiseAndroid ──────────────
  inner class AndroidBridge {
    @JavascriptInterface
    fun launchCamera() {
      runOnUiThread {
        if (ContextCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.CAMERA
          ) != PackageManager.PERMISSION_GRANTED
        ) {
          requestPermissionLauncher.launch(Manifest.permission.CAMERA)
          webViewInstance?.evaluateJavascript(
            "window.onCameraError('Camera permission required. Please grant and retry.')", null
          )
          return@runOnUiThread
        }
        val file = createTempImageFile()
        if (file != null) {
          try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
              this@MainActivity, "${packageName}.fileprovider", file
            )
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
          } catch (e: Exception) {
            Log.e("DaywiseCamera", "Launch failed", e)
            webViewInstance?.evaluateJavascript(
              "window.onCameraError('Failed to open camera.')", null
            )
          }
        }
      }
    }

    @JavascriptInterface
    fun updateAlarm(id: String, label: String, enabled: Boolean, timeStr: String, message: String) {
      runOnUiThread {
        this@MainActivity.updateAndroidAlarm(id, label, enabled, timeStr, message)
      }
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  private fun createTempImageFile(): java.io.File? = try {
    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
    java.io.File.createTempFile("CAM_${ts}_", ".jpg", cacheDir)
  } catch (e: Exception) { Log.e("DaywiseCamera", "Temp file failed", e); null }

  private fun compressUriToBase64(uri: Uri): String? {
    return try {
      val maxDim = 640

      // EXIF rotation
      var rot = 0
      try {
        contentResolver.openInputStream(uri)?.use {
          val exif = ExifInterface(it)
          rot = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
          }
        }
      } catch (_: Exception) {}

      // Bounds
      val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
      val sw = opts.outWidth; val sh = opts.outHeight
      if (sw <= 0 || sh <= 0) return null

      // Sample size
      var ss = 1
      val mx = maxOf(sw, sh)
      while (mx / (ss * 2) >= maxDim) ss *= 2

      // Decode
      var bmp = contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = ss })
      } ?: return null

      // Scale + rotate
      val cw = bmp.width; val ch = bmp.height
      val cm = maxOf(cw, ch)
      if (cm > maxDim || rot != 0) {
        val sc = if (cm > maxDim) maxDim.toFloat() / cm else 1f
        val m = Matrix().apply { if (sc < 1f) postScale(sc, sc); if (rot != 0) postRotate(rot.toFloat()) }
        val r = Bitmap.createBitmap(bmp, 0, 0, cw, ch, m, true)
        if (r != bmp) { bmp.recycle(); bmp = r }
      }

      // To base64
      val bos = ByteArrayOutputStream()
      bmp.compress(Bitmap.CompressFormat.JPEG, 60, bos)
      bmp.recycle()
      Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
      Log.e("DaywiseCamera", "compressUriToBase64 failed", e); null
    }
  }

  private fun compressUriToFileProviderUri(uri: Uri): Uri? {
    return try {
      val maxDim = 640
      var rot = 0
      try {
        contentResolver.openInputStream(uri)?.use {
          val exif = ExifInterface(it)
          rot = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90; ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270; else -> 0
          }
        }
      } catch (_: Exception) {}

      val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
      val sw = opts.outWidth; val sh = opts.outHeight
      if (sw <= 0 || sh <= 0) return null

      var ss = 1; val mx = maxOf(sw, sh)
      while (mx / (ss * 2) >= maxDim) ss *= 2

      var bmp = contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = ss })
      } ?: return null

      val cw = bmp.width; val ch = bmp.height; val cm = maxOf(cw, ch)
      if (cm > maxDim || rot != 0) {
        val sc = if (cm > maxDim) maxDim.toFloat() / cm else 1f
        val m = Matrix().apply { if (sc < 1f) postScale(sc, sc); if (rot != 0) postRotate(rot.toFloat()) }
        val r = Bitmap.createBitmap(bmp, 0, 0, cw, ch, m, true)
        if (r != bmp) { bmp.recycle(); bmp = r }
      }

      val tf = createTempImageFile() ?: return null
      java.io.FileOutputStream(tf).use { bmp.compress(Bitmap.CompressFormat.JPEG, 60, it) }
      bmp.recycle()
      androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", tf)
    } catch (e: Exception) { null }
  }

  private fun processAndSendUris(uris: Array<Uri>) {
    val cb = filePathCallback; filePathCallback = null; if (cb == null) return
    lifecycleScope.launch(Dispatchers.IO) {
      val compressed = uris.map { compressUriToFileProviderUri(it) ?: it }.toTypedArray()
      withContext(Dispatchers.Main) { cb.onReceiveValue(compressed) }
    }
  }

  fun updateAndroidAlarm(id: String, label: String, enabled: Boolean, timeStr: String, message: String) {
      com.example.daywise.ui.screens.reminders.AlarmHelper.updateAlarm(this, id, label, enabled, timeStr, message)
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    // Request permissions at startup (Camera and notifications for Android 13+)
    val permissionsToRequest = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.CAMERA)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    if (permissionsToRequest.isNotEmpty()) {
        requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

    setContent {
      WebViewScreen(
        onWebViewCreated = { wv ->
          webViewInstance = wv
          wv.addJavascriptInterface(AndroidBridge(), "DaywiseAndroid")
        },
        onPageFinished = {
          val b64 = pendingCameraBase64
          if (b64 != null) {
            pendingCameraBase64 = null
            webViewInstance?.evaluateJavascript("window.onCameraImageReady('$b64')", null)
          }
        },
        onShowFileChooser = { params, cb ->
          filePathCallback?.onReceiveValue(null)
          filePathCallback = cb
          try {
            val intent = params.createIntent()
            if (intent != null) { fileChooserLauncher.launch(intent); true }
            else { filePathCallback?.onReceiveValue(null); filePathCallback = null; false }
          } catch (e: Exception) {
            filePathCallback?.onReceiveValue(null); filePathCallback = null; false
          }
        }
      )
    }
  }

  override fun onDestroy() {
    webViewInstance?.removeJavascriptInterface("DaywiseAndroid")
    webViewInstance = null
    super.onDestroy()
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
  onWebViewCreated: (WebView) -> Unit,
  onPageFinished: () -> Unit,
  onShowFileChooser: (WebChromeClient.FileChooserParams, android.webkit.ValueCallback<Array<Uri>>?) -> Boolean
) {
  val useDevServer = false

  AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
    WebView(context).apply {
      setBackgroundColor(Color.TRANSPARENT)

      webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          onPageFinished()
        }
      }

      webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
          Log.d("WebViewConsole", "${msg?.message()} [${msg?.sourceId()}:${msg?.lineNumber()}]")
          return true
        }
        override fun onShowFileChooser(
          wv: WebView?,
          cb: android.webkit.ValueCallback<Array<Uri>>?,
          params: FileChooserParams?
        ): Boolean {
          if (params == null) { cb?.onReceiveValue(null); return false }
          return onShowFileChooser(params, cb)
        }
      }

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        @Suppress("DEPRECATION") databaseEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        allowFileAccessFromFileURLs = true
        allowUniversalAccessFromFileURLs = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        cacheMode = if (useDevServer) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
      }

      onWebViewCreated(this)

      if (useDevServer) {
        loadUrl("http://10.116.225.39:6272/")
      } else {
        loadUrl("file:///android_asset/index.html")
      }
    }
  })
}
