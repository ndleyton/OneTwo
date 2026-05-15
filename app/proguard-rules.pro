# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Nicue\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# ---- Room ----
# Keep all Room-generated _Impl classes and their constructors.
# R8 strips these by default since they are only referenced via reflection.
-keep class **.*_Impl { *; }
-keep class **.*_Impl$* { *; }

# Keep Room database subclasses
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Keep Room entity and DAO annotations so R8 doesn't rename them
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
