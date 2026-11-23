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
import com.unity3d.ads.UnityAds
import com.unity3d.ads.InitializationConfiguration
import com.unity3d.ads.InitializationListener
import com.unity3d.ads.BannerAd
import com.unity3d.ads.BannerLoadConfiguration
import com.unity3d.ads.BannerSize
import com.unity3d.ads.InterstitialAd
import com.unity3d.ads.LoadConfiguration
import com.unity3d.ads.ShowConfiguration
import com.unity3d.ads.RewardedAd
import com.unity3d.ads.BannerLoadListener
import com.unity3d.ads.InterstitialShowListener
import com.unity3d.ads.RewardedShowListener
import com.unity3d.ads.ShowFinishState
import com.unity3d.ads.UnityAdsError

private const val UNITY_GAME_ID = "800449377"
private const val BANNER_PLACEMENT = "BP_Banner_Android"
private const val INTERSTITIAL_PLACEMENT = "BP_Interstitial_Android"
private const val REWARDED_PLACEMENT = "BP_Rewarded_Android"
private const val UNITY_TEST_MODE = false

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
    private var initialized = false
    private var bannerAd: BannerAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var generationsSinceAd = 0
    private var fullScreenAdShowing = false

    fun initialize() {
        val config = InitializationConfiguration.Builder(UNITY_GAME_ID)
            .withTestMode(UNITY_TEST_MODE)
            .build()
        UnityAds.initialize(config, InitializationListener { error ->
            if (error == null) {
                initialized = true
                loadBanner()
                loadInterstitial()
                loadRewarded()
            } else {
                Toast.makeText(activity, "Unity Ads initialization failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadBanner() {
        if (!initialized) return
        val config = BannerLoadConfiguration.Builder(BANNER_PLACEMENT, BannerSize(320, 50)).build()
        BannerAd.load(config, BannerLoadListener { ad, error ->
            if (ad != null) {
                bannerAd = ad
                bannerView = ad.view
            } else if (error != null) {
                android.util.Log.w("UnityAds", "Banner load failed: ${error.message}")
            }
        })
    }

    private fun loadInterstitial() {
        if (!initialized) return
        val config = LoadConfiguration.Builder(INTERSTITIAL_PLACEMENT).build()
        InterstitialAd.load(config) { ad, error ->
            if (ad != null) interstitialAd = ad
            else if (error != null) android.util.Log.w("UnityAds", "Interstitial load failed: ${error.message}")
        }
    }

    private fun loadRewarded() {
        if (!initialized) return
        val config = LoadConfiguration.Builder(REWARDED_PLACEMENT).build()
        RewardedAd.load(config) { ad, error ->
            if (ad != null) {
                rewardedAd = ad
                ad.onAdExpired = com.unity3d.ads.AdExpiredListener { rewardedAd = null; loadRewarded() }
            } else if (error != null) {
                android.util.Log.w("UnityAds", "Rewarded load failed: ${error.message}")
            }
        }
    }

    fun onGenerationCompleted() {
        generationsSinceAd++
        if (generationsSinceAd >= 4) {
            val ad = interstitialAd
            if (ad != null && !fullScreenAdShowing) {
                generationsSinceAd = 0
                interstitialAd = null
                fullScreenAdShowing = true
                ad.show(activity, ShowConfiguration.Builder().build(), object : InterstitialShowListener {
                    override fun onStarted(unityAd: InterstitialAd) {}
                    override fun onClicked(unityAd: InterstitialAd) {}
                    override fun onCompleted(unityAd: InterstitialAd, state: ShowFinishState) {
                        fullScreenAdShowing = false
                        loadInterstitial()
                    }
                    override fun onFailed(unityAd: InterstitialAd, error: UnityAdsError) {
                        fullScreenAdShowing = false
                        loadInterstitial()
                    }
                })
            }
        }
        if (interstitialAd == null) loadInterstitial()
    }

    fun showRewarded(onReward: () -> Unit) {
        val ad = rewardedAd
        if (!initialized || ad == null || fullScreenAdShowing) {
            Toast.makeText(activity, "Rewarded ad is not ready yet. Please try again.", Toast.LENGTH_SHORT).show()
            if (initialized && ad == null) loadRewarded()
            return
        }
        rewardedAd = null
        fullScreenAdShowing = true
        var rewardGranted = false
        ad.show(activity, ShowConfiguration.Builder().build(), object : RewardedShowListener {
            override fun onStarted(unityAd: RewardedAd) {}
            override fun onClicked(unityAd: RewardedAd) {}
            override fun onRewarded(unityAd: RewardedAd) {
                if (!rewardGranted) {
                    rewardGranted = true
                    activity.runOnUiThread { onReward() }
                }
            }
            override fun onCompleted(unityAd: RewardedAd, state: ShowFinishState) {
                fullScreenAdShowing = false
                loadRewarded()
            }
            override fun onFailed(unityAd: RewardedAd, error: UnityAdsError) {
                fullScreenAdShowing = false
                Toast.makeText(activity, "Ad could not be shown: ${error.message}", Toast.LENGTH_SHORT).show()
                loadRewarded()
            }
        })
    }

    fun destroy() {
        (bannerView?.parent as? FrameLayout)?.removeView(bannerView)
        bannerView = null
        bannerAd = null
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
