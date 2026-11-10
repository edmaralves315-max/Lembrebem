package org.lembrebem.clinical

import java.time.Instant
import java.util.UUID

/** Engines must return source text; none may activate or mutate a medication plan. */
interface TranscriptionEngine {
    fun supportedLocales(): Set<String>
    suspend fun transcribe(audio: ByteArray, locale: String): TranscriptionResult
}
sealed interface TranscriptionResult {
    data class Available(val literalText: String) : TranscriptionResult
    data object ManualRequired : TranscriptionResult
}
class ManualTranscriptionEngine : TranscriptionEngine {
    override fun supportedLocales() = emptySet<String>()
    override suspend fun transcribe(audio: ByteArray, locale: String) = TranscriptionResult.ManualRequired
}
interface DocumentExtractionEngine { suspend fun extract(uri: android.net.Uri): String }
enum class DraftCategory { MEDICATION, EXAM, RETURN, GUIDANCE }
data class EvidenceProposal(val category: DraftCategory, val literalText: String, val line: Int)
data class ConsultationDraft(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: String = Instant.now().toString(),
    val source: String,
    val proposals: List<EvidenceProposal>,
    val status: String = "UNVALIDATED"
)
interface ConsultationSummaryEngine { fun draft(literalText: String): ConsultationDraft }
/** Conservative lexical organization, never dosage/date interpretation or fabricated confidence. */
class EvidenceSummaryEngine : ConsultationSummaryEngine {
    override fun draft(literalText: String): ConsultationDraft {
        val proposals = literalText.lineSequence().mapIndexedNotNull { index, text ->
            val line = text.trim()
            if (line.isEmpty()) null else {
                val lower = line.lowercase(java.util.Locale.ROOT)
                val category = when {
                    listOf("medic", "remédio", "remedio", "薬").any(lower::contains) -> DraftCategory.MEDICATION
                    listOf("exam", "検査").any(lower::contains) -> DraftCategory.EXAM
                    listOf("retorn", "return", "follow-up", "再診", "revisão").any(lower::contains) -> DraftCategory.RETURN
                    else -> DraftCategory.GUIDANCE
                }
                EvidenceProposal(category, line, index + 1)
            }
        }.toList()
        return ConsultationDraft(source = literalText, proposals = proposals)
    }
}
