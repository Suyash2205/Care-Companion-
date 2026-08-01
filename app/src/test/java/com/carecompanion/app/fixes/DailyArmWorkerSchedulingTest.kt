package com.carecompanion.app.fixes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.carecompanion.app.reminder.DailyArmWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests the scheduling/cancellation surface of DailyArmWorker — the fix for "reminders
 * only armed when the elder opened the app". [DailyArmWorker] itself is a `@HiltWorker`
 * built via `@AssistedInject` from three repositories that Hilt would normally supply;
 * constructing the worker instance and calling `doWork()` in a plain JVM test would
 * require either a full Hilt test component or a hand-rolled WorkerFactory wired to fake
 * repositories, neither of which this suite sets up. So `doWork()`'s scheduling logic
 * itself (which doses get picked, day-of-week bitmask filtering, etc.) is NOT covered
 * here — only the companion object's `ensureScheduled` / `cancel`, which is exactly the
 * surface responsible for the bug ("no periodic re-arm exists unless the app is opened").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyArmWorkerSchedulingTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    private val workName = "cc_daily_arm"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    private fun infosForName(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(workName).get()

    @Test
    fun `ensureScheduled enqueues unique periodic work under the expected name`() {
        DailyArmWorker.ensureScheduled(context)

        val infos = infosForName()
        assertEquals("exactly one periodic work item must be enqueued", 1, infos.size)
        assertTrue(
            "the enqueued work must not be finished/cancelled immediately",
            infos.single().state == WorkInfo.State.ENQUEUED,
        )
    }

    @Test
    fun `calling ensureScheduled twice does not create duplicates (KEEP policy)`() {
        DailyArmWorker.ensureScheduled(context)
        DailyArmWorker.ensureScheduled(context)
        DailyArmWorker.ensureScheduled(context)

        val infos = infosForName()
        assertEquals(
            "repeated calls must KEEP the existing periodic work, never stack duplicates",
            1,
            infos.size,
        )
    }

    @Test
    fun `cancel stops the scheduled work`() {
        DailyArmWorker.ensureScheduled(context)
        assertEquals(1, infosForName().size)

        DailyArmWorker.cancel(context)

        val infos = infosForName()
        assertTrue(
            "after cancel, the unique work must be CANCELLED (WorkManager keeps a tombstone record, not zero entries)",
            infos.isEmpty() || infos.all { it.state == WorkInfo.State.CANCELLED },
        )
    }
}
