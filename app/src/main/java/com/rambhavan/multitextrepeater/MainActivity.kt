package com.rambhavan.multitextrepeater

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.ads.UnityAdsShowCompletionState
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

private const val UNITY_GAME_ID = "800449377"
private const val BANNER_PLACEMENT = "BP_Banner_Android"
private const val INTERSTITIAL_PLACEMENT = "BP_Interstitial_Android"
private const val REWARDED_PLACEMENT = "BP_Rewarded_Android"
private const val UNITY_TEST_MODE = true

private val Accent = Color(0xFF5B5FEF)
private val Canvas = Color(0xFFF5F6FC)
private val Ink = Color(0xFF172033)

class MainActivity : ComponentActivity() {
    private lateinit var ads: UnityAdsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ads = UnityAdsController(this)
        ads.initialize()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Canvas)) {
                RepeaterScreen(ads)
            }
        }
    }

    override fun onDestroy() {
        if (::ads.isInitialized) ads.destroy()
        super.onDestroy()
    }
}

private class UnityAdsController(private val activity: ComponentActivity) {
    var bannerView by mutableStateOf<View?>(null)
        private set
    private var banner: BannerView? = null
    private var initialized = false
    private var interstitialLoaded = false
    private var rewardedLoaded = false
    private var generationsSinceAd = 0
    private var fullScreenAdShowing = false

