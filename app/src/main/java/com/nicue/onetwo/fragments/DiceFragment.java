package com.nicue.onetwo.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.nicue.onetwo.R;
import com.nicue.onetwo.adapters.DiceListAdapter;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Random;

public class DiceFragment extends android.support.v4.app.Fragment implements View.OnClickListener,
        DiceListAdapter.DiceAdapterOnClickHandler,DiceListAdapter.ItemClickListener {
    private ArrayList<String> mItems = new ArrayList<>();
    private ArrayList<String> mFaces = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private DiceListAdapter mListAdapter;
    private GridLayoutManager layoutManager;
    private boolean started = false;
    private Handler handler = new Handler();
    private Random rand = new Random();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View diceView = inflater.inflate(R.layout.dice_layout, container, false);

        mRecyclerView = (RecyclerView) diceView.findViewById(R.id.recyclerview_dice);

        layoutManager
                = new GridLayoutManager(getActivity(), 2);

        mRecyclerView.setLayoutManager(layoutManager);

        mListAdapter = new DiceListAdapter(this,this);

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
        mRecyclerView.setAdapter(mListAdapter);

        FloatingActionButton fab_dice = (FloatingActionButton) diceView.findViewById(R.id.fab_dice);
        fab_dice.setOnClickListener(this);
        mRecyclerView = (RecyclerView) diceView.findViewById(R.id.recyclerview_dice);
        readItems();
        mListAdapter.setmData(mItems, mFaces);
        mListAdapter.notifyDataSetChanged();
        return diceView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.fab_dice:
                fabDiceClick(v);
        }
    }

    @Override
    public void onClick(View v, int pos) {
        int id = v.getId();
        Log.d("Clicked", "onFragment");
        switch (id) {
            case R.id.throw_button:
                //
        }
    }

    @Override
    public void onItemLongClick(View view, int position) {
        mFaces.remove(position);
        mItems.remove(position);
        writeItems();
        mListAdapter.setmData(mItems, mFaces);
        mListAdapter.notifyDataSetChanged();
    }

    /*
    If there is  need to roll the dice from the Fragment
    public void rollDice(View v) {
        int id = v.getId();
        Log.d("Clicked", "onFragment");
        switch (id) {
            case R.id.throw_button:
                //int max_dice = Integer.parseInt(items.get(pos));
                int max_dice = 6;
                Random r = new Random();
                int new_num = r.nextInt(max_dice);
                TextView rolledNumber = (TextView) v.findViewById(R.id.tv_dice);
                rolledNumber.setText(new_num);
        }
    }
    */

    public void fabDiceClick(View view) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View alertView = inflater.inflate(R.layout.dice_alert_dialog, null);

        final EditText et_dice = (EditText) alertView.findViewById(R.id.et_dice);
        //et_dice.setRawInputType(InputType.TYPE_CLASS_NUMBER |InputType.TYPE_NUMBER_FLAG_DECIMAL);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(alertView)
                .setTitle("Dice\'s Faces:")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String faces = et_dice.getText().toString();
                        if(faces.equals("")){
                            faces = "6";
                        }
                        int int_faces = Integer.parseInt(faces);
                        if (int_faces<2){
                            faces ="2";
                        }
                        mFaces.add(faces);
                        mItems.add(faces);
                        mListAdapter.setmData(mItems,mFaces);
                        writeItems();

                        dialog.dismiss();

                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }

    private void readItems() {
        SharedPreferences prefs = getActivity().getSharedPreferences("SHARED_PREFS_FILE",   Context.MODE_PRIVATE);
        String myJSONArrayString = prefs.getString("DICES", "");
        try {
            JSONArray jsonArray = new JSONArray(myJSONArrayString);
            mFaces = new ArrayList<String>();
            for (int i=0;i<jsonArray.length();i++){
                mFaces.add(jsonArray.get(i).toString());
            }
        }catch (org.json.JSONException e){
            mFaces = new ArrayList<String>();
        }
        mItems.clear();
        for (int i = 0 ; i<mFaces.size(); i++)
        {mItems.add(mFaces.get(i));
        }
    }

    private void writeItems() {
        JSONArray mJSONArray = new JSONArray(mFaces);
        SharedPreferences prefs = getActivity().getSharedPreferences("SHARED_PREFS_FILE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("DICES", mJSONArray.toString());
        editor.apply();
    }

    public void rollAllDices(){
        try {

            final int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
            final int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0,15,10,15,10,15,10,15,10,15};
            vibrator.vibrate(pattern,-1);
            for (int i = firstVisibleItemPosition; i <= lastVisibleItemPosition; ++i) {
                DiceListAdapter.ViewHolder holder = (DiceListAdapter.ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
                int max_dice = Integer.valueOf(holder.facesTextView.getText().toString());
                ExteriorRollingRunnable rollingRunnable = new ExteriorRollingRunnable(holder.mTextView, max_dice);
                handler.post(rollingRunnable);
                handler.postDelayed(rollingRunnable, 50);
                handler.postDelayed(rollingRunnable, 110);
                handler.postDelayed(rollingRunnable, 190);
                handler.postDelayed(rollingRunnable, 280);
                if (rand.nextBoolean()){
                    handler.postDelayed(rollingRunnable, 500);
                }
                //RecyclerView.ViewHolder vh = (RecyclerView.ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public class ExteriorRollingRunnable implements Runnable{
        private TextView mView;
        private int mMaxDice;

        public ExteriorRollingRunnable (TextView v, int maxDice){
            mView = v;
            mMaxDice = maxDice;
        }
        @Override
        public void run() {
            realRun(mView, mMaxDice);
        }

        public void realRun(TextView v, int maxDice){
            Random random = new Random();
            int new_num = random.nextInt(maxDice) + 1;
            v.setText(String.valueOf(new_num));

        }
    }
}

