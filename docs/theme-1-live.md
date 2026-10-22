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

## 3.0.4 공간 배치와 소등 조명

축소 책장은 공부방의 바닥 접점은 유지하면서 중심을 오른쪽으로 옮기고 높이를 줄였다. 세로 배치의 source anchor는 center .46, feet .605, height .20이며, 가로 배치는 center .40, feet .69, height .34이다. 스탠드 터치 영역과 축소 책장 사이의 간격을 회귀 테스트한다.

`study_lamp_off_{portrait,landscape}.webp`와 `study_lamp_off_night_{portrait,landscape}.webp`는 원본 방 그림을 참조해 image_gen으로 제작한 소등용 조명 변형이다. 단순한 검은 오버레이와 전구 마스크는 제거했다. 밤의 책상·노트·벽에서 스탠드가 만든 주황색 반사광과 강한 그림자를 없애고, 창문 주변광에 맞춰 재조명했다. 낮의 햇빛과 태양 그림자는 유지한다. 생성 PNG는 WebP로 인코딩하고 리소스 크기를 제한했다.

앱은 원래 창문 마스크 바깥의 실내만 소등 그림으로 교체한다. 따라서 영상/창밖 풍경은 원래 픽셀과 좌표를 유지한다. 스위치 클릭은 방 영상 디코더를 재시작하지 않고 조명 그림만 교체한다. 소등 중 책장 음영은 더 차갑게 바꾸고, 책장 아래 방향성 그림자는 짧고 흐린 주변광 그림자로 바꾼다. 접지 그림자는 남겨 가구가 떠 보이지 않게 한다.

기존 방 LRU 캐시는 12 MiB로 유지한다. 사용 중인 on/off 낮·밤 그림의 합계는 별도로 20 MiB 이하를 검증한다. 소등 그림은 소등 상태에서만 뷰가 참조하고 점등 시 참조를 해제한다. 그림 decode/변환은 onDraw에서 하지 않는다.

Robolectric native graphics 결과는 build/reports/lamp의 day/night-lamp-on/off.png와 room-night-on/off.png로 남긴다. 이는 실제 앱 렌더러를 사용하는 검사 이미지이며 휴대폰 촬영 화면은 아니다.

## 3.0.5 책상 높이와 원근 교정

책상이 낮은 탁자처럼 보이고 상판을 과하게 내려다보던 인상을 줄이기 위해, 공부방 전경의 책상·노트·책·의자 비율과 소실 방향을 다시 그렸다. 일반적인 작업용 책상과 앉은 눈높이를 시각적 기준으로 사용했다. 실제 3D 치수를 가진 모델로 바꾼 것은 아니며 기존 2D 방 + 별도 책장 렌더러를 유지한다.

세로/가로 점등 낮 그림을 기준 구도로 확정한 뒤, 같은 구도를 참조해 밤과 소등 그림을 다시 제작했다. 창문 프레임, 스탠드 위치, 벽/바닥 접점은 유지해 기존 터치 영역·책장 바닥 배치와 맞췄다. 공부방 정지 그림 8개와 창문 cinemagraph 4개를 교체한다. 영상은 scripts/build_study_video.py의 기존 6초/24fps/무음 H.264 생성 절차로 다시 만든다.

생성 원본 PNG는 /workspace/generated_images에 남기고, 앱에는 원래 크기/메모리 예산에 맞게 WebP로 인코딩한 파일만 넣는다. 3.0.4 리소스는 작업 중 /workspace/.sitebox-tools/pre-desk-3.0.4에 보존했다. 소등 시 반사광/그림자를 전환하는 로직, 축소 책장의 스탠드 이격, 로그인과 동기화, 앱 데이터 형식 및 서명키는 유지한다.
