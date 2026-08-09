package com.carecompanion.app.fixes

import com.carecompanion.app.data.model.MedicineDto
import com.carecompanion.app.ui.guardian.MedicinesUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the state semantics behind "the Save button does nothing" on the Schedule
 * Medicine screen.
 *
 * Every screen driven by MedicinesViewModel navigates away on `done == true`. `load()`
 * used to publish a brand-new [MedicinesUiState] when its network calls returned, which
 * reset `done` to its default of false. The schedule builder calls load() on entry and
 * pre-selects every field, so the guardian can press Save while that load is still in
 * flight — the load then lands afterwards, wipes done=true, and the screen never
 * navigates. The save had actually succeeded; only the acknowledgement was lost.
 *
 * These tests lock in the two properties that fix depends on: a fresh state must not be
 * treated as done, and a copy()-style refresh must carry `done` through.
 */
class MedicinesUiStateDoneTest {

    private val med = MedicineDto(id = "m1", elderId = "e1", name = "Thyroid Medicine")

    @Test
    fun `a fresh state is not done - which is why rebuilding one ate the save`() {
        assertFalse(MedicinesUiState().done)
        // The exact call load() used to make. If this ever reads true, the regression
        // has been reintroduced somewhere else.
        assertFalse(MedicinesUiState(false, listOf(med), emptyMap()).done)
    }

    @Test
    fun `refreshing loaded data with copy preserves an already-signalled done`() {
        val saved = MedicinesUiState(saving = false, done = true)

        val afterLoad = saved.copy(
            loading = false, medicines = listOf(med), schedules = emptyMap(), error = null,
        )

        assertTrue("a load landing after a save must not cancel the navigation", afterLoad.done)
        assertEquals(listOf(med), afterLoad.medicines)
    }

    @Test
    fun `a load landing after a save keeps done even while clearing a stale error`() {
        val saved = MedicinesUiState(done = true, error = null)
        val afterLoad = saved.copy(loading = false, error = null)

        assertTrue(afterLoad.done)
        assertEquals(null, afterLoad.error)
    }

    @Test
    fun `the medicine the schedule builder updates is found by id`() {
        // addSchedules writes water/meal onto the parent medicine, which it can only do
        // if load() has populated `medicines` by then.
        val state = MedicinesUiState(false, listOf(med), emptyMap())

        assertEquals(med, state.medicines.find { it.id == "m1" })
        assertEquals(null, state.medicines.find { it.id == "nope" })
    }
}
