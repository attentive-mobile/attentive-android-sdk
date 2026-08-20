package com.attentive.androidsdk

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.attentive.androidsdk.push.TokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.util.Collections

/**
 * Drives the MSDK-470 identity guards on a real device against real SharedPreferences, with the
 * network stubbed at the OkHttp layer so no `/user-update` reaches production.
 *
 * Each scenario logs `IDENTITY-SCENARIO` markers so the behaviour can be read straight out of
 * logcat alongside the SDK's own Timber output.
 */
@RunWith(AndroidJUnit4::class)
class IdentitySyncScenariosIT {
    private lateinit var application: Application
    private val userUpdates = Collections.synchronizedList(mutableListOf<String>())

    private val client =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.encodedPath.endsWith("user-update")) {
                    val body = okio.Buffer().also { request.body?.writeTo(it) }.readUtf8()
                    userUpdates.add(body)
                    Timber.i("IDENTITY-SCENARIO fake backend received /user-update: $body")
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("".toResponseBody())
                    .build()
            }.build()

    @Before
    fun setUp() {
        application =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        TokenProvider.getInstance().token = PUSH_TOKEN
    }

    @After
    fun tearDown() {
        TokenProvider.getInstance().token = null
    }

    /**
     * Wipes everything the SDK persists — the fresh-install state.
     */
    private fun wipeStorage() {
        PersistentStorage(application).deleteAll()
        userUpdates.clear()
    }

    /**
     * Builds a brand new [AttentiveConfig] over the same on-disk storage and installs it, which is
     * exactly what a process recreation does: identifiers rebuilt from the persisted visitor ID
     * alone, with no email or phone in memory.
     */
    private fun launch(domain: String = DOMAIN): AttentiveConfig {
        Timber.uprootAll()
        val config =
            AttentiveConfig.Builder()
                .applicationContext(application)
                .mode(AttentiveConfig.Mode.DEBUG)
                .domain(domain)
                .logLevel(AttentiveLogLevel.VERBOSE)
                .okHttpClient(client)
                .build()
        AttentiveEventTracker.instance.config = config
        val field = AttentiveSdk::class.java.getDeclaredField("_config")
        field.isAccessible = true
        field.set(AttentiveSdk, config)
        return config
    }

    private fun scenario(name: String) {
        Timber.i("IDENTITY-SCENARIO ===== $name =====")
    }

    private fun step(description: String) {
        Timber.i("IDENTITY-SCENARIO   -> $description")
    }

    private fun updateUser(
        email: String?,
        phone: String?,
    ): Int {
        val before = userUpdates.size
        val result = runBlocking { AttentiveSdk.updateUserSuspend(email = email, phoneNumber = phone) }
        assertTrue("updateUser failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        return userUpdates.size - before
    }

    private fun clearUser(): Int {
        val before = userUpdates.size
        AttentiveSdk.clearUser()
        Thread.sleep(500)
        return userUpdates.size - before
    }

    private fun report(
        requestsSent: Int,
        visitorBefore: String?,
        visitorAfter: String?,
    ) {
        step(
            "requests sent = $requestsSent, visitor ${if (visitorBefore == visitorAfter) {
                "unchanged ($visitorAfter)"
            } else {
                "ROTATED ($visitorBefore -> $visitorAfter)"
            }}",
        )
    }

    @Test
    fun scenario1_firstUpdateUser_sendsAndRotates() {
        wipeStorage()
        scenario("1. first ever updateUser (fresh install)")
        var config = launch()
        val before = config.userIdentifiers.visitorId

        val sent = updateUser(EMAIL, PHONE)
        val after = config.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("expected the first update to be sent", 1, sent)
        assertNotEquals("expected a genuine identity change to rotate the visitor", before, after)
    }

    @Test
    fun scenario2_repeatUpdateUser_sameProcess_isNoOp() {
        wipeStorage()
        scenario("2. identical updateUser repeated in the same process")
        val config = launch()
        updateUser(EMAIL, PHONE)
        val before = config.userIdentifiers.visitorId

        step("calling updateUser again with the same identifiers")
        val sent = updateUser(EMAIL, PHONE)
        val after = config.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("the repeat call should send nothing", 0, sent)
        assertEquals("the repeat call should not rotate the visitor", before, after)
    }

    /** The fix for the process-recreation P1. */
    @Test
    fun scenario3_repeatUpdateUser_afterColdLaunch_isNoOp() {
        wipeStorage()
        scenario("3. identical updateUser after a cold launch (process recreation)")
        launch()
        updateUser(EMAIL, PHONE)

        step("simulating process death: rebuilding AttentiveConfig from disk")
        val relaunched = launch()
        val before = relaunched.userIdentifiers.visitorId
        step("after relaunch, in-memory email=${relaunched.userIdentifiers.email} phone=${relaunched.userIdentifiers.phone}")

        val sent = updateUser(EMAIL, PHONE)
        val after = relaunched.userIdentifiers.visitorId
        report(sent, before, after)
        step("identifiers now email=${relaunched.userIdentifiers.email} phone=${relaunched.userIdentifiers.phone}")

        assertEquals("a relaunch with identical identifiers should send nothing", 0, sent)
        assertEquals("a relaunch should not rotate the visitor", before, after)
        assertEquals("the confirmed email should be restored locally", EMAIL, relaunched.userIdentifiers.email)
        assertEquals("the confirmed phone should be restored locally", PHONE, relaunched.userIdentifiers.phone)
    }

    @Test
    fun scenario4_differentUser_afterColdLaunch_stillRotates() {
        wipeStorage()
        scenario("4. cold launch, then updateUser for a DIFFERENT user")
        launch()
        updateUser(EMAIL, PHONE)

        step("simulating process death, then identifying someone else")
        val relaunched = launch()
        val before = relaunched.userIdentifiers.visitorId

        val sent = updateUser(OTHER_EMAIL, PHONE)
        val after = relaunched.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("a genuine new user must be sent", 1, sent)
        assertNotEquals("a genuine new user must rotate the visitor", before, after)
    }

    /** The fix for the domain-scoping P1. */
    @Test
    fun scenario5_afterDomainChange_resendsWithoutRotating() {
        wipeStorage()
        scenario("5. changeDomain, then the identical updateUser")
        val config = launch()
        updateUser(EMAIL, PHONE)
        val before = config.userIdentifiers.visitorId

        step("changing domain to $OTHER_DOMAIN")
        config.changeDomain(OTHER_DOMAIN)

        val sent = updateUser(EMAIL, PHONE)
        val after = config.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("the new domain has never heard of this user, so it must be sent", 1, sent)
        assertEquals("re-associating the same user must not rotate the visitor", before, after)
        assertTrue(
            "the request should carry the new domain",
            userUpdates.last().contains(OTHER_DOMAIN),
        )
    }

    @Test
    fun scenario6_afterLocalOnlyClear_rotatedVisitor_resends() {
        wipeStorage()
        scenario("6. deprecated AttentiveConfig.clearUser() rotates the visitor, then updateUser")
        val config = launch()
        updateUser(EMAIL, PHONE)

        step("calling the deprecated local-only clearUser (rotates the visitor, tells nobody)")
        @Suppress("DEPRECATION")
        config.clearUser()
        val before = config.userIdentifiers.visitorId

        val sent = updateUser(EMAIL, PHONE)
        val after = config.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("the new visitor was never associated, so it must be sent", 1, sent)
    }

    @Test
    fun scenario7_clearUser_repeated_isNoOp() {
        wipeStorage()
        scenario("7. clearUser twice")
        val config = launch()
        updateUser(EMAIL, PHONE)

        step("first clearUser")
        val firstSent = clearUser()
        val before = config.userIdentifiers.visitorId
        step("requests sent = $firstSent, visitor now $before")

        step("second clearUser")
        val sent = clearUser()
        val after = config.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("the first clear must reach the backend", 1, firstSent)
        assertEquals("the repeat clear should send nothing", 0, sent)
        assertEquals("the repeat clear should not rotate the visitor", before, after)
    }

    @Test
    fun scenario8_clearUser_afterColdLaunch_isNoOp() {
        wipeStorage()
        scenario("8. clearUser, cold launch, clearUser again")
        launch()
        updateUser(EMAIL, PHONE)
        clearUser()

        step("simulating process death")
        val relaunched = launch()
        val before = relaunched.userIdentifiers.visitorId

        val sent = clearUser()
        val after = relaunched.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("a relaunch with nothing to clear should send nothing", 0, sent)
        assertEquals("a relaunch should not rotate the visitor", before, after)
    }

    @Test
    fun scenario9_clearUser_afterDomainChange_resends() {
        wipeStorage()
        scenario("9. clearUser, changeDomain, clearUser again")
        val config = launch()
        updateUser(EMAIL, PHONE)
        clearUser()
        val before = config.userIdentifiers.visitorId

        step("changing domain to $OTHER_DOMAIN")
        config.changeDomain(OTHER_DOMAIN)

        val sent = clearUser()
        val after = config.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("the detach must be repeated for the new domain", 1, sent)
        assertEquals("re-detaching must not rotate the visitor", before, after)
    }

    @Test
    fun scenario10_rotatedPushToken_resendsWithoutRotating() {
        wipeStorage()
        scenario("10. push token rotates, then the identical updateUser")
        val config = launch()
        updateUser(EMAIL, PHONE)
        val before = config.userIdentifiers.visitorId

        step("rotating the FCM token")
        TokenProvider.getInstance().token = "rotatedPushToken"

        val sent = updateUser(EMAIL, PHONE)
        val after = config.userIdentifiers.visitorId
        report(sent, before, after)

        assertEquals("the new token must be attached", 1, sent)
        assertEquals("attaching a new token must not rotate the visitor", before, after)
        assertTrue(
            "the request should carry the rotated token",
            userUpdates.last().contains("rotatedPushToken"),
        )
    }

    private companion object {
        const val DOMAIN = "someDomain"
        const val OTHER_DOMAIN = "someOtherDomain"
        const val EMAIL = "identity.scenarios@example.com"
        const val OTHER_EMAIL = "someone.else@example.com"
        const val PHONE = "+15556667777"
        const val PUSH_TOKEN = "instrumentedPushToken"
    }
}
