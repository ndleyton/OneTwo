package com.nicue.onetwo.fragments;


import android.content.Context;
import android.content.DialogInterface;
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

import com.nicue.onetwo.R;
import com.nicue.onetwo.Utils.TimerBackend;

import java.util.ArrayList;

public class TimerFragment extends Fragment implements View.OnClickListener {
    private LinearLayout mLayout;
    private LinearLayout exteriorLayout;
    private ArrayList<TimerBackend> mTimers = new ArrayList<>();
    private LayoutInflater mInflater;
    private int runningTimer = 0;   // Could change this to initiate when you press a play sign
    private boolean isPaused = true;
    private int setSeconds = 300;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.timer_layout, container, false);
        exteriorLayout = (LinearLayout) view.findViewById(R.id.timer_r_layout);
        Button playButton = (Button) view.findViewById(R.id.play_button);
        Button editButton = (Button) view.findViewById(R.id.edit_button);
        mLayout = (LinearLayout) exteriorLayout.findViewById(R.id.linear_timers);
        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = mInflater.inflate(R.layout.list_item_timer, null, false);
        Button button = (Button) v.findViewById(R.id.chrono);
        button.setOnClickListener(this);
        playButton.setOnClickListener(this);
        editButton.setOnClickListener(this);
        //v.setOnClickListener(this);

        TimerBackend timerBackend = new TimerBackend(v);
        //timerBackend.startTimer();

        mLayout.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mTimers.add(timerBackend);
        addChrono();
        return view;
    }

    public void addChrono() {
        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View va = mInflater.inflate(R.layout.list_item_timer, null, false);
        Button button = (Button) va.findViewById(R.id.chrono);
        button.setOnClickListener(this);
        //va.setOnClickListener(this);

        TimerBackend timerBackend = new TimerBackend(va);
        //timerBackend.startTimer();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLayout.addView(va, lp);
        mTimers.add(timerBackend);
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
        } else {
            //mTimers.get(runningTimer).pauseTimer();
            /*
            for (TimerBackend tb: mTimers
                 ) {
                if (!tb.getisPaused()){
                    tb.pauseTimer();
                    tb.setClickable();
                }
            }
            */
        }
        //isPaused = !isPaused;
    }

    public void clickedEdit() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(30);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View alertView = inflater.inflate(R.layout.minutes_alert_dialog, null);

        final NumberPicker npMinutes = (NumberPicker) alertView.findViewById(R.id.minute_picker);
        final NumberPicker npSeconds = (NumberPicker) alertView.findViewById(R.id.seconds_picker);
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(999);
        npMinutes.setValue(5);
        npSeconds.setValue(0);
        npSeconds.setMaxValue(60);
        npSeconds.setMinValue(0);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(alertView)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setSeconds = npMinutes.getValue() * 60 + npSeconds.getValue();
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
            TimerBackend new_tb = new TimerBackend(v,setSeconds);
            mTimers.set(i,new_tb);
        }
    }
}
