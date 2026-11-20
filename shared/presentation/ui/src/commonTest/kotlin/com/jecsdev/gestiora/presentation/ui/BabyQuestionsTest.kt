package com.jecsdev.gestiora.presentation.ui

import kotlin.test.*
import gestiora.shared.presentation.ui.generated.resources.*

class BabyQuestionsTest {
    @Test fun explorerBoundariesSelectOneStageWithoutHidingGeneralOrUrgentInformation() {
        for (week in 4..40) {
            val entries = babyQuestionsFor(week)
            assertEquals(5, entries.size)
            assertEquals(1, entries.count { it.id.endsWith("development") })
            assertTrue(entries.any { it.id == "changed-movements" })
            assertTrue(entries.any { it.id == "prenatal-contacts" })
        }
        assertEquals("early-development", babyQuestionsFor(13).first().id)
        assertEquals("middle-development", babyQuestionsFor(14).first().id)
        assertEquals("middle-development", babyQuestionsFor(27).first().id)
        assertEquals("late-development", babyQuestionsFor(28).first().id)
        assertTrue(babyQuestionsFor(3).isEmpty())
        assertTrue(babyQuestionsFor(41).isEmpty())
    }

    @Test fun homeUsesOnlyTheTwoWhoAnswersWithTheSameSource() {
        assertEquals(listOf("prenatal-contacts", "early-ultrasound"), homeQuestions().map { it.id })
        homeQuestions().forEach { assertTrue(it.source.url.startsWith("https://www.who.int/")) }
    }

    @Test fun catalogHasUniqueIdsAndSpecificOfficialSources() {
        assertEquals(babyQuestions.size, babyQuestions.map { it.id }.toSet().size)
        val approved = setOf("www.who.int", "www.unicef.org", "www.nhs.uk")
        babyQuestions.forEach {
            assertNotEquals(it.question, it.answer)
            assertTrue(it.source.url.startsWith("https://"))
            val host = it.source.url.removePrefix("https://").substringBefore('/')
            assertTrue(host in approved)
            assertTrue(it.source.url.substringAfter(host).length > 1)
            assertTrue(it.source.institution in setOf(Res.string.who, Res.string.unicef, Res.string.nhs))
        }
    }
}
