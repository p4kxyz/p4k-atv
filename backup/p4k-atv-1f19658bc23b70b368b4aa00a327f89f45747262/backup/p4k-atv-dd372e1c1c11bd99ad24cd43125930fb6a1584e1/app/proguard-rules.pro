# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Android TV Leanback classes
-keep class androidx.leanback.** { *; }
-dontwarn androidx.leanback.**

# Keep ExoPlayer classes - BASIC WORKING VERSION
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Keep PlayerActivity - SIMPLE PROTECTION
-keep class com.files.codes.view.PlayerActivity { *; }

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Gson classes and annotations
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Room classes and entities
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep ALL model classes (CRITICAL for Room and API)
-keep class com.files.codes.model.** { *; }
-keep class com.files.codes.utils.** { *; }
-keep class com.files.codes.database.** { *; }

# Keep API response classes
-keep class com.files.codes.model.home.** { *; }
-keep class com.files.codes.model.movie.** { *; }
-keep class com.files.codes.model.tvseries.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Picasso classes
-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**

# Keep Glide classes  
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# Keep generic signature for parameterized types
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep TypeToken for Gson
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Keep all serializable classes
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

# Remove most logging but keep error logs for debugging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    # Keep error logs for crash debugging
    # public static *** e(...);
    # public static *** wtf(...);
}

# Remove System.out.println calls
-assumenosideeffects class java.lang.System {
    public static void out.println(...);
    public static void out.print(...);
    public static void err.println(...);
    public static void err.print(...);
}

# Remove printStackTrace calls
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace(...);
}

# Remove debug-related annotations and classes
-assumenosideeffects class * {
    @androidx.annotation.VisibleForTesting <methods>;
}

# Additional optimizations for release
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-keep class android.os.Handler { *; }
-keep class android.view.View { *; }
-keep class android.view.MotionEvent { *; }
-keep class android.view.KeyEvent { *; }
-keep class android.widget.* { *; }

# ALL interfaces used by PlayerActivity - Complete protection
-keep interface android.view.View$OnFocusChangeListener { *; }
-keep interface android.view.View$OnClickListener { *; }
-keep interface java.lang.Runnable { *; }

# Reflection protection - Keep all method names that might be called via reflection
-keepclassmembers class com.files.codes.view.PlayerActivity {
    *;
}

# Lambda method names protection (Java 8+ synthetic methods)
-keepclassmembers class * {
    synthetic void lambda$*(...);
}

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Gson classes and annotations
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Room classes and entities
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep ALL model classes (CRITICAL for Room and API)
-keep class com.files.codes.model.** { *; }
-keep class com.files.codes.utils.** { *; }
-keep class com.files.codes.database.** { *; }

# Keep API response classes
-keep class com.files.codes.model.home.** { *; }
-keep class com.files.codes.model.movie.** { *; }
-keep class com.files.codes.model.tvseries.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Handler and Runnable for seek functionality - ENHANCED
-keep class android.os.Handler {
    public *;
    public void post(java.lang.Runnable);
    public void postDelayed(java.lang.Runnable, long);
    public void removeCallbacks(java.lang.Runnable);
}

-keep class java.lang.Runnable {
    public void run();
}

# Keep Runnable implementations specifically for PlayerActivity
-keep class * implements java.lang.Runnable {
    public void run();
}

# Keep View focus and click handling - ESSENTIAL for remote control
-keep class android.view.View {
    public void setOnFocusChangeListener(android.view.View$OnFocusChangeListener);
    public void setOnClickListener(android.view.View$OnClickListener);
    public void requestFocus();
    public boolean hasFocus();
}

# Keep KeyEvent handling for DPAD navigation
-keep class android.view.KeyEvent {
    public int getKeyCode();
    public int getAction();
    public static int KEYCODE_DPAD_*;
    public static int ACTION_*;
}

# Keep essential Activity methods for PlayerActivity
-keep class android.app.Activity {
    public void findViewById(int);
    public void runOnUiThread(java.lang.Runnable);
}

# Keep Picasso classes
-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**

# Keep Glide classes  
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# Keep generic signature for parameterized types
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep TypeToken for Gson
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Keep all serializable classes
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

# Remove most logging but keep error logs for debugging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    # Keep error logs for crash debugging
    # public static *** e(...);
    # public static *** wtf(...);
}

# Remove System.out.println calls
-assumenosideeffects class java.lang.System {
    public static void out.println(...);
    public static void out.print(...);
    public static void err.println(...);
    public static void err.print(...);
}

# Remove printStackTrace calls
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace(...);
}

# Remove debug-related annotations and classes
-assumenosideeffects class * {
    @androidx.annotation.VisibleForTesting <methods>;
}

# Additional optimizations for release
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!method/inlining/*
-optimizationpasses 2
-allowaccessmodification
-dontpreverify

# DISABLE problematic optimizations that break seek functionality
-dontoptimize
-dontobfuscate