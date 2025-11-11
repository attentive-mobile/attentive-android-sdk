package com.attentive.bonni.settings.debug

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.creatives.Creative
import com.attentive.bonni.SimpleToolbar
import com.attentive.bonni.ui.theme.BonniYellow

val fakeJson = "{\n" +
        "    \"targets\": {\n" +
        "        \"overlay\": {\n" +
        "            \"immediately\": {\n" +
        "                \"contentUrl\": \"https://creatives.attn.tv/creatives-dynamic/multiPage/index.html\",\n" +
        "                \"renderingFields\": {\n" +
        "                    \"allAttentiveTermsLinks\": {\n" +
        "                        \"en-US\": \"https://attnl.tv/t/7te/DlJx\"\n" +
        "                    },\n" +
        "                    \"anonymousId\": \"\",\n" +
        "                    \"attentiveAPI\": \"https://api.attentivemobile.com\",\n" +
        "                    \"attentivePrivacyLink\": \"https://attnl.tv/p/7te\",\n" +
        "                    \"companyAddress\": \"3 Banner Way, Camden, New Jersey 08103\",\n" +
        "                    \"companyName\": \"Philadelphia+76ers\",\n" +
        "                    \"companyUrl\": \"nba.com/sixers/\",\n" +
        "                    \"country\": \"US\",\n" +
        "                    \"creativeConfig\": \"{\\\"base\\\": {\\\"fields\\\": {\\\"logo\\\": {\\\"src\\\": \\\"https://creatives.attn.tv/76ers/Logo%3D1_b441bbf5.png\\\", \\\"height\\\": \\\"48px\\\", \\\"styles\\\": {\\\"marginTop\\\": \\\"0px\\\", \\\"marginBottom\\\": \\\"32px\\\"}, \\\"altText\\\": \\\"Philadelphia 76ers\\\", \\\"elementID\\\": \\\"nbWW\\\"}, \\\"fonts\\\": [{\\\"type\\\": \\\"google\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Roboto\\\", \\\"weight\\\": \\\"700\\\"}, {\\\"type\\\": \\\"google\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Roboto\\\", \\\"weight\\\": \\\"400\\\"}, {\\\"type\\\": \\\"google\\\", \\\"family\\\": \\\"Roboto Condensed\\\", \\\"weight\\\": \\\"400\\\"}, {\\\"type\\\": \\\"google\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Roboto\\\", \\\"weight\\\": \\\"400\\\"}, {\\\"type\\\": \\\"google\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Roboto\\\", \\\"weight\\\": \\\"700\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Cactus-Black_8bbeca72.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Cactus.otf\\\", \\\"weight\\\": \\\"900\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Cactus-BlackItalic_44c4332c.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"italic\\\", \\\"family\\\": \\\"Cactus.otf\\\", \\\"weight\\\": \\\"900\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Cactus-Bold_c6af740b.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Cactus.otf\\\", \\\"weight\\\": \\\"700\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Cactus-BoldItalic_aabf8198.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"italic\\\", \\\"family\\\": \\\"Cactus.otf\\\", \\\"weight\\\": \\\"700\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Cactus-Light_636fae3e.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Cactus.otf\\\", \\\"weight\\\": \\\"300\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Cactus-LightItalic_c6af740b.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"italic\\\", \\\"family\\\": \\\"Cactus.otf\\\", \\\"weight\\\": \\\"300\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Cactus-Medium_c6af740b.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Cactus.otf\\\", \\\"weight\\\": \\\"500\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Cactus-MediumItalic_c6af740b.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"italic\\\", \\\"family\\\": \\\"Cactus.otf\\\", \\\"weight\\\": \\\"500\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Whitney-Black_8f31ad1f.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Whitney.otf\\\", \\\"weight\\\": \\\"900\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Whitney-Bold-Pro_35c0b7fe.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Whitney.otf\\\", \\\"weight\\\": \\\"700\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Whitney-BoldItalic_0db52fd3.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"italic\\\", \\\"family\\\": \\\"Whitney.otf\\\", \\\"weight\\\": \\\"700\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Whitney-Light-Pro_a96d94a3.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Whitney.otf\\\", \\\"weight\\\": \\\"300\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Whitney-LightItalic-Pro_35c0b7fe.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"italic\\\", \\\"family\\\": \\\"Whitney.otf\\\", \\\"weight\\\": \\\"300\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Whitney-Medium_3cff35c0.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Whitney.otf\\\", \\\"weight\\\": \\\"500\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Whitney-Semibold-Pro_d125b478.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"normal\\\", \\\"family\\\": \\\"Whitney.otf\\\", \\\"weight\\\": \\\"600\\\"}, {\\\"url\\\": \\\"https://creatives.attn.tv/76ers/Whitney-SemiboldItalic-Pro_5747dc00.otf\\\", \\\"type\\\": \\\"custom\\\", \\\"style\\\": \\\"italic\\\", \\\"family\\\": \\\"Whitney.otf\\\", \\\"weight\\\": \\\"600\\\"}], \\\"layout\\\": {\\\"layout\\\": \\\"fullscreen\\\", \\\"layoutStyle\\\": \\\"fullBleed\\\", \\\"contentHeight\\\": \\\"fixed\\\", \\\"backgroundColor\\\": \\\"#003da6\\\", \\\"backgroundImage\\\": {\\\"src\\\": \\\"\\\", \\\"alignment\\\": \\\"center\\\", \\\"maskColor\\\": \\\"#000000\\\", \\\"maskOpacity\\\": \\\"0\\\"}}, \\\"smsBody\\\": \\\"Send this text to subscribe to recurring automated personalized marketing alerts (e.g. cart reminders) from Philadelphia 76ers\\\", \\\"ageGating\\\": {\\\"input\\\": {\\\"color\\\": \\\"#000000\\\", \\\"border\\\": {\\\"color\\\": \\\"#979797\\\", \\\"width\\\": \\\"1px\\\", \\\"radius\\\": \\\"\\\"}, \\\"alignment\\\": \\\"left\\\", \\\"placeholder\\\": {\\\"color\\\": \\\"#767676\\\"}, \\\"backgroundColor\\\": \\\"#FFFFFF\\\"}, \\\"label\\\": {\\\"font\\\": 0, \\\"size\\\": \\\"1rem\\\", \\\"text\\\": \\\"Enter your birthday\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"alignment\\\": \\\"left\\\", \\\"marginTop\\\": \\\"2px\\\", \\\"marginBottom\\\": \\\"2px\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, \\\"active\\\": false, \\\"elementID\\\": \\\"s5kQ\\\", \\\"dateFormat\\\": \\\"MM/DD/YYYY\\\", \\\"minimumAge\\\": \\\"21\\\", \\\"checkboxActive\\\": false, \\\"minimumAgeError\\\": \\\"You must be 21 or older to sign up\\\"}, \\\"closeIcon\\\": {\\\"size\\\": \\\"18px\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"margin\\\": \\\"0px\\\", \\\"position\\\": \\\"right\\\", \\\"ariaLabel\\\": \\\"Dismiss this popup\\\", \\\"elementID\\\": \\\"qa9d\\\", \\\"borderRadius\\\": \\\"0px\\\", \\\"strokeWeight\\\": \\\"4px\\\", \\\"backgroundColor\\\": \\\"#000000\\\", \\\"backgroundOpacity\\\": \\\"0\\\"}, \\\"customCSS\\\": \\\"\\\", \\\"pageTitle\\\": \\\"Philadelphia 76ers - Sign Up \\\", \\\"creativeName\\\": \\\"Mobile App SDK\\\", \\\"customErrorText\\\": {\\\"subscriberConflict\\\": \\\"\\\"}, \\\"countdownSection\\\": {\\\"date\\\": \\\"\\\", \\\"time\\\": \\\"12:00\\\", \\\"type\\\": \\\"RELATIVE\\\", \\\"timeZone\\\": \\\"America/New_York\\\", \\\"timerUnit\\\": \\\"MINUTES\\\", \\\"timerAmount\\\": \\\"5\\\"}, \\\"globalBorderRadius\\\": \\\"10px\\\", \\\"socialProofSection\\\": {\\\"numUsers\\\": 0, \\\"frequency\\\": \\\"WEEK\\\", \\\"threshold\\\": {\\\"active\\\": false, \\\"customThreshold\\\": 500}}, \\\"thirdPartyAnalytics\\\": {\\\"DATALAYER_PUSH\\\": {\\\"id\\\": \\\"\\\", \\\"enabled\\\": false, \\\"eventOptions\\\": {\\\"LEAD\\\": {\\\"name\\\": \\\"submitSMS\\\"}, \\\"CLOSE\\\": {\\\"name\\\": \\\"close\\\"}, \\\"EXPAND\\\": {\\\"name\\\": \\\"expand\\\"}, \\\"EMAIL_LEAD\\\": {\\\"name\\\": \\\"submitEmail\\\", \\\"reportEmail\\\": false}, \\\"IMPRESSION\\\": {\\\"name\\\": \\\"impression\\\"}}, \\\"reportCreative\\\": true}, \\\"FACEBOOK_PIXEL\\\": {\\\"id\\\": \\\"\\\", \\\"enabled\\\": false, \\\"eventOptions\\\": {\\\"LEAD\\\": {\\\"name\\\": \\\"Lead\\\"}, \\\"CLOSE\\\": {\\\"name\\\": \\\"Close\\\"}, \\\"EXPAND\\\": {\\\"name\\\": \\\"Expand\\\"}, \\\"EMAIL_LEAD\\\": {\\\"name\\\": \\\"Contact\\\", \\\"reportEmail\\\": false}, \\\"IMPRESSION\\\": {\\\"name\\\": \\\"ViewContent\\\"}}, \\\"reportCreative\\\": true}, \\\"NORTHBEAM_PIXEL\\\": {\\\"id\\\": \\\"\\\", \\\"enabled\\\": false, \\\"eventOptions\\\": {\\\"LEAD\\\": {\\\"name\\\": \\\"submitSMS\\\", \\\"reportPhone\\\": true}, \\\"EMAIL_LEAD\\\": {\\\"name\\\": \\\"submitEmail\\\", \\\"reportEmail\\\": true}}, \\\"reportCreative\\\": true}, \\\"GOOGLE_ANALYTICS\\\": {\\\"id\\\": \\\"\\\", \\\"enabled\\\": false, \\\"eventOptions\\\": {\\\"LEAD\\\": {\\\"name\\\": \\\"submitSMS\\\"}, \\\"CLOSE\\\": {\\\"name\\\": \\\"close\\\"}, \\\"EXPAND\\\": {\\\"name\\\": \\\"expand\\\"}, \\\"EMAIL_LEAD\\\": {\\\"name\\\": \\\"submitEmail\\\", \\\"reportEmail\\\": false}, \\\"IMPRESSION\\\": {\\\"name\\\": \\\"impression\\\"}}, \\\"eventCategory\\\": \\\"Attentive\\\", \\\"reportCreative\\\": true}, \\\"GOOGLE_ANALYTICS_4\\\": {\\\"id\\\": \\\"\\\", \\\"enabled\\\": false, \\\"eventOptions\\\": {\\\"LEAD\\\": {\\\"name\\\": \\\"submitSMS\\\", \\\"reportPageType\\\": false}, \\\"CLOSE\\\": {\\\"name\\\": \\\"close\\\", \\\"reportPageType\\\": false}, \\\"EXPAND\\\": {\\\"name\\\": \\\"expand\\\", \\\"reportPageType\\\": false}, \\\"EMAIL_LEAD\\\": {\\\"name\\\": \\\"submitEmail\\\", \\\"reportEmail\\\": false, \\\"reportPageType\\\": false}, \\\"IMPRESSION\\\": {\\\"name\\\": \\\"impression\\\", \\\"reportPageType\\\": false}}, \\\"reportCreative\\\": true}}}}, \\\"type\\\": \\\"ON_SITE\\\", \\\"pages\\\": [{\\\"type\\\": \\\"bubble\\\", \\\"fields\\\": {\\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1.12rem\\\", \\\"text\\\": \\\"GET 10% OFF\\\", \\\"color\\\": \\\"#003da6\\\", \\\"elementID\\\": \\\"JDVA\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"position\\\": \\\"left\\\", \\\"closeIcon\\\": {\\\"size\\\": \\\"18px\\\", \\\"color\\\": \\\"#003da6\\\", \\\"margin\\\": \\\"0px\\\", \\\"position\\\": \\\"right\\\", \\\"ariaLabel\\\": \\\"Dismiss this popup\\\", \\\"borderRadius\\\": \\\"0px\\\", \\\"strokeWeight\\\": \\\"4px\\\", \\\"backgroundColor\\\": \\\"#000000\\\", \\\"backgroundOpacity\\\": \\\"0\\\"}, \\\"elementID\\\": \\\"And0\\\", \\\"hasShadow\\\": {\\\"active\\\": false}, \\\"borderRadius\\\": \\\"10px\\\", \\\"showBubblePV\\\": \\\"1\\\", \\\"backgroundColor\\\": \\\"#ffffff\\\", \\\"countdownSection\\\": {\\\"active\\\": false, \\\"elementID\\\": \\\"zaxO\\\", \\\"countdownDisplay\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"1.12rem\\\", \\\"color\\\": \\\"#FFFFFF\\\", \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"1px\\\", \\\"active\\\": false, \\\"radius\\\": \\\"0\\\"}, \\\"location\\\": \\\"LEFT\\\", \\\"marginAbove\\\": \\\"0px\\\", \\\"marginBelow\\\": \\\"0px\\\", \\\"backgroundColor\\\": \\\"#000000\\\", \\\"backgroundOpacity\\\": \\\"100\\\", \\\"transparentBackground\\\": true}, \\\"countdownCustomLabels\\\": {\\\"active\\\": false, \\\"customMinLabel\\\": \\\"Minutes\\\", \\\"customSecLabel\\\": \\\"Seconds\\\", \\\"customDaysLabel\\\": \\\"Days\\\", \\\"customHoursLabel\\\": \\\"Hours\\\"}, \\\"countdownHideDaysDisplay\\\": false, \\\"countdownHideSecondsDisplay\\\": false}, \\\"floatingPosition\\\": {\\\"bottom\\\": \\\"70px\\\", \\\"horizontal\\\": \\\"16px\\\"}, \\\"verticalPosition\\\": \\\"bottom\\\"}}, {\\\"type\\\": \\\"field_capture\\\", \\\"fields\\\": {\\\"inputs\\\": [{\\\"type\\\": \\\"email\\\", \\\"color\\\": \\\"#000000\\\", \\\"border\\\": {\\\"color\\\": \\\"#979797\\\", \\\"width\\\": \\\"1px\\\", \\\"radius\\\": \\\"\\\"}, \\\"alignment\\\": \\\"left\\\", \\\"elementID\\\": \\\"VQxs\\\", \\\"placeholder\\\": {\\\"font\\\": 1, \\\"size\\\": \\\"1rem\\\", \\\"text\\\": \\\"Email Address\\\", \\\"color\\\": \\\"#767676\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, \\\"backgroundColor\\\": \\\"#FFFFFF\\\"}], \\\"buttons\\\": [{\\\"type\\\": \\\"cta\\\", \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"0px\\\", \\\"radius\\\": \\\"\\\"}, \\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1.25rem\\\", \\\"text\\\": \\\"CONTINUE\\\", \\\"color\\\": \\\"#003da6\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, {\\\"font\\\": 1, \\\"size\\\": \\\"0.8125rem\\\", \\\"text\\\": \\\"\\\", \\\"color\\\": \\\"#003da6\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"elementID\\\": \\\"Yp7D\\\", \\\"headerAlign\\\": \\\"left\\\", \\\"backgroundColor\\\": \\\"#ffffff\\\"}, {\\\"type\\\": \\\"dismiss\\\", \\\"active\\\": false, \\\"border\\\": {\\\"color\\\": \\\"#ffffff\\\", \\\"width\\\": \\\"0px\\\", \\\"radius\\\": \\\"\\\"}, \\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1rem\\\", \\\"text\\\": \\\"NO, I'LL PAY FULL PRICE\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"elementID\\\": \\\"VK8u\\\", \\\"buttonStyle\\\": {\\\"value\\\": \\\"underline\\\"}, \\\"backgroundColor\\\": \\\"\\\", \\\"backgroundOpacity\\\": \\\"100\\\"}, {\\\"type\\\": \\\"skip\\\", \\\"active\\\": false, \\\"border\\\": {\\\"color\\\": \\\"#FFFFFF\\\", \\\"width\\\": \\\"0px\\\", \\\"radius\\\": \\\"\\\"}, \\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1rem\\\", \\\"text\\\": \\\"ENTER PHONE NUMBER INSTEAD\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"elementID\\\": \\\"F-tA\\\", \\\"buttonStyle\\\": {\\\"value\\\": \\\"underline\\\"}, \\\"backgroundColor\\\": \\\"\\\", \\\"backgroundOpacity\\\": \\\"100\\\"}], \\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1.5rem\\\", \\\"text\\\": \\\"UNLOCK\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"alignment\\\": \\\"center\\\", \\\"elementID\\\": \\\"6u-T\\\", \\\"marginTop\\\": \\\"2.5px\\\", \\\"marginBottom\\\": \\\"2.5px\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, {\\\"font\\\": 0, \\\"size\\\": \\\"4.5rem\\\", \\\"text\\\": \\\"10% OFF\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"alignment\\\": \\\"center\\\", \\\"elementID\\\": \\\"L9tf\\\", \\\"marginTop\\\": \\\"9px\\\", \\\"marginBottom\\\": \\\"9px\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, {\\\"font\\\": 0, \\\"size\\\": \\\"1.25rem\\\", \\\"text\\\": \\\"YOUR ORDER\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"alignment\\\": \\\"center\\\", \\\"elementID\\\": \\\"b5NT\\\", \\\"marginTop\\\": \\\"2.5px\\\", \\\"marginBottom\\\": \\\"2.5px\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"legalText\\\": [], \\\"countdownSection\\\": {\\\"active\\\": false, \\\"elementID\\\": \\\"5Rx4\\\", \\\"colonSeparator\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"2rem\\\", \\\"color\\\": \\\"#000000\\\", \\\"active\\\": true}, \\\"countdownLabel\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"1rem\\\", \\\"color\\\": \\\"#000000\\\", \\\"marginAbove\\\": \\\"0px\\\", \\\"marginBelow\\\": \\\"0px\\\", \\\"backgroundColor\\\": \\\"#000000\\\", \\\"backgroundOpacity\\\": \\\"100\\\", \\\"transparentBackground\\\": true}, \\\"countdownDisplay\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"1.5rem\\\", \\\"color\\\": \\\"#000000\\\", \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"1px\\\", \\\"active\\\": false, \\\"radius\\\": \\\"0\\\"}, \\\"location\\\": \\\"ABOVE\\\", \\\"marginAbove\\\": \\\"0px\\\", \\\"marginBelow\\\": \\\"0px\\\", \\\"backgroundColor\\\": \\\"#FFFFFF\\\", \\\"backgroundOpacity\\\": \\\"100\\\", \\\"transparentBackground\\\": true}, \\\"countdownCustomLabels\\\": {\\\"active\\\": false, \\\"customMinLabel\\\": \\\"Minutes\\\", \\\"customSecLabel\\\": \\\"Seconds\\\", \\\"customDaysLabel\\\": \\\"Days\\\", \\\"customHoursLabel\\\": \\\"Hours\\\"}, \\\"countdownHideDaysDisplay\\\": false, \\\"countdownHideSecondsDisplay\\\": false, \\\"countdownCustomTimerContainer\\\": {\\\"width\\\": \\\"Hugs contents\\\", \\\"active\\\": false, \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"1px\\\", \\\"active\\\": false, \\\"radius\\\": \\\"8px\\\"}, \\\"backgroundColor\\\": \\\"#FFFFFF\\\", \\\"backgroundOpacity\\\": \\\"100\\\"}}, \\\"socialProofSection\\\": {\\\"active\\\": false, \\\"socialProofIcon\\\": {\\\"iconSize\\\": \\\"1.5rem\\\", \\\"iconColor\\\": \\\"#000000\\\", \\\"iconActive\\\": true}, \\\"socialProofText\\\": {\\\"textAfter\\\": \\\"people signed up\\\", \\\"textBefore\\\": \\\"\\\", \\\"textAlignment\\\": \\\"left\\\"}, \\\"socialProofDisplay\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"1rem\\\", \\\"color\\\": \\\"#000000\\\", \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"1px\\\", \\\"active\\\": false, \\\"radius\\\": \\\"5px\\\"}, \\\"location\\\": \\\"Above headlines\\\", \\\"containerWidth\\\": \\\"Full width\\\", \\\"backgroundColor\\\": \\\"#FFFFFF\\\", \\\"backgroundOpacity\\\": \\\"100\\\", \\\"containerMarginAbove\\\": \\\"10px\\\", \\\"containerMarginBelow\\\": \\\"10px\\\", \\\"transparentBackground\\\": true}}, \\\"productHighlightSection\\\": {\\\"active\\\": false, \\\"productImage\\\": {\\\"src\\\": \\\"\\\", \\\"url\\\": \\\"\\\", \\\"border\\\": {\\\"color\\\": \\\"#C6C7C8\\\", \\\"width\\\": \\\"2px\\\", \\\"radius\\\": \\\"8px\\\"}, \\\"height\\\": \\\"170px\\\", \\\"margin\\\": {\\\"top\\\": \\\"8px\\\", \\\"bottom\\\": \\\"8px\\\"}}, \\\"viewProductPill\\\": {\\\"border\\\": {\\\"color\\\": \\\"#C6C7C8\\\", \\\"width\\\": \\\"2px\\\", \\\"radius\\\": \\\"8px\\\"}, \\\"showIcon\\\": true, \\\"textContent\\\": {\\\"font\\\": 1, \\\"size\\\": \\\"1rem\\\", \\\"text\\\": \\\"View Product\\\", \\\"color\\\": \\\"#000000\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, \\\"backgroundColor\\\": \\\"#FFFFFF\\\"}}}, \\\"dataPoints\\\": [\\\"email\\\"]}, {\\\"type\\\": \\\"field_capture\\\", \\\"fields\\\": {\\\"inputs\\\": [], \\\"buttons\\\": [{\\\"type\\\": \\\"cta\\\", \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"0px\\\", \\\"radius\\\": \\\"\\\"}, \\\"ctaIcon\\\": {\\\"src\\\": \\\"\\\", \\\"color\\\": \\\"#003da6\\\", \\\"active\\\": true}, \\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1.25rem\\\", \\\"text\\\": \\\"GET 10% OFF NOW\\\", \\\"color\\\": \\\"#003da6\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, {\\\"font\\\": 1, \\\"size\\\": \\\"0.8125rem\\\", \\\"text\\\": \\\"when you sign up for email and texts\\\", \\\"color\\\": \\\"#003da6\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"elementID\\\": \\\"Sj8X\\\", \\\"headerAlign\\\": \\\"left\\\", \\\"backgroundColor\\\": \\\"#ffffff\\\"}, {\\\"type\\\": \\\"dismiss\\\", \\\"active\\\": false, \\\"border\\\": {\\\"color\\\": \\\"#ffffff\\\", \\\"width\\\": \\\"1px\\\", \\\"radius\\\": \\\"\\\"}, \\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1rem\\\", \\\"text\\\": \\\"NO, I'LL PAY FULL PRICE\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"elementID\\\": \\\"zK3R\\\", \\\"buttonStyle\\\": {\\\"value\\\": \\\"outline\\\"}, \\\"backgroundColor\\\": \\\"\\\", \\\"backgroundOpacity\\\": \\\"100\\\"}], \\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1.5rem\\\", \\\"text\\\": \\\"UNLOCK\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"alignment\\\": \\\"center\\\", \\\"elementID\\\": \\\"al-s\\\", \\\"marginTop\\\": \\\"2.5px\\\", \\\"marginBottom\\\": \\\"2.5px\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, {\\\"font\\\": 0, \\\"size\\\": \\\"4.5rem\\\", \\\"text\\\": \\\"10% OFF\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"alignment\\\": \\\"center\\\", \\\"elementID\\\": \\\"InLn\\\", \\\"marginTop\\\": \\\"9px\\\", \\\"marginBottom\\\": \\\"9px\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, {\\\"font\\\": 0, \\\"size\\\": \\\"1.25rem\\\", \\\"text\\\": \\\"YOUR ORDER\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"alignment\\\": \\\"center\\\", \\\"elementID\\\": \\\"Y4F-\\\", \\\"marginTop\\\": \\\"2.5px\\\", \\\"marginBottom\\\": \\\"2.5px\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"legalText\\\": [{\\\"v\\\": 2, \\\"styles\\\": {\\\"font\\\": 2, \\\"color\\\": \\\"#FFFFFF\\\", \\\"fontSize\\\": \\\"0.625rem\\\", \\\"textAlign\\\": \\\"justify\\\", \\\"backgroundColor\\\": \\\"#003da6\\\", \\\"backgroundOpacity\\\": \\\"100%\\\", \\\"transparentBackground\\\": false}, \\\"content\\\": {}, \\\"elementID\\\": \\\"w2Zc\\\", \\\"placement\\\": \\\"below-headers\\\"}], \\\"countdownSection\\\": {\\\"active\\\": false, \\\"elementID\\\": \\\"8X7i\\\", \\\"colonSeparator\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"2rem\\\", \\\"color\\\": \\\"#000000\\\", \\\"active\\\": true}, \\\"countdownLabel\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"1rem\\\", \\\"color\\\": \\\"#000000\\\", \\\"marginAbove\\\": \\\"0px\\\", \\\"marginBelow\\\": \\\"0px\\\", \\\"backgroundColor\\\": \\\"#000000\\\", \\\"backgroundOpacity\\\": \\\"100\\\", \\\"transparentBackground\\\": true}, \\\"countdownDisplay\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"1.5rem\\\", \\\"color\\\": \\\"#000000\\\", \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"1px\\\", \\\"active\\\": false, \\\"radius\\\": \\\"0\\\"}, \\\"location\\\": \\\"ABOVE\\\", \\\"marginAbove\\\": \\\"0px\\\", \\\"marginBelow\\\": \\\"0px\\\", \\\"backgroundColor\\\": \\\"#FFFFFF\\\", \\\"backgroundOpacity\\\": \\\"100\\\", \\\"transparentBackground\\\": true}, \\\"countdownCustomLabels\\\": {\\\"active\\\": false, \\\"customMinLabel\\\": \\\"Minutes\\\", \\\"customSecLabel\\\": \\\"Seconds\\\", \\\"customDaysLabel\\\": \\\"Days\\\", \\\"customHoursLabel\\\": \\\"Hours\\\"}, \\\"countdownHideDaysDisplay\\\": false, \\\"countdownHideSecondsDisplay\\\": false, \\\"countdownCustomTimerContainer\\\": {\\\"width\\\": \\\"Hugs contents\\\", \\\"active\\\": false, \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"1px\\\", \\\"active\\\": false, \\\"radius\\\": \\\"8px\\\"}, \\\"backgroundColor\\\": \\\"#FFFFFF\\\", \\\"backgroundOpacity\\\": \\\"100\\\"}}, \\\"socialProofSection\\\": {\\\"active\\\": false, \\\"socialProofIcon\\\": {\\\"iconSize\\\": \\\"1.5rem\\\", \\\"iconColor\\\": \\\"#000000\\\", \\\"iconActive\\\": true}, \\\"socialProofText\\\": {\\\"textAfter\\\": \\\"people signed up\\\", \\\"textBefore\\\": \\\"\\\", \\\"textAlignment\\\": \\\"left\\\"}, \\\"socialProofDisplay\\\": {\\\"font\\\": 2, \\\"size\\\": \\\"1rem\\\", \\\"color\\\": \\\"#000000\\\", \\\"border\\\": {\\\"color\\\": \\\"#000000\\\", \\\"width\\\": \\\"1px\\\", \\\"active\\\": false, \\\"radius\\\": \\\"5px\\\"}, \\\"location\\\": \\\"Above headlines\\\", \\\"containerWidth\\\": \\\"Full width\\\", \\\"backgroundColor\\\": \\\"#FFFFFF\\\", \\\"backgroundOpacity\\\": \\\"100\\\", \\\"containerMarginAbove\\\": \\\"10px\\\", \\\"containerMarginBelow\\\": \\\"10px\\\", \\\"transparentBackground\\\": true}}, \\\"completeYourSignupImage\\\": {\\\"src\\\": \\\"\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"active\\\": true, \\\"elementID\\\": \\\"GAIw\\\"}, \\\"productHighlightSection\\\": {\\\"active\\\": false, \\\"productImage\\\": {\\\"src\\\": \\\"\\\", \\\"url\\\": \\\"\\\", \\\"border\\\": {\\\"color\\\": \\\"#C6C7C8\\\", \\\"width\\\": \\\"2px\\\", \\\"radius\\\": \\\"8px\\\"}, \\\"height\\\": \\\"170px\\\", \\\"margin\\\": {\\\"top\\\": \\\"8px\\\", \\\"bottom\\\": \\\"8px\\\"}}, \\\"viewProductPill\\\": {\\\"border\\\": {\\\"color\\\": \\\"#C6C7C8\\\", \\\"width\\\": \\\"2px\\\", \\\"radius\\\": \\\"8px\\\"}, \\\"showIcon\\\": true, \\\"textContent\\\": {\\\"font\\\": 1, \\\"size\\\": \\\"1rem\\\", \\\"text\\\": \\\"View Product\\\", \\\"color\\\": \\\"#000000\\\", \\\"letterSpacing\\\": \\\"0px\\\"}, \\\"backgroundColor\\\": \\\"#FFFFFF\\\"}}}, \\\"dataPoints\\\": [\\\"sms\\\"], \\\"fieldOverrides\\\": {\\\"skipEmailPage\\\": {\\\"buttons\\\": [{\\\"headers\\\": [{}, {\\\"text\\\": \\\"when you sign up for texts\\\"}]}, {\\\"type\\\": \\\"dismiss\\\", \\\"active\\\": true, \\\"border\\\": {\\\"color\\\": \\\"#ffffff\\\", \\\"width\\\": \\\"1px\\\", \\\"radius\\\": \\\"\\\"}, \\\"headers\\\": [{\\\"font\\\": 0, \\\"size\\\": \\\"1rem\\\", \\\"text\\\": \\\"NO THANKS\\\", \\\"color\\\": \\\"#ffffff\\\", \\\"letterSpacing\\\": \\\"0px\\\"}], \\\"buttonStyle\\\": {\\\"value\\\": \\\"outline\\\"}, \\\"backgroundColor\\\": \\\"\\\", \\\"backgroundOpacity\\\": \\\"100\\\"}], \\\"completeYourSignupImage\\\": {\\\"active\\\": false}}}}], \\\"subType\\\": \\\"DYNAMIC\\\", \\\"devTemplateId\\\": 110, \\\"prodTemplateId\\\": 117}\",\n" +
        "                    \"creativeId\": \"1130223\",\n" +
        "                    \"customerTermsLink\": \"https://www.nba.com/termsofuse\",\n" +
        "                    \"deciders\": {\n" +
        "                        \"ENABLE_SIGNUP_UNIT_EXIT_INTENT\": true,\n" +
        "                        \"ENABLE_NEW_CREATIVES_MOBILE_FLOATING_LAYOUT\": true,\n" +
        "                        \"ENABLE_CREATIVE_SPECIFIC_ALREADY_SUB_MESSAGE\": true,\n" +
        "                        \"ENABLE_INVISIBLE_CAPTCHA\": true,\n" +
        "                        \"CREATIVES_VQA_SPINTOWIN\": true,\n" +
        "                        \"DATADOG_JAVASCRIPT_SAMPLING_RATE\": true,\n" +
        "                        \"ENABLE_CDS_MESSAGE_PARAMETERS\": true\n" +
        "                    },\n" +
        "                    \"defaultTermsLink\": \"https://attnl.tv/t/7te\",\n" +
        "                    \"displayName\": \"Philadelphia+76ers\",\n" +
        "                    \"emailAddress\": \"\",\n" +
        "                    \"encodedCompanyExternalId\": \"7te\",\n" +
        "                    \"encodedSubscriberExternalId\": \"0\",\n" +
        "                    \"environment\": \"prod\",\n" +
        "                    \"impressionId\": \"0\",\n" +
        "                    \"isSubscriber\": false,\n" +
        "                    \"language\": \"en-US\",\n" +
        "                    \"pageview\": \"2\",\n" +
        "                    \"pageviewRuleLowerBound\": \"1\",\n" +
        "                    \"privacyLink\": \"https://www.nba.com/privacy-policy\",\n" +
        "                    \"showEmail\": true,\n" +
        "                    \"sourceUrl\": \"https://creatives.attn.tv/mobile-apps/index.html?domain=76ers&amp;debug=matter-trip-grass-symbol&amp;sdkVersion=1.0.2-beta.8&amp;sdkName=attentive-android-sdk&amp;skipFatigue=false&amp;vid=97ae11f2fadb4137b805ceda2b549714\",\n" +
        "                    \"subscriberChannels\": [],\n" +
        "                    \"templateId\": \"117\",\n" +
        "                    \"termsLink\": \"https://attnl.tv/t/7te\",\n" +
        "                    \"userId\": \"98184b00e9a743a0aee4d7f9ab56ebfa\"\n" +
        "                }\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "}"
