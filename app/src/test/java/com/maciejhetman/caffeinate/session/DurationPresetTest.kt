package com.maciejhetman.caffeinate.session

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationPresetTest {

    @Test
    fun serializeRoundTrips() {
        listOf(DurationPreset.Infinite, DurationPreset.Timed(1), DurationPreset.Timed(90))
            .forEach { assertEquals(it, DurationPreset.fromSerialized(it.serialize())) }
    }

    @Test
    fun fromSerializedFallsBackForMissingOrGarbage() {
        assertEquals(DurationPreset.Default, DurationPreset.fromSerialized(null))
        assertEquals(DurationPreset.Default, DurationPreset.fromSerialized(""))
        assertEquals(DurationPreset.Default, DurationPreset.fromSerialized("nonsense"))
        assertEquals(DurationPreset.DefaultTimed, DurationPreset.fromSerialized("Timed:abc"))
    }

    @Test
    fun fromSerializedClampsOutOfRangeMinutes() {
        assertEquals(DurationPreset.Timed(DurationPreset.MAX_MINUTES), DurationPreset.fromSerialized("Timed:9999"))
        assertEquals(DurationPreset.Timed(DurationPreset.MIN_MINUTES), DurationPreset.fromSerialized("Timed:0"))
    }

    @Test
    fun fromSerializedMigratesLegacyChipNames() {
        assertEquals(DurationPreset.Timed(5), DurationPreset.fromSerialized("Minutes5"))
        assertEquals(DurationPreset.Timed(120), DurationPreset.fromSerialized("Hour2"))
    }

    @Test
    fun snapToSliderStepRoundsAndClamps() {
        assertEquals(5, DurationPreset.snapToSliderStep(1))
        assertEquals(10, DurationPreset.snapToSliderStep(8))
        assertEquals(30, DurationPreset.snapToSliderStep(31))
        assertEquals(DurationPreset.MAX_MINUTES, DurationPreset.snapToSliderStep(1000))
    }

    @Test
    fun sliderStepsCoverEveryFiveMinuteValue() {
        // Material slider "steps" excludes both endpoints.
        val values = DurationPreset.SLIDER_STEPS + 2
        assertEquals(
            (DurationPreset.MAX_MINUTES - DurationPreset.SLIDER_MIN_MINUTES) /
                DurationPreset.SLIDER_STEP_MINUTES + 1,
            values,
        )
    }

    @Test
    fun formatMinutes() {
        assertEquals("5m", DurationPreset.formatMinutes(5))
        assertEquals("1h", DurationPreset.formatMinutes(60))
        assertEquals("1h 30m", DurationPreset.formatMinutes(90))
    }
}
