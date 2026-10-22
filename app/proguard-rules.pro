# Google Identity Services / Auth: keep public SDK types and callback contracts.
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.api.Scope { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Drive is accessed through HTTPS REST, not the separate Google Drive Java SDK.
# Preserve authorization/result handling and the versioned backup codec.
-keep class com.sitebox.app.GoogleDriveBackup** { *; }
-keep class com.sitebox.app.BackupController** { *; }
-keep class com.sitebox.app.BackupCodec** { *; }

# Android entry point, WebView bridge annotations, and Parcelable creators.
-keep class com.sitebox.app.MainActivity { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Sync state is exercised by the native release smoke checks.
-keep class com.sitebox.app.SyncPlanner { *; }
