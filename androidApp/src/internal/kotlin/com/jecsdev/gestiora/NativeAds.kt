package com.jecsdev.gestiora

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.jecsdev.gestiora.presentation.ui.NativeAdContent
import com.jecsdev.gestiora.presentation.ui.NativeAdPlacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val NATIVE_TEST_UNIT = "ca-app-pub-3940256099942544/2247696110"
private const val LOG_TAG = "GestioraAds"

@Composable
internal fun rememberNativeAdContent(activity: Activity): NativeAdContent {
    val consent = remember(activity) { AdConsent(activity) }
    val sdkReady = remember(activity) { mutableStateOf(false) }
    LaunchedEffect(consent.canRequestAds) {
        if (consent.canRequestAds && !sdkReady.value) {
            sdkReady.value = try {
                withContext(Dispatchers.IO) {
                    MobileAds.setRequestConfiguration(
                        MobileAds.getRequestConfiguration().toBuilder()
                            .setPublisherPrivacyPersonalizationState(
                                RequestConfiguration.PublisherPrivacyPersonalizationState.DISABLED,
                            )
                            .build(),
                    )
                    MobileAds.initialize(activity.applicationContext)
                    MobileAds.putPublisherFirstPartyIdEnabled(false)
                }
            } catch (error: IllegalStateException) {
                Log.w(LOG_TAG, "Test ad SDK initialization failed", error)
                false
            }
        }
    }
    return remember(activity, consent) {
        val content: NativeAdContent = { placement -> NativeAdFooter(activity, consent, sdkReady.value, placement) }
        content
    }
}

private class AdConsent(private val activity: Activity) {
    private val information = UserMessagingPlatform.getConsentInformation(activity)
    private var requested = false
    private var privacyFormOpen = false
    var canRequestAds by mutableStateOf(false)
        private set
    var needsPrivacyOptions by mutableStateOf(false)
        private set

    fun refresh() {
        if (requested) return
        requested = true
        information.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                if (!activity.isDestroyed) {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                        applyConsent(error?.errorCode)
                    }
                }
            },
            { error -> applyConsent(error.errorCode) },
        )
    }

    fun showPrivacyOptions() {
        if (privacyFormOpen) return
        privacyFormOpen = true
        canRequestAds = false
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            privacyFormOpen = false
            applyConsent(error?.errorCode)
        }
    }

    private fun applyConsent(errorCode: Int?) {
        if (activity.isDestroyed) return
        if (errorCode != null) Log.d(LOG_TAG, "Consent flow finished with code $errorCode")
        needsPrivacyOptions = information.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        canRequestAds = information.canRequestAds()
    }
}

@Composable
private fun NativeAdFooter(activity: Activity, consent: AdConsent, sdkReady: Boolean, placement: NativeAdPlacement) {
    LaunchedEffect(consent) { consent.refresh() }
    if (placement == NativeAdPlacement.SettingsFooter && consent.needsPrivacyOptions) {
        TextButton(onClick = consent::showPrivacyOptions) {
            Text(stringResource(R.string.ad_privacy_options))
        }
    }
    if (sdkReady && consent.canRequestAds) TestNativeAd(activity, placement)
}

@Composable
private fun TestNativeAd(activity: Activity, placement: NativeAdPlacement) {
    var ad by remember(activity, placement) { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(activity, placement) {
        var disposed = false
        val loader = AdLoader.Builder(activity, NATIVE_TEST_UNIT)
            .forNativeAd { loaded ->
                if (disposed || activity.isDestroyed) {
                    loaded.destroy()
                } else {
                    ad = loaded
                    Log.d(LOG_TAG, "Test native ad loaded for $placement")
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(LOG_TAG, "Test native ad unavailable for $placement (code ${error.code})")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build(),
            )
            .build()
        val extras = Bundle().apply { putInt("npa", 1) }
        loader.loadAd(AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build())
        onDispose {
            disposed = true
            ad?.destroy()
        }
    }
    ad?.let { creative -> key(creative) { NativeAdCard(creative) } }
}

@Composable
private fun NativeAdCard(ad: NativeAd) {
    val colors = MaterialTheme.colorScheme
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context -> createNativeAdView(context, ad) },
        onRelease = { it.destroy() },
        update = { view ->
            view.background = roundedBackground(view.context, colors.surface.toArgb(), colors.outlineVariant.toArgb())
            (view.headlineView as? TextView)?.setTextColor(colors.onSurface.toArgb())
            (view.bodyView as? TextView)?.setTextColor(colors.onSurfaceVariant.toArgb())
            (view.callToActionView as? TextView)?.apply {
                setTextColor(colors.onSecondaryContainer.toArgb())
                background = roundedBackground(context, colors.secondaryContainer.toArgb())
            }
        },
    )
}

private fun createNativeAdView(context: Context, ad: NativeAd): NativeAdView {
    val nativeView = NativeAdView(context)
    val inset = context.dp(12)
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(inset, inset, inset, inset)
    }
    nativeView.addView(content, MATCH_PARENT, WRAP_CONTENT)

    val attribution = TextView(context).apply {
        text = context.getString(R.string.native_ad_label)
        textSize = 12f
        gravity = Gravity.CENTER_VERTICAL
        minHeight = context.dp(18)
        minWidth = context.dp(18)
        setTextColor(android.graphics.Color.DKGRAY)
        setPadding(context.dp(4), 0, context.dp(4), 0)
        background = roundedBackground(context, android.graphics.Color.WHITE)
    }
    content.addView(attribution, WRAP_CONTENT, WRAP_CONTENT)

    val identity = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    content.addView(identity, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = inset })
    ad.icon?.let { icon ->
        val image = ImageView(context).apply { setImageDrawable(icon.drawable) }
        identity.addView(image, LinearLayout.LayoutParams(context.dp(36), context.dp(36)).apply {
            marginEnd = context.dp(8)
        })
        nativeView.iconView = image
    }
    val copy = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    identity.addView(copy, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
    val headline = TextView(context).apply {
        text = ad.headline
        textSize = 15f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    copy.addView(headline, MATCH_PARENT, WRAP_CONTENT)
    nativeView.headlineView = headline
    ad.body?.let { body ->
        val description = TextView(context).apply { text = body; textSize = 13f }
        copy.addView(description, MATCH_PARENT, WRAP_CONTENT)
        nativeView.bodyView = description
    }

    ad.mediaContent?.let { media ->
        val mediaView = MediaView(context).apply {
            mediaContent = media
            setImageScaleType(ImageView.ScaleType.FIT_CENTER)
        }
        content.addView(mediaView, LinearLayout.LayoutParams(MATCH_PARENT, context.dp(120)).apply { topMargin = inset })
        nativeView.mediaView = mediaView
    }
    ad.callToAction?.let { action ->
        val cta = TextView(context).apply {
            text = action
            textSize = 14f
            gravity = Gravity.CENTER
            minimumHeight = context.dp(48)
            setPadding(inset, 0, inset, 0)
        }
        content.addView(cta, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = inset })
        nativeView.callToActionView = cta
    }
    nativeView.setNativeAd(ad)
    return nativeView
}

private fun roundedBackground(context: Context, color: Int, stroke: Int? = null) =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = context.dp(12).toFloat()
        if (stroke != null) setStroke(context.dp(1), stroke)
    }

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
