package com.maciejhetman.caffeinate.widget

import com.maciejhetman.caffeinate.session.CaffeineSession
import com.maciejhetman.caffeinate.session.DurationPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSessionTest {

    private fun timed(remainingMillis: Long) =
        CaffeineSession.On(DurationPreset.Timed(30), remainingMillis, null)

    @Test
    fun perSecondTicksWithinAMinuteMapToTheSameWidgetState() {
        // distinctUntilChanged relies on this to skip recompositions between minute boundaries.
        assertEquals(
            WidgetSession.from(timed(29 * 60_000L + 59_000L)),
            WidgetSession.from(timed(29 * 60_000L + 1_000L)),
        )
    }

    @Test
    fun modeMatching() {
        val infinite = WidgetSession.from(CaffeineSession.On(DurationPreset.Infinite, null, null))
        assertTrue(infinite.isModeActive(timed = false))
        assertFalse(infinite.isModeActive(timed = true))
        assertFalse(WidgetSession.from(CaffeineSession.Off).isModeActive(timed = false))
    }
}
