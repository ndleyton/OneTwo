package com.nicue.onetwo.fragments;


import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.nicue.onetwo.R;
import com.nicue.onetwo.Utils.TimerBackend;

import java.util.ArrayList;

public class TimerFragment extends Fragment implements View.OnClickListener {
    private LinearLayout mLayout;
    private ArrayList<TimerBackend> mTimers = new ArrayList<>();
    private LayoutInflater mInflater;
    private int runningTimer = 0;   // Could change this to initiate when you press a play sign
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.timer_layout, container, false);
        mLayout = (LinearLayout) view.findViewById(R.id.timer_r_layout);
        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = mInflater.inflate(R.layout.list_item_timer, null, false);
        Button button = (Button) v.findViewById(R.id.chrono);
        button.setOnClickListener(this);
        //v.setOnClickListener(this);

        TimerBackend timerBackend = new TimerBackend(v);
        timerBackend.startTimer();


        mLayout.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mTimers.add(timerBackend);
        addChrono();
        return view;
    }
    public void addChrono(){
        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View va = mInflater.inflate(R.layout.list_item_timer, null, false);
        Button button = (Button) va.findViewById(R.id.chrono);
        button.setOnClickListener(this);
        //va.setOnClickListener(this);

        TimerBackend timerBackend = new TimerBackend(va);
        //timerBackend.startTimer();


        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLayout.addView(va,lp);
        mTimers.add(timerBackend);
    }

    @Override
    public void onClick(View v) {
        int _id =v.getId();
        switch (_id){
            case R.id.chrono:
                clickedTimer(v);
        }
    }


    public void clickedTimer(View v){
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(30);
        mTimers.get(runningTimer).pauseTimer();
        runningTimer = (runningTimer +1)%mTimers.size();
        mTimers.get(runningTimer).startTimer();
    }
}
