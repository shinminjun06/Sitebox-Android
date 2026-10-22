# Theme 1 target photograph — 3.0.11

The user identified the supplied night photograph as the visual target. The target places a modest, almost front-facing cabinet left of the curtain, with a cool moonlit room, visible leaf/window shadows and a local warm desk lamp. The preceding release placed the cabinet farther right and used a broad orange night plate.

Portrait placement now follows a silhouette measured in the 591×1206 photographic viewport: approximately (155,530), (334,533), (334,773), (155,766). A standalone check compares all front corners with that target. The portrait projection follows the cabinet in the photograph instead of imposing the illustrated ceiling/baseboard's different convergence. The landscape geometry remains separately calibrated. Actual opaque foot contact and rear-leg support remain covered by native rendered-pixel checks.

The existing registered moonlit lamp-off plate supplies cool wall, curtain and floor illumination even with the lamp on. A clipped compositing layer retains warm light locally around the bulb and desk, while preserving exact window/video registration. Lamp-off uses its full independently relit plate as before. All room artwork files are retained; no screenshot UI or baked reference books enter the APK. The furniture and its top props receive a cooler night tint, and the prop scale follows the reference.

The image-editing service failed twice with HTTP 400 (Unknown parameter: reasoning). No new background image was generated. The implementation uses existing room artwork and runtime lighting composition; it does not claim pixel identity with the supplied photograph.

Validation: target-silhouette check, existing pure geometry and asset checks, native day/night/lampon/lampoff previews with actual library children, a new regression for cool moonlit walls with a warm-white bulb and local warm desktop, all unit tests, debug/release lint and signed release verification. Test books are preview fixtures only. Physical-device behavior remains unverified in this environment.
