package com.attentive.bonni

import android.app.Application
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.androidsdk.internal.network.ApiVersion
import com.attentive.androidsdk.journeys.CartAbandonmentConfig
import com.attentive.bonni.database.AppDatabase
import com.attentive.bonni.journeys.NanoAbandonmentCopyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BonniApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appInstance = this
        initAttentiveTracker()
        // Timber tree is planted by AttentiveSdk based on logLevel config
        AppDatabase.getInstance().initWithMockProducts()
        syncCartWithAttentive()
    }

    private fun initAttentiveTracker() {
        val prefs = getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        val domain = prefs.getString(ATTENTIVE_DOMAIN_PREFS, "games")!!
        val email = prefs.getString(ATTENTIVE_EMAIL_PREFS, null)
        val phone = prefs.getString(ATTENTIVE_PHONE_PREFS, null)
        val apiVersion =
            try {
                ApiVersion.valueOf(prefs.getString(ATTENTIVE_ENDPOINT_PREFS, null) ?: "OLD")
            } catch (e: IllegalArgumentException) {
                ApiVersion.OLD
            }

        val attentiveConfig =
            AttentiveConfig
                .Builder()
                .applicationContext(this)
                .domain(domain)
                .notificationIconId(R.drawable.bonni_logo)
                .notificationIconBackgroundColor(R.color.purple_200)
                .mode(AttentiveConfig.Mode.PRODUCTION)
                .logLevel(AttentiveLogLevel.VERBOSE)
                .apiVersion(apiVersion)
                .build()

        AttentiveSdk.initialize(attentiveConfig)
        enableCartAbandonment(prefs.getBoolean(CART_ABANDONMENT_FAST_DELAY_PREFS, false))

        // Restore user identifiers if they exist (using identify to preserve visitorId)
        if (email != null || phone != null) {
            val userIdentifiers = UserIdentifiers.Builder()
            email?.let {
                userIdentifiers.withEmail(it)
            }
            phone?.let {
                userIdentifiers.withPhone(it)
            }

            attentiveConfig.identify(userIdentifiers.build())
        }
    }

    /** Keeps cart abandonment's view of the cart in step with Bonni's cart, including removals. */
    private fun syncCartWithAttentive() {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance().cartItemDao().getAll().collect { cartItems ->
                AttentiveSdk.syncCart(cartItems.map { it.product.item.copy(quantity = it.quantity) })
            }
        }
    }

    private val nanoCopyProvider by lazy {
        NanoAbandonmentCopyProvider().also { provider ->
            CoroutineScope(Dispatchers.IO).launch { provider.prepare() }
        }
    }

    /** Enables cart abandonment, checking after 30 seconds instead of an hour when [fastDelay] is set. */
    fun enableCartAbandonment(fastDelay: Boolean) {
        val builder =
            CartAbandonmentConfig.Builder()
                .cartDeeplink("bonni://cart")
                .copyProvider(nanoCopyProvider)
        if (fastDelay) builder.delay(30, TimeUnit.SECONDS)
        AttentiveSdk.enableCartAbandonment(builder.build())
    }

    companion object {
        private lateinit var appInstance: BonniApp

        const val ATTENTIVE_PREFS = "ATTENTIVE_PREFS"
        const val ATTENTIVE_DOMAIN_PREFS = "ATTENTIVE_DOMAIN_PREFS"

        const val ATTENTIVE_EMAIL_PREFS = "ATTENTIVE_EMAIL_PREFS"
        const val ATTENTIVE_PHONE_PREFS = "ATTENTIVE_PHONE_PREFS"

        const val ATTENTIVE_ENDPOINT_PREFS = "ATTENTIVE_ENDPOINT_PREFS"

        const val CART_ABANDONMENT_FAST_DELAY_PREFS = "CART_ABANDONMENT_FAST_DELAY_PREFS"

        fun getInstance(): BonniApp {
            return appInstance
        }
    }
}
