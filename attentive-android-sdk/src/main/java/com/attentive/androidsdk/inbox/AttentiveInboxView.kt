package com.attentive.androidsdk.inbox

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.attentive.androidsdk.R

/**
 * A custom Android View that wraps the Compose AttentiveInbox for legacy View system apps.
 * This View can be used in XML layouts or created programmatically.
 *
 * Features:
 * - Supports XML attributes for colors and fonts
 * - Provides setters for programmatic customization
 * - Automatically handles lifecycle through AbstractComposeView
 * - Observes AttentiveSdk.inboxState for message updates
 *
 * XML Usage:
 * ```xml
 * <com.attentive.androidsdk.inbox.AttentiveInboxView
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:attentive_backgroundColor="@color/white"
 *     app:attentive_titleTextColor="@color/black"
 *     app:attentive_titleFontFamily="@font/custom_font" />
 * ```
 *
 * Programmatic Usage (Kotlin):
 * ```kotlin
 * val inboxView = AttentiveInboxView(context)
 * inboxView.setTitleTextColor(Color.BLACK)
 * inboxView.setTitleFontFamily(R.font.my_font)
 * ```
 *
 * @param context The Android Context
 * @param attrs Optional AttributeSet for XML attributes
 * @param defStyleAttr Optional default style attribute
 */
class AttentiveInboxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    // Color properties - loaded from resources in init block
    // Using mutableStateOf so Compose automatically recomposes when these change
    private var backgroundColor by mutableStateOf(Color.Unspecified)
    private var unreadIndicatorColor by mutableStateOf(Color.Unspecified)
    private var titleTextColor by mutableStateOf(Color.Unspecified)
    private var bodyTextColor by mutableStateOf(Color.Unspecified)
    private var timestampTextColor by mutableStateOf(Color.Unspecified)
    private var swipeBackgroundColor by mutableStateOf(Color.Unspecified)

    // Font properties (nullable - use system defaults when null)
    private var titleFontFamily by mutableStateOf<FontFamily?>(null)
    private var bodyFontFamily by mutableStateOf<FontFamily?>(null)
    private var timestampFontFamily by mutableStateOf<FontFamily?>(null)

    init {
        backgroundColor = Color(ContextCompat.getColor(context, R.color.attentive_inbox_background))
        unreadIndicatorColor = Color(ContextCompat.getColor(context, R.color.attentive_inbox_unread_indicator))
        titleTextColor = Color(ContextCompat.getColor(context, R.color.attentive_inbox_title_text))
        bodyTextColor = Color(ContextCompat.getColor(context, R.color.attentive_inbox_body_text))
        timestampTextColor = Color(ContextCompat.getColor(context, R.color.attentive_inbox_timestamp_text))
        swipeBackgroundColor = Color(ContextCompat.getColor(context, R.color.attentive_inbox_swipe_background))

        // Parse XML attributes if provided (these override the resource defaults)
        attrs?.let { parseAttributes(it) }
    }

    /**
     * Parses XML attributes and updates color/font properties
     */
    private fun parseAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AttentiveInboxView)

        try {
            // Read color attributes (convert Android Color int to Compose Color)
            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_backgroundColor)) {
                backgroundColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_backgroundColor,
                        ContextCompat.getColor(context, R.color.attentive_inbox_background)
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_unreadIndicatorColor)) {
                unreadIndicatorColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_unreadIndicatorColor,
                        ContextCompat.getColor(context, R.color.attentive_inbox_unread_indicator)
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_titleTextColor)) {
                titleTextColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_titleTextColor,
                        ContextCompat.getColor(context, R.color.attentive_inbox_title_text)
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_bodyTextColor)) {
                bodyTextColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_bodyTextColor,
                        ContextCompat.getColor(context, R.color.attentive_inbox_body_text)
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_timestampTextColor)) {
                timestampTextColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_timestampTextColor,
                        ContextCompat.getColor(context, R.color.attentive_inbox_timestamp_text)
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_swipeBackgroundColor)) {
                swipeBackgroundColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_swipeBackgroundColor,
                        ContextCompat.getColor(context, R.color.attentive_inbox_swipe_background)
                    )
                )
            }

            // Read font attributes (convert resource ID to FontFamily)
            val titleFontRes = typedArray.getResourceId(
                R.styleable.AttentiveInboxView_attentive_titleFontFamily,
                0
            )
            if (titleFontRes != 0) {
                titleFontFamily = FontFamily(Font(titleFontRes))
            }

            val bodyFontRes = typedArray.getResourceId(
                R.styleable.AttentiveInboxView_attentive_bodyFontFamily,
                0
            )
            if (bodyFontRes != 0) {
                bodyFontFamily = FontFamily(Font(bodyFontRes))
            }

            val timestampFontRes = typedArray.getResourceId(
                R.styleable.AttentiveInboxView_attentive_timestampFontFamily,
                0
            )
            if (timestampFontRes != 0) {
                timestampFontFamily = FontFamily(Font(timestampFontRes))
            }

        } finally {
            // Always recycle TypedArray to prevent memory leak
            typedArray.recycle()
        }
    }

    /**
     * Override Content() to provide the Compose UI
     * This is called automatically by AbstractComposeView
     */
    @Composable
    override fun Content() {
        AttentiveInbox(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = backgroundColor,
            unreadIndicatorColor = unreadIndicatorColor,
            titleTextColor = titleTextColor,
            bodyTextColor = bodyTextColor,
            timestampTextColor = timestampTextColor,
            swipeBackgroundColor = swipeBackgroundColor,
            titleFontFamily = titleFontFamily,
            bodyFontFamily = bodyFontFamily,
            timestampFontFamily = timestampFontFamily
        )
    }

    // Public setters for programmatic customization
    // Note: Since we use mutableStateOf, Compose automatically recomposes when these properties change

    /**
     * Sets the background color of the inbox
     * @param color Android color int (e.g., Color.WHITE, 0xFFFFFFFF.toInt())
     */
    fun setInboxBackgroundColor(color: Int) {
        backgroundColor = Color(color)
    }

    /**
     * Sets the unread indicator dot color
     * @param color Android color int
     */
    fun setUnreadIndicatorColor(color: Int) {
        unreadIndicatorColor = Color(color)
    }

    /**
     * Sets the message title text color
     * @param color Android color int
     */
    fun setTitleTextColor(color: Int) {
        titleTextColor = Color(color)
    }

    /**
     * Sets the message body text color
     * @param color Android color int
     */
    fun setBodyTextColor(color: Int) {
        bodyTextColor = Color(color)
    }

    /**
     * Sets the timestamp text color
     * @param color Android color int
     */
    fun setTimestampTextColor(color: Int) {
        timestampTextColor = Color(color)
    }

    /**
     * Sets the swipe gesture background color
     * @param color Android color int
     */
    fun setSwipeBackgroundColor(color: Int) {
        swipeBackgroundColor = Color(color)
    }

    /**
     * Sets the font family for message titles
     * @param fontResId Font resource ID (e.g., R.font.my_font)
     */
    fun setTitleFontFamily(fontResId: Int) {
        titleFontFamily = FontFamily(Font(fontResId))
    }

    /**
     * Sets the font family for message body text
     * @param fontResId Font resource ID
     */
    fun setBodyFontFamily(fontResId: Int) {
        bodyFontFamily = FontFamily(Font(fontResId))
    }

    /**
     * Sets the font family for timestamps
     * @param fontResId Font resource ID
     */
    fun setTimestampFontFamily(fontResId: Int) {
        timestampFontFamily = FontFamily(Font(fontResId))
    }
}
