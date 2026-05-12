package com.velcuri.bassride.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean = false,
    // EQ band levels in millibels
    val band0: Int = 0,   // 32 Hz  (sub-bass)
    val band1: Int = 0,   // 64 Hz  (bass)
    val band2: Int = 0,   // 125 Hz (upper bass)
    val band3: Int = 0,   // 250 Hz (low-mid)
    val band4: Int = 0,   // 500 Hz (mid)
    val band5: Int = 0,   // 1 kHz  (upper-mid)
    val band6: Int = 0,   // 2 kHz  (presence)
    val band7: Int = 0,   // 4 kHz  (high-mid)
    val band8: Int = 0,   // 8 kHz  (high)
    val band9: Int = 0,   // 16 kHz (air)
    val bassBoostStrength: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun bands(): IntArray = intArrayOf(band0, band1, band2, band3, band4, band5, band6, band7, band8, band9)
}