    private val bannerListener = object : BannerView.IListener {
        override fun onBannerLoaded(bannerAdView: BannerView) {
            activity.runOnUiThread { bannerView = bannerAdView }
        }
        override fun onBannerShown(bannerAdView: BannerView) {}
        override fun onBannerClick(bannerAdView: BannerView) {}
        override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
            android.util.Log.w("UnityAds", "Banner failed to load: $errorInfo")
        }
        override fun onBannerLeftApplication(bannerAdView: BannerView) {}
    }

    fun initialize() {
        UnityAds.initialize(activity.applicationContext, UNITY_GAME_ID, UNITY_TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    initialized = true
                    loadBanner()
                    loadInterstitial()
                    loadRewarded()
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    android.util.Log.e("UnityAds", "Initialization failed: $message")
                }
            }
        )
    }

    private fun loadBanner() {
        if (!initialized || banner != null) return
        val bannerAd = BannerView(activity, BANNER_PLACEMENT, UnityBannerSize(320, 50))
        bannerAd.setListener(bannerListener)
        banner = bannerAd
        bannerAd.load()
    }

    private fun loadInterstitial() {
        if (!initialized || interstitialLoaded) return
        UnityAds.load(INTERSTITIAL_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                interstitialLoaded = true
            }
            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                interstitialLoaded = false
                android.util.Log.w("UnityAds", "Interstitial load failed: $message")
            }
        })
    }

    private fun loadRewarded() {
        if (!initialized || rewardedLoaded) return
        UnityAds.load(REWARDED_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                rewardedLoaded = true
            }
            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                rewardedLoaded = false
                android.util.Log.w("UnityAds", "Rewarded load failed: $message")
            }
        })
    }

    fun onGenerationCompleted() {
        generationsSinceAd++
        if (generationsSinceAd >= 4 && interstitialLoaded && !fullScreenAdShowing) {
            generationsSinceAd = 0
            interstitialLoaded = false
            fullScreenAdShowing = true
            UnityAds.show(activity, INTERSTITIAL_PLACEMENT, UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placementId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        fullScreenAdShowing = false
                        loadInterstitial()
                    }
                    override fun onUnityAdsShowStart(placementId: String) {}
                    override fun onUnityAdsShowClick(placementId: String) {}
                    override fun onUnityAdsShowComplete(
                        placementId: String,
                        state: UnityAdsShowCompletionState
                    ) {
                        fullScreenAdShowing = false
                        loadInterstitial()
                    }
                }
            )
        } else if (!interstitialLoaded) {
            loadInterstitial()
        }
    }

    fun showRewarded(onReward: () -> Unit) {
        if (!initialized || !rewardedLoaded || fullScreenAdShowing) {
            Toast.makeText(activity, "Rewarded ad is not ready yet. Please try again.", Toast.LENGTH_SHORT).show()
            if (initialized && !rewardedLoaded) loadRewarded()
            return
        }
        rewardedLoaded = false
        fullScreenAdShowing = true
        UnityAds.show(activity, REWARDED_PLACEMENT, UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String
                ) {
                    fullScreenAdShowing = false
                    Toast.makeText(activity, "Ad could not be shown: $message", Toast.LENGTH_SHORT).show()
                    loadRewarded()
                }
                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}
                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAdsShowCompletionState
                ) {
                    fullScreenAdShowing = false
                    if (state == UnityAdsShowCompletionState.COMPLETED) {
                        activity.runOnUiThread { onReward() }
                    }
                    loadRewarded()
                }
            }
        )
    }

    fun destroy() {
        banner?.destroy()
        banner = null
        bannerView = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeaterScreen(ads: UnityAdsController) {
    var source by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("10") }
    var separator by remember { mutableStateOf("New line") }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var premiumUnlocked by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(output) }
            }.onSuccess { Toast.makeText(context, "Text file saved", Toast.LENGTH_SHORT).show() }
             .onFailure { Toast.makeText(context, "Could not save file", Toast.LENGTH_SHORT).show() }
        }
    }

    fun generate() {
        val lines = source.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        val count = countText.toIntOrNull()
        val maxCount = if (premiumUnlocked) 50000 else 10000
        if (lines.isEmpty()) { error = "Enter at least one text line."; output = ""; return }
        if (count == null || count !in 1..maxCount) {
            error = "Repeat count must be between 1 and ${"%,d".format(maxCount)}."
            output = ""
            return
        }
        val joiner = when (separator) { "Space" -> " "; "Comma" -> ", "; else -> "\\n" }
        val estimated = lines.sumOf { it.length.toLong() + prefix.length + suffix.length } * count + (lines.size.toLong() * count * joiner.length)
        val maxBytes = if (premiumUnlocked) 5_000_000L else 2_000_000L
        if (estimated > maxBytes) { error = "Output is too large. Reduce the repeat count or text length."; output = ""; return }
        output = lines.flatMap { line -> List(count) { "$prefix$line$suffix" } }.joinToString(joiner)
        error = ""
        ads.onGenerationCompleted()
    }

    Scaffold(containerColor = Canvas) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MULTI TEXT", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Repeater", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Repeat one or many lines in seconds.", color = Color(0xFF687086), fontSize = 14.sp)
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("YOUR TEXT", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        OutlinedTextField(
                            value = source, onValueChange = { source = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                            placeholder = { Text("Type or paste text here…\\nPut each different text on a new line.") },
                            minLines = 4
                        )
                        Text("Each non-empty line is treated as a separate text.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("REPEAT SETTINGS", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        OutlinedTextField(
                            value = countText, onValueChange = { countText = it.filter(Char::isDigit).take(5) },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Repeat count (1–${if (premiumUnlocked) "50,000" else "10,000"})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                        )
                        Text("SEPARATOR", color = Color(0xFF687086), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("New line", "Space", "Comma").forEach { item ->
                                FilterChip(selected = separator == item, onClick = { separator = item }, label = { Text(item) })
                            }
                        }
                        OutlinedTextField(value = prefix, onValueChange = { prefix = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Prefix (optional)") }, singleLine = true)
                        OutlinedTextField(value = suffix, onValueChange = { suffix = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Suffix (optional)") }, singleLine = true)
                    }
                }
                Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) {
                    Text("Generate text", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (!premiumUnlocked) {
                    OutlinedButton(
                        onClick = { ads.showRewarded { premiumUnlocked = true; Toast.makeText(context, "Premium limit unlocked: 50,000 repeats!", Toast.LENGTH_LONG).show() } },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)
                    ) { Text("Watch ad to unlock 50,000 repeats") }
                } else {
                    Text("Premium limit unlocked for this session", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                if (output.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GENERATED OUTPUT", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${output.length} characters", color = Color.Gray, fontSize = 11.sp)
                            }
                            Text(output.take(5000) + if (output.length > 5000) "\\n… preview limited" else "", color = Ink, fontSize = 13.sp, lineHeight = 19.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { clipboard.setText(AnnotatedString(output)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) { Text("Copy all") }
                                OutlinedButton(onClick = { saveLauncher.launch("repeated-text.txt") }, modifier = Modifier.weight(1f)) { Text("Save TXT") }
                            }
                            OutlinedButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, output) }
                                context.startActivity(Intent.createChooser(intent, "Share text"))
                            }, modifier = Modifier.fillMaxWidth()) { Text("Share output") }
                        }
                    }
                }
                Text("Multi Text Repeater • Version 1.1", modifier = Modifier.fillMaxWidth(), color = Color.Gray, fontSize = 11.sp)
            }
            AndroidView(
                factory = { ctx -> FrameLayout(ctx).apply { layoutParams = android.view.ViewGroup.LayoutParams(-1, 50) } },
                update = { container ->
                    val adView = ads.bannerView
                    if (adView != null && adView.parent !== container) {
                        (adView.parent as? android.view.ViewGroup)?.removeView(adView)
                        container.removeAllViews()
                        container.addView(adView, FrameLayout.LayoutParams(-1, -1))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        }
    }
}
