# SiteBox 3.1 · photographed room, assets and transforms

## Implemented architecture

The app remains Java 17 with Android native View/Canvas (minSdk 24, target 35). No Compose, game engine, GLB or external UI dependency was introduced. The supplied student-room image is the design reference. Generated production plates keep the central Japanese neighbourhood window, rear desk/chair, right bed and close foreground table. The original left bookshelf and close foreground are removed from the base plate. Real bookmark data fills a separate photographed 3×3 cabinet.

| Z order | Component | Content and transform |
| --- | --- | --- |
| 0 | RoomPlateView outsideLayer | Frame-free day neighbourhood texture, clamped overscan camera |
| 1 | StudyLiveView | Registered night outside texture, window video and weather; behind the room shell |
| 2 | RoomPlateView backdrop | Day/night/lamp photographic plates masked by room_shell alpha, preserving mullions, lace, indoor leaves and lamp |
| 3 | FurnitureShadow | Ground occlusion/cast shadow using the actually drawn cabinet soles |
| 4 | ProjectedCabinetView | The same photographed frame and live BookshelfView book children through one Matrix |
| 5 | ProjectedCabinetView decor | Existing alpha plant/picture/mug, attached to cabinet coordinates and smoothly faded |
| 6 | RoomPlateView foreground | Transparent registered foreground table/pot/notebook, faster camera depth |
| 7 | Shared lighting | Native image crossfade, lamp states, cloud tint and cabinet color filter; no library rebuild |
| 8 | LibraryStage / native controls | Inverse-matrix cabinet hits, two-finger retreat, guarded moving state and native editor book/drag controls |

`RoomExperience` coordinates the layers. `RoomTransition` alone owns progress: 0 is the native editor and 1 the room. Its 550 ms decelerating timeline can reverse from its current progress; identical requests do not restart it and generation checks reject cancelled completion callbacks. Room photo scale/pan approaches the cabinet's source anchor. Foreground has more depth movement; outside scenery has less, constrained to cover the current glass region. Background focus/foreground/labels/decor fades use the same progress.

At the editor endpoint, the projected matrix equals the actual native BookshelfView bounds and its identical frame image hands off without replacing furniture. Books draw as real native children during movement, rather than enlarged text snapshots. Sidebar/header controls are inaccessible during motion; LibraryStage still accepts a pinch or a cabinet tap to reverse. Android Back (including API 33+ back callbacks) retreats from the editor to the room. The initial experience is the room; restored room/editor state, search and paging survive Activity recreation.

If a library update has just replaced the native cabinet and Android has not measured it yet, the latest camera request is retained and applied after layout. Repeated pre-layout requests replace the pending intent instead of being silently dropped.

## Coordinate source of truth

`RoomScene.java` contains normalized source-image coordinates and camera/overscan formulas. `CabinetGeometry.java` supplies identical book regions to native layout and projected hit tests. `ProjectedCabinetView` caches the draw Matrix and its inverse; touches map into canonical frame coordinates before cell and alpha-silhouette tests.

The cabinet front quad is TL (.035,.385), TR (.225,.384), BR (.225,.590), BL (.035,.670) on the 941×1672 room canvas. The two vertical posts stay upright; the far foot recedes toward the left wall's rear corner. The outdoor mask is generous by design: **room_shell alpha**, rather than rough window rectangles, defines the final photographic occlusion.

Frame image: 1100×1430. Book left edges are .066/.368/.670, width .264; tops .042/.323/.617, height .258. Baselines .300/.581/.875 meet measured front shelf lips. Seven equal native slots per cell plus existing paging keep large libraries inside cubbies. Category labels retain native accessible text and click/drop handling, with explicit cached Canvas lettering for reliable rendering.

Portrait uses a left-biased safe crop bounded to 89% of source width, keeping the cabinet and central window. Very tall/foldable/landscape ratios use an intentional ambient surround when a single portrait composition cannot fill the viewport safely. Camera motion is clamped to opaque coverage and outside imagery has independent overscan. A separately composed landscape plate would improve wide-screen use of space; the implemented portrait composition remains usable without cutting the shelf away.

