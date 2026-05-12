# ────────────────────────────────────────────────────────────────────────────
# BassRide ProGuard / R8 rules
# ────────────────────────────────────────────────────────────────────────────

# Keep important attributes for debugging and reflection
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions

# ── Android AudioEffect API ──────────────────────────────────────────────────
-keep class android.media.audiofx.** { *; }
-keep class android.media.AudioManager { *; }
-keep class android.media.AudioFocusRequest { *; }

# ── Google Play Billing ───────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }

# ── Hilt / Dagger ────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# ── Room Database ─────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclasseswithmembers class * {
    @androidx.room.* <fields>;
}
-keepclasseswithmembers class * {
    @androidx.room.* <methods>;
}
# Keep BassRide entities and DAOs by package
-keep class com.velcuri.bassride.data.entity.** { *; }
-keep class com.velcuri.bassride.data.dao.** { *; }
-keep class com.velcuri.bassride.data.db.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# ── Kotlin metadata / reflection ──────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# ── Jetpack Glance (App Widget) ───────────────────────────────────────────────
-keep class androidx.glance.** { *; }
-keep class com.velcuri.bassride.widget.** { *; }

# ── Tarsos DSP ────────────────────────────────────────────────────────────────
-keep class be.tarsos.dsp.** { *; }

# ── BassRide billing domain ───────────────────────────────────────────────────
-keep class com.velcuri.bassride.billing.** { *; }

# ── BroadcastReceivers and Services referenced from manifest ─────────────────
-keep class com.velcuri.bassride.bluetooth.BluetoothReceiver { *; }
-keep class com.velcuri.bassride.audio.BassRideService { *; }

# ── Suppress spurious warnings ───────────────────────────────────────────────
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
