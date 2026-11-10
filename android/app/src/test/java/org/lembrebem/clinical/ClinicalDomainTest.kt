package org.lembrebem.clinical

import org.junit.Assert.*
import org.junit.Test

class ClinicalDomainTest {
    @Test fun preservesLiteralPrescriptionAndNeverActivates() {
        val source = "Medicamento fictício AB 0,25 mg — conforme receita\nRetorno sem data"
        val draft = EvidenceSummaryEngine().draft(source)
        assertEquals(source, draft.source)
        assertEquals("UNVALIDATED", draft.status)
        assertEquals(2, draft.proposals.size)
        assertEquals(source.lines()[0], draft.proposals[0].literalText)
        assertEquals(1, draft.proposals[0].line)
        assertEquals(DraftCategory.RETURN, draft.proposals[1].category)
    }
    @Test fun noInventedDateForUnstructuredText() {
        val draft = EvidenceSummaryEngine().draft("  Orientação literal  \n\n検査について")
        assertEquals(2, draft.proposals.size)
        assertEquals(3, draft.proposals[1].line)
        assertEquals(DraftCategory.EXAM, draft.proposals[1].category)
        assertEquals("UNVALIDATED", draft.status)
    }
    @Test fun emptyInputProducesNoProposal() {
        assertTrue(EvidenceSummaryEngine().draft("\n  ").proposals.isEmpty())
        assertTrue(ManualTranscriptionEngine().supportedLocales().isEmpty())
    }
}
