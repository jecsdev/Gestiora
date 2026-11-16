# Offline pregnancy questions — editorial record

Catalog version: 1. Sources checked against their official pages on 2026-11-16.

Presentation update: the approved answers now live in ES/EN/FR string resources. Home reuses the two WHO entries with manual controls; Baby FAQ has finite fade/expansion. Official URLs remain original. NHS/UNICEF titles remain in their published language; the WHO title is a localized descriptive citation. Language labels and consultation dates are localized. These translations are editorial adaptations, not independently clinically reviewed translations. Source review date is unchanged. See LOCALIZATION_AND_MOTION.md.

## Scope

Seven educational summaries in Spanish, English and French in Mi bebé: one stage question and four general questions are available at each explorer week. This is a bundled, deterministic FAQ, without free-text input, AI, backend calls or access to stored medical records. The source pages are external links and require internet; reading the answers does not.

## Source mapping

| Stable question ID | Official source | Supported topic |
| --- | --- | --- |
| early-development | [UNICEF: Your first trimester guide](https://www.unicef.org/parenting/pregnancy-milestones/first-trimester) | Early formation of organs/structures; variability. |
| middle-development | [UNICEF: Your second trimester guide](https://www.unicef.org/parenting/pregnancy-milestones/second-trimester) | Bone, hearing and movement development. |
| late-development | [UNICEF: Your third trimester guide](https://www.unicef.org/parenting/pregnancy-milestones/third-trimester) | Later development, fat accumulation and limb activity. |
| prenatal-contacts; early-ultrasound | [WHO: antenatal care recommendations](https://www.who.int/news-room/detail/07-11-2016-pregnant-women-must-be-able-to-access-the-right-care-at-the-right-time-says-who) | Contact model and purpose/timing of early ultrasound. This foundational release is dated 2016, not 2026. |
| first-movements; changed-movements | [NHS: Your baby's movements](https://www.nhs.uk/pregnancy/keeping-well/your-babys-movements/) | Onset of perceived movement and action when the established pattern changes. Source page reports last review 2024-07-08. |

## Editorial policy

- These are paraphrases/adaptations by Gestiora, not quotations, official translations, institutional endorsements or independently clinically reviewed material. The UI names the institution, source title, English language, official link and date consulted. Consultation date must not be described as clinical approval or the source publication date.
- The ranges 4–13, 14–27 and 28–40 select related reading. They do not define diagnostic deadlines or claim that every milestone occurs at the selected week. Institutions use differing stage groupings. The UI says “semana explorada” rather than claiming the selected week is the patient's current week.
- General questions remain available across all supported explorer weeks; in particular, navigation must not hide the movement-change question. Its wording must preserve immediate referral rather than suggesting waiting, reassurance from an app or a home device.
- Individual care follows the person's prenatal team/local protocol. No diagnosis, result interpretation, treatment or dosage is supplied.
- Check the actual linked page when updating claims. Never substitute an institution's homepage as evidence for a specific answer. Record changes to claims, URLs, version and consultation date together; recheck links/content before release and when guidance changes.
- Existing fruit comparisons and weekly-stage copy elsewhere in Baby are inherited from the prototype. These FAQ citations do not retroactively validate or endorse those assets/text. Clinical review remains a release task.

## Verification

Android debug build and presentation UI host tests passed on 2026-11-16 (30 tests, zero failures). Two new tests cover every supported week, stage boundary selection, always-available general/movement safety entries, invalid-week handling, stable unique IDs and specific HTTPS official-source hosts. They do not validate clinical correctness, rendered UI, external link delivery or iOS.
