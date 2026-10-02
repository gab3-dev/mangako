package com.gabedev.mangako.backup

import android.content.Context
import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.gabedev.mangako.data.local.BackupFrequency
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class BackupSchedulerTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(Executors.newSingleThreadExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
        clearWork()
    }

    @After
    fun tearDown() {
        clearWork()
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun `enqueueAfterChange replaces the pending trigger`() {
        BackupScheduler.enqueueAfterChange(context)

        val pending = work(BackupScheduler.CHANGE_WORK_NAME)
        assertEquals(1, pending.size)
        assertTrue(BackupScheduler.CHANGE_TRIGGER_TAG in pending.single().tags)
    }

    @Test
    fun `on change frequency cancels periodic backup`() {
        BackupScheduler.configure(context, BackupFrequency.DAILY)

        BackupScheduler.configure(context, BackupFrequency.ON_CHANGE)

        assertTrue(
            work(BackupScheduler.PERIODIC_WORK_NAME).none { it.state == WorkInfo.State.ENQUEUED }
        )
    }

    @Test
    fun `daily and weekly frequencies update periodic backup`() {
        BackupScheduler.configure(context, BackupFrequency.DAILY)
        val daily = work(BackupScheduler.PERIODIC_WORK_NAME).single()
        BackupScheduler.configure(context, BackupFrequency.WEEKLY)

        val weekly = work(BackupScheduler.PERIODIC_WORK_NAME).single()
        assertEquals(daily.id, weekly.id)
        assertTrue(weekly.generation > daily.generation)
    }

    private fun work(name: String) = workManager.getWorkInfosForUniqueWork(name).get(10, TimeUnit.SECONDS)

    private fun clearWork() {
        workManager.cancelUniqueWork(BackupScheduler.CHANGE_WORK_NAME).result.get(10, TimeUnit.SECONDS)
        workManager.cancelUniqueWork(BackupScheduler.PERIODIC_WORK_NAME).result.get(10, TimeUnit.SECONDS)
        workManager.pruneWork().result.get(10, TimeUnit.SECONDS)
    }
}