@Composable
fun DebugScreenComposables(navHostController: NavHostController) {
    DebugScreenContent(navHostController)
}

@Composable
fun DebugScreenContent(navHostController: NavHostController) {
    val activity = requireNotNull(LocalActivity.current) {
        "Activity required for Creative initialization"
    }
    val context = LocalContext.current.applicationContext as Application

    // Create the FrameLayout once
    val frameLayout = remember {
        FrameLayout(activity.baseContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }


    // Configure AttentiveConfig once
    val config = remember {
        AttentiveConfig.Builder()
            .domain("YOUR_ATTENTIVE_DOMAIN")
            .mode(AttentiveConfig.Mode.DEBUG)
            .applicationContext(context)
            .build()
    }
    AttentiveEventTracker.instance.config = config

    // Create the Creative instance once
    val creative = remember(frameLayout, activity) {
        Creative(AttentiveEventTracker.instance.config!!, frameLayout, activity)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val context = LocalContext.current
        SimpleToolbar("Debug Screen", actions = {
            IconButton(
                onClick = {
                    shareDebugOutput(context)
                }
            ) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
            }
        }, navHostController)
        Column (modifier = Modifier.fillMaxSize().align(Alignment.CenterHorizontally)){
            var isTextVisible by remember { mutableStateOf(false) }
            Button(modifier = Modifier.align(Alignment.CenterHorizontally), colors = ButtonDefaults.buttonColors(containerColor = BonniYellow), onClick = {
                isTextVisible = true
               // creative.trigger() // Call the trigger method
            }) {
                Text("Get creative json", color = Color.Black)
            }

            if(isTextVisible) {
                Text(
                    fakeJson
                )
            }
            AdvertisementView(frameLayout)

        }
    }
}

fun shareDebugOutput(context: Context){
    // Create a share intent
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$fakeJson")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Add this flag
    }

    // Start the share intent
    context.startActivity(Intent.createChooser(shareIntent, "Share Push Token"))

}

@Composable
fun AdvertisementView(frameLayout: FrameLayout) {
    AndroidView(factory = { frameLayout })
}