package com.attentive.bonni.inbox

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.attentive.androidsdk.inbox.Message
import com.attentive.bonni.R
import com.attentive.bonni.SimpleToolbar
import com.attentive.bonni.ui.theme.BonniPink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InboxScreen(
    navHostController: NavHostController,
    viewModel: InboxViewModel = ViewModelProvider(
        LocalActivity.current as ComponentActivity
    )[InboxViewModel::class.java]
) {
    val inboxState by viewModel.inboxState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SimpleToolbar(
            title = "Inbox",
            navController = navHostController
        )

        if (inboxState.messages.isEmpty()) {
            EmptyInboxView()
        } else {
            MessageList(
                messages = inboxState.messages,
                onMessageClick = { message ->
                    if (!message.isRead) {
                        viewModel.markMessageAsRead(message.id)
                    }
                },
                onMessageSwipe = { message ->
                    viewModel.markMessageAsUnread(message.id)
                }
            )
        }
    }
}

@Composable
fun EmptyInboxView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "No Messages",
            fontSize = 24.sp,
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            fontWeight = FontWeight.Medium,
            color = colorResource(id = R.color.attentive_black)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You don't have any messages yet",
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageList(
    messages: List<Message>,
    onMessageClick: (Message) -> Unit,
    onMessageSwipe: (Message) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageItem(
                message = message,
                onClick = { onMessageClick(message) },
                onSwipe = { onMessageSwipe(message) }
            )
            HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageItem(
    message: Message,
    onClick: () -> Unit,
    onSwipe: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipe()
                    false // Don't actually dismiss, just trigger the action
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Background shown during swipe
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BonniPink)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MailOutline,
                    contentDescription = "Mark as unread",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
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
            if (!message.isRead) {
                Spacer(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            BonniPink,
                            shape = CircleShape
                        )
                        .align(Alignment.Top)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }

            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.title,
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                        fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = colorResource(id = R.color.attentive_black),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.body,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                        fontWeight = FontWeight.Normal,
                        color = Color.DarkGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                message.imageUrl?.let { imageUrl ->
                    Spacer(modifier = Modifier.width(12.dp))
                    AsyncImage(
                        model = imageUrl,
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
                    fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                    color = Color.Gray
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
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
