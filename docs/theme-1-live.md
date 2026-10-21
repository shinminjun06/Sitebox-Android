# Theme 1: time and weather (2.8)

Based on 2.7 (62fabd7). Only the study room gains motion; all eleven other scenes and bookmark preferences stay compatible.

## Rendering

The approved room remains stationary. Four silent six-second H.264 baseline MP4s (portrait/landscape, day/night) expose moving pixels only through calibrated window glass masks. These are locally produced cinemagraphs derived from the approved still artwork, not fully filmed or AI-generated videos. The source leaves shift by at most 1.35 pixels horizontally and 0.65 vertically; camera, furniture, window rails and the live cabinet do not move. `scripts/build_study_video.py` reproduces them with ffmpeg.

Separate aligned night plates provide warm desk lighting and a blue exterior. The day/night plates crossfade at local sunrise/sunset. Each corresponding video fades through zero opacity at twilight; a newly prepared opposite-phase clip cannot flash over the wrong light plate. Clouds, rain and snow are clipped to glass. Stars remain embedded in the night artwork, so they never appear over tree silhouettes. Overcast weather dims the room and miniature cabinet, while the desk lamp supplies a modest warm fill. The miniature cabinet is dimmed along with its room; the expanded library retains readable book labels.

TextureView and MediaPlayer decode asynchronously. First-frame gating, missing-file fallback, orientation replacement, decoder error recovery, pause/detach release and a persisted motion switch avoid black flashes and background playback. Atmospheric updates are capped at 24 per second; time is checked once a minute. Artwork/background/preview caches remain 12/12/2 MiB (26 MiB total).

## Weather and privacy

Settings → 시간 · 날씨 · 영상 provides motion, city search, current observation time and manual refresh. Seoul is explicitly the initial city; no GPS/location permission is requested. Selected-city coordinates go to Open-Meteo over HTTPS. City and weather cache persist separately from all existing bookmark preferences. Weather refresh is limited to once per 30 minutes automatically while this room is foregrounded. Old observations are marked after 30 minutes. Offline mode retains cached weather; absent weather uses the device clock. Selected-city sunrise/sunset and UTC offset drive lighting where available, including solar-date rollover and polar is_day fallback.

Network work runs off the UI thread with bounded responses, timeouts, a bounded/coalescing queue and generation checks. No API key or new dependency is bundled. Attribution is visible in settings: [Open-Meteo weather](https://open-meteo.com/) / [GeoNames city data](https://www.geonames.org/). The free endpoint is for noncommercial use; a commercial release needs provider terms/billing review.

## Validation design

Pure tests cover weather validation, solar transitions, UTC offsets, stale cache, precipitation and existing bookmark/layout/projection rules. Asset validation decodes images and probes every MP4's codec, dimensions, duration and lack of audio. Native Android instrumentation injects fixed noon/night/rain/snow data for repeatable screenshots, independently requires advancing visible day/night video frames, exercises pause/resume, rotation, motion-off, decoder failure/recovery, non-study teardown and existing bookmark/drag/search/theme regression tests. These injected scenarios are test fixtures, not four simultaneous real weather observations. CI also records a six-second actual emulator capture. Final build/native results are documented with the delivered APK rather than inferred from source review.

Limitations: the room is artistic, not a webcam of the chosen city; motion is deliberately subtle, and rain/cloud overlays do not reconstruct physically exact outdoor lighting. Device clock errors affect lighting. A stale selected-city UTC offset may be one hour off across DST until refreshed. Physical devices and Play Protect classification require separate verification.

Native correction: the decoder rendering-start event unlocks the TextureView while actual texture-update callbacks alone provide frame evidence. A one-level alpha floor primes surface creation and keeps twilight consumption active. Old buffered texture callbacks after pause do not count until the new decoder generation starts rendering. The instrumentation propagates UI assertions back to its worker so a test failure can restore the fixture instead of terminating the app process. Emulator progress checks allow up to one second for two additional real frames; this is a playback check, not a physical-device frame-rate benchmark.

Rotation correction: the live layer starts INVISIBLE (measured) rather than GONE. Enabling a previously unmeasured GONE child from the backdrop layout callback could leave it at 0×0 after recreation. Native rotation checks caught this after all weather/playback lifecycle checks passed. Non-study rooms still hide it with GONE and release playback.

Readiness correction: native checks now require three additional current-decoder texture updates within the existing 12-second asynchronous startup deadline before checking two further updates within one second. A single initial frame is insufficient evidence of warmed playback, especially when rapidly recreating Goldfish decoders. Failure logs include dimensions, surface state, alpha, frame count and playhead.
