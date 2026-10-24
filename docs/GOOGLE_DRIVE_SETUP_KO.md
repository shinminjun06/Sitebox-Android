# Google 로그인과 자동 동기화 (3.0.1)

SiteBox의 계정 화면에는 **Google로 로그인**, 연결된 계정의 **Google 계정 다시 연결**, **Google 계정 연결 해제**만 있습니다. 이메일/비밀번호 회원가입, 파일 제공자를 이용한 로그인, 수동 백업·복원 버튼은 제공하지 않습니다.

로그인한 Google 계정은 Google Identity Services의 인증 결과에서 먼저 확인해 기기에 기억합니다. Drive API가 비활성화되거나 네트워크가 끊겨도 이미 확인된 계정을 "연결되지 않음"으로 바꾸지 않습니다. 계정 표시와 동기화 성공 여부는 별개입니다. 토큰은 메모리에만 두며 만료 시 갱신을 시도하고, 재동의가 필요하면 다시 연결하도록 안내합니다. 앱 서버나 웹 클라이언트 시크릿을 APK에 넣지 않습니다.

## 로그인 후 자동 동작

- 해당 Google 계정의 Drive **앱 전용 데이터 영역(appDataFolder)** 에서 최근 SiteBox 스냅샷을 조회합니다. 기존 호환 스냅샷도 읽습니다.
- 첫 연결에서는 기기의 기존 책과 클라우드 책을 병합하고 책·메모·즐겨찾기·순서·카테고리·테마를 자동 반영합니다. 클라우드에 아무것도 없으면 기기 내용을 자동 저장합니다.
- 로컬 변경 후 1.5초 뒤 동기화를 시작하고, 앱 복귀 시와 실행 중 약 60초마다 원격 내용을 다시 확인합니다. 앱이 종료된 동안의 즉시 업로드는 보장하지 않습니다.
- 마지막 동기화 내용을 기준으로 삭제와 변경을 비교합니다. 같은 책을 양쪽에서 다르게 수정하면 두 버전을 보존합니다. 동시 업로드를 서버 트랜잭션으로 직렬화하지는 않습니다.
- 다른 계정으로 바꾸면 이전 계정의 책을 새 계정으로 자동 업로드하지 않습니다. 계정별 기기 사본을 보관합니다. 앱 삭제 시 사본도 삭제됩니다.
- 편집 창·드래그·장면 애니메이션 중에는 반영을 미룹니다. 반영 전 기기 사본을 원자적으로 보관하며, 손상되었거나 2 MiB를 넘는 스냅샷은 거부합니다.
- 일반 Drive 폴더에 수동으로 올린 JSON은 appDataFolder의 자동 동기화 대상이 아닙니다. 다른 Google Cloud 프로젝트의 앱 전용 데이터도 자동으로 가져올 수 없습니다. 기존 앱 데이터 복원을 위해 원래와 같은 Cloud 프로젝트를 사용하세요.

## 소유자가 Google Cloud Console에서 해야 할 설정

이 단계는 앱 코드나 빌드만으로 대신할 수 없습니다. 기존 데이터가 있다면 새 프로젝트 대신 **기존 앱의 Google Cloud 프로젝트**를 선택하세요.

1. **APIs & Services → Library → Google Drive API → Enable**.
2. **Google Auth Platform**에서 Branding(앱 이름, 지원 이메일, 개발자 연락처), Audience(대상 사용자), Data Access를 설정합니다. 테스트 모드라면 로그인할 실제 Google 계정을 Test users에 추가합니다. 조직 내부 앱이라면 조직 계정만 사용할 수 있습니다.
3. Data Access에 `https://www.googleapis.com/auth/drive.appdata`를 등록합니다. 앱도 이 범위만 요청합니다. 일반 Drive 파일 전체 접근 권한은 필요하지 않습니다.
4. **Clients → Create client → Android** 에서 실제 APK의 패키지명과 **APK 서명 인증서 SHA-1**을 등록합니다. 웹 클라이언트 ID나 클라이언트 시크릿을 APK/저장소에 넣지 마세요.
5. 검증용 release APK의 패키지는 `com.sitebox.app`, 별도 설치 preview APK는 `com.sitebox.app.preview`입니다. 두 패키지는 각각 Android OAuth 클라이언트로 등록해야 합니다. 정확한 인증서는 전달 APK 옆의 `APK-INFO.md`를 사용하세요. 저장소의 예전 debug/release SHA-1을 새 APK에 재사용하면 안 됩니다.
6. Play App Signing을 사용하면 기기에 설치되는 앱의 **앱 서명 키 인증서**도 해당 패키지의 Android OAuth 클라이언트에 등록합니다. 업로드 키와 다를 수 있습니다.
7. 외부 사용자 배포 전에는 Google이 콘솔에서 요구하는 동의 화면 게시·검증 절차를 완료합니다. 테스트 사용자 등록은 검증을 대신하지 않습니다.

인증 코드 10이면 패키지/SHA-1/프로젝트 설정을 먼저 확인합니다. Drive HTTP 403이면 Google Drive API 활성화, 테스트 사용자, 범위 및 조직 접근 정책을 확인합니다. 계정이 표시되더라도 Drive 오류가 남아 있으면 동기화 완료 상태가 아닙니다.

## 실제 기기에서 완료해야 하는 검증

Google Play services가 설치된 Android 7.0 이상 기기에서, 위 콘솔 설정을 마친 APK를 사용합니다.

