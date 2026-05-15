package com.nicue.onetwo.core;

public interface TimerScheduler {
    interface TickListener {
        void onTick(long nowMs);
    }

    void start(TickListener listener);

    void stop();

    long now();
}
