package com.ops.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Runs [SyncManager.syncNow] in the background. Two trigger shapes share
 * this one worker class (see DISCOVERY.md section 6, "Trigger"):
 *
 *  - a ~15 min [androidx.work.PeriodicWorkRequest] fallback heartbeat,
 *    constrained to [NetworkType.CONNECTED], enqueued once at app start
 *    ([enqueuePeriodic]);
 *  - a one-time EXPEDITED request enqueued after every local create/update
 *    ([enqueueOneTime]), under a unique work name with
 *    [ExistingWorkPolicy.KEEP] so a burst of rapid local writes collapses
 *    into a single pending sync rather than spamming the queue.
 *
 * Sync never blocks a screen: this worker is the "always a background
 * coroutine/worker" the UI only ever observes indirectly, via Room `Flow`s
 * and [SyncManager.observeChipState] — nothing awaits this worker directly.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return when (val outcome = syncManager.syncNow()) {
            is SyncOutcome.Success -> Result.success()
            is SyncOutcome.NotSignedIn -> Result.success() // nothing to do yet, not a failure
            is SyncOutcome.Failed -> Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "ops_sync_periodic"
        private const val ONE_TIME_WORK_NAME = "ops_sync_one_time"

        fun enqueuePeriodic(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()
            workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Fired after every local create/update. KEEP + a stable unique name
         * means a rapid burst of edits (e.g. typing through a multi-field
         * form) collapses into one pending sync instead of queueing one per
         * keystroke-triggered save. */
        fun enqueueOneTime(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
