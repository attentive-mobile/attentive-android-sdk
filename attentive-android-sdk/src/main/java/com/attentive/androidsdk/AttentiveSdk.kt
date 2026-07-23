@file:Suppress("DEPRECATION")

package com.attentive.androidsdk

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo
import com.attentive.androidsdk.AttentiveSdk.getPushToken
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.inbox.InboxState
import com.attentive.androidsdk.inbox.Message
import com.attentive.androidsdk.inbox.Style
import com.attentive.androidsdk.internal.network.DeleteMessageRequest
import com.attentive.androidsdk.internal.network.GetMessagesRequest
import com.attentive.androidsdk.internal.network.InboxMessageDto
import com.attentive.androidsdk.internal.network.MarkMessagesReadRequest
import com.attentive.androidsdk.internal.network.RetrofitInboxApiService
import com.attentive.androidsdk.internal.network.TrackClickRequest
import com.attentive.androidsdk.internal.network.UnreadCountRequest
import com.attentive.androidsdk.internal.network.buffer.FlushWorker
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.androidsdk.internal.util.isEmail
import com.attentive.androidsdk.internal.util.isPhoneNumber
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.push.TokenFetchResult
import com.attentive.androidsdk.push.TokenProvider
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import org.jetbrains.annotations.VisibleForTesting
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

object AttentiveSdk {
    private var _config: AttentiveConfig? = null

    /**
     * Gets the initialized config. Throws if not initialized.
     * @throws IllegalStateException if the SDK has not been initialized
     */
    private val config: AttentiveConfig
        get() =
            _config ?: throw IllegalStateException(
                "AttentiveSdk must be initialized with AttentiveSdk.initialize(config) before calling this method. " +
                    "Please call AttentiveSdk.initialize() in your Application.onCreate() method.",
            )

    // Inbox state management
    private val _inboxState = MutableStateFlow(InboxState())

    /**
     * Subscribe to the inbox state stream to receive updates when messages change.
     * This StateFlow emits a new InboxState whenever messages are updated.
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Inbox is not yet available for public use.",
        level = DeprecationLevel.WARNING,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val inboxState: StateFlow<InboxState> = _inboxState.asStateFlow()

    // Inbox server API (created from manifest meta-data if present)
    private var inboxApi: RetrofitInboxApiService? = null

    private const val INBOX_BASE_URL_META_KEY = "com.attentive.sdk.INBOX_BASE_URL"

    // Inbox endpoints — paths relative to the host configured via INBOX_BASE_URL_META_KEY.
    private const val DEFAULT_INBOX_HOST = "https://mobile.attentivemobile.com/"
    private var inboxHost: String = DEFAULT_INBOX_HOST
    private val inboxMessagesUrl get() = "${inboxHost.trimEnd('/')}/inbox/messages"
    private val inboxUnreadCountUrl get() = "${inboxHost.trimEnd('/')}/inbox/messages/unread/count"
    private val inboxMessagesReadUrl get() = "${inboxHost.trimEnd('/')}/inbox/messages/read"
    private val inboxMessagesUnreadUrl get() = "${inboxHost.trimEnd('/')}/inbox/messages/unread"
    private val inboxEventsClickedUrl get() = "${inboxHost.trimEnd('/')}/inbox/events/clicked"

    // Pagination management
    private val paginationLock = Mutex()
    private const val INBOX_PAGE_SIZE = 20
    private var nextPageToken: String? = null

    /**
     * Initializes the inbox by fetching the first page from the server if configured,
     * otherwise falls back to mock data.
     */
    @SuppressLint("DefaultLocale")
    internal fun initializeInbox() {
        if (inboxApi != null) return
        val context = config.applicationContext
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName, PackageManager.GET_META_DATA,
        )
        val inboxBaseUrl = DEFAULT_INBOX_HOST
        inboxHost = inboxBaseUrl
        val client = ClassFactory.buildOkHttpClient(
            config.logLevel,
            ClassFactory.buildUserAgentInterceptor(context),
        )
        inboxApi = Retrofit.Builder()
            .baseUrl(inboxBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RetrofitInboxApiService::class.java)
        Timber.d("Inbox API configured with base URL: $inboxBaseUrl")

