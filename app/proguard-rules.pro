# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Widgets i broadcast receivers: es referencien des del manifest pel nom
# complet; els mantenim sempre sencers (AGP ja els conserva, però així
# queda garantit fins i tot si canvien els default keep rules).
-keep class com.example.cronos.CronosWidget { *; }
-keep class com.example.cronos.CronosWidgetApple { *; }
-keep class com.example.cronos.CronosWidgetLarge { *; }
-keep class com.example.cronos.CronosWidgetTickReceiver { *; }
-keep class com.example.cronos.BootReceiver { *; }

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