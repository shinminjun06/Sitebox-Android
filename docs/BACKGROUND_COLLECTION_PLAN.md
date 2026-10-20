# Background collection implementation · 2026-10-21

User supplied a10-concept contact sheet and requested adding these backgrounds.
Keep original classroom as default and add classic library, private bedroom,
glass greenhouse, book cafe, Japanese tatami room, attic, future laboratory,
wizard study, seaside villa and train compartment. All independently selectable
from cabinet/book finishes, persisted with a new preference key only.

1. Inspect latest source and reference; implement in isolated background-collection branch.
2. Generate ten individual tall portrait production room images with a clear,
   front-facing furniture installation zone, no baked-in extra cabinet. Convert
   reviewed output to bounded WebP; retain prompts and provenance.
3. Add a catalog/model with independent selection, previews, safe persisted IDs,
   per-room center/feet/height, matched image transform and contact-shadow anchors.
   Existing classroom wide/portrait assets and default remain. New portrait art
   uses an anchor-aware crop in wide viewports, keeping the furniture floor visible.
4. Test selection through actual settings UI, persistence, all11 rooms in collapsed
   view, rotation, pinch/tap, existing CRUD/drag/search/themes. Build standard and
   parallel APKs via Actions; inspect screenshots; fix material findings; deliver.

Ruling: implement all10 concepts, not weather variants. This follows the labels
and spatial differences in the supplied sheet. No live weather/time service.
Ruling: do not bake the user's live cabinet into background imagery; the existing
interactive snapshot and independent finish remain responsible for furniture.
