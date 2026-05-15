package com.nicue.onetwo.core;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

public class HandlerTimerScheduler implements TimerScheduler {
    private static final long TICK_INTERVAL_MS = 50L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TickListener tickListener;
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (tickListener == null) {
                return;
            }
            tickListener.onTick(now());
            handler.postDelayed(this, TICK_INTERVAL_MS);
        }
    };

    @Override
    public void start(TickListener listener) {
        stop();
        tickListener = listener;
        handler.post(tickRunnable);
    }

    @Override
    public void stop() {
        handler.removeCallbacks(tickRunnable);
        tickListener = null;
    }

    @Override
    public long now() {
        return SystemClock.elapsedRealtime();
    }
}