        CoroutineScope(Dispatchers.IO).launch { refreshInbox() }
    }

    /**
     * Refetches the first page of inbox messages and the unread count, replacing
     * [inboxState]. Safe to call repeatedly (e.g., on screen resume or push receipt).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun refreshInbox() {
        val inboxApi = inboxApi ?: run {
            Timber.d("Skipping refreshInbox — inbox API not configured")
            return
        }
        try {
            val request = buildGetMessagesRequest(pageToken = null) ?: run {
                Timber.w("Skipping inbox refresh — no visitor id")
                return
            }
            val response = inboxApi.getMessages(inboxMessagesUrl, request)
            val messages = response.messages.map { it.toMessage() }
            nextPageToken = response.nextPageToken
            _inboxState.value = InboxState(
                messages = messages,
                unreadCount = messages.count { !it.isRead },
                currentOffset = messages.size,
                hasMoreMessages = response.nextPageToken != null,
            )
            Timber.d("Refreshed inbox from server with ${messages.size} messages")
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh inbox from server")
            if (_inboxState.value.messages.isEmpty()) initializeMockInbox()
        }
        refreshInboxUnreadCount()
    }

    private fun fcmPushToken(): String? =
        TokenProvider.getInstance().token?.takeIf { it.isNotBlank() }?.let { "fcm:$it" }

    private fun buildGetMessagesRequest(pageToken: String?): GetMessagesRequest? {
        val identifiers = config.userIdentifiers
        val visitorId = identifiers.visitorId ?: return null
        val pushToken = fcmPushToken()
        return GetMessagesRequest(
            domain = config.domain,
            visitorId = visitorId,
            pushToken = pushToken,
            email = identifiers.email?.trim()?.takeIf { it.isNotBlank() },
            phone = identifiers.phone?.trim()?.takeIf { it.isNotBlank() },
            pageSize = INBOX_PAGE_SIZE,
            pageToken = pageToken,
        )
    }

    private fun InboxMessageDto.toMessage(): Message {
        val timestampMs = sentAt?.let { parseIso8601ToMillis(it) } ?: 0L
        return Message(
            id = inboxMessageId,
            title = title.orEmpty(),
            body = body.orEmpty(),
            timestamp = timestampMs,
            isRead = isRead,
            imageUrl = imageUrl,
            style = if (imageUrl != null) Style.Large else Style.Small,
        )
    }

    private fun parseIso8601ToMillis(iso: String): Long {
        // Handles "2026-05-01T12:00:00Z" and ISO timestamps with optional fractional seconds.
        val formats = arrayOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX")
        for (pattern in formats) {
            try {
                return java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(iso)?.time ?: continue
            } catch (_: Exception) { /* try next */ }
        }
        Timber.w("Failed to parse timestamp: $iso")
        return 0L
    }

    @SuppressLint("DefaultLocale")
    private fun initializeMockInbox() {
        val originalMessages =
            listOf(
                Message(
                    id = "msg_001",
                    title = "Welcome to Attentive!",
                    body = "Thanks for joining us. Check out our latest offers.",
                    timestamp = System.currentTimeMillis() - 86400000,
                    isRead = false,
                    actionUrl = "https://example.com/offers",
                    style = Style.Small,
                ),
                Message(
                    id = "msg_002",
                    title = "New Sale Alert",
                    body = "50% off on all items this weekend!",
                    timestamp = System.currentTimeMillis() - 172800000,
                    isRead = true,
                    imageUrl = "https://as1.ftcdn.net/v2/jpg/03/98/30/92/1000_F_398309275_84cKyqzV2RLTbYmBtt0dzpZkEvqapPZo.jpg",
                    actionUrl = "https://example.com/sale",
                    style = Style.Small,
                ),
                Message(
                    id = "msg_003",
                    title = "Your Order Has Shipped",
                    body = "Your order #12345 is on its way!",
                    timestamp = System.currentTimeMillis() - 259200000,
                    isRead = false,
                    actionUrl = "https://shippingeasy.com/wp-content/uploads/2021/04/Easy_Graphics_USPS-Priority-Mail-Blog-01.png",
                    imageUrl = "https://shippingeasy.com/wp-content/uploads/2021/04/Easy_Graphics_USPS-Priority-Mail-Blog-01.png",
                    style = Style.Large,
                ),
                Message(
                    id = "msg_004",
                    title = "Your cart is waiting",
                    body = "Pickup where you left off!",
                    timestamp = System.currentTimeMillis() - 259200000,
                    isRead = false,
                    actionUrl = "bonni://cart",
                    style = Style.Small,
                ),
            )

        val generatedMessages =
            List(16) { index ->
                val messageNumber = index + 5
                Message(
                    id = "msg_${String.format("%03d", messageNumber)}",
                    title = "Message $messageNumber",
                    body = "This is the content of message number $messageNumber",
                    timestamp = System.currentTimeMillis() - (index + 4) * 3600000L,
                    isRead = messageNumber % 3 == 0,
                    imageUrl = if (messageNumber % 5 == 0) "https://picsum.photos/200/300?random=$messageNumber" else null,
                    style = if (messageNumber % 5 == 0) Style.Large else Style.Small,
                )
            }

        val mockMessages = originalMessages + generatedMessages

        _inboxState.value =
            InboxState(
                messages = mockMessages,
                unreadCount = mockMessages.count { !it.isRead },
                currentOffset = mockMessages.size,
                hasMoreMessages = true,
            )

        Timber.d("Initialized inbox with ${mockMessages.size} mock messages (4 original + 16 generated)")
    }

    /**
     * Loads more inbox messages (pagination).
     * Call this when the user scrolls near the end of the message list.
     * Uses the server API when configured, otherwise falls back to mock data.
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Inbox is not yet available for public use.",
        level = DeprecationLevel.WARNING,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun loadMoreInboxMessages() {
        paginationLock.withLock {
            val currentState = _inboxState.value

            if (currentState.isLoadingMore || !currentState.hasMoreMessages) {
                Timber.d(
                    "Skipping loadMoreInboxMessages - isLoadingMore: ${currentState.isLoadingMore}, hasMoreMessages: ${currentState.hasMoreMessages}",
                )
                return
            }

            Timber.d("Loading more inbox messages from offset ${currentState.currentOffset}")
            _inboxState.value = currentState.copy(isLoadingMore = true)

            try {
                val offsetToFetch = currentState.currentOffset
                val inboxApi = inboxApi

                if (inboxApi != null) {
                    val request = buildGetMessagesRequest(pageToken = nextPageToken) ?: run {
                        Timber.w("Skipping loadMore — no visitor id")
                        return@withLock
                    }
                    val response = inboxApi.getMessages(inboxMessagesUrl, request)
                    val newMessages = response.messages.map { it.toMessage() }
                    nextPageToken = response.nextPageToken
                    val latestState = _inboxState.value
                    val updatedMessages = latestState.messages + newMessages

                    _inboxState.value = InboxState(
                        messages = updatedMessages,
                        unreadCount = updatedMessages.count { !it.isRead },
                        isLoadingMore = false,
                        hasMoreMessages = response.nextPageToken != null,
                        currentOffset = offsetToFetch + newMessages.size,
                    )
                    Timber.d("Loaded ${newMessages.size} more messages from server. Total: ${updatedMessages.size}")
                } else {
                    delay(5000)

                    val mockMessages =
                        List(INBOX_PAGE_SIZE) { index ->
                            Message(
                                id = "msg_${offsetToFetch + index}",
                                title = "Message ${offsetToFetch + index + 1}",
                                body = "This is the content of message number ${offsetToFetch + index + 1}",
                                timestamp = System.currentTimeMillis() - (offsetToFetch + index) * 3600000L,
                                isRead = (offsetToFetch + index) % 3 == 0,
                                imageUrl = if ((offsetToFetch + index) % 5 == 0) "https://picsum.photos/200/300?random=${offsetToFetch + index}" else null,
                                style = if ((offsetToFetch + index) % 5 == 0) Style.Large else Style.Small,
                            )
                        }

                    val latestState = _inboxState.value
                    val updatedMessages = latestState.messages + mockMessages
                    val totalMockMessages = 100
                    val hasMore = updatedMessages.size < totalMockMessages

                    _inboxState.value = InboxState(
                        messages = updatedMessages,
                        unreadCount = updatedMessages.count { !it.isRead },
                        isLoadingMore = false,
                        hasMoreMessages = hasMore,
                        currentOffset = offsetToFetch + mockMessages.size,
                    )
                    Timber.d("Loaded ${mockMessages.size} more mock messages. Total: ${updatedMessages.size}, hasMore: $hasMore")
                }
            } finally {
                if (_inboxState.value.isLoadingMore) {
                    _inboxState.value = _inboxState.value.copy(isLoadingMore = false)
                }
            }
        }
    }

    /**
     * Initializes the Attentive SDK with the provided configuration.
     * This should be called once during app initialization, typically in your Application.onCreate() method.
     *
     * @param config The AttentiveConfig containing domain, mode, and other SDK settings
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun initialize(config: AttentiveConfig) {
        synchronized(AttentiveSdk::class.java) {
            this._config = config
            AttentiveEventTracker.instance.initializeInternal(config)
            FlushWorker.recoverOrphansAndSchedule(config.applicationContext)
        }
    }

    @get:JvmStatic
    val domain: String
        get() = config.domain

    /**
     * Records an analytics event with Attentive in a fire-and-forget manner. Errors are
     * logged but not surfaced to the caller. For coroutine-aware error handling, use
     * [recordEventSuspend].
     *
     * @param event The [Event] to record (e.g. [com.attentive.androidsdk.events.ProductViewEvent],
     *   [com.attentive.androidsdk.events.AddToCartEvent], [com.attentive.androidsdk.events.PurchaseEvent]).
     */
    @Suppress("DEPRECATION")
    fun recordEvent(event: Event) {
        AttentiveEventTracker.instance.recordEvent(event)
    }

    /**
     * Records an analytics event with Attentive and suspends until the request completes.
     */
    suspend fun recordEventSuspend(event: Event): Result<Unit> {
        return AttentiveEventTracker.instance.recordEventSuspend(event)
    }

    /**
     * Callback-based variant of [recordEventSuspend] for Java interop.
     */
    @JvmStatic
    fun recordEventWithCallback(event: Event, callback: AttentiveCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            dispatchResult(recordEventSuspend(event), callback)
        }
    }

    /**
     * Determines whether the given Firebase [RemoteMessage] was sent by Attentive.
     * Use this in your FirebaseMessagingService to route messages to [sendNotification]
     * while leaving other push messages to your own handling.
     */
    fun isAttentiveFirebaseMessage(remoteMessage: RemoteMessage): Boolean {
        Timber.d(
            "%s%s",
            "Checking if message is from Attentive - data: ${remoteMessage.data}, ",
            "title: ${remoteMessage.notification?.title}, body: ${remoteMessage.notification?.body}",
        )

        val isAttentiveMessage =
            remoteMessage.data.containsKey(Constants.Companion.KEY_NOTIFICATION_TITLE) ||
                remoteMessage.data.containsKey("attentiveData")

        Timber.d("isAttentiveMessage: $isAttentiveMessage")
        return isAttentiveMessage
    }

    /**
     * Subscribes the user to Attentive marketing on email and/or SMS.
     *
     * Unlike [updateUser] and [clearUser], this does not change the visitor ID. It creates
     * a subscription record on the backend; repeated calls with the same identifier are
     * safe and will not create duplicates. Invalid email/phone values are dropped; if both
     * become blank after validation the call returns a failure.
     *
     * @param email Email address. Optional if [phoneNumber] is provided.
     * @param phoneNumber Phone number in E.164 format. Optional if [email] is provided.
     */
    suspend fun optUserIntoMarketingSubscription(
        email: String = "",
        phoneNumber: String = "",
    ): Result<Unit> {
        var validPhone = phoneNumber
        if (phoneNumber.isNotBlank() && phoneNumber.isPhoneNumber().not()) {
            Timber.e("Invalid phone number: $phoneNumber")
            validPhone = ""
        }
        var validEmail = email
        if (email.isNotBlank() && email.isEmail().not()) {
            Timber.e("Invalid email: $email")
            validEmail = ""
        }
        if (validPhone.isBlank() && validEmail.isBlank()) {
            val msg = "No valid email or phone number provided."
            Timber.e(msg)
            return Result.failure(IllegalArgumentException(msg))
        }

        return AttentiveEventTracker.instance.optIn(validEmail, validPhone)
    }

    /**
     * Callback-based variant of [optUserIntoMarketingSubscription] for Java interop.
     */
    @JvmStatic
    fun optUserIntoMarketingSubscriptionWithCallback(
        email: String = "",
        phoneNumber: String = "",
        callback: AttentiveCallback,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            dispatchResult(optUserIntoMarketingSubscription(email, phoneNumber), callback)
        }
    }

    /**
     * Unsubscribes the user from Attentive marketing on email and/or SMS. Safe to call
     * repeatedly with the same identifier. At least one of [email] or [phoneNumber] must
     * be provided.
     */
    suspend fun optUserOutOfMarketingSubscription(
        email: String = "",
        phoneNumber: String = "",
    ): Result<Unit> {
        var validPhone = phoneNumber
        if (phoneNumber.isNotBlank() && phoneNumber.isPhoneNumber().not()) {
            Timber.e("Invalid phone number: $phoneNumber")
            validPhone = ""
        }
        var validEmail = email
        if (email.isNotBlank() && email.isEmail().not()) {
            Timber.e("Invalid email: $email")
            validEmail = ""
        }
        if (validPhone.isBlank() && validEmail.isBlank()) {
            val msg = "No valid email or phone number provided."
            Timber.e(msg)
            return Result.failure(IllegalArgumentException(msg))
        }

        return AttentiveEventTracker.instance.optOut(validEmail, validPhone)
    }

    /**
     * Callback-based variant of [optUserOutOfMarketingSubscription] for Java interop.
     */
    @JvmStatic
    fun optUserOutOfMarketingSubscriptionWithCallback(
        email: String = "",
        phoneNumber: String = "",
        callback: AttentiveCallback,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            dispatchResult(optUserOutOfMarketingSubscription(email, phoneNumber), callback)
        }
    }

    /**
     * Forwards an Attentive push message to the SDK to build and display the notification.
     * Call this from your own [com.google.firebase.messaging.FirebaseMessagingService] after
     * [isAttentiveFirebaseMessage] returns `true`.
     *
     * If your app does not declare its own `FirebaseMessagingService` subclass, you do not
     * need to call this — the SDK's built-in service receives Attentive messages and
     * displays the notification automatically.
     */
    fun sendNotification(remoteMessage: RemoteMessage) {
        if (!AttentiveEventTracker.instance.isPushEnabled()) {
            Timber.d("Push is disabled via AttentiveConfig.Builder.pushEnabled(false); dropping incoming notification")
            return
        }
        AttentivePush.getInstance().sendNotification(remoteMessage)
        if (inboxApi != null) {
            CoroutineScope(Dispatchers.IO).launch { refreshInbox() }
        }
    }

    @VisibleForTesting
    fun sendMockNotification(
        title: String,
        body: String,
        dataMap: Map<String, String>,
        notificationIconId: Int = 0,
        application: Application,
    ) {
        AttentivePush.getInstance().sendNotification(
            messageTitle = title,
            messageBody = body,
            dataMap = dataMap,
            context = application,
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/Cat_November_2010-1a.jpg/960px-Cat_November_2010-1a.jpg",
        )
    }

    /**
     * Fetches the current FCM push token, optionally prompting the user for notification
     * permission first.
     *
     * @param application Application context, used to request permission if needed.
     * @param requestPermission If `true`, prompts for `POST_NOTIFICATIONS` on Android 13+
     *   before fetching.
     */
    suspend fun getPushToken(
        application: Application,
        requestPermission: Boolean,
    ): Result<TokenFetchResult> {
        return AttentivePush.getInstance().fetchPushToken(application, requestPermission)
    }

    /**
     * Callback-based variant of [getPushToken] for Java interop.
     */
    @JvmStatic
    fun getPushTokenWithCallback(
        application: Application,
        requestPermission: Boolean,
        callback: PushTokenCallback,
    ) {
        Timber.d("Synchronously fetching push token with requestPermission: $requestPermission")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result =
                    AttentivePush.getInstance().fetchPushToken(application, requestPermission)
                result.getOrNull()?.let {
                    Timber.d("Push token fetched successfully: ${it.token}")
                    callback.onSuccess(it)
                } ?: run {
                    Timber.e("Push token fetch result is null")
                    callback.onFailure(Exception("Push token fetch result is null"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch push token")
                callback.onFailure(e)
            }
        }
    }

    /**
     * Switches the current device identity to a different user (fire-and-forget variant).
     *
     * Semantically a "login as different user" operation. The SDK resets the local visitor
     * ID and fires `POST /user-update` to detach the push token from the prior user and
     * re-associate it with the new one. **Requires an FCM push token** — if none is
     * available, the network call is skipped (see
     * [MSDK-345](https://attentivemobile.atlassian.net/browse/MSDK-345)).
     *
     * Errors are logged but not surfaced. Use [updateUserSuspend] for coroutine-aware
     * error handling.
     */
    fun updateUser(
        email: String? = null,
        phoneNumber: String? = null,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = updateUserSuspend(email, phoneNumber)
            if (result.isFailure) {
                Timber.e("updateUser failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Suspend variant of [updateUser]. Returns success or wrapped failure once the network
     * call completes.
     */
    suspend fun updateUserSuspend(
        email: String? = null,
        phoneNumber: String? = null,
    ): Result<Unit> {
        if (_config == null) {
            val msg = "AttentiveSdk must be initialized before calling updateUserSuspend"
            Timber.e(msg)
            return Result.failure(IllegalStateException(msg))
        }

        val trimmedEmail = email?.trim()?.ifBlank { null }
        val trimmedPhone = phoneNumber?.trim()?.ifBlank { null }

        if (trimmedEmail.isNullOrEmpty() && trimmedPhone.isNullOrEmpty()) {
            val msg = "Both email and phone number are empty or null. At least one must be provided to update the user."
            Timber.e(msg)
            return Result.failure(IllegalArgumentException(msg))
        }

        var validatedNumber = trimmedPhone
        trimmedPhone?.let {
            if (it.isPhoneNumber().not()) {
                Timber.e("Invalid phone number: $trimmedPhone")
                validatedNumber = null
            }
        }

        var validatedEmail = trimmedEmail
        trimmedEmail?.let {
            if (it.isEmail().not()) {
                Timber.e("Invalid email: $trimmedEmail")
                validatedEmail = null
            }
        }

        if (validatedEmail.isNullOrEmpty() && validatedNumber.isNullOrEmpty()) {
            val msg = "No valid email or phone number provided after validation."
            Timber.e(msg)
            return Result.failure(IllegalArgumentException(msg))
        }

        config.resetIdentifiers()
        val domain = config.domain
        val visitorId = config.userIdentifiers.visitorId
        val pushToken = TokenProvider.getInstance().token
        if (visitorId == null || pushToken == null) {
            Timber.w("Skipping user update network call: visitorId=$visitorId, pushToken=$pushToken")
            return Result.failure(IllegalArgumentException("Visitor id $visitorId and pushToken $pushToken must not be null"))
        }
        return config.attentiveApi.sendUserUpdate(domain, validatedEmail, validatedNumber, visitorId, pushToken)
    }

    /**
     * Callback-based variant of [updateUserSuspend] for Java interop.
     */
    @JvmStatic
    fun updateUserWithCallback(
        email: String? = null,
        phoneNumber: String? = null,
        callback: AttentiveCallback,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            dispatchResult(updateUserSuspend(email, phoneNumber), callback)
        }
    }

    /**
     * Logs the current user out. Resets local visitor identity and tells the backend to
     * detach the push token from the prior user.
     *
     * Strongly recommended on logout — without this the push token remains associated with
     * the logged-out user on the backend and they may continue to receive targeted marketing
     * on this device.
     *
     * Prefer this over the deprecated `AttentiveConfig.clearUser()`, which only clears local
     * state.
     */
    fun clearUser() {
        config.resetIdentifiers()
        val domain = config.domain
        val visitorId = config.userIdentifiers.visitorId
        val pushToken = TokenProvider.getInstance().token
        if (visitorId == null || pushToken == null) {
            Timber.w("Skipping clear user: visitorId=$visitorId, pushToken=$pushToken")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            config.attentiveApi.sendUserUpdate(domain, null, null, visitorId, pushToken, logLabel = "clear user")
        }
    }

    /**
     * Callback for [getPushTokenWithCallback] (Java interop).
     */
    interface PushTokenCallback {
        fun onSuccess(result: TokenFetchResult)

        fun onFailure(exception: Exception)
    }

    /**
     * Generic success/failure callback used by `*WithCallback` variants for Java interop.
     */
    interface AttentiveCallback {
        fun onSuccess()

        fun onFailure(exception: Exception)
    }

    private fun dispatchResult(result: Result<Unit>, callback: AttentiveCallback) {
        result.fold(
            onSuccess = { callback.onSuccess() },
            onFailure = { callback.onFailure(it as? Exception ?: Exception(it)) },
        )
    }

    /**
     * Whether the user has granted notification permission to the app.
     */
    fun isPushPermissionGranted(context: Context): Boolean {
        return AttentivePush.getInstance().checkPushPermission(context)
    }

    /**
     * Re-registers the current push token with Attentive to reflect the latest permission
     * state. Call this after the user changes notification permission (e.g. returning from
     * system settings). Also called automatically when the app is brought to the foreground,
     * so manual invocation is only needed when you want immediate sync.
     */
    fun updatePushPermissionStatus(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            AttentiveEventTracker.instance.registerPushToken(context)
        }
    }

    // Inbox Message Functions

    /**
     * Gets all messages from the current inbox state.
     * This is a lightweight operation that returns the current snapshot of messages.
     * Call this function every time the app comes to the foreground.
     *
     * @return List of all messages in the inbox
     */
    fun getAllMessages(): List<Message> {
        return inboxState.value.messages
    }

    /**
     * Refreshes the unread inbox message count from the server and updates [inboxState].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun refreshInboxUnreadCount() {
        val inboxApi = inboxApi ?: run {
            Timber.d("Skipping refreshInboxUnreadCount — inbox API not configured")
            return
        }
        val identifiers = config.userIdentifiers
        val visitorId = identifiers.visitorId ?: run {
            Timber.w("Skipping refreshInboxUnreadCount — visitor id is null")
            return
        }
        val pushToken = fcmPushToken()
        try {
            val response = inboxApi.getUnreadCount(
                url = inboxUnreadCountUrl,
                body = UnreadCountRequest(
                    domain = config.domain,
                    visitorId = visitorId,
                    pushToken = pushToken,
                    email = identifiers.email?.trim()?.takeIf { it.isNotBlank() },
                    phone = identifiers.phone?.trim()?.takeIf { it.isNotBlank() },
                ),
            )
            _inboxState.value = _inboxState.value.copy(unreadCount = response.unreadCount)
            Timber.d("Inbox unread count refreshed: ${response.unreadCount}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh inbox unread count")
        }
    }

    /**
     * Gets the count of unread messages from the current inbox state.
     *
     * The first call opts this app in to the inbox: it lazily initializes the inbox
     * API client and kicks off a background fetch of the first page of messages plus
     * the unread count. That fetch is async, so the first invocation returns the
     * current snapshot (0 on cold start); callers observing [inboxState] will see
     * the real count emit shortly after. Subsequent calls are cheap — inbox
     * initialization is idempotent.
     *
     * @return The number of unread messages currently in state
     */
    fun getUnreadCount(): Int {
        initializeInbox()
        return inboxState.value.unreadCount
    }

    /**
     * Marks a message as read and emits a new inbox state.*
     * @param messageId The ID of the message to mark as read
     */
    fun markRead(messageId: String) {
        val currentState = _inboxState.value
        val updatedMessages =
            currentState.messages.map { message ->
                if (message.id == messageId) {
                    message.copy(isRead = true)
                } else {
                    message
                }
            }
        _inboxState.value =
            currentState.copy(
                messages = updatedMessages,
                unreadCount = updatedMessages.count { !it.isRead },
            )
        Timber.d("Message $messageId marked as read")
        inboxApi?.also { api ->
            val visitorId = config.userIdentifiers.visitorId
            if (visitorId == null) {
                Timber.w("Skipping markRead network call — visitor id is null")
                return@also
            }
            val pushToken = fcmPushToken()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.markMessagesRead(
                        url = inboxMessagesReadUrl,
                        body = MarkMessagesReadRequest(
                            domain = config.domain,
                            visitorId = visitorId,
                            pushToken = pushToken,
                            messageIds = listOf(messageId),
                        ),
                    )
                    Timber.d("markMessagesRead response: $response")
                    response.unreadCount?.let { serverCount ->
                        _inboxState.value = _inboxState.value.copy(unreadCount = serverCount)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync markRead to server")
                }
            }
        }
    }

    /**
     * Marks a message as unread and emits a new inbox state.
     * @param messageId The ID of the message to mark as unread
     */
    fun markUnread(messageId: String) {
        val currentState = _inboxState.value
        val updatedMessages =
            currentState.messages.map { message ->
                if (message.id == messageId) {
                    message.copy(isRead = false)
                } else {
                    message
                }
            }
        _inboxState.value =
            currentState.copy(
                messages = updatedMessages,
                unreadCount = updatedMessages.count { !it.isRead },
            )
        Timber.d("Message $messageId marked as unread")
        inboxApi?.also { api ->
            val visitorId = config.userIdentifiers.visitorId
            if (visitorId == null) {
                Timber.w("Skipping markUnread network call — visitor id is null")
                return@also
            }
            val pushToken = fcmPushToken()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.markMessagesUnread(
                        url = inboxMessagesUnreadUrl,
                        body = MarkMessagesReadRequest(
                            domain = config.domain,
                            visitorId = visitorId,
                            pushToken = pushToken,
                            messageIds = listOf(messageId),
                        ),
                    )
                    response.unreadCount?.let { serverCount ->
                        _inboxState.value = _inboxState.value.copy(unreadCount = serverCount)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync markUnread to server")
                }
            }
        }
    }

    /**
     * Deletes a message from the inbox and emits a new inbox state.*
     * @param messageId The ID of the message to delete
     */
    fun deleteMessage(messageId: String) {
        val currentState = _inboxState.value
        val updatedMessages =
            currentState.messages.filter { message ->
                message.id != messageId
            }
        _inboxState.value =
            currentState.copy(
                messages = updatedMessages,
                unreadCount = updatedMessages.count { !it.isRead },
            )
        Timber.d("Message $messageId deleted from inbox")
        val inboxApi = inboxApi ?: return
        val visitorId = config.userIdentifiers.visitorId
        if (visitorId == null) {
            Timber.w("Skipping deleteMessage network call — visitor id is null")
            return
        }
        val pushToken = fcmPushToken()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                inboxApi.deleteMessage(
                    url = "$inboxMessagesUrl/$messageId",
                    body = DeleteMessageRequest(domain = config.domain, visitorId = visitorId, pushToken = pushToken),
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync deleteMessage to server")
            }
        }
    }

    /**
     * Reports a click on an inbox message link to the backend. Fire-and-forget.
     */
    fun trackInboxClick(messageId: String, actionUrl: String? = null) {
        val inboxApi = inboxApi ?: return
        val visitorId = config.userIdentifiers.visitorId
        if (visitorId == null) {
            Timber.w("Skipping trackInboxClick — visitor id is null")
            return
        }
        val pushToken = fcmPushToken()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                inboxApi.trackClick(
                    url = inboxEventsClickedUrl,
                    body = TrackClickRequest(
                        domain = config.domain,
                        visitorId = visitorId,
                        pushToken = pushToken,
                        messageId = messageId,
                        actionUrl = actionUrl,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to track inbox click")
            }
        }
    }
}
