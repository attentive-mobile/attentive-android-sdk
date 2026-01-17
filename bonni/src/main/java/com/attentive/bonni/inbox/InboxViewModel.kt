package com.attentive.bonni.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.inbox.InboxState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class InboxViewModel : ViewModel() {
    val inboxState: StateFlow<InboxState> = AttentiveSdk.inboxState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = InboxState()
    )

    fun markMessageAsRead(messageId: String) {
        AttentiveSdk.markRead(messageId)
    }

    fun markMessageAsUnread(messageId: String) {
        AttentiveSdk.markUnread(messageId)
    }
}
