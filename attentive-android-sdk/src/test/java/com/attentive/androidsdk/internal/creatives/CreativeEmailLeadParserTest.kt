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
    fun parse_withForwardedOrigin_removesOriginFromRawPayload() {
        val message =
            """{"action":"EMAIL_LEAD","email":"user@example.com","metadata":{"a":1},"__origin":"https://creatives.attn.tv"}"""

        val lead = CreativeEmailLeadParser.parse(message)

        assertEquals("""{"action":"EMAIL_LEAD","email":"user@example.com","metadata":{"a":1}}""", lead?.rawPayload)
    }

    @Test
    fun parseLead_withNewEmail_returnsLead() {
        val message =
            """{"action":"LEAD","email":"new@example.com","userEmail":"known@example.com","phone":"+15555550100"}"""

        assertEquals(CreativeEmailLead(email = "new@example.com", rawPayload = message), CreativeEmailLeadParser.parseLead(message))
    }

    @Test
    fun parseLead_withoutUserEmail_returnsLead() {
        val message = """{"action":"LEAD","email":"new@example.com"}"""

        assertEquals("new@example.com", CreativeEmailLeadParser.parseLead(message)?.email)
    }

    @Test
    fun parseLead_withKnownEmail_returnsNull() {
        val message = """{"action":"LEAD","email":"Known@Example.com","userEmail":"known@example.com"}"""

        assertNull(CreativeEmailLeadParser.parseLead(message))
    }

    @Test
    fun parseLead_withoutEmail_returnsNull() {
        assertNull(CreativeEmailLeadParser.parseLead("""{"action":"LEAD","phone":"+15555550100"}"""))
        assertNull(CreativeEmailLeadParser.parseLead("""{"action":"LEAD","email":""}"""))
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
