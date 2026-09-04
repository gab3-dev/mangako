package com.gabedev.mangako.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gabedev.mangako.data.local.BackupFrequency
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupSchedulerTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        workManager = WorkManager.getInstance(context)
        clearWork()
    }

    @After
    fun tearDown() {
        clearWork()
    }

    @Test
    fun enqueueAfterChange_replacesPendingWork_andMarksTrigger() {
        BackupScheduler.enqueueAfterChange(context)
        BackupScheduler.enqueueAfterChange(context)

        val pending = work(BackupScheduler.CHANGE_WORK_NAME)
            .filter { it.state == WorkInfo.State.ENQUEUED }

        assertEquals(1, pending.size)
        assertTrue(BackupScheduler.CHANGE_TRIGGER_TAG in pending.single().tags)
    }

    @Test
    fun configure_updatesPeriodicWork_andOnChangeCancelsIt() {
        BackupScheduler.configure(context, BackupFrequency.DAILY)
        val daily = work(BackupScheduler.PERIODIC_WORK_NAME)
            .single { it.state == WorkInfo.State.ENQUEUED }

        BackupScheduler.configure(context, BackupFrequency.WEEKLY)
        val weekly = work(BackupScheduler.PERIODIC_WORK_NAME)
            .single { it.state == WorkInfo.State.ENQUEUED }

        assertEquals(daily.id, weekly.id)
        assertTrue(weekly.generation > daily.generation)

        BackupScheduler.configure(context, BackupFrequency.ON_CHANGE)

        assertTrue(
            work(BackupScheduler.PERIODIC_WORK_NAME)
                .none { it.state == WorkInfo.State.ENQUEUED }
        )
    }

    private fun work(name: String): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(name).get(10, TimeUnit.SECONDS)

    private fun clearWork() {
        workManager.cancelUniqueWork(BackupScheduler.CHANGE_WORK_NAME)
            .result.get(10, TimeUnit.SECONDS)
        workManager.cancelUniqueWork(BackupScheduler.PERIODIC_WORK_NAME)
            .result.get(10, TimeUnit.SECONDS)
        workManager.pruneWork().result.get(10, TimeUnit.SECONDS)
    }
}
