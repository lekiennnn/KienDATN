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

# Keep Compose-related classes 
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-dontwarn com.google.firebase.**

# Don't obfuscate ViewModels
-keep public class * extends androidx.lifecycle.ViewModel {*;}
-keep public class * extends androidx.lifecycle.AndroidViewModel {*;}