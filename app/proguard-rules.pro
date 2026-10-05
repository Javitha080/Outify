-keep class cc.tomko.outify.core.SessionCallback { *; }
-keep class cc.tomko.outify.core.spirc.SpircInitializationCallback { *; }
-keep class cc.tomko.outify.core.spirc.SpircBufferCallback { *; }
-keep class cc.tomko.outify.core.spirc.SpircDeviceCallback { *; }

-keep class cc.tomko.outify.playback.callbacks.PlayerEventCallback { *; }
-keep class cc.tomko.outify.playback.AudioEngine { *; }

-keepclasseswithmembernames class * { native <methods>; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialInfo *;
}
-keep class * extends androidx.room.RoomDatabase
-keep @dagger.hilt.android.HiltAndroidApp class *
