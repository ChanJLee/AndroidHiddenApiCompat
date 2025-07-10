-keep class me.chan.lib.hiddenapi.** { *; }
-keepclassmembers class ** {
    @androidx.annotation.Keep <methods>;
}