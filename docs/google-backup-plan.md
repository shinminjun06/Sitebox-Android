# Google backup implementation plan

Request: connect a Google account so SiteBox data can be saved. Preserve existing scenes, video, bookmarks, SharedPreferences schema and gestures.

1. Strict, bounded versioned JSON codec; preserve book metadata and ordering.
2. Google Identity Services authorization + hidden Drive appDataFolder manual append backups/latest restore; no stored credentials.
3. Native settings and SAF export/import fallback usable without app OAuth registration; restore confirmation + atomic local rollback.
4. Native tests, build/lint, existing regression suite, final code review, exact commit APK verification.

External configuration: app owner's Google Cloud project must register Android package/signing certificate and Drive API. Do not fabricate credentials or claim real account tests.

Ruling: continue without routine design/commit approval because user explicitly authorized implementation and GitHub updates. Google Cloud registration remains an external owner action; file-based Drive saving remains immediately usable.

Recovery note: local workspace reset destroyed unpublished prior drafts. Recreated against main27263e6. Sources are to be checkpointed to GitHub before final build.
