package com.attentive.example;

import static android.content.Intent.ACTION_VIEW;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.uiautomator.By.textContains;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.example.activities.MainActivity;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


@LargeTest
public class CreativeTest {

    private static final String ATTENTIVE_PERSISTENT_STORAGE_KEY = "com.attentive.androidsdk.PERSISTENT_STORAGE";
    private static final String SMS_STRING_URL = "sms://+18332115910?body=Send%20this%20text%20to%20subscribe%20to%20recurring%20automated%20personalized%20marketing%20alerts%20%28e.g.%20cart%20reminders%29%20from%20Attentive%20Mobile%20Apps%20Test";
    private static final String SMS_STRING = "Send this text to subscribe to recurring automated personalized marketing alerts (e.g. r reminders) from Attentive Mobile Apps Test";
    private static final String PRIVACY_URL = "https://creatives.attn.tv/creatives-dynamic/multiPage/index.html";
    private static final String PRIVACY_STRING = "Attentive Mobile Inc. Privacy Policy";
    private static final String PUSH_ME_FOR_CREATIVE = "PUSH ME FOR CREATIVE!";
    private static final String PUSH_ME_FOR_CREATIVE_PAGE_PROD = "PUSH ME FOR CREATIVE PAGE (PRODUCTION)";
    private static final String PUSH_ME_FOR_CREATIVE_PAGE_DEBUG = "PUSH ME FOR CREATIVE PAGE (DEBUG)";
    private static final String DEBUG_OUTPUT_SUCCESS = "Your creative (ID: 372725) should be displayed correctly!";
    private static final String DEBUG_OUTPUT_JSON = "Debug output JSON:";

    private UiDevice device;
    private UiSelector selector;


    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setup() {
        clearSharedPreferences();
        clearCookies();

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        selector = new UiSelector();

        // start listening for intents
        Intents.init();
    }

    @After
    public void cleanup() {
        // stop listening for intents
        Intents.release();
    }

    @AfterClass
    public static void classSetup() {
        clearSharedPreferences();
        clearCookies();
    }

    @Test
    public void loadCreative_clickClose_closesCreative() throws UiObjectNotFoundException {
        loadCreative(AttentiveConfig.Mode.PRODUCTION);

        // Close the Creative
        UiObject closeButton = device.findObject(selector.resourceId("closeIconContainer"));
        closeButton.click();

        checkIfCreativeClosed();
    }


    @Test
    public void loadCreative_fillOutFormAndSubmit_launchesSmsAppWithPrePopulatedText() throws UiObjectNotFoundException {
        loadCreative(AttentiveConfig.Mode.PRODUCTION);

        // fill in email
        UiObject emailInput = device.findObject(selector.resourceId("input0input"));
        emailInput.setText("testmail@attentivemobile.com");
        device.pressEnter();

        // submit email
        UiObject emailSubmitButton = device.findObject(selector.resourceId("ctabutton1"));
        emailSubmitButton.click();

        // click subscribe button
        UiObject subscribeButton = device.findObject(selector.textContains("GET 10% OFF NOW"));
        subscribeButton.click();

        device.waitForIdle();

        // Verify intent to open sms app
        Intents.intended(allOf(hasAction(ACTION_VIEW), hasData(hasToString(startsWith(SMS_STRING_URL)))));

        // Verify sms app opened
        device.wait(Until.findObject(textContains(SMS_STRING)), 3000);
    }

    @Test
    public void loadCreative_clickPrivacyLink_opensPrivacyPageInWebApp() throws UiObjectNotFoundException {
        loadCreative(AttentiveConfig.Mode.PRODUCTION);

        // Click privacy link
        UiObject privacyLink = device.findObject(selector.textMatches("Privacy"));
        privacyLink.click();

        device.waitForIdle();

        // Verify intent to open privacy page in external browser
        Intents.intended(allOf(hasAction(ACTION_VIEW), hasData(hasToString(startsWith(PRIVACY_URL)))));

        // Verify that the privacy page is visible in the external browser
        device.wait(Until.hasObject(textContains(PRIVACY_STRING)), 3000);
    }

    @Test
    public void loadCreative_inDebugMode_showsDebugMessage() throws UiObjectNotFoundException {
        loadCreative(AttentiveConfig.Mode.DEBUG);

        device.wait(Until.hasObject(textContains(DEBUG_OUTPUT_SUCCESS)), 3000);
        device.wait(Until.hasObject(textContains(DEBUG_OUTPUT_JSON)), 3000);
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
        String launchPageButtonString = mode == AttentiveConfig.Mode.PRODUCTION
                ? PUSH_ME_FOR_CREATIVE_PAGE_PROD
                : PUSH_ME_FOR_CREATIVE_PAGE_DEBUG;

        UiObject launchPageButton = device.findObject(selector.textContains(launchPageButtonString));
        launchPageButton.click();

        device.waitForIdle();

        // Click "Push me for creative!"
        UiObject creativeButton = device.findObject(selector.textContains(PUSH_ME_FOR_CREATIVE));
        creativeButton.click();
    }

    private void checkIfCreativeClosed() {
        // Check if we're redirected back to the "Push me for creative!" page
        boolean creativeButtonShown = device.wait(Until.hasObject(textContains(PUSH_ME_FOR_CREATIVE)), 3000);
        assertTrue(creativeButtonShown);
    }
}