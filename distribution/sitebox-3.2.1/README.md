# SiteBox 3.2.1 · 실제 GLB 방

`SiteBox-3.2.1.apk`를 내려받아 설치하세요. 이전 검증판과 같은 패키지와 서명키로 업데이트할 수 있습니다.
사용자 제공 GLB 전체를 Filament로 렌더링하며 같은 책장에 접근해 실제 책을 관리합니다.
책장 터치 확대, 물리 핀치/Back 축소, 네이티브 책 추가·수정·삭제·드래그와 Google 자동 동기화를 유지합니다.
빈 공간 길게 누르기로 화면이 숨겨지는 동작은 제거했습니다. 창문은 실제 낮·밤·비·눈 이미지로 바뀝니다.

검증 소스 `1e06eebe91a40a643383bc67f8d42177343f55ba`, [Android 15 전체 검사](https://github.com/shinminjun06/Sitebox-Android/actions/runs/40109298541).
단위 테스트 66개, debug/preview/release/AndroidTest 빌드, debug/release lint 오류 0.
실제 Google 동의/실계정 Drive와 실물 기기60fps는 검증 범위 밖입니다.
책은 네이티브 View를 캐시한 평면 텍스처로 방·책장과 같은 GPU 프레임에서 렌더링합니다. 프레임/방은 실제 3D입니다. 세부 구조와 한계는 FULL_ROOM_3D.md에 있습니다.
화면의 책은 검사 데이터이며 APK에는 미리 넣지 않습니다.

![실제 GLB 방](3d-day.png)
![책 관리](3d-editor-books.png)
![밤의 방](3d-night.png)
