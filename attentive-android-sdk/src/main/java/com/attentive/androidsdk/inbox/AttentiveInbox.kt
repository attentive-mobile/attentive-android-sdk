package com.attentive.androidsdk.inbox

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.R
import com.attentive.androidsdk.inbox.Style
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A ready-to-use inbox UI component that displays messages from the Attentive SDK.
 * This is a stateful component that automatically observes AttentiveSdk.inboxState.
 *
 * Features:
 * - Displays message list with title, body, timestamp, and optional images
 * - Shows unread indicator for unread messages
 * - Supports swipe-to-mark-as-unread gesture
 * - Fully customizable colors and fonts
 * - Empty state when no messages
 *
 * @param modifier Modifier to be applied to the component
 * @param backgroundColor Background color of the message list
 * @param unreadIndicatorColor Color of the unread indicator dot
 * @param titleTextColor Color of the message title text
 * @param bodyTextColor Color of the message body text
 * @param timestampTextColor Color of the timestamp text
 * @param swipeBackgroundColor Background color shown during swipe gesture
 * @param titleFontFamily Font family for message titles (null uses system default)
 * @param bodyFontFamily Font family for message body text (null uses system default)
 * @param timestampFontFamily Font family for timestamps (null uses system default)
 * @param onMessageClick Callback invoked when a message is clicked (default marks as read)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentiveInbox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = colorResource(R.color.attentive_inbox_background),
    unreadIndicatorColor: Color = colorResource(R.color.attentive_inbox_unread_indicator),
    titleTextColor: Color = colorResource(R.color.attentive_inbox_title_text),
    bodyTextColor: Color = colorResource(R.color.attentive_inbox_body_text),
    timestampTextColor: Color = colorResource(R.color.attentive_inbox_timestamp_text),
    swipeBackgroundColor: Color = colorResource(R.color.attentive_inbox_swipe_background),
    titleFontFamily: FontFamily? = null,
    bodyFontFamily: FontFamily? = null,
    timestampFontFamily: FontFamily? = null,
    onMessageClick: ((Message) -> Unit)? = null
) {
    val context = LocalContext.current
    val inboxState by AttentiveSdk.inboxState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll detection for infinite scrolling
    LaunchedEffect(listState) {
        var lastTriggeredAt = 0L  // Track when we last triggered to prevent rapid re-triggers

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount

            lastVisibleItem?.index to totalItems
        }
        .collect { (lastVisibleIndex, totalItems) ->
            val now = System.currentTimeMillis()

            // Only trigger if:
            // 1. Near end of list
            // 2. Not currently loading
            // 3. Has more messages
            // 4. At least 500ms since last trigger (debounce)
            if (lastVisibleIndex != null &&
                lastVisibleIndex >= totalItems - 5 &&
                !inboxState.isLoadingMore &&
                inboxState.hasMoreMessages &&
                (now - lastTriggeredAt) > 500
            ) {
                lastTriggeredAt = now
                Timber.d("Pagination trigger: index=$lastVisibleIndex, total=$totalItems")
                AttentiveSdk.loadMoreInboxMessages()
            }
        }
    }

    if (inboxState.messages.isEmpty() && !inboxState.isLoadingMore) {
        EmptyInboxView(
            titleTextColor = titleTextColor,
            bodyTextColor = bodyTextColor,
            titleFontFamily = titleFontFamily,
            bodyFontFamily = bodyFontFamily,
            modifier = modifier
        )
    } else {
        MessageList(
            messages = inboxState.messages,
            isLoadingMore = inboxState.isLoadingMore,
            listState = listState,
            backgroundColor = backgroundColor,
            unreadIndicatorColor = unreadIndicatorColor,
            titleTextColor = titleTextColor,
            bodyTextColor = bodyTextColor,
            timestampTextColor = timestampTextColor,
            swipeBackgroundColor = swipeBackgroundColor,
            titleFontFamily = titleFontFamily,
            bodyFontFamily = bodyFontFamily,
            timestampFontFamily = timestampFontFamily,
            onMessageClick = onMessageClick ?: { message: Message ->
                if (!message.isRead) {
                    AttentiveSdk.markRead(message.id)
                }

                // Handle deep link if actionUrl is present
                message.actionUrl?.let { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                }
                Unit
            },
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyInboxView(
    titleTextColor: Color,
    bodyTextColor: Color,
    titleFontFamily: FontFamily?,
    bodyFontFamily: FontFamily?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Messages",
            fontSize = 24.sp,
            fontFamily = titleFontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            color = titleTextColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You don't have any messages yet",
            fontSize = 16.sp,
            fontFamily = bodyFontFamily ?: FontFamily.Default,
            color = bodyTextColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageList(
    messages: List<Message>,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    backgroundColor: Color,
    unreadIndicatorColor: Color,
    titleTextColor: Color,
    bodyTextColor: Color,
    timestampTextColor: Color,
    swipeBackgroundColor: Color,
    titleFontFamily: FontFamily?,
    bodyFontFamily: FontFamily?,
    timestampFontFamily: FontFamily?,
    onMessageClick: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        items(
            items = messages,
            key = { it.id },
            contentType = { it.style }  // Optimize recycling based on message style
        ) { message ->
            Column(
                modifier = Modifier.animateItem()  // Smooth animation when items move
            ) {
                MessageItem(
                    message = message,
                    unreadIndicatorColor = unreadIndicatorColor,
                    titleTextColor = titleTextColor,
                    bodyTextColor = bodyTextColor,
                    timestampTextColor = timestampTextColor,
                    swipeBackgroundColor = swipeBackgroundColor,
                    titleFontFamily = titleFontFamily,
                    bodyFontFamily = bodyFontFamily,
                    timestampFontFamily = timestampFontFamily,
                    onClick = { onMessageClick(message) },
                    onSwipeMarkUnread = { AttentiveSdk.markUnread(message.id) },
                    onSwipeDelete = { AttentiveSdk.deleteMessage(message.id) }
                )
                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
            }
        }

        // Loading indicator at bottom
        if (isLoadingMore) {
            item(key = "loading_indicator") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageItem(
    message: Message,
    unreadIndicatorColor: Color,
    titleTextColor: Color,
    bodyTextColor: Color,
    timestampTextColor: Color,
    swipeBackgroundColor: Color,
    titleFontFamily: FontFamily?,
    bodyFontFamily: FontFamily?,
    timestampFontFamily: FontFamily?,
    onClick: () -> Unit,
    onSwipeMarkUnread: () -> Unit,
    onSwipeDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeDelete()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeMarkUnread()
                    false // Don't dismiss, just trigger action
                }
                else -> false
            }
        },
        // Require swiping 75% of the width to trigger - prevents accidental swipes during scrolling
        positionalThreshold = { totalDistance -> totalDistance * 0.75f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                SwipeToDismissBoxValue.EndToStart -> Arrangement.End
                else -> Arrangement.End
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.MailOutline
                else -> Icons.Filled.MailOutline
            }
            val iconDescription = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "Delete"
                SwipeToDismissBoxValue.EndToStart -> "Mark as unread"
                else -> "Mark as unread"
            }
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color.Red
                SwipeToDismissBoxValue.EndToStart -> swipeBackgroundColor
                else -> swipeBackgroundColor
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = alignment,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        when (message.style) {
            Style.Small -> SmallMessageContent(
                message = message,
                unreadIndicatorColor = unreadIndicatorColor,
                titleTextColor = titleTextColor,
                bodyTextColor = bodyTextColor,
                timestampTextColor = timestampTextColor,
                titleFontFamily = titleFontFamily,
                bodyFontFamily = bodyFontFamily,
                timestampFontFamily = timestampFontFamily,
                onClick = onClick
            )
            Style.Large -> LargeMessageContent(
                message = message,
                unreadIndicatorColor = unreadIndicatorColor,
                titleTextColor = titleTextColor,
                bodyTextColor = bodyTextColor,
                timestampTextColor = timestampTextColor,
                titleFontFamily = titleFontFamily,
                bodyFontFamily = bodyFontFamily,
                timestampFontFamily = timestampFontFamily,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun SmallMessageContent(
    message: Message,
    unreadIndicatorColor: Color,
    titleTextColor: Color,
    bodyTextColor: Color,
    timestampTextColor: Color,
    titleFontFamily: FontFamily?,
    bodyFontFamily: FontFamily?,
    timestampFontFamily: FontFamily?,
    onClick: () -> Unit
) {
    val backgroundColor = if (message.isRead) Color.White else Color(0xFFF5F5F5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Unread indicator
        if (!message.isRead) {
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .background(unreadIndicatorColor, shape = CircleShape)
                    .align(Alignment.Top)
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.title,
                fontSize = 16.sp,
                fontFamily = titleFontFamily ?: FontFamily.Default,
                fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
                color = titleTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.body,
                fontSize = 14.sp,
                fontFamily = bodyFontFamily ?: FontFamily.Default,
                fontWeight = FontWeight.Normal,
                color = bodyTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Display image if available
        message.imageUrl?.let { imageUrl ->
            Spacer(modifier = Modifier.width(12.dp))
            val imageRequest = ImageRequest.Builder(LocalPlatformContext.current)
                .data(imageUrl)
                .crossfade(200)  // Smooth crossfade animation
                .build()

            AsyncImage(
                model = imageRequest,
                contentDescription = "Message image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatTimestamp(message.timestamp),
            fontSize = 12.sp,
            fontFamily = timestampFontFamily ?: FontFamily.Default,
            color = timestampTextColor
        )
    }
}

@Composable
private fun LargeMessageContent(
    message: Message,
    unreadIndicatorColor: Color,
    titleTextColor: Color,
    bodyTextColor: Color,
    timestampTextColor: Color,
    titleFontFamily: FontFamily?,
    bodyFontFamily: FontFamily?,
    timestampFontFamily: FontFamily?,
    onClick: () -> Unit
) {
    val backgroundColor = if (message.isRead) Color.White else Color(0xFFF5F5F5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Unread indicator at top of card
            if (!message.isRead) {
                Spacer(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(8.dp)
                        .background(unreadIndicatorColor, shape = CircleShape)
                )
            }

            // Image takes up 80% of card height
            message.imageUrl?.let { imageUrl ->
                val imageRequest = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .crossfade(200)  // Smooth crossfade animation
                    .build()

                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Message image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                )
            }

            // Title and body below image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = message.title,
                    fontSize = 18.sp,
                    fontFamily = titleFontFamily ?: FontFamily.Default,
                    fontWeight = if (message.isRead) FontWeight.Medium else FontWeight.Bold,
                    color = titleTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message.body,
                    fontSize = 14.sp,
                    fontFamily = bodyFontFamily ?: FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    color = bodyTextColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 12.sp,
                    fontFamily = timestampFontFamily ?: FontFamily.Default,
                    color = timestampTextColor
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
