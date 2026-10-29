package com.attentive.androidsdk.internal.context

import com.attentive.androidsdk.PersistentStorage
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

/** A [PersistentStorage] mock backed by a map, so a new store can reload what an old one saved. */
internal class InMemoryStorage {
    val values = mutableMapOf<String?, String?>()
    var saveCount = 0
        private set

    val mock: PersistentStorage =
        mock {
            on { read(anyOrNull()) } doAnswer { values[it.getArgument(0)] }
            on { save(anyOrNull(), anyOrNull<String>()) } doAnswer {
                saveCount++
                values[it.getArgument(0)] = it.getArgument(1)
                Unit
            }
            on { delete(any()) } doAnswer {
                values.remove(it.getArgument<String?>(0))
                Unit
            }
        }
}
