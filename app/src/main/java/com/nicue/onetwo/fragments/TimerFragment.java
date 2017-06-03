package com.nicue.onetwo.fragments;


import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import com.nicue.onetwo.MainActivity;
import com.nicue.onetwo.R;
import com.nicue.onetwo.Utils.TimerBackend;

import java.util.ArrayList;

public class TimerFragment extends Fragment implements View.OnClickListener, TimerBackend.VibratorInterface {
    private LinearLayout mLayout;
    private LinearLayout exteriorLayout;
    private Button playButton;
    private Button editButton;
    private ArrayList<TimerBackend> mTimers = new ArrayList<>();
    private LayoutInflater mInflater;
    private int runningTimer = 0;   // Could change this to initiate when you press a timer
    private boolean isPaused = true;
    private long setMiliSeconds = 300000;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true); // to persist data on rotation
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isPaused = true;
        View view = inflater.inflate(R.layout.timer_layout, container, false);
        exteriorLayout = (LinearLayout) view.findViewById(R.id.timer_r_layout);
        playButton = (Button) view.findViewById(R.id.play_button);
        editButton = (Button) view.findViewById(R.id.edit_button);
        mLayout = (LinearLayout) exteriorLayout.findViewById(R.id.linear_timers);
        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ((MainActivity)getActivity()).loadTimersData();
        Log.d("mTimers0",String.valueOf(mTimers));

        playButton.setOnClickListener(this);
        editButton.setOnClickListener(this);

        /*
        View v = mInflater.inflate(R.layout.list_item_timer, null, false);
        Button button = (Button) v.findViewById(R.id.chrono);
        button.setOnClickListener(this);

        TimerBackend timerBackend = new TimerBackend(v,this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 50,1);
        mLayout.addView(v, lp);

        v.setFitsSystemWindows(true);
        mTimers.add(timerBackend);
        */
        int timersCount = mTimers.size();
        Log.d("mTimers1",String.valueOf(mTimers));
        if (timersCount == 0) {
            addChrono();
            addChrono();
        }else{
            if (runningTimer >= maxTimers()){
                runningTimer=0;
            }

            long[] times = new long[mTimers.size()];
            for (int j=0;j<times.length;j++){
                times[j]=mTimers.get(j).getPausedTime();
            }
            for (TimerBackend tb: mTimers){
                tb.deleteTimer();
            }
            mTimers.clear();
            for (int i=0;i<times.length;i++){
                if (i<maxTimers()) {
                    long time = times[i];
                    addChronoTimed(time);
                }
            }
        }
        Log.d("mTimers2",String.valueOf(mTimers));
        return view;
    }

    @Override
    public void onDestroyView() {
        stopAllTimers();
        super.onDestroyView();
    }

    public void addChrono() {
        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View va = mInflater.inflate(R.layout.list_item_timer, null, false);
        va.setFitsSystemWindows(true);
        Button button = (Button) va.findViewById(R.id.chrono);
        button.setOnClickListener(this);

        TimerBackend timerBackend = new TimerBackend(va, setMiliSeconds,this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,50,1);
        mLayout.addView(va, lp);
        mTimers.add(timerBackend);

        //turn the first timer if there is only 2 of them
        if (mTimers.size() == 2){
            mTimers.get(0).getmView().setRotation(180);
        }else{
            mTimers.get(0).getmView().setRotation(0);
        }
    }

    public void addChronoTimed(long miliseconds) {
        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View va = mInflater.inflate(R.layout.list_item_timer, null, false);
        va.setFitsSystemWindows(true);
        Button button = (Button) va.findViewById(R.id.chrono);
        button.setOnClickListener(this);

        TimerBackend timerBackend = new TimerBackend(va, miliseconds,this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,50,1);
        mLayout.addView(va, lp);
        mTimers.add(timerBackend);

        //turn the first timer if there is only 2 of them
        if (mTimers.size() == 2){
            mTimers.get(0).getmView().setRotation(180);
        }else{
            mTimers.get(0).getmView().setRotation(0);
        }
    }

    @Override
    public void onClick(View v) {
        int _id = v.getId();
        Log.d("Clicked_id", String.valueOf(_id));
        switch (_id) {
            case R.id.chrono:
                clickedTimer(v);
                break;
            case R.id.play_button:
                clickedPlayPause();
                break;
            case R.id.edit_button:
                clickedEdit();
                break;
        }
    }

    @Override
    public void finishedTimer() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(300);
    }

    public void clickedTimer(View v) {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(30);
        mTimers.get(runningTimer).pauseTimer();
        runningTimer = (runningTimer + 1) % mTimers.size();
        mTimers.get(runningTimer).startTimer();
    }

    public void clickedPlayPause() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(30);
        if (isPaused) {
            mTimers.get(runningTimer).startTimer();
            playButton.setText(getString(R.string.pause));
        } else {
            mTimers.get(runningTimer).pauseTimer();
            playButton.setText(getString(R.string.play));
        }
        isPaused = !isPaused;
    }

    public void clickedEdit() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(30);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View alertView = inflater.inflate(R.layout.minutes_alert_dialog, null);

        final NumberPicker npMinutes = (NumberPicker) alertView.findViewById(R.id.minute_picker);
        final NumberPicker npSeconds = (NumberPicker) alertView.findViewById(R.id.seconds_picker);
        long showSecs = (setMiliSeconds/1000)%60;
        long showMins = (setMiliSeconds/1000)/60;
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(999);
        npMinutes.setValue((int) showMins);
        npSeconds.setMaxValue(60);
        npSeconds.setMinValue(0);
        npSeconds.setValue((int) showSecs);
        npSeconds.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int index) {
                return String.format("%02d",index);
            }
        });
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(alertView)
                .setTitle("Set Time:")
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setMiliSeconds = npMinutes.getValue() * 60 + npSeconds.getValue();
                        setMiliSeconds = setMiliSeconds *1000;
                        editTimers();
                        dialog.dismiss();


                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    public void editTimers(){
        for (int i=0; i<mTimers.size();i++){
            TimerBackend tb = mTimers.get(i);
            View v = tb.getmView();
            tb.stopTimer();
            TimerBackend new_tb = new TimerBackend(v,setMiliSeconds, this);
            mTimers.set(i,new_tb);
        }
        isPaused = true;
        runningTimer = 0;
        playButton.setText(getString(R.string.play));
    }

    public void addTimer(){
        getScreenHeight();
        if (mTimers.size() < maxTimers()){
            addChrono();
            mLayout.invalidate();
        }
    }

    public void delTimer(){
        if (mTimers.size() > 1){
            if (runningTimer == mTimers.size()-1){
                runningTimer = 0;
                mTimers.get(0).startTimer();
            }
            TimerBackend tb = mTimers.get(mTimers.size()-1);
            tb.deleteTimer();
            mTimers.remove(mTimers.size()-1);
            mLayout.invalidate();

            if (mTimers.size() == 2){
                mTimers.get(0).getmView().setRotation(180);
            }else{
                mTimers.get(0).getmView().setRotation(0);
            }
        }
    }

    public void stopAllTimers(){
        for (TimerBackend tb:mTimers
                ) {
            tb.deleteTimer();
        }
    }

    public ArrayList<TimerBackend> getData(){
        return mTimers;
    }

    public void setData(ArrayList<TimerBackend> timers){
        mTimers.clear();

        for (int i=0;i<timers.size();i++){
            long time = timers.get(i).getPausedTime();
            addChronoTimed(time);
         }
    }


    //The current height of the available screen space, in dp units
    // corresponding to screen height resource qualifier.
    public int getScreenHeight(){
        Configuration configuration = getActivity().getResources().getConfiguration();
        return configuration.screenHeightDp;
    }

    // returns max timers withouth them getting weird
    public int maxTimers(){
        return (getScreenHeight()-22)/78;
    }
}
