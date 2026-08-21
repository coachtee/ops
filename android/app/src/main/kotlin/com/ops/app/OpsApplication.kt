package com.ops.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.ops.app.data.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OpsApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // The ~15 min fallback heartbeat (DISCOVERY.md section 6) — enqueued
        // once here with KEEP semantics, so re-launching the app never
        // duplicates it. It's a no-op per run if the user isn't signed in
        // yet (SyncManager.syncNow() short-circuits to NotSignedIn).
        SyncWorker.enqueuePeriodic(WorkManager.getInstance(this))
    }
}
