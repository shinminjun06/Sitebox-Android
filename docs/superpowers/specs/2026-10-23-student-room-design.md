# SiteBox photographic 2.5D student room

The user's 23 October brief and supplied 720×1280 student-room photograph define the design. Success is a coherent photographed room, one stable 3×3 bookshelf during approach/retreat, and unchanged bookmark management, accounts and persistence. This is a rendering/interaction architecture change in the existing app, not a new UI framework.

## Existing implementation

Java 17; Android Activity, FrameLayout, LinearLayout, ScrollView, ImageView and custom Canvas views; minSdk 24/target 35. MainActivity builds nine seven-slot book rows with native click/drag/drop, categories, paging and search. JSONArray sites contains name, URL, category, optional notes/favourite/order and sync metadata; SharedPreferences persists it. BackupController and GoogleDriveBackup maintain Google-only account libraries and automatic Drive sync. WeatherRepository already calls Open-Meteo and caches the selected city; disconnected fallback reports time-only or cached weather honestly. StudyLiveView supplies day/night, weather, lamp and lifecycle-managed window video.

The old transition uses two independent animators, two furniture shapes and book snapshots. It rejects every input while moving; Android Back opens the book editor from the room. Its generated board renderer cannot meet the new photographic-frame requirement.

## Approach

Use the existing Android Views and Canvas. A single high resolution photographed front-frame sprite and the same live BookshelfView children render in both modes. A cached four-point homography maps canonical cabinet coordinates to room coordinates. Room/background, outside window, ground contact, frame/books, foreground table and lighting form separate layers. The foreground and empty background are prepared photographic assets, never primitive final substitutes.

A single reversible 550 ms transition owns progress (0 = editor, 1 = room) and drives all transforms/opacity. Retarget from current progress, cancel stale completion callbacks, and shorten duration proportionally to remaining distance. Native editor controls receive touches only at the settled editor endpoint. Tapping the accurately mapped room bookshelf approaches it; pinch-in and Android Back return to the room. Repeated identical requests do not restart movement. Layout changes rebase the same progress.

## Coordinates and live books

Asset metadata defines normalized frame openings, room cabinet quadrilateral, floor contacts, window polygons, lamp hit region and foreground registration. The same Matrix and inverse serve frame/book drawing and hit tests. Native book children draw through that matrix during motion, preserving text resolution without a captured book bitmap. The editor uses the identical sprite/opening layout. Seven equal slots per cell and existing paging prevent overlap for up to the existing validated 10,000-item library limit. Cloud updates retain the existing interaction deferral.

The room is the initial experience for a fresh launch; saved interaction state survives rotation and process recreation. Existing library, finish indices, search and account state are not migrated destructively. Three shelf finishes remain selectable; three existing book styles remain available.

## Photographic layers and atmosphere

Preserve the reference's window/rear desk/chair, left-wall shelf bay, right bed and foreground desk. Remove the baked shelf and foreground desk from the background; render the foreground separately. Window pixels are clipped inside calibrated glass polygons; architecture moves with the room, outside scenery more slowly, foreground faster. Clamp motion to overscan and provide a deliberate ambient surround on extreme aspect ratios rather than slicing away important furniture. The cabinet stays anchored to the room image.

Keep Open-Meteo, cached/offline weather, city-based time and day/night. Relit night and lamp states change room lighting; weather never rebuilds the library. Existing media playback remains supported with registered loops for the new artwork. Day/night and lamp transitions invalidate atmosphere/frame lighting only. Low-quality mode reduces parallax, shadow and atmospheric work, without affecting data or touch geometry.

## Assets and constraints

New production assets: shelf-free/foreground-free day and relit night plates, lamp-off night plate, transparent day/night foreground cutouts, high resolution alpha cabinet front and metadata. Recommended room/foreground canvas 1080×1920 (up to 1280×2276), frame approximately 1100×1430, lossless alpha WebP for silhouettes, quality WebP for opaque plates. Decode once with device-sized sampling and independent caches; do not decode or allocate large drawing objects on animation frames. Existing book images, shelf finishes, source art and account code remain reusable.

No Blender/GLB/game engine, new UI toolkit, new login provider, fake weather, decorative fake books or performance claims without measurements. Missing optional bespoke landscape plates/extra dawn artwork must be identified in the asset report; portrait art must still function on landscape without clipping furniture.

## Verification

Pure geometry and transition tests: all nine cells and boundaries, inverse mapping, phone/foldable/landscape safe regions, continuous reversal and bounded parallax. Native graphics/integration: identical frame at the handoff, real book edits/overflow/paging, repeated gestures, restored state, atmosphere changes without library reload, persistence and existing account/sync tests. Build debug/preview/release and instrumentation; run lint; verify unchanged release certificate. Use GitHub Actions for reproducible checks where possible and provide signed APK, source and actual-render previews. State the distinction between native software rendering, emulator execution and physical-device/60 fps measurements.
