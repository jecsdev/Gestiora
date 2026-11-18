# Educational content — offline v1 and editorial updates

Decision, 2026-11-17: **ship the existing small educational catalog locally; defer Supabase/CMS/synchronization, login and AI to phase 2.** Local-first does not prohibit later public-content sync; clinical data, favorite IDs and searches remain local.

## V1 publishing process

1. Assign an editorial owner before release. Review the existing seven entries and their exact supporting WHO/UNICEF/NHS links; cited authority is not equivalent to an independent clinical review of our summary or translation.
2. Check sources at least monthly and whenever a correction or official update is reported. This cadence is a project process choice, not an institutional endorsement or a guarantee of current medical advice.
3. Change summaries in the ES/EN/FR resources together. Keep stable article IDs. Correct citations and translations together; do not infer clinical meaning merely from a changed web page.
4. Increase the bundled catalog version and the content `updatedAt` when shipping editorial changes. Record the actual source consultation date separately. Populate `reviewedOn` only for a documented review; never label the technical build date as a medical review.
5. Run resource/seed parity and Room seed/update tests. Verify an existing favorite survives the catalog update and searches show revised text in each language.
6. Distribute a new app release containing the correction. Old offline installations retain their installed version until updated; the app cannot remotely retract or fix those copies. Use a prompt patch release for material corrections rather than waiting for unrelated features.

Source links, language, content update/source consultation dates and the educational disclaimer remain visible in the library. The disclaimer is also shown on the welcome screen. These controls do not replace review. Asset distribution rights, privacy declarations and store publication checks remain release gates.

## When to add remote publishing

The current seven-entry catalog does not justify an operational CMS for v1. If editors must publish urgent corrections without waiting for an app update, or the catalog grows beyond what can reliably be reviewed with code changes, remote publishing becomes necessary for that requirement.

Phase 2 can use Supabase as the public catalog database with a protected editorial interface. Supabase alone is not an editorial workflow: publication/review permissions and ownership still need to be defined. Public read-only access requires no patient login. Room remains the offline read path. Implement the cursor/snapshot, tombstone, validation and atomic checkpoint rules in EDUCATIONAL_CONTENT_SYNC_PLAN.md before enabling sync. Do not implement naive timestamp-only pagination or overwrite local favorites.
