> **3.2.0 실제 3D 방:** 제공된 GLB 전체를 Filament로 렌더링하고, 같은 3×3 책장으로 접근해 실제 책을 관리합니다. [구조와 검증](docs/FULL_ROOM_3D.md).

# SiteBox Android · 학교 서재

웹사이트를 책으로 정리하는 Java/Android View 앱입니다. Android 7.0(API24) 및 OpenGL ES3.0 이상.

- 사용자 제공 GLB의 벽·창문·책상·침대·책장을 실제로 렌더링합니다.
- 책장 터치로 접근, 두 손가락 오므리기 또는 뒤로 가기로 방에 복귀합니다. 550ms 전환은 현재 위치에서 반전합니다.
- 3×3 실제 책 데이터, 칸당7권 페이지 분할, 제목·주소 검색, 메모·즐겨찾기·드래그 이동을 유지합니다.
- 빈 공간 길게 누르기는 편집 화면을 숨기지 않습니다. 책 길게 누르기는 이동입니다.
- 낮·밤·흐림·비·눈에 맞춰 창문 이미지와 실내 조명을 바꿉니다. 스탠드 조명을 별도로 끌 수 있습니다.
- 계정 연결·자동 저장·Drive 동기화와 기존 데이터 구조를 유지합니다.

## 빌드와 검증

JDK17, Android SDK35, Gradle8.9 사용. Gradle wrapper는 원본 프로젝트와 같이 미포함.

```sh
python3 -m pip install Pillow
sh tests/run.sh
gradle :app:testDebugUnitTest :app:assembleDebug :app:assemblePreview :app:assembleDebugAndroidTest :app:lintDebug --no-daemon
```

