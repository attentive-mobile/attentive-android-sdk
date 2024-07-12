package com.attentive.example;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.uiautomator.By.textContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.example.activities.LoadCreativeActivity;
import java.io.IOException;
import java.util.Locale;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

@LargeTest
public class CreativeUITest {

    @ClassRule
    public static final ForceLocaleRule localeTestRule = new ForceLocaleRule(Locale.US);

    private static final String ATTENTIVE_PERSISTENT_STORAGE_KEY = "com.attentive.androidsdk.PERSISTENT_STORAGE";
    private static final String SMS_STRING = "Send this text to subscribe to recurring automated personalized marketing alerts (e.g. cart reminders) from Attentive Mobile Apps Test";
    private static final String PUSH_ME_FOR_CREATIVE = "PUSH ME FOR CREATIVE!";
    private static final String DEBUG_OUTPUT_JSON = "Debug output JSON";

    final Instrumentation instrumentation = new Instrumentation();
    private UiDevice device;
    private UiSelector selector;

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        selector = new UiSelector();

        // start listening for intents
        Intents.init();
    }

    @After
    public void cleanup() {
        clearSharedPreferences();
        clearCookies();
        // stop listening for intents
        Intents.release();
    }

    @AfterClass
    public static void classTeardown() {
        clearSharedPreferences();
        clearCookies();
    }

    @Test
    public void loadCreative_clickClose_closesCreative() throws UiObjectNotFoundException {
        loadCreative(AttentiveConfig.Mode.PRODUCTION);

        device.waitForIdle();
        // Close the Creative
        UiObject closeButton = device.findObject(selector.resourceId("closeIconContainer"));
        // Typo dissmiss on the web view so even though it looks like is wrong, is the needed
        // value for the test to work.
        UiObject dismissButton = device.findObject(selector.descriptionContains("Dissmiss this popup"));
        UiObject clickOutside = device.findObject(selector.textContains("Attentive Example"));
        if (closeButton.exists()) {
            closeButton.click();
        } else if (dismissButton.exists()) {
            dismissButton.click();
        } else {
            clickOutside.click();
        }

        checkIfCreativeClosed();
    }

    @Test
    public void loadCreative_fillOutFormAndSubmit_launchesSmsAppWithPrePopulatedText() throws UiObjectNotFoundException {
        loadCreative(AttentiveConfig.Mode.PRODUCTION);
        device.waitForIdle();

        // fill in email
        UiObject emailInput = device.findObject(selector.resourceId("input0input"));
        if (emailInput.waitForExists(1000)) {
            emailInput.setText("testmail@attentivemobile.com");
        } else {
            UiObject emailSubmitButton = device.findObject(selector.textContains("Continue"));
            if (emailSubmitButton.exists()) {
                emailSubmitButton.click();
                device.waitForIdle();
            }
            instrumentation.sendStringSync("testmail@attentivemobile.com");
        }
        if (isKeyboardOpenedShellCheck()) {
            device.pressBack();
        }
        device.waitForIdle();

        // submit email
        UiObject emailSubmitButton = device.findObject(selector.textContains("Continue"));
        if (emailSubmitButton.exists()) {
            emailSubmitButton.click();
        }

        // click subscribe button
        device.waitForIdle();
        UiObject subscribeButton = device.findObject(selector.textContains("GET 10% OFF NOW"));
        subscribeButton.click();
        device.waitForIdle();

        // Verify that the SMS app is opened with prepopulated text if running locally
        // (AWS Device Farm doesn't allow use of SMS apps)
        String testHost = InstrumentationRegistry.getArguments().getString("testHost");
        if (testHost != null && testHost.equals("local")) {
            assertTrue(device.wait(Until.hasObject(textContains(SMS_STRING)), 3000));
        }
    }

    public boolean isKeyboardOpenedShellCheck() {
        String checkKeyboardCmd = "dumpsys input_method | grep mInputShown";

        try {
            return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).executeShellCommand(checkKeyboardCmd).contains("mInputShown=true");
        } catch (IOException e) {
            throw new RuntimeException("Keyboard check failed", e);
        }
    }

    @Test
    public void loadCreative_clickPrivacyLink_opensPrivacyPageInWebApp() throws UiObjectNotFoundException {
        loadCreative(AttentiveConfig.Mode.PRODUCTION);
        device.waitForIdle();
        // Click privacy link
        UiObject privacyLink = device.findObject(selector.textMatches("Privacy"));
        privacyLink.click();
        device.waitForIdle();

        // Verify that the privacy page is visible in the external browser
        String currentPackage = device.getCurrentPackageName();
        assertEquals("com.android.chrome", currentPackage);
    }

    @Test
    public void loadCreative_inDebugMode_showsDebugMessage() throws UiObjectNotFoundException {
        loadCreative(AttentiveConfig.Mode.DEBUG);

        device.waitForIdle();
        assertTrue(device.wait(Until.hasObject(textContains(DEBUG_OUTPUT_JSON)), 3000));
    }

    private static void clearSharedPreferences() {
        // Clear shared preferences after each test so that it resets the visitor ID
        SharedPreferences sharedPreferences =
                getInstrumentation().getTargetContext().getSharedPreferences(ATTENTIVE_PERSISTENT_STORAGE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    private static void clearCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }

    private void loadCreative(AttentiveConfig.Mode mode) throws UiObjectNotFoundException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), LoadCreativeActivity.class);
        intent.putExtra("DOMAIN", "mobileapps");
        intent.putExtra("MODE", mode.toString());
        intent.putExtra("SKIP_FATIGUE", true);
        ActivityScenario.launch(intent);

        device.waitForIdle();

        // Click "Push me for creative!"
        UiObject creativeButton = device.findObject(selector.textContains(PUSH_ME_FOR_CREATIVE));
        creativeButton.click();
    }

    private void checkIfCreativeClosed() {
        // Check if we're redirected back to the "Push me for creative!" page
        assertTrue(device.wait(Until.hasObject(textContains(PUSH_ME_FOR_CREATIVE)), 3000));
    }
}
