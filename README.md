# SiteBox Android · 학교 서재

웹사이트를 책으로 정리하는 Android Java 앱입니다. 햇살 교실 배경, 3×3 이미지 책장,
독립적인 책장·책 테마와 직접 드래그 이동을 제공합니다. Android 7.0(API24) 이상.

- 책장: 햇살 원목 / 짙은 월넛 / 화이트 오크
- 책: 고전 양장본 / 파스텔 북 / 모던 북 — 총 9가지 조합
- 제목·주소·카테고리 검색, 즐겨찾기, 메모, 사이트 미리보기, 페이지 전환
- 책을 길게 눌러 같은 칸 순서 변경 또는 다른 칸 이동 (한 칸 7권)
- 두 손가락으로 오므리면 배경 속 작은 책장으로 축소, 작은 책장을 누르면 복원
- 명패를 눌러 해당 칸에 추가, 설정에서 카테고리 이름·테마 변경
- 빈 배경이나 헤더를 길게 누르면 교실만 표시, 화면을 누르면 서재 복원

## 빌드와 검증

JDK17, Android SDK35, Gradle8.9 사용. Gradle wrapper는 원본 프로젝트와 같이 미포함.

```sh
python3 -m pip install Pillow
sh tests/run.sh
gradle :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --no-daemon
```

[Actions](https://github.com/shinminjun06/Sitebox-Android/actions)의 최신 성공 실행에서
`SiteBox-debug-apk`와 실제 에뮬레이터 스크린샷 `SiteBox-device-qa`를 받을 수 있습니다.
APK의 앱 정보와 `SOURCE_COMMIT.txt`에 빌드 소스 SHA가 기록됩니다.

## 데이터 및 설치

기존 MainActivity SharedPreferences와 `sites`, `categories`, `shelfTheme`, `bookTheme`
키를 유지합니다. 데이터 구조는 호환됩니다. 단, **기존 APK와 서명이 다르면 Android가
덮어쓰기를 거부합니다.** 원래 서명 개인키 없이는 이 제한을 해결할 수 없습니다.
기존 북마크가 있다면 데이터를 확보하지 않은 상태로 기존 앱을 삭제하지 마세요.
`app-preview.apk`는 `com.sitebox.app.preview`로 별도 설치되어 기존 앱과 공존합니다.
미리보기 앱은 기존 앱의 비공개 북마크를 자동으로 가져오지 않습니다.
Actions debug 키는 이후 빌드 사이에 캐시로 유지하지만, 운영 배포에는 별도 보관한
정식 서명키를 저장소 secrets로 공급해야 합니다.

[그래픽 제작·적용 기록](docs/ARTWORK.md) · [자체 검토와 한계](docs/REVIEW.md)

사용자 ZIP 테마 가져오기는 미지원이며, 폴더·JSON 확장 규격은
[테마 문서](app/src/main/assets/themes/README.md)에 정리되어 있습니다.