## Production assets

All files are in `app/src/main/assets/themes/rooms/student/`. The generated PNG originals remain outside the source tree under `/workspace/generated_images`; WebP encoding changes format only.

| File | Shipped pixels | Role / alpha |
| --- | --- | --- |
| room_day.webp | 941×1672 | Shelf-free, foreground-free sunny room; RGB |
| room_day_off.webp | 941×1672 | Day lamp-off detail; locally masked so sunlight/window stay fixed |
| room_night.webp | 941×1672 | Cool room with local rear-desk lamp light; RGB |
| room_night_off.webp | 941×1672 | Relit lamp-off room, removed warm pools/reflections; RGB |
| room_shell.webp | 941×1672 | RGBA indoor matte: actual window holes, preserved indoor silhouettes |
| foreground_day.webp | 941×1672 | RGBA foreground desk and props, empty upper canvas |
| foreground_night.webp | 941×1672 | Registered RGBA cool foreground, removed afternoon sun shadows |
| outside_day.webp / outside_night.webp | 1254×1254 | Frame-free photographed residential vista with overscan |
| cabinet_front.webp | 1100×1430 | High detail RGBA 3×3 empty cabinet with solid backs and transparent exterior |
| window_day.mp4 / window_night.mp4 | 480×480, 24 fps, 6 s | Silent registered static-photo fallback loops; clouds/precipitation are native animation |

Recommended replacements: 1080×1920 room/foreground canvases (up to 1280×2276 if memory-tested), approximately 1100×1430 cabinet, 1024–1536 square frame-free outside textures. Preserve normalized registrations or update RoomScene/CabinetGeometry together. Use lossless alpha WebP for matte edges and the cabinet. No fake books are baked into the furniture image.

Existing three book styles and book images are reused. Three persisted cabinet finishes remain selectable using photographic-frame tone filters. Legacy room/video/timber assets remain available as fallback/reference; their old procedural cabinet is no longer the active room renderer. Account, backup and bookmark formats are retained.

## Memory and effects

ThemeAssets samples images to screen/low-RAM limits, samples outside textures independently, keeps the frame in the artwork cache and separates room/preview caches. No bitmap decode or book snapshot occurs during draw/animation. Paths and matrices are reused; text/UV geometry is prepared on layout/state changes. The raw ten-image source set is below 64 MiB, and runtime sampling lowers it further (outside art is sampled independently). Active photographic references survive cache eviction, avoiding weather flicker; caches are bounded separately, not claimed as the total process memory.

A persisted per-device reduced-effects switch disables camera parallax and window decoding, and reduces weather particles/ticks. Low-RAM devices default to reduced effects. It does not change the library or cloud format. The existing motion switch, lifecycle decoder pause/release, Open-Meteo city/cache/offline status and time-based day/night remain supported.

## Verification and remaining limits

Tests cover visible label ink, actual photographed shelf baselines, eight aspect ratios/overscan, all nine inverse-matrix cells, 71-book paging, live rebind/weather isolation, reversal continuity, persistence/quality preferences, lamp and night-window rendering, existing account/sync behavior and Android builds/lint/signature. GitHub Actions `room-verify.yml` builds and runs the existing native Android 15 instrumentation, including CRUD, drag, gestures, rotation, account/sync fixtures and decoder lifecycle.

The media loops contain registered still images; native clouds/rain/snow supply movement. Bespoke wind-moving curtains/foliage video, separate dawn/sunset photographic plates and a full-width landscape composition are optional additional artwork, not falsely reported as shipped. Morning/evening currently blend the day/night endpoints using the existing city-time model. This is a restrained planar photo system, not freely orbitable 3D.

Physical-device FPS, thermal/GPU behaviour and real Google consent/live Drive transfer require device/account testing. Emulator frame statistics and native software renders do not prove 60 fps on a phone. OAuth console configuration remains as documented in GOOGLE_DRIVE_SETUP_KO.md; no new console setup is required for this rendering change and the release signing key is unchanged.
