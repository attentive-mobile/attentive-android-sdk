@file:Suppress("DEPRECATION")
@file:SuppressLint("RestrictedApi")

package com.attentive.bonni.inbox

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.inbox.InboxState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Suppress("DEPRECATION")
class InboxViewModel : ViewModel() {
    val inboxState: StateFlow<InboxState> =
        AttentiveSdk.inboxState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = InboxState(),
        )

    fun markMessageAsRead(messageId: String) {
        AttentiveSdk.markRead(messageId)
    }

    fun markMessageAsUnread(messageId: String) {
        AttentiveSdk.markUnread(messageId)
    }
}
