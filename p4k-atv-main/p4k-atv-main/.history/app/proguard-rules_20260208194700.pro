# ============================================================================
# ProGuard Rules - OXOO TV Android TV App v2.3.3
# ============================================================================

# ---- General Settings ----
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Keep source file & line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

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

# ---- App Utils (contains API clients, helpers) ----
-keep class com.files.codes.utils.** { *; }

# ---- App Activities & Fragments (registered in Manifest) ----
-keep class com.files.codes.view.** extends android.app.Activity { *; }
-keep class com.files.codes.view.** extends androidx.fragment.app.Fragment { *; }
-keep class com.files.codes.view.fragments.** extends androidx.fragment.app.Fragment { *; }
-keep class com.files.codes.DetailsActivity { *; }
-keep class com.files.codes.PlaybackActivity { *; }
-keep class com.files.codes.BrowseErrorActivity { *; }

# ---- App Presenters (Leanback uses reflection) ----
-keep class com.files.codes.view.presenter.** { *; }

# ---- App OTA Update Service ----
-keep class com.files.codes.view.OTAUpdateService { *; }

# ---- Native / JNI Methods ----
-keepclasseswithmembernames class * {
    native <methods>;
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
-keep class androidx.leanback.** { *; }
-dontwarn androidx.leanback.**

# ---- ExoPlayer + FFmpeg Extension ----
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# ---- Retrofit + OkHttp ----
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
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
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.firebaseui.** { *; }
-dontwarn com.firebaseui.**

# ---- Glide ----
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

# ---- Picasso ----
-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**

# ---- RxJava ----
-keep class io.reactivex.** { *; }
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
