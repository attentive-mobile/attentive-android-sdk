package com.attentive.androidsdk.internal.creatives

import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.creatives.CreativeEmailLead
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreativeEmailLeadParserTest {
    @Test
    fun parse_withEmail_returnsEmailAndRawPayload() {
        val message = """{"action":"EMAIL_LEAD","email":"user@example.com","creativeId":"123"}"""

        val lead = CreativeEmailLeadParser.parse(message)

        assertEquals(CreativeEmailLead(email = "user@example.com", rawPayload = message), lead)
    }

    @Test
    fun parse_withoutEmail_returnsNullEmail() {
        val message = """{"action":"EMAIL_LEAD"}"""

        assertNull(CreativeEmailLeadParser.parse(message)?.email)
    }

    @Test
    fun parse_withNonStringEmail_returnsNullEmail() {
        val message = """{"action":"EMAIL_LEAD","email":42}"""

        assertNull(CreativeEmailLeadParser.parse(message)?.email)
    }

    @Test
    fun parse_withInvalidJson_returnsNull() {
        assertNull(CreativeEmailLeadParser.parse("{not json"))
    }

    @Test
    fun emitCreativeEmailLead_deliversToCollector() =
        runTest {
            val lead = CreativeEmailLead(email = "user@example.com", rawPayload = "{}")
            val received = async(UnconfinedTestDispatcher(testScheduler)) { AttentiveSdk.creativeEmailLeads.first() }

            AttentiveSdk.emitCreativeEmailLead(lead)

            assertEquals(lead, received.await())
        }
}
