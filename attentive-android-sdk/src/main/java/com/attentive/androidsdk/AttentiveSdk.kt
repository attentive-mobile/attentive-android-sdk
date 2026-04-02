package com.attentive.androidsdk

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.attentive.androidsdk.AttentiveSdk.getPushToken
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.inbox.InboxState
import com.attentive.androidsdk.inbox.Message
import com.attentive.androidsdk.inbox.Style
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.androidsdk.internal.util.isPhoneNumber
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.push.TokenFetchResult
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
import org.jetbrains.annotations.VisibleForTesting
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
    val inboxState: StateFlow<InboxState> = _inboxState.asStateFlow()

    // Pagination management
    private val paginationLock = Mutex()
    private const val INBOX_PAGE_SIZE = 20

    /**
     * Initializes the inbox with mock messages for local testing.
     * Starts with original 4 messages, then generates 16 more for a total of 20 (first page).
     * TODO: Remove this function once the backend API is ready.
     */
    @SuppressLint("DefaultLocale")
    @VisibleForTesting
    internal fun initializeMockInbox() {
        // Original 4 messages
        val originalMessages =
            listOf(
                Message(
                    id = "msg_001",
                    title = "Welcome to Attentive!",
                    body = "Thanks for joining us. Check out our latest offers.",
                    timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                    isRead = false,
                    actionUrl = "https://example.com/offers",
                    style = Style.Small,
                ),
                Message(
                    id = "msg_002",
                    title = "New Sale Alert",
                    body = "50% off on all items this weekend!",
                    timestamp = System.currentTimeMillis() - 172800000, // 2 days ago
                    isRead = true,
                    imageUrl = "https://as1.ftcdn.net/v2/jpg/03/98/30/92/1000_F_398309275_84cKyqzV2RLTbYmBtt0dzpZkEvqapPZo.jpg",
                    actionUrl = "https://example.com/sale",
                    style = Style.Small,
                ),
                Message(
                    id = "msg_003",
                    title = "Your Order Has Shipped",
                    body = "Your order #12345 is on its way!",
                    timestamp = System.currentTimeMillis() - 259200000, // 3 days ago
                    isRead = false,
                    actionUrl = "https://shippingeasy.com/wp-content/uploads/2021/04/Easy_Graphics_USPS-Priority-Mail-Blog-01.png",
                    imageUrl = "https://shippingeasy.com/wp-content/uploads/2021/04/Easy_Graphics_USPS-Priority-Mail-Blog-01.png",
                    style = Style.Large,
                ),
                Message(
                    id = "msg_004",
                    title = "Your cart is waiting",
                    body = "Pickup where you left off!",
                    timestamp = System.currentTimeMillis() - 259200000, // 3 days ago
                    isRead = false,
                    actionUrl = "bonni://cart",
                    style = Style.Small,
                ),
            )

        // Generate 16 additional messages to complete the first page (total 20)
        val generatedMessages =
            List(16) { index ->
                val messageNumber = index + 5 // Start from 5 since we have 4 original
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
                currentOffset = mockMessages.size, // Start offset = 20
                hasMoreMessages = true, // More messages available to load
            )

        Timber.d("Initialized inbox with ${mockMessages.size} mock messages (4 original + 16 generated)")
    }

    /**
     * Loads more inbox messages (pagination).
     * Call this when the user scrolls near the end of the message list.
     * This function uses mock data until the backend API is ready.
     */
    suspend fun loadMoreInboxMessages() {
        paginationLock.withLock {
            val currentState = _inboxState.value

            // Guard: already loading or no more messages
            if (currentState.isLoadingMore || !currentState.hasMoreMessages) {
                Timber.d(
                    "Skipping loadMoreInboxMessages - isLoadingMore: ${currentState.isLoadingMore}, hasMoreMessages: ${currentState.hasMoreMessages}",
                )
                return
            }

            Timber.d("Loading more inbox messages from offset ${currentState.currentOffset}")
            _inboxState.value = currentState.copy(isLoadingMore = true)

            try {
                // Capture offset before suspending - this determines which page to fetch
                val offsetToFetch = currentState.currentOffset

                // Simulate network delay
                delay(5000)

                // Generate mock messages based on the offset we requested
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

                // Merge with the CURRENT state to preserve
                // any changes to the made during the network call
                // like deletions
                val latestState = _inboxState.value
                val updatedMessages = latestState.messages + mockMessages
                val totalMockMessages = 100 // Mock total
                // todo hasMore pagination value should come from backend
                val hasMore = updatedMessages.size < totalMockMessages

                _inboxState.value =
                    InboxState(
                        messages = updatedMessages,
                        unreadCount = updatedMessages.count { !it.isRead },
                        isLoadingMore = false,
                        hasMoreMessages = hasMore,
                        currentOffset = offsetToFetch + mockMessages.size,
                    )

                Timber.d("Loaded ${mockMessages.size} more messages. Total: ${updatedMessages.size}, hasMore: $hasMore")
            } finally {
                // Clear isLoadingMore when loading fails via cancellation or exception
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
    @JvmStatic
    fun initialize(config: AttentiveConfig) {
        synchronized(AttentiveSdk::class.java) {
            this._config = config
            AttentiveEventTracker.instance.initializeInternal(config)
            initializeMockInbox()
        }
    }

    fun recordEvent(event: Event) {
        AttentiveEventTracker.instance.recordEvent(event)
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
    ) {
        if (phoneNumber.isNotBlank() && phoneNumber.isPhoneNumber().not()) {
            Timber.e("Invalid phone number: $phoneNumber")
        }

        AttentiveEventTracker.instance.optIn(email, phoneNumber)
    }

    suspend fun optUserOutOfMarketingSubscription(
        email: String = "",
        phoneNumber: String = "",
    ) {
        if (phoneNumber.isNotBlank() && phoneNumber.isPhoneNumber().not()) {
            Timber.e("Invalid phone number: $phoneNumber")
        }
        AttentiveEventTracker.instance.optOut(email, phoneNumber)
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
        if (email.isNullOrEmpty() && phoneNumber.isNullOrEmpty()) {
            Timber.e("Both email and phone number are empty or null. At least one must be provided to update the user.")
            return
        }

        var number = phoneNumber
        phoneNumber?.let {
            if (it.isPhoneNumber().not()) {
                Timber.e("Invalid phone number: $phoneNumber")
                number = null
            }
        }

        val domain = config.domain
        CoroutineScope(Dispatchers.IO).launch {
            config.attentiveApi.sendUserUpdate(domain, email, number)
        }
    }

    interface PushTokenCallback {
        fun onSuccess(result: TokenFetchResult)

        fun onFailure(exception: Exception)
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
    fun getAllMessages(): List<Message> {
        return inboxState.value.messages
    }

    /**
     * Gets the count of unread messages from the current inbox state.
     * This is a lightweight operation that returns the current unread count.
     *
     * @return The number of unread messages
     */
    fun getUnreadCount(): Int {
        return inboxState.value.unreadCount
    }

    /**
     * Marks a message as read and emits a new inbox state.
     * TODO: This will send an update to the backend once the API is ready.
     *
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
    }

    /**
     * Marks a message as unread and emits a new inbox state.
     * TODO: This will send an update to the backend once the API is ready.
     *
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
    }

    /**
     * Deletes a message from the inbox and emits a new inbox state.
     * TODO: This will send a delete request to the backend once the API is ready.
     *
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
    }
}
