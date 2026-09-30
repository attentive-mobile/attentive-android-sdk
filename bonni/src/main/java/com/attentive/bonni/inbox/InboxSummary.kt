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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber

/** Summarizes up to this many unread messages; keeps the prompt well under Gemini Nano's token limit. */
private const val MAX_MESSAGES = 10

/**
 * Matches the model's `[phrase](…)` references. The parenthesized part should be one 1-based message
 * number, but the model sometimes lists several ("1 & 5", "2, 7 and 8") or writes something else.
 */
private val MESSAGE_LINK = Regex("""\[([^\]]+)\]\(([^)]*)\)""")
private val NUMBER = Regex("""\d+""")

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
        val text = model.generateContent(buildPrompt(unread)).candidates.firstOrNull()?.text?.trim()
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
            "Whenever you mention a message, wrap the words about it as [words](N), where N is the " +
                "message's number. Use exactly one number per link, never a list like (1 & 5). " +
                "Example: Your [order has shipped](3) and there's a [weekend sale](2).",
        )
        appendLine()
        unread.forEachIndexed { i, m -> appendLine("${i + 1}. ${m.title}: ${m.body}") }
    }

/**
 * Turns `[phrase](N)` references into links to message N. When a reference lists several numbers,
 * the phrase links to the first one in [unread]. References to no message in [unread] keep their
 * phrase as plain text.
 */
private fun linkify(
    text: String,
    unread: List<Message>,
    onClick: (Message) -> Unit,
): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        for (match in MESSAGE_LINK.findAll(text)) {
            append(text, last, match.range.first)
            val phrase = match.groupValues[1]
            val message =
                NUMBER.findAll(match.groupValues[2])
                    .firstNotNullOfOrNull { unread.getOrNull(it.value.toInt() - 1) }
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
            last = match.range.last + 1
        }
        append(text, last, text.length)
    }
