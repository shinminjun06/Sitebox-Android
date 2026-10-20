# SiteBox review record

Base examined: 6c74f8e (default branch, 20 October 2026).
Supplied APK: com.sitebox.app, versionCode 1, versionName 1.0, minSdk24/target35;
29 bundled assets. Its classroom background SHA-256 equals the source image.
The legacy APK has no source revision metadata, so an exact commit match cannot
be proven from the APK. All implementation follows the latest GitHub source.

## Pass 1: artwork, structure and local build
- Viewed target 323593.png and current screenshots 323622.png/1792489765847.png.
- Replaced procedural grain generation with reviewed generated raster surfaces.
- Equal 3×3 fractional interaction cells, complete furniture sprite, 3 usable book
  widths per cubby and a compact header below system insets.
- Restored live favorite markers in bitmap-rendering branch and exposed favorite/delete.
- JVM rules and asset-validation suite pass. Debug APK + device test APK compile.
- Lint's eight WrongConstant findings fixed; subsequent lint passes with zero errors.

## Pass 2: independent code review and corrections
- Filtered drops used a full-list index for paging: preserve self-drop page and
  compute new page using matching books only.
- Restored the existing long-press classroom-only view and tap-to-return overlay.
- Corrected bare host:port and internationalized-domain acceptance; real JVM
  regression was observed failing before correcting URL normalization.
- Added native long-press drag input test, independent nonmatching theme persistence,
  filtered-drop regressions and failure screenshots to device instrumentation.
- Added drag-edge scrolling and detached WebView before destroy.
- Existing MainActivity private SharedPreferences identity and JSON keys stay intact.

## Signing and upgrade limitation
The uploaded APK certificate SHA-256 is
`d68000b16c560a995b8d6328cae0575a96f5ad4e287a6a0d73197e7edbc13b1d`.
Its private signing key is not included. A newly signed APK cannot replace that
installed package while keeping its app data unless the original key is supplied.
Do not uninstall an existing copy with irreplaceable bookmarks merely to test this
build. The code preserves storage compatibility; signing compatibility is separate.
The Actions workflow now caches its debug keystore for consistent subsequent debug
updates. This is debug distribution, not production release signing. Cache deletion
can still change signing. A production release should use an owned, backed-up
keystore supplied through repository secrets.

## Deferred limitations
- Third-party theme ZIP import remains disabled; v1 folder/JSON format documented.
- Category nameplates use a lightweight native metallic drawable with live text.
- Font sizing follows system scaling; huge accessibility fonts may ellipsize labels.
- Web previews depend on site/network policies; external opening remains available.
- A passing emulator run does not establish compatibility/performance on every phone.
