package com.attentive.bonni.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentive.androidsdk.inbox.Message
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.random.Random

/** Summarizes up to this many unread messages; keeps the prompt well under Gemini Nano's token limit. */
private const val MAX_MESSAGES = 10

/**
 * Matches the model's `[phrase](Message title)` links, and bare `[…]` brackets it sometimes writes
 * instead. Group 1 is any leading whitespace, group 2 the bracketed text, group 3 the target if present.
 */
private val MESSAGE_REFERENCE = Regex("""(\s*)\[([^\]]+)](?:\(([^)]*)\))?""")
private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")

private sealed interface SummaryState {
    data object Loading : SummaryState

    data object Downloading : SummaryState

    data object Unavailable : SummaryState

    data class Ready(val text: String) : SummaryState

    data class Failed(val message: String) : SummaryState
}

/**
 * An on-device Gemini Nano summary of the unread [messages], with phrases linking to the messages
 * they describe. Hidden when nothing is unread.
 */
@Composable
fun InboxSummary(
    messages: List<Message>,
    fontFamily: FontFamily,
    onMessageClick: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unread = messages.filter { !it.isRead }.take(MAX_MESSAGES)
    if (unread.isEmpty()) return

    val model = remember { Generation.getClient() }
    DisposableEffect(model) { onDispose { model.close() } }

    var state by remember { mutableStateOf<SummaryState>(SummaryState.Loading) }
    LaunchedEffect(unread.map { it.id }) {
        state = SummaryState.Loading
        state = summarize(model, unread) { state = it }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xFFFFF1EE), RoundedCornerShape(12.dp))
                .padding(16.dp),
    ) {
        Text(
            text = "✨ Inbox summary",
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.DarkGray,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text =
                when (val s = state) {
                    SummaryState.Loading -> AnnotatedString("Summarizing ${unread.size} unread messages…")
                    SummaryState.Downloading -> AnnotatedString("Downloading Gemini Nano…")
                    SummaryState.Unavailable -> AnnotatedString("Gemini Nano isn't available on this device.")
                    is SummaryState.Ready -> linkify(s.text, unread, onMessageClick)
                    is SummaryState.Failed -> AnnotatedString("Couldn't summarize: ${s.message}")
                },
            fontFamily = fontFamily,
            fontSize = 16.sp,
            color = Color.Black,
        )
    }
}

private suspend fun summarize(
    model: GenerativeModel,
    unread: List<Message>,
    onProgress: (SummaryState) -> Unit,
): SummaryState {
    return try {
        when (model.checkStatus()) {
            FeatureStatus.UNAVAILABLE -> return SummaryState.Unavailable
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                onProgress(SummaryState.Downloading)
                val result =
                    model.download().first {
                        Timber.d("Gemini Nano download: $it")
                        it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed
                    }
                if (result is DownloadStatus.DownloadFailed) throw result.e
                onProgress(SummaryState.Loading)
            }
        }
        // A fresh seed per request so each summary is worded differently.
        val request =
            generateContentRequest(TextPart(buildPrompt(unread))) {
                temperature = 0.6f
                topK = 40
                seed = Random.nextInt(Int.MAX_VALUE)
            }
        val text = model.generateContent(request).candidates.firstOrNull()?.text?.trim()
        Timber.d("Inbox summary: $text")
        if (text.isNullOrEmpty()) SummaryState.Failed("empty response") else SummaryState.Ready(text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Inbox summary failed")
        SummaryState.Failed(e.message ?: e.javaClass.simpleName)
    }
}

private fun buildPrompt(unread: List<Message>): String =
    buildString {
        appendLine(
            "Summarize these unread messages from a shopping app in one or two short, friendly " +
                "sentences addressed to the shopper. Mention the most useful offers or order updates. " +
                "No bullet points, no preamble.",
        )
        appendLine(
            "Whenever you mention a message, wrap a few words about it as [words](Title), where Title " +
                "is that message's exact title. Link every message you mention.",
        )
        appendLine()
        appendLine("Example messages:")
        appendLine("- Summer Sale: 30% off all sandals through Sunday.")
        appendLine("- Order Delivered: Order #1182 was left at your door.")
        appendLine("- Points Update: You have 400 points, enough for a free tote.")
        appendLine("Example summary:")
        appendLine(
            "Your [order was delivered](Order Delivered), you have [enough points for a free tote](Points Update), " +
                "and [sandals are 30% off](Summer Sale) through Sunday.",
        )
        appendLine()
        appendLine("Messages:")
        unread.forEach { appendLine("- ${it.title}: ${it.body}") }
    }

/**
 * Turns `[phrase](Message title)` references into links to that message. A target that matches no
 * title, and a bare `[phrase]`, keep the phrase as plain text; a bare `[Message title]` is dropped,
 * since it only repeats a title the sentence already describes.
 */
private fun linkify(
    text: String,
    unread: List<Message>,
    onClick: (Message) -> Unit,
): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        for (match in MESSAGE_REFERENCE.findAll(text)) {
            append(text, last, match.range.first)
            last = match.range.last + 1
            val (space, phrase, target) = match.destructured
            if (match.groups[3] == null) {
                if (findMessage(phrase, unread) == null) append(space + phrase)
                continue
            }
            append(space)
            val message = findMessage(target, unread)
            if (message == null) {
                append(phrase)
            } else {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = message.id,
                        styles =
                            TextLinkStyles(
                                SpanStyle(color = Color(0xFFD45D79), textDecoration = TextDecoration.Underline),
                            ),
                    ) { onClick(message) },
                ) { append(phrase) }
            }
        }
        append(text, last, text.length)
    }

/**
 * The message whose title [reference] names: an exact match ignoring case and punctuation, then one
 * title containing the other, then the title sharing the most words (at least half of them).
 */
private fun findMessage(
    reference: String,
    unread: List<Message>,
): Message? {
    val key = normalize(reference)
    if (key.isEmpty()) return null
    unread.firstOrNull { normalize(it.title) == key }?.let { return it }
    unread.firstOrNull {
        val title = normalize(it.title)
        title.contains(key) || key.contains(title)
    }?.let { return it }
    val keyWords = key.split(' ').toSet()
    return unread
        .map { it to normalize(it.title).split(' ').toSet() }
        .map { (message, words) -> message to words.intersect(keyWords).size.toDouble() / words.union(keyWords).size }
        .filter { (_, overlap) -> overlap >= 0.5 }
        .maxByOrNull { (_, overlap) -> overlap }
        ?.first
}

private fun normalize(text: String): String = text.lowercase().replace(NON_WORD, " ").trim()
