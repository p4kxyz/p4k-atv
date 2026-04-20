# ============================================================================
# ProGuard Rules - OXOO TV Android TV App v2.3.3
# ============================================================================

# ---- General Settings ----
-optimizationpasses 7
-allowaccessmodification
-dontpreverify
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-overloadaggressively
-adaptclassstrings

# Hide original source file names to reduce reverse-engineering hints.
-keepattributes LineNumberTable
-renamesourcefileattribute ""

# Keep generic signatures (needed for Retrofit, Gson, Room)
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,Exceptions

# ---- Strip Logs in Release ----
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
-assumenosideeffects class java.lang.System {
    public static void out.println(...);
    public static void out.print(...);
    public static void err.println(...);
    public static void err.print(...);
}

# ---- App Model Classes (CRITICAL - used by Gson/Retrofit/Room) ----
-keep class com.files.codes.model.** { *; }
-keep class com.files.codes.database.** { *; }

# ---- App Activities & Fragments (registered in Manifest) ----
-keep class com.files.codes.view.** extends android.app.Activity { *; }
-keep class com.files.codes.view.** extends androidx.fragment.app.FragmentActivity { *; }
-keep class com.files.codes.view.** extends androidx.fragment.app.Fragment { *; }
-keep class com.files.codes.view.fragments.** extends androidx.fragment.app.Fragment { *; }
-keep class com.files.codes.view.fragments.testFolder.** { *; }
-keep class com.files.codes.DetailsActivity { *; }
-keep class com.files.codes.PlaybackActivity { *; }
-keep class com.files.codes.BrowseErrorActivity { *; }

# ---- App Presenters (Leanback uses reflection) ----
-keep class com.files.codes.view.presenter.** { *; }

# ---- App OTA Update Service ----
-keep class com.files.codes.service.OTAUpdateService { *; }
-keep class com.files.codes.service.OTADownloadManager { *; }
-keep class com.files.codes.view.OTAUpdateManager { *; }

# ---- Native / JNI Methods ----
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI bridge class/methods used by explicit RegisterNatives.
-keep class com.files.codes.AppConfig {
    public static native java.lang.String getApiServerUrl();
    public static native java.lang.String getApiKey();
    public static native java.lang.String getPurchaseCode();
    public static native java.lang.String getAdsWorkerSecret();
}

-keep class com.files.codes.utils.VideoTokenGenerator {
    private static native java.lang.String nativeGenVideoUrl(java.lang.String);
    private static native java.lang.String nativeGetDynamicSecret2();
}

# ---- Serializable ----
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---- Parcelable ----
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ---- Enums ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Lambda support ----
-keepclassmembers class * {
    synthetic void lambda$*(...);
}

# ============================================================================
# THIRD-PARTY LIBRARIES
# ============================================================================

# ---- AndroidX Leanback ----
-dontwarn androidx.leanback.**

# ---- ExoPlayer + FFmpeg Extension ----
-dontwarn com.google.android.exoplayer2.**

# ---- Retrofit + OkHttp ----
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- Gson ----
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- Room ----
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ---- Firebase ----
-dontwarn com.google.firebase.**
-dontwarn com.firebaseui.**

# ---- Glide ----
-dontwarn com.bumptech.glide.**
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

# ---- Picasso ----
-dontwarn com.squareup.picasso.**

# ---- RxJava ----
-dontwarn io.reactivex.**

# ---- Guava ----
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue

# ---- Multidex ----
-keep class androidx.multidex.** { *; }

# ---- FileProvider ----
-keep class androidx.core.content.FileProvider { *; }
