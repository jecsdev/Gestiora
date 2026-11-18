package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.*

/** Reads the actual shipping XML, without Android stubs or a device locale override. */
class LocalizationContractTest {
    private val root = sequenceOf(
        File("src/commonMain/composeResources"),
        File("shared/presentation/ui/src/commonMain/composeResources"),
    ).first { it.isDirectory }

    private fun strings(folder: String): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val nodes = factory.newDocumentBuilder().parse(File(root, "$folder/strings.xml")).getElementsByTagName("string")
        val result = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val value = node.textContent.replace("\\n", "\n")
            assertFalse(value.contains("\\'"), "Compose string contains an Android-style apostrophe escape: $folder/$name")
            assertFalse(Regex("%([0-9]+)[$]s|%%").replace(value, "").contains('%'), "Invalid format token $folder/$name")
            assertNull(result.put(name, value), "Duplicate key $folder/$name")
            assertTrue(value.isNotBlank(), "Empty translation $folder/$name")
        }
        return result
    }

    private fun copy(folder: String): UiCopy {
        val values = strings(folder)
        return UiCopy(Res.allStringResources.map { (name, resource) -> resource to values.getValue(name) }.toMap())
    }

    @Test fun everyLanguageHasTheSameKeysAndCompatiblePlaceholders() {
        val base = strings("values")
        val token = Regex("%([0-9]+)[$]s")
        for (folder in listOf("values-es", "values-en", "values-fr")) {
            val translated = strings(folder)
            assertEquals(base.keys, translated.keys, folder)
            base.forEach { (name, value) ->
                assertEquals(token.findAll(value).map { it.groupValues[1] }.toList().sorted(),
                    token.findAll(translated.getValue(name)).map { it.groupValues[1] }.toList().sorted(), "$folder/$name")
            }
        }
        assertEquals(base, strings("values-en"))
    }

    @Test fun datesUseRealResourcesWithoutChangingIsoCivilValues() {
        val iso = "2028-02-29"
        assertEquals("29 de febrero de 2028", dateLabel(copy("values-es"), iso, true))
        assertEquals("February 29, 2028", dateLabel(copy("values-en"), iso, true))
        assertEquals("29 février 2028", dateLabel(copy("values-fr"), iso, true))
        assertEquals(iso, utcMillisToIso(requireNotNull(isoToUtcMillis(iso))))
    }

    @Test fun patientTextIsNotReinterpretedAsResourceFormatting() {
        val text = "%2\$s — Zoé & María\n100%"
        assertEquals("Hello, $text", copy("values-en").s(Res.string.hello_2, text))
        assertEquals("Bonjour, $text", copy("values-fr").s(Res.string.hello_2, text))
        assertEquals("Your journey · 25%", copy("values-en").s(Res.string.your_journey, 25))
    }

    @Test fun homeQuestionCountersRenderWithoutLeftoverFormatFragments() {
        assertEquals("Pregunta 2 de 2", copy("values-es").s(Res.string.question_position, 2, 2))
        assertEquals("Question 2 of 2", copy("values-en").s(Res.string.question_position, 2, 2))
        assertEquals("Question 2 sur 2", copy("values-fr").s(Res.string.question_position, 2, 2))
        assertEquals("Tu recorrido · 25%", copy("values-es").s(Res.string.your_journey, 25))
    }

    @Test fun packagedCatalogUsesRealTranslationsStableIdsAndExistingCitations() {
        val ids = babyQuestions.map { it.id }.toSet()
        for (locale in listOf("es", "en", "fr")) {
            val ui = copy("values-$locale")
            val seed = educationalSeed(ui)
            assertEquals(7, seed.size)
            assertEquals(ids, seed.map { it.id }.toSet())
            seed.forEach { article ->
                val question = babyQuestions.single { it.id == article.id }
                assertEquals(locale, article.locale)
                assertEquals(ui.s(question.question), article.title)
                assertEquals(ui.s(question.answer), article.body)
                assertEquals(question.source.url, article.sources.single().url)
                assertEquals(BABY_QUESTIONS_CHECKED_ON, article.sourceCheckedOn)
                assertEquals("2026-11-16", utcMillisToIso(article.updatedAt))
                assertNull(article.reviewedOn)
                assertFalse(article.isFavorite)
            }
            assertTrue(ui.s(Res.string.catalog_disclaimer).isNotBlank())
        }
    }
}
