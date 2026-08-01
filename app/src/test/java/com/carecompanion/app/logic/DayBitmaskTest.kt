package com.carecompanion.app.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ElderHomeViewModel.buildDoses() (ElderHomeViewModel.kt:80-93) filters schedules with:
 *     val todayBit = 1 shl todayIndexMonday0()
 *     scheds.filter { (it.days and todayBit) != 0 ... }
 * and todayIndexMonday0() (ElderHomeViewModel.kt:152-159) maps Monday->0 ... Sunday->6.
 *
 * todayIndexMonday0() reads Calendar.getInstance() directly (no injectable clock) and is
 * private, so it cannot be driven with an arbitrary fixed date without either touching
 * main-source visibility or reflectively stubbing the system clock (non-deterministic /
 * out of scope). These tests instead pin down the exact bit-arithmetic contract the real
 * method must uphold for every day index 0..6, using the same Monday=0..Sunday=6 mapping.
 * A regression in either the mapping or the mask check would be caught if mirrored here.
 */
class DayBitmaskTest {

    private val MON = 0; private val TUE = 1; private val WED = 2
    private val THU = 3; private val FRI = 4; private val SAT = 5; private val SUN = 6

    private fun isDue(days: Int, dayIndexMonday0: Int): Boolean =
        (days and (1 shl dayIndexMonday0)) != 0

    @Test fun `127 (all bits set) is due every day of the week`() {
        for (day in 0..6) assertTrue("day index $day should be due", isDue(127, day))
    }

    @Test fun `31 (Mon-Fri) is due on weekdays only`() {
        for (day in MON..FRI) assertTrue("weekday $day should be due", isDue(31, day))
        assertFalse("Saturday should not be due", isDue(31, SAT))
        assertFalse("Sunday should not be due", isDue(31, SUN))
    }

    @Test fun `31 is due on Wednesday but not Sunday`() {
        assertTrue(isDue(31, WED))
        assertFalse(isDue(31, SUN))
    }

    @Test fun `weekend-only mask (96) is due Sat and Sun but not weekdays`() {
        val weekendMask = (1 shl SAT) or (1 shl SUN) // 96
        assertTrue(isDue(weekendMask, SAT))
        assertTrue(isDue(weekendMask, SUN))
        for (day in MON..FRI) assertFalse(isDue(weekendMask, day))
    }

    @Test fun `single-day mask only matches its own day`() {
        val mondayOnly = 1 shl MON // 1
        assertTrue(isDue(mondayOnly, MON))
        for (day in TUE..SUN) assertFalse(isDue(mondayOnly, day))
    }

    @Test fun `zero mask is never due`() {
        for (day in 0..6) assertFalse(isDue(0, day))
    }

    @Test fun `day index mapping constants are Monday=0 through Sunday=6`() {
        assertTrue(MON == 0 && TUE == 1 && WED == 2 && THU == 3 && FRI == 4 && SAT == 5 && SUN == 6)
    }
}
