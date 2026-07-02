# Keep Vosk JNI-facing classes available when release minification is enabled.
# Minification is currently disabled in app/build.gradle.kts, but the release
# build type already references this file.
-keep class org.vosk.** { *; }
-keep class com.alphacephei.** { *; }
