package com.attentive.androidsdk.internal.context

import com.attentive.androidsdk.PersistentStorage
import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.attentive.androidsdk.internal.screens.ScreenNameFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * What the user is doing in the app: screens, products and how the app was opened. Kept on
 * device only. Screen tracking, commerce events and link handling write to it, and
 * [AppContextReader] reads from it.
 */
internal interface AttentiveContextStore : AppContextReader {
    fun onScreenView(name: String, atMs: Long)

    fun onProductView(products: List<ContextProduct>)

    fun onAddToCart(products: List<ContextProduct>)

    fun onPurchase()

    fun onLaunch(source: LaunchSource, utms: Map<UtmKey, String>, landingUrl: String?)

    fun onSessionStart()

    fun onForeground(atMs: Long)

    fun onBackground(atMs: Long)

    fun clear()
}

/** Records the products in a [ProductViewEvent] or [AddToCartEvent], and empties the cart on a [PurchaseEvent]. */
internal fun AttentiveContextStore.recordEvent(event: Event, atMs: Long) {
    when (event) {
        is ProductViewEvent -> onProductView(event.items.map { it.toContextProduct(atMs) })
        is AddToCartEvent -> onAddToCart(event.items.map { it.toContextProduct(atMs) })
        is PurchaseEvent -> onPurchase()
        else -> Unit
    }
}

/**
 * The persisted part of the store. Session-scoped fields reset when a session starts, product
 * history survives restarts, and each UTM key carries its own expiry.
 */
@Serializable
internal data class ContextState(
    val sessionStarted: Boolean = false,
    val currentScreenName: String? = null,
    val previousScreenName: String? = null,
    val previousScreenDurationMs: Long? = null,
    val screenIndex: Int = 0,
    /** Foreground time on the current screen before the current foreground stretch. */
    val currentScreenElapsedMs: Long = 0,
    val recentlyViewed: List<ContextProduct> = emptyList(),
    val recentlyAddedToCart: List<ContextProduct> = emptyList(),
    val launchSource: LaunchSource = LaunchSource.ORGANIC,
    val utmParams: Map<UtmKey, UtmValue> = emptyMap(),
    val landingUrl: String? = null,
    /** When the app last went to the background, used to decide whether a session has lapsed. */
    val lastActiveAtMs: Long? = null,
)

/**
 * [AttentiveContextStore] persisted as one JSON value in [PersistentStorage], written only when
 * the state changes.
 *
 * A session starts on the first foreground, and again on a foreground more than
 * [SESSION_TIMEOUT_MS] after the app was last active. A link opened mid-session updates the
 * launch source and UTMs without starting a new one.
 */
