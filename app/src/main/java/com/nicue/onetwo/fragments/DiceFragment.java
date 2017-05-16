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
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import com.nicue.onetwo.R;
import com.nicue.onetwo.adapters.DiceListAdapter;

import org.json.JSONArray;

import java.util.ArrayList;

public class DiceFragment extends android.support.v4.app.Fragment implements View.OnClickListener,
        DiceListAdapter.DiceAdapterOnClickHandler,DiceListAdapter.ItemClickListener {
    private ArrayList<String> mItems = new ArrayList<>();
    private ArrayList<String> mFaces = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private DiceListAdapter mListAdapter;
    private boolean started = false;
    private Handler handler = new Handler();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View diceView = inflater.inflate(R.layout.dice_layout, container, false);

        mRecyclerView = (RecyclerView) diceView.findViewById(R.id.recyclerview_dice);

        RecyclerView.LayoutManager layoutManager
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
        final EditText et_dice = new EditText(getActivity());
        et_dice.setRawInputType(InputType.TYPE_CLASS_NUMBER |InputType.TYPE_NUMBER_FLAG_DECIMAL);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(et_dice)
                .setTitle("Dice\'s Faces:")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String faces = et_dice.getText().toString();
                        if(faces.equals("")){
                            faces = "6";
                        }
                        mFaces.add(faces);
                        mItems.add(faces);
                        Log.d("Items", String.valueOf(mItems));
                        mListAdapter.setmData(mItems,mFaces);
                        writeItems();

                        dialog.dismiss();

                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        //dialog.getWindow().setSoftInputMode(
        //        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
}

