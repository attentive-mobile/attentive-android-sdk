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
import com.attentive.androidsdk.internal.network.RetrofitInboxApiService
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

    // Pagination management
    private val paginationLock = Mutex()
    private const val INBOX_PAGE_SIZE = 20

    /**
     * Initializes the inbox by fetching the first page from the server if configured,
     * otherwise falls back to mock data.
     */
    @SuppressLint("DefaultLocale")
    @VisibleForTesting
    internal fun initializeInbox() {
        val context = config.applicationContext
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName, PackageManager.GET_META_DATA,
        )
        val inboxBaseUrl = appInfo.metaData?.getString(INBOX_BASE_URL_META_KEY)
        if (inboxBaseUrl != null) {
            inboxApi = Retrofit.Builder()
                .baseUrl(inboxBaseUrl)
                .client(OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RetrofitInboxApiService::class.java)
            Timber.d("Inbox API configured with base URL: $inboxBaseUrl")
        }

        val inboxApi = this.inboxApi
        if (inboxApi != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = inboxApi.getMessages(0, INBOX_PAGE_SIZE)
                    _inboxState.value = InboxState(
                        messages = response.messages,
                        unreadCount = response.unreadCount,
                        currentOffset = response.messages.size,
                        hasMoreMessages = response.hasMoreMessages,
                    )
                    Timber.d("Initialized inbox from server with ${response.messages.size} messages")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch inbox from server, falling back to mock data")
                    initializeMockInbox()
                }
            }
        } else {
            initializeMockInbox()
        }
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
                    val response = inboxApi.getMessages(offsetToFetch, INBOX_PAGE_SIZE)
                    val latestState = _inboxState.value
                    val updatedMessages = latestState.messages + response.messages

                    _inboxState.value = InboxState(
                        messages = updatedMessages,
                        unreadCount = response.unreadCount,
                        isLoadingMore = false,
                        hasMoreMessages = response.hasMoreMessages,
                        currentOffset = offsetToFetch + response.messages.size,
                    )
                    Timber.d("Loaded ${response.messages.size} more messages from server. Total: ${updatedMessages.size}")
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
            initializeInbox()
        }
    }

    @Suppress("DEPRECATION")
    fun recordEvent(event: Event) {
        AttentiveEventTracker.instance.recordEvent(event)
    }

    suspend fun recordEventSuspend(event: Event): Result<Unit> {
        return AttentiveEventTracker.instance.recordEventSuspend(event)
    }

    @JvmStatic
    fun recordEventWithCallback(event: Event, callback: AttentiveCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            dispatchResult(recordEventSuspend(event), callback)
        }
    }

    /**
     * Determines whether the given Firebase RemoteMessage is from Attentive.
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
     * Forwards a push message to the SDK to display the notification.
     */
    fun sendNotification(remoteMessage: RemoteMessage) {
        AttentivePush.getInstance().sendNotification(remoteMessage)
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
     * Fetches the push token from Firebase, requesting permission if needed.
     */
    suspend fun getPushToken(
        application: Application,
        requestPermission: Boolean,
    ): Result<TokenFetchResult> {
        return AttentivePush.getInstance().fetchPushToken(application, requestPermission)
    }

    /***
     * Does the same as [getPushToken] but uses a callback instead of coroutines for Java interop.
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

        var number = trimmedPhone
        trimmedPhone?.let {
            if (it.isPhoneNumber().not()) {
                Timber.e("Invalid phone number: $trimmedPhone")
                number = null
            }
        }

        var validatedEmail = email
        email?.let {
            if (it.isNotEmpty() && it.isEmail().not()) {
                Timber.e("Invalid email: $email")
                validatedEmail = null
            }
        }

        if (validatedEmail.isNullOrEmpty() && number.isNullOrEmpty()) {
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
        return config.attentiveApi.sendUserUpdate(domain, trimmedEmail, number, visitorId, pushToken)
    }

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

    interface PushTokenCallback {
        fun onSuccess(result: TokenFetchResult)

        fun onFailure(exception: Exception)
    }

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

    fun isPushPermissionGranted(context: Context): Boolean {
        return AttentivePush.getInstance().checkPushPermission(context)
    }

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
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Inbox is not yet available for public use.",
        level = DeprecationLevel.WARNING,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getAllMessages(): List<Message> {
        return inboxState.value.messages
    }

    /**
     * Gets the count of unread messages from the current inbox state.
     * This is a lightweight operation that returns the current unread count.
     *
     * @return The number of unread messages
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Inbox is not yet available for public use.",
        level = DeprecationLevel.WARNING,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getUnreadCount(): Int {
        return inboxState.value.unreadCount
    }

    /**
     * Marks a message as read and emits a new inbox state.
     * TODO: This will send an update to the backend once the API is ready.
     *
     * @param messageId The ID of the message to mark as read
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Inbox is not yet available for public use.",
        level = DeprecationLevel.WARNING,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            CoroutineScope(Dispatchers.IO).launch {
                try { api.updateMessage(messageId, mapOf("isRead" to true)) }
                catch (e: Exception) { Timber.e(e, "Failed to sync markRead to server") }
            }
        }
    }

    /**
     * Marks a message as unread and emits a new inbox state.
     * TODO: This will send an update to the backend once the API is ready.
     *
     * @param messageId The ID of the message to mark as unread
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Inbox is not yet available for public use.",
        level = DeprecationLevel.WARNING,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            CoroutineScope(Dispatchers.IO).launch {
                try { api.updateMessage(messageId, mapOf("isRead" to false)) }
                catch (e: Exception) { Timber.e(e, "Failed to sync markUnread to server") }
            }
        }
    }

    /**
     * Deletes a message from the inbox and emits a new inbox state.
     * TODO: This will send a delete request to the backend once the API is ready.
     *
     * @param messageId The ID of the message to delete
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Inbox is not yet available for public use.",
        level = DeprecationLevel.WARNING,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
        val inboxApi = inboxApi
        if (inboxApi != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try { inboxApi.deleteMessage(messageId) }
                catch (e: Exception) { Timber.e(e, "Failed to sync deleteMessage to server") }
            }
        }
    }
}
