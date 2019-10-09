package com.dparker.apps.checkvalve;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

@TargetApi(21)
public class BackgroundJobUtil {
    private static final String TAG = BackgroundJobUtil.class.getSimpleName();

    public static void scheduleJob(Context context, boolean runNow) {
        Log.i(TAG, "scheduleJob(): Building new job.");
        ComponentName serviceComponent = new ComponentName(context, BackgroundJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(Values.JOB_ID, serviceComponent);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setRequiresDeviceIdle(false);
        builder.setRequiresCharging(false);

        if( ! runNow ) {
            Bundle settings = Values.getSettings(context);

            int delay = settings.getInt(Values.SETTING_BACKGROUND_QUERY_FREQUENCY);
            long minLatency = (delay * 60000);
            long maxLatency = ((delay+1) * 60000);

            builder.setMinimumLatency(minLatency);
            builder.setOverrideDeadline(maxLatency);

            Log.d(TAG, "scheduleJob(): Job will run in " + minLatency + "ms (max " + maxLatency + "ms).");
        }

        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

    public static void cancelJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancelAll();
        Log.i(TAG, "cancelJob(): Canceled all pending jobs.");
    }
}
