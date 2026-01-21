package com.attentive.androidsdk.inbox

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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

    // Color properties with defaults matching AttentiveInbox composable
    private var backgroundColor: Color = Color.White
    private var unreadIndicatorColor: Color = Color(0xFFFFC5B9)
    private var titleTextColor: Color = Color(0xFF1A1E22)
    private var bodyTextColor: Color = Color.DarkGray
    private var timestampTextColor: Color = Color.Gray
    private var swipeBackgroundColor: Color = Color(0xFFFFC5B9)

    // Font properties (nullable - use system defaults when null)
    private var titleFontFamily: FontFamily? = null
    private var bodyFontFamily: FontFamily? = null
    private var timestampFontFamily: FontFamily? = null

    init {
        // Parse XML attributes if provided
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
                        android.graphics.Color.WHITE
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_unreadIndicatorColor)) {
                unreadIndicatorColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_unreadIndicatorColor,
                        0xFFFFC5B9.toInt()
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_titleTextColor)) {
                titleTextColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_titleTextColor,
                        0xFF1A1E22.toInt()
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_bodyTextColor)) {
                bodyTextColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_bodyTextColor,
                        android.graphics.Color.DKGRAY
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_timestampTextColor)) {
                timestampTextColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_timestampTextColor,
                        android.graphics.Color.GRAY
                    )
                )
            }

            if (typedArray.hasValue(R.styleable.AttentiveInboxView_attentive_swipeBackgroundColor)) {
                swipeBackgroundColor = Color(
                    typedArray.getColor(
                        R.styleable.AttentiveInboxView_attentive_swipeBackgroundColor,
                        0xFFFFC5B9.toInt()
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

    /**
     * Helper to trigger recomposition when properties change
     */
    private fun triggerRecomposition() {
        disposeComposition()
        createComposition()
    }

    // Public setters for programmatic customization

    /**
     * Sets the background color of the inbox
     * @param color Android color int (e.g., Color.WHITE, 0xFFFFFFFF.toInt())
     */
    fun setInboxBackgroundColor(color: Int) {
        backgroundColor = Color(color)
        triggerRecomposition()
    }

    /**
     * Sets the unread indicator dot color
     * @param color Android color int
     */
    fun setUnreadIndicatorColor(color: Int) {
        unreadIndicatorColor = Color(color)
        triggerRecomposition()
    }

    /**
     * Sets the message title text color
     * @param color Android color int
     */
    fun setTitleTextColor(color: Int) {
        titleTextColor = Color(color)
        triggerRecomposition()
    }

    /**
     * Sets the message body text color
     * @param color Android color int
     */
    fun setBodyTextColor(color: Int) {
        bodyTextColor = Color(color)
        triggerRecomposition()
    }

    /**
     * Sets the timestamp text color
     * @param color Android color int
     */
    fun setTimestampTextColor(color: Int) {
        timestampTextColor = Color(color)
        triggerRecomposition()
    }

    /**
     * Sets the swipe gesture background color
     * @param color Android color int
     */
    fun setSwipeBackgroundColor(color: Int) {
        swipeBackgroundColor = Color(color)
        triggerRecomposition()
    }

    /**
     * Sets the font family for message titles
     * @param fontResId Font resource ID (e.g., R.font.my_font)
     */
    fun setTitleFontFamily(fontResId: Int) {
        titleFontFamily = FontFamily(Font(fontResId))
        triggerRecomposition()
    }

    /**
     * Sets the font family for message body text
     * @param fontResId Font resource ID
     */
    fun setBodyFontFamily(fontResId: Int) {
        bodyFontFamily = FontFamily(Font(fontResId))
        triggerRecomposition()
    }

    /**
     * Sets the font family for timestamps
     * @param fontResId Font resource ID
     */
    fun setTimestampFontFamily(fontResId: Int) {
        timestampFontFamily = FontFamily(Font(fontResId))
        triggerRecomposition()
    }
}
