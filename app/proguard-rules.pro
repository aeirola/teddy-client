# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/opt/android-sdk/tools/proguard/proguard-android.txt
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

# No need to obfuscate since the source is open
-dontobfuscate

# Jackson is referencing some unused classes
-dontwarn com.fasterxml.jackson.databind.**

# Some data model classes rely on reflection, need to explicitly keep them
-keep public class fi.iki.aeirola.teddyclientlib.models.**