1. 설정 → Google 계정 · 자동 동기화 → **Google로 로그인** → 실제 계정 선택과 Drive 동의를 마칩니다. 계정 이메일과 `자동 동기화 완료`를 확인합니다.
2. 기존 클라우드 서재가 자동 반영되는지 확인합니다. 기존 기기 책이 있다면 합쳐졌는지도 확인합니다. 수동 복원 버튼은 없어야 합니다.
3. 책·메모·즐겨찾기·테마를 수정하고 자동 동기화 완료를 기다립니다. 같은 프로젝트의 다른 설치본에서 같은 계정으로 로그인해 반영을 확인합니다.
4. 비행기 모드에서 수정한 뒤 재연결하고, 앱을 재실행해 계정 표시와 저장 내용이 유지되는지 확인합니다.
5. Drive 권한을 철회한 뒤 앱에서 재연결합니다. 계정 표시와 오류 안내가 구분되고 재동의 후 동기화가 재개되는지 확인합니다. 다른 계정으로 전환할 때 이전 계정의 서재가 새 계정에 올라가지 않는지도 확인합니다.

이 클라우드 작업에는 실제 Android 기기가 연결되어 있지 않아, **실제 Google 계정 선택/동의/Drive 서버 왕복은 검증하지 못했습니다**. Robolectric 테스트는 SDK 인증 결과 처리, 계정 유지, 자동 복원/삭제/병합 및 화면을 검사합니다. 테스트 fixture를 실제 Google 로그인 성공으로 간주하지 않습니다.

## 빌드와 서명

표준 개발 검증:

```sh
sh tests/run.sh
gradle :app:testDebugUnitTest :app:assembleDebug :app:assemblePreview :app:assembleDebugAndroidTest :app:lintDebug --no-daemon
```

릴리스에는 `SITEBOX_RELEASE_STORE_FILE`, `SITEBOX_RELEASE_STORE_PASSWORD`, `SITEBOX_RELEASE_KEY_ALIAS`, `SITEBOX_RELEASE_KEY_PASSWORD`가 필요합니다. 환경변수 또는 개인 Gradle 설정을 사용하고 저장소에 비밀번호를 추가하지 마세요. 누락된 설정과 `androiddebugkey` 별칭은 빌드 단계에서 거부합니다. `release`는 `debuggable=false`, R8와 리소스 축소를 사용합니다.

이번 작업은 사용자 선택에 따라 **새 검증용 릴리스 키**를 사용합니다. 기존 운영 인증서와 다르므로 기존 설치본에 덮어설치할 수 없습니다. 기존 앱의 데이터를 지우지 말고, 병렬 테스트에는 preview APK를 사용하세요. preview는 기존 앱의 비공개 로컬 데이터를 직접 읽지 못합니다. 키 파일과 개인 서명 설정은 `.signing/`의 Git 무시 경로에 권한을 제한해 보관하며 배포 산출물에는 포함하지 않습니다. 향후 이 검증판을 업데이트하려면 같은 키가 필요하지만, 운영 배포에는 원래의 운영 키를 사용하세요.

공식 문서:

- https://developer.android.com/identity/authorization
- https://developers.google.com/workspace/drive/api/guides/appdata
- https://developers.google.com/identity/protocols/oauth2/native-app

## 3.0.2: ‘연결을 취소했어요’로 표시되던 오류 구분

이전 버전은 로그인 Activity의 반환 코드가 RESULT_OK가 아니면 Intent에 담긴 Google 오류를 읽지 않고 무조건 취소로 표시했습니다. 3.0.2는 Google SDK로 반환 데이터를 먼저 해석하여 실제 오류 코드를 표시합니다. 성공 payload가 있더라도 Activity가 취소된 경우 로그인 완료로 처리하지 않습니다.

- **인증 코드 10**: Android OAuth 개발자 설정 오류입니다. 사용자가 설치한 APK의 패키지명과 실제 서명 SHA-1을 기존 Google Cloud 프로젝트의 Android OAuth 클라이언트에 등록해야 합니다. 이번 3.0.2 release는 3.0.1 검증판과 같은 키이며 SHA-1은 `CD:84:36:B7:75:DA:5A:45:9F:76:26:8F:D5:65:C6:E5:8E:1A:D1:9C`, 패키지는 `com.sitebox.app`입니다.
- **인증 코드 7**: Google SDK가 네트워크 오류를 반환한 경우입니다.
- **인증 코드 16 / 12501**: Google SDK의 취소/중단 상태입니다. 사용자 동의 화면 종료와 Google 서비스의 중단을 사진만으로 구별할 수 없으므로 사용자가 취소했다고 단정하지 않습니다.
- **화면 반환 코드 0**: Android가 RESULT_CANCELED로 반환했으나 구체적인 Google 오류 결과를 얻지 못한 경우입니다. 이것만으로 OAuth 오류나 사용자 취소를 확정할 수 없습니다.
- **화면 반환 코드 -1**: Android는 RESULT_OK였으나 필수 Google 결과 데이터가 없는 경우입니다. 로그인 성공으로 취급하지 않습니다.

문구를 구분한 것은 실제 Google 인증 실패 원인을 해결했다는 의미가 아닙니다. 실제 계정 선택/동의 화면과 위 결과 코드를 확인하고, 해당 원인을 수정한 뒤 기기에서 재검증해야 합니다. Google Play Protect의 설치 보안 경고와도 별개의 문제입니다.
