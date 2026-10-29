package com.maciejhetman.caffeinate.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaffeineSessionTest {

    @Test
    fun countdownRoundsUpToTheNextSecond() {
        assertEquals("30:00", CaffeineSession.formatCountdown(30 * 60_000L))
        assertEquals("30:00", CaffeineSession.formatCountdown(30 * 60_000L - 1))
        assertEquals("0:01", CaffeineSession.formatCountdown(1L))
        assertEquals("0:00", CaffeineSession.formatCountdown(0L))
        assertEquals("1:00:00", CaffeineSession.formatCountdown(60 * 60_000L))
    }

    @Test
    fun remainingWholeMinutesRoundsUpAndNeverHitsZero() {
        assertEquals(30, CaffeineSession.remainingWholeMinutes(30 * 60_000L))
        assertEquals(30, CaffeineSession.remainingWholeMinutes(29 * 60_000L + 1))
        assertEquals(1, CaffeineSession.remainingWholeMinutes(1L))
        assertEquals(1, CaffeineSession.remainingWholeMinutes(0L))
    }

    @Test
    fun remainingFraction() {
        val half = CaffeineSession.On(DurationPreset.Timed(10), 5 * 60_000L, null)
        assertEquals(0.5f, half.remainingFraction!!, 0.0001f)
        assertNull(CaffeineSession.On(DurationPreset.Infinite, null, null).remainingFraction)
    }
}
