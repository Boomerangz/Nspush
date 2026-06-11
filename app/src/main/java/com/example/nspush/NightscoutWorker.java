package com.example.nspush;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public final class NightscoutWorker extends Worker {
    static final String UNIQUE_WORK_NAME = "nightscout_polling";

    public NightscoutWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppConfig config = AppConfig.from(getApplicationContext());
        if (!config.isEnabled()) {
            return Result.success();
        }
        if (!AppConfig.isValidUrl(config.getBaseUrl())) {
            NotificationHelper.showError(getApplicationContext(), "Set a valid Nightscout URL");
            return Result.failure();
        }
        try {
            NightscoutEntry entry = NightscoutClient.fetchLatest(config.getBaseUrl(), config.getSecret());
            boolean outOfRange = entry.sgv < config.getLowThreshold() || entry.sgv > config.getHighThreshold();
            if (config.shouldNotifyEveryCheck() || outOfRange || entry.isStale(System.currentTimeMillis())) {
                NotificationHelper.showReading(getApplicationContext(), entry, config.getLowThreshold(), config.getHighThreshold());
            }
            return Result.success();
        } catch (Exception e) {
            NotificationHelper.showError(getApplicationContext(), e.getMessage() == null ? "Unknown error" : e.getMessage());
            return Result.retry();
        }
    }

    static void schedule(Context context, int intervalMinutes) {
        int safeInterval = Math.max(AppConfig.getMinimumIntervalMinutes(), intervalMinutes);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(NightscoutWorker.class, safeInterval, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    static void runOnce(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NightscoutWorker.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueue(request);
    }

    static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_WORK_NAME);
    }
}
