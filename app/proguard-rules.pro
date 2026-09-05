# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\SDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# Gson reads API models reflectively.
-keepclassmembers class com.app.chao.chaoapp.bean.** { <fields>; }
-keepattributes Signature,*Annotation*

# GSY factories invoke Class.newInstance(), and fullscreen uses the boxed-Boolean constructor.
# Keep only the selected engines and the constructor actually used by this app.
-keep,allowobfuscation class com.app.chao.chaoapp.playback.SubtitlePlayerManager { public <init>(); }
-keep,allowobfuscation class tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager { public <init>(); }
-keep class com.app.chao.chaoapp.playback.PlaybackVideoPlayer {
    public <init>(android.content.Context, java.lang.Boolean);
}

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