[Actions](https://github.com/shinminjun06/Sitebox-Android/actions)의 최신 성공 실행에서
`SiteBox-debug-apk`와 실제 에뮬레이터 스크린샷 `SiteBox-device-qa`를 받을 수 있습니다.
APK의 앱 정보와 `SOURCE_COMMIT.txt`에 빌드 소스 SHA가 기록됩니다.

3.2.0 실제 3D 방 검증은 `Verify native 3D student room` 워크플로를 사용합니다.
[SiteBox 3.2.0 서명 APK 직접 다운로드](https://raw.githubusercontent.com/shinminjun06/Sitebox-Android/8b2111ccd7457d70fb498563641fdc965e8905b9/distribution/sitebox-3.2.0/SiteBox-3.2.0.apk) · [Actions 다운로드](https://github.com/shinminjun06/Sitebox-Android/actions/runs/40088952482) (`SiteBox-3.2.0-installable-apk`).
[3D 모델·좌표·에셋과 검증 결과](docs/FULL_ROOM_3D.md).

## 데이터 및 설치

기존 MainActivity SharedPreferences와 `sites`, `categories`, `shelfTheme`, `bookTheme`
키를 유지합니다. 독립적인 `backgroundTheme` 키에는 공간의 고정 ID를 저장하며,
배경은 테마 1 공부방만 제공합니다. 삭제된 테마 또는 알 수 없는 ID는 공부방으로 전환하며, 예전 테마의 백업도 책 데이터를 유지해 복원합니다. 데이터 구조는 호환됩니다. 단, **기존 APK와 서명이 다르면 Android가
덮어쓰기를 거부합니다.** 원래 서명 개인키 없이는 이 제한을 해결할 수 없습니다.
기존 북마크가 있다면 데이터를 확보하지 않은 상태로 기존 앱을 삭제하지 마세요.
`app-preview.apk`는 `com.sitebox.app.preview`로 별도 설치되어 기존 앱과 공존합니다.
미리보기 앱은 기존 앱의 비공개 북마크를 자동으로 가져오지 않습니다.
Actions debug 키는 이후 빌드 사이에 캐시로 유지하지만, 운영 배포에는 별도 보관한
정식 서명키를 저장소 secrets로 공급해야 합니다.

[그래픽 제작·적용 기록](docs/ARTWORK.md) · [자체 검토와 한계](docs/REVIEW.md)

사용자 ZIP 테마 가져오기는 미지원이며, 폴더·JSON 확장 규격은
[테마 문서](app/src/main/assets/themes/README.md)에 정리되어 있습니다.

## 정식 릴리스 서명

`release` 빌드는 `debuggable=false`, R8 난독화·최적화 및 리소스 축소를 적용합니다.
Google Auth와 Drive 백업 관련 보존 규칙은 `app/proguard-rules.pro`에 있습니다.
앱 기능·데이터 구조·권한은 변경하지 않으며, 매니페스트 권한은 `INTERNET` 하나입니다.
`debug`와 `preview`는 개발용입니다. 배포 파일은 `app-release.apk`를 사용하세요.

다음 네 값을 **환경변수 또는 개인용 `~/.gradle/gradle.properties`**에 설정하세요.
환경변수가 우선합니다. 프로젝트의 추적되는 `gradle.properties`에 비밀번호를 추가하지 마세요.

- `SITEBOX_RELEASE_STORE_FILE`: 키스토어 절대 경로 (상대 경로는 저장소 루트 기준)
- `SITEBOX_RELEASE_STORE_PASSWORD`: 키스토어 비밀번호
- `SITEBOX_RELEASE_KEY_ALIAS`: 예: `sitebox-release`
- `SITEBOX_RELEASE_KEY_PASSWORD`: 개인키 비밀번호

릴리스 설정이 없거나 키 파일이 없으면 빌드가 실패합니다. debug 키로 자동 대체하지 않습니다.
`androiddebugkey` 별칭도 거부합니다. 키스토어와 비밀번호는 Git에 커밋하지 말고 별도로 안전하게 보관하세요.
개인용 Gradle 설정 파일을 이용한다면 파일 권한을 제한하세요 (`chmod 600 ~/.gradle/gradle.properties`).

### 키 생성과 OAuth 인증서 확인

아래 명령은 비밀번호를 대화형으로 입력받습니다. 비밀번호를 명령 인수에 쓰지 마세요.
JKS 형식이므로 키스토어·개인키 비밀번호를 별도로 설정할 수 있습니다.

```sh
mkdir -p "$HOME/.android"
keytool -genkeypair -v \
  -keystore "$HOME/.android/sitebox-release.jks" \
  -storetype JKS -alias sitebox-release \
  -keyalg RSA -keysize 3072 -validity 10000
chmod 600 "$HOME/.android/sitebox-release.jks"

keytool -list -v \
  -keystore "$HOME/.android/sitebox-release.jks" \
  -alias sitebox-release
```

출력의 **SHA1**을 `com.sitebox.app`용 Google Cloud Android OAuth 클라이언트에 등록하세요.
기존 debug 인증서 SHA-1과 새 release SHA-1은 다릅니다. 기존 안내의 debug SHA-1을 릴리스에 재사용하면 안 됩니다.
Play App Signing으로 배포할 경우에는 기기에 설치되는 앱의 **앱 서명 인증서** SHA-1도 등록해야 합니다.
이 값은 업로드 키 인증서와 다를 수 있습니다.

네 서명 값을 설정한 다음 빌드·확인합니다. 로컬에 wrapper가 없으므로 Gradle 8.9를 사용합니다.

```sh
gradle :app:assembleRelease :app:lintRelease --no-daemon

# Android SDK Build Tools 35.0.0을 PATH에 추가한 환경
apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk

aapt dump badging app/build/outputs/apk/release/app-release.apk
```

`apksigner`의 인증서가 위 `keytool` 출력과 일치해야 합니다.
`aapt dump badging` 출력에는 `application-debuggable`이 없어야 하고,
`uses-permission`은 `android.permission.INTERNET` 하나여야 합니다.
R8 매핑 파일 `app/build/outputs/mapping/release/mapping.txt`도 버전별로 보관하세요.

### 서명 키를 바꾸기 전 반드시 백업

**현재 debug 서명 설치본에서 새 release 키로 전환하면 같은 패키지에 덮어설치할 수 없습니다.
기존 앱 삭제 시 앱 내부 데이터와 복원 전 기기 사본도 삭제되므로, 먼저 외부 백업을 완료하세요.**

1. **2.9 이하 기존 앱에서** 설정 → Google 계정 · 백업 → 파일로 저장을 누릅니다.
2. Google Drive 또는 앱 내부 폴더가 아닌 곳에 JSON을 저장하고, 실제 파일을 열거나 내려받을 수 있는지 확인합니다.
3. 백업 파일을 확보한 뒤에만 기존 `com.sitebox.app` 설치본을 삭제합니다.
4. 서명 전환은 파일 복원이 있는 2.9 릴리스에서 먼저 마칩니다. 3.0은 같은 릴리스 키로 업데이트하므로 삭제하지 마세요. 3.0의 계정 화면은 자동 동기화 방식입니다.
5. Google 직접 연결을 사용한다면 새 release 인증서 SHA-1 등록 후 연결을 확인합니다.

서명을 정식화하고 디버깅을 끄는 것만으로 Play Protect 경고 해제가 보장되지는 않습니다.
잘못 탐지됐다고 판단되면 [공식 Play Protect 개발자 안내](https://developers.google.com/android/play-protect/warning-dev-guidance?hl=ko)를 따라 검토를 요청하세요.

릴리스 검증 CI가 만드는 임시 키는 자동 검사 전용입니다. 운영 키로 사용하거나 해당 인증서 SHA-1을 OAuth에 등록하지 마세요.


## 3.0 Google 계정 자동 동기화

설정 → Google 계정 · 자동 동기화에서 계정을 연결하면 Drive의 SiteBox 앱 전용
데이터 영역에 저장된 최근 서재를 자동으로 불러옵니다. 기존 2.9 Drive 스냅샷도 읽습니다.
수동 백업·복원 버튼은 계정 화면에서 제거했습니다. 기존 문서 처리 코드는 호환성 검사용으로만 유지합니다.

- 책·메모·즐겨찾기·순서·카테고리·서재 테마 변경을 1.5초 간격으로 묶어 자동 저장합니다.
- 앱 실행·복귀 시 동기화하고, 실행 중 60초마다 다시 확인합니다. 종료 상태에서 즉시 업로드를 보장하지 않습니다.
- 오프라인 변경은 기기에 유지하고 다음 연결 때 재시도합니다. 액세스 토큰은 저장하지 않습니다.
- 확인된 계정 이메일과 표시 이름만 기기에 기억합니다. Drive 오류가 나도 확인된 계정 표시는 사라지지 않습니다.
- 책 편집 창이 열려 있으면 원격 내용 적용을 미루고, 편집을 마친 뒤 최신 기기 내용과 병합합니다.
- 마지막으로 확인한 클라우드 내용과 기기 내용을 비교합니다. 한쪽에서만 삭제한 책은 삭제를 반영하고,
  양쪽에서 같은 책을 다르게 바꾼 경우 두 버전을 보존합니다. 최초 연결 시 서로 다른 책은 합칩니다.
- 다른 계정으로 바꾸면 이전 계정의 서재를 새 계정에 자동 업로드하지 않습니다. 새 계정 서재를 사용하며,
  교체 전 내용과 미전송 변경사항은 계정별 기기 사본으로 남기고, 그 계정에 다시 연결할 때 병합합니다.
- 클라우드 파일과 동기화 기준은 검증된 스키마와 크기 제한을 적용합니다. 업로드는 이전 스냅샷을 삭제하지 않습니다.
  두 기기의 정확히 동시 업로드는 서버 트랜잭션으로 직렬화되지 않으며, 마지막 업로드가 최근 서재가 됩니다.

### Google 연결을 실제로 활성화하는 소유자 설정

3.0.1의 새 검증용 릴리스 키는 아래 기존 운영 인증서와 다릅니다. 전달 APK의 `APK-INFO.md`에서 실제 SHA-1을 확인하고 [현재 로그인·자동 동기화 설정 안내](docs/GOOGLE_DRIVE_SETUP_KO.md)를 따르세요. 검증 APK는 기존 운영 설치본에 덮어설치할 수 없습니다.

앱 코드만으로 Google Cloud 설정을 대신할 수 없습니다. 해당 Cloud 프로젝트에서:

1. Google Drive API를 사용 설정합니다.
2. Android OAuth 클라이언트의 패키지를 `com.sitebox.app`으로 등록합니다.
3. 배포 APK를 서명한 **정식 인증서 SHA-1**을 등록합니다. 2.9 정식 키로 계속 서명합니다:
   `97:45:51:3D:E2:92:B8:AA:51:DF:3F:06:9F:F7:CE:BD:F7:2B:57:33`.
4. OAuth 동의 화면에 `https://www.googleapis.com/auth/drive.appdata` 범위를 설정합니다.
   테스트 모드라면 로그인할 Google 계정을 테스트 사용자로 등록합니다.
5. 실제 기기에서 연결·재실행·다른 설치본에서 불러오기·오프라인 변경 후 재접속을 확인합니다.

인증 오류 코드 10은 패키지/인증서 등 개발자 OAuth 설정 확인 안내를 표시합니다.
Drive HTTP 403은 인증 실패와 구분하여 API 활성화/접근 권한 문제를 표시합니다.
CI는 계정 선택·Google 동의·실제 Drive 전송을 성공했다고 주장하지 않습니다.
