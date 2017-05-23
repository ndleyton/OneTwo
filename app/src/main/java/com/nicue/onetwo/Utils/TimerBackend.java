package com.nicue.onetwo.Utils;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

import com.nicue.onetwo.R;

public class TimerBackend {
    private View mView;
    private CardView mCardView;
    private CountDownTimer timer;
    private Button mButton;
    private long pausedTime;
    private boolean isPaused = true;
    private VibratorInterface vibratorInterface;
    private long panicMiliSec = 10000;

    public interface VibratorInterface{
        void finishedTimer();
    }

    public TimerBackend(View v, VibratorInterface vInterface){
        vibratorInterface = vInterface;
        long defaulTime = 300000;
        mView = v;
        mCardView = (CardView) v.findViewById(R.id.cv_timer);
        mButton = (Button) mView.findViewById(R.id.chrono);
        timer = new CountDownTimer(defaulTime,10) {
            @Override
            public void onTick(long millisUntilFinished) {
                pausedTime = millisUntilFinished;
                long currentSec = pausedTime/1000;
                long min = currentSec/60;
                long dim_secs = currentSec%60;
                String min_sec;
                if (millisUntilFinished >= panicMiliSec ) {
                    min_sec = String.format("%d:%02d", min, dim_secs);
                }else{
                    long deci_sec = (millisUntilFinished %1000)/10;
                    min_sec = String.format("%d:%02d:%03d", min, dim_secs,deci_sec);
                }

                mButton.setText(min_sec);
            }

            @Override
            public void onFinish() {
                mButton.setText("0:00");
                mButton.setBackgroundColor(0xff424242);
                vibratorInterface.finishedTimer();
            }
        };
        pausedTime = defaulTime;
        long currentSec = defaulTime/1000;
        long min = currentSec/60;
        long dim_secs = currentSec%60;
        String min_sec = String.format("%d:%02d",min,dim_secs);

        mButton.setText(min_sec);
        setNonClickable();
    }

    public TimerBackend(View v, long miliseconds, VibratorInterface vInterface){
        vibratorInterface = vInterface;
        mView = v;
        mCardView = (CardView) v.findViewById(R.id.cv_timer);
        mButton = (Button) mView.findViewById(R.id.chrono);
        timer = new CountDownTimer(miliseconds,10) {
            @Override
            public void onTick(long millisUntilFinished) {
                pausedTime = millisUntilFinished;
                long currentSecs = millisUntilFinished/1000;
                long min = currentSecs/60;
                long dim_secs = currentSecs%60;
                String min_sec;
                if (millisUntilFinished >= panicMiliSec ) {
                    min_sec = String.format("%d:%02d", min, dim_secs);
                }else{
                    long deci_sec = (millisUntilFinished %1000)/10;
                    min_sec = String.format("%d:%02d:%03d", min, dim_secs,deci_sec);
                }

                mButton.setText(min_sec);
            }

            @Override
            public void onFinish() {
                mButton.setText("0:00");
                mButton.setBackgroundColor(0xff424242);
                vibratorInterface.finishedTimer();
            }

        };
        setNonClickable();
        pausedTime = miliseconds;
        long currentSecs = pausedTime /1000;
        long min = currentSecs/60;
        long dim_secs = currentSecs%60;
        String min_sec = String.format("%d:%02d",min,dim_secs);
        mButton.setText(min_sec);
    }
    public void startTimer(){
        if (isPaused) {
            timer.start();
            setClickable();
            isPaused = false;
        }
    }

    public void pauseTimer() {
        if (!isPaused) {
            setNonClickable();
            timer.cancel();
            timer = new CountDownTimer(pausedTime, 10) {
                @Override
                public void onTick(long millisUntilFinished) {
                    pausedTime = millisUntilFinished;
                    long currentSec = pausedTime /1000;
                    long min = currentSec / 60;
                    long dim_secs = currentSec % 60;
                    String min_sec;
                    if (millisUntilFinished >= panicMiliSec ) {
                        min_sec = String.format("%d:%02d", min, dim_secs);
                    }else{
                        long deci_sec = (millisUntilFinished %1000)/10;
                        min_sec = String.format("%d:%02d:%03d", min, dim_secs,deci_sec);
                    }

                    mButton.setText(min_sec);
                }
                @Override
                public void onFinish() {
                    mButton.setText("0:00");
                    mButton.setBackgroundColor(0xff424242);
                    vibratorInterface.finishedTimer();

                }
            };
            isPaused = true;
            Log.d("PausingTimer", "pauseTimer");
        }
    }


    public void stopTimer(){
        timer.cancel();
    }
    public View getmView(){
        return mView;
    }
    public void setClickable(){
        mView.setClickable(true);
        mButton.setClickable(true);
        mButton.setEnabled(true);
        mButton.setBackgroundColor(0xffbd2430);
        //Animation
        ObjectAnimator animator = ObjectAnimator.ofFloat(mCardView, "cardElevation", 2, 30);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(200);
        animator.start();
    }
    public void setNonClickable(){
        Log.d("Clickable: ", "NO");
        mView.setClickable(false);
        mButton.setClickable(false);
        mButton.setEnabled(false);
        mButton.setBackgroundColor(0xff850009);
        //Animation
        ObjectAnimator animator = ObjectAnimator.ofFloat(mCardView, "cardElevation", 30, 2);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(200);
        animator.start();
    }
    public boolean getisPaused(){
        return isPaused;
    }

    public long getPausedTime(){return pausedTime;}

    public void deleteTimer(){
        timer.cancel();
        mView.setVisibility(View.GONE);
    }

}
