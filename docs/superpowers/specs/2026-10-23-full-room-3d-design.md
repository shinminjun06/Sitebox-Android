# Full room GLB replacement

The user's supplied `SiteBox_FullRoom_3D_HQ_v4_fixed.glb` replaces the photographic room, furniture composites and alternate cabinet frame. Keep Java/View, existing preferences, book CRUD, search, pagination, drag, Google account and automatic Drive sync. Use native Filament/gltfio rather than a web bridge or a game engine. This is an implementation choice; the user explicitly requested the complete model replacement and execution.

Render the unchanged Z-up GLB with one root conversion to Y-up. Ignore its generated overhead camera; fit a room camera to show the shelf/window/desk, then animate one camera toward the same model's frontal cabinet. Native books use measured shelf openings and the same projected camera; frame and books never use separate camera transforms. Keep native book interaction in the expanded state. Hide helper anchor meshes. Long press on empty UI does nothing; book long press retains drag; real two-pointer contraction and Back retreat.

The model has no lights. Supply sun/moon directional light, warm desk light and ambient irradiance, with contact shadows/AO. Existing city/weather state controls lighting and a separate window material/texture. Render only when camera, surface, weather or content changes; stop rendering when paused, release native resources on destruction. Unsupported renderer errors must be visible rather than reported as a loaded model.

Preserve all data and signing identity. Validate camera projection on narrow/wide screens, actual GLB geometry/anchors, gesture regression, book persistence/sync, Android build and native GPU loading on CI. Emulator screenshots prove rendering and interaction; they do not establish physical-device 60fps or actual Google OAuth completion.
