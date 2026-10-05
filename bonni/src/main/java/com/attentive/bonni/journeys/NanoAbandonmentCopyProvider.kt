package com.attentive.bonni.journeys

import com.attentive.androidsdk.journeys.AbandonedCart
import com.attentive.androidsdk.journeys.AbandonmentCopyProvider
import com.attentive.androidsdk.journeys.NotificationCopy
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Writes cart abandonment notification copy on-device with ML Kit GenAI Prompt (Gemini Nano).
 * Returns `null`, so the SDK uses its default copy, when Nano isn't ready on this device.
 */
class NanoAbandonmentCopyProvider : AbandonmentCopyProvider {
    private val model by lazy { Generation.getClient() }

    override suspend fun createCopy(cart: AbandonedCart): NotificationCopy? {
        return try {
            val status = model.checkStatus()
            if (status != FeatureStatus.AVAILABLE) {
                Timber.d("Gemini Nano not ready for cart copy (status $status)")
                return null
            }
            val text = model.generateContent(buildPrompt(cart)).candidates.firstOrNull()?.text
            Timber.d("Gemini Nano cart copy: $text")
            parse(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Gemini Nano cart copy failed")
            null
        }
    }

    private fun buildPrompt(cart: AbandonedCart): String =
        buildString {
            appendLine(
                "Write a push notification for a shopper who left items in their cart at Bonni Beauty. " +
                    "Be warm and brief, mention the product by name, and don't invent discounts or deadlines.",
            )
            appendLine("Reply with exactly two lines: TITLE: <at most 40 characters> and BODY: <at most 100 characters>.")
            appendLine()
            appendLine("Cart:")
            cart.items.forEach { item ->
                appendLine("- ${item.quantity} x ${item.name ?: "item"} (${item.price.price} ${item.price.currency.currencyCode})")
            }
        }

    private fun parse(text: String?): NotificationCopy? {
        if (text.isNullOrBlank()) return null
        val lines = text.lines().map { it.trim() }
        val title = lines.firstOrNull { it.startsWith("TITLE:", ignoreCase = true) }?.substringAfter(':')?.trim()
        val body = lines.firstOrNull { it.startsWith("BODY:", ignoreCase = true) }?.substringAfter(':')?.trim()
        return if (title.isNullOrEmpty() || body.isNullOrEmpty()) null else NotificationCopy(title, body)
    }
}