internal class PersistentContextStore(
    private val persistentStorage: PersistentStorage,
    private val packageName: String?,
    private val clock: () -> Long = System::currentTimeMillis,
) : AttentiveContextStore {
    private val lock = Any()
    private var state: ContextState = load()

    /** Start of the current foreground stretch, or null while in the background. */
    private var foregroundSinceMs: Long? = null

    override val screen: ScreenContext
        get() = synchronized(lock) { state.toScreenContext() }

    override val products: ProductContext
        get() = synchronized(lock) { state.toProductContext() }

    override val launch: LaunchContext
        get() = synchronized(lock) { state.toLaunchContext(clock()) }

    override fun snapshot(): AppContext =
        synchronized(lock) {
            AppContext(state.toScreenContext(), state.toProductContext(), state.toLaunchContext(clock()))
        }

    override fun onScreenView(name: String, atMs: Long) {
        synchronized(lock) {
            enterForeground(atMs)
            val previousName = state.currentScreenName
            update(
                state.copy(
                    currentScreenName = name.take(ScreenNameFormatter.MAX_LENGTH),
                    previousScreenName = previousName,
                    previousScreenDurationMs = if (previousName != null) currentScreenElapsedMs(atMs) else null,
                    screenIndex = state.screenIndex + 1,
                    currentScreenElapsedMs = 0,
                ),
            )
            foregroundSinceMs = atMs
        }
    }

    override fun onProductView(products: List<ContextProduct>) {
        synchronized(lock) {
            val viewed = (products + state.recentlyViewed).distinctBy { it.productId }.take(MAX_RECENTLY_VIEWED)
            update(state.copy(recentlyViewed = viewed))
        }
    }

    override fun onAddToCart(products: List<ContextProduct>) {
        synchronized(lock) {
            update(state.copy(recentlyAddedToCart = (products + state.recentlyAddedToCart).take(MAX_CART)))
        }
    }

    override fun onPurchase() {
        synchronized(lock) {
            update(state.copy(recentlyAddedToCart = emptyList()))
        }
    }

    override fun onLaunch(
        source: LaunchSource,
        utms: Map<UtmKey, String>,
        landingUrl: String?,
    ) {
        synchronized(lock) {
            val now = clock()
            enterForeground(now)
            val expiresAtMs = now + UTM_LIFETIME_MS
            update(
                state.copy(
                    launchSource = source,
                    utmParams = state.utmParams + utms.mapValues { UtmValue(it.value, expiresAtMs) },
                    landingUrl = landingUrl ?: state.landingUrl,
                ),
            )
        }
    }

    override fun onSessionStart() {
        synchronized(lock) {
            startSession(clock())
        }
    }

    override fun onForeground(atMs: Long) {
        synchronized(lock) {
            enterForeground(atMs)
        }
    }

    override fun onBackground(atMs: Long) {
        synchronized(lock) {
            val since = foregroundSinceMs ?: return
            foregroundSinceMs = null
            update(
                state.copy(
                    currentScreenElapsedMs = state.currentScreenElapsedMs + (atMs - since).coerceAtLeast(0),
                    lastActiveAtMs = atMs,
                ),
            )
        }
    }

    override fun clear() {
        synchronized(lock) {
            val now = clock()
            update(ContextState())
            if (foregroundSinceMs != null) {
                startSession(now)
            }
        }
    }

    /** Starts a session if none is running or the last one lapsed, then starts the screen timer. */
    private fun enterForeground(atMs: Long) {
        if (foregroundSinceMs != null) return
        val lastActive = state.lastActiveAtMs
        if (!state.sessionStarted || lastActive == null || atMs - lastActive >= SESSION_TIMEOUT_MS) {
            startSession(atMs)
        }
        foregroundSinceMs = atMs
    }

    private fun startSession(atMs: Long) {
        update(
            state.copy(
                sessionStarted = true,
                currentScreenName = null,
                previousScreenName = null,
                previousScreenDurationMs = null,
                screenIndex = 0,
                currentScreenElapsedMs = 0,
                launchSource = LaunchSource.ORGANIC,
                landingUrl = null,
                lastActiveAtMs = atMs,
            ),
        )
        if (foregroundSinceMs != null) {
            foregroundSinceMs = atMs
        }
    }

    private fun currentScreenElapsedMs(atMs: Long): Long =
        state.currentScreenElapsedMs + (foregroundSinceMs?.let { (atMs - it).coerceAtLeast(0) } ?: 0)

    private fun ContextState.toScreenContext() =
        ScreenContext(
            currentScreenName = currentScreenName,
            currentScreenUrl =
                if (currentScreenName != null && packageName != null) "app://$packageName/$currentScreenName" else null,
            previousScreenName = previousScreenName,
            previousScreenDurationMs = previousScreenDurationMs,
            screenIndex = screenIndex,
        )

    private fun ContextState.toProductContext() = ProductContext(recentlyViewed, recentlyAddedToCart)

    private fun ContextState.toLaunchContext(nowMs: Long) =
        LaunchContext(
            source = launchSource,
            utmParams = utmParams.filterValues { it.expiresAtMs > nowMs },
            landingUrl = landingUrl,
        )

    private fun update(newState: ContextState) {
        if (newState == state) return
        state = newState
        persistentStorage.save(STORAGE_KEY, json.encodeToString(ContextState.serializer(), newState))
    }

    private fun load(): ContextState {
        val stored = persistentStorage.read(STORAGE_KEY) ?: return ContextState()
        return try {
            json.decodeFromString(ContextState.serializer(), stored)
        } catch (e: Exception) {
            Timber.w(e, "Discarding unreadable context store")
            ContextState()
        }
    }

    companion object {
        internal const val STORAGE_KEY = "com.attentive.androidsdk.CONTEXT_STORE"
        internal const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L
        internal const val UTM_LIFETIME_MS = 30 * 60 * 1000L
        internal const val MAX_RECENTLY_VIEWED = 3
        internal const val MAX_CART = 20

        private val json = Json { ignoreUnknownKeys = true }
    }
}
