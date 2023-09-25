package com.nicue.onetwo.fragments;

// Counter Fragment, set by default

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;

import com.nicue.onetwo.R;
import com.nicue.onetwo.adapters.ListAdapter;
import com.nicue.onetwo.db.TaskContract;
import com.nicue.onetwo.db.TaskDbHelper;

import java.util.ArrayList;

public class CounterFragment extends Fragment implements ListAdapter.ListAdapterOnClickHandler, View.OnClickListener {
    private RecyclerView mRecyclerView;
    private TextView instructionTextView;
    private TaskDbHelper mHelper;
    private ListAdapter mListAdapter;

    private ArrayList<String> mObjects = new ArrayList<String>();
    private ArrayList<Integer> mObjectsNumbers = new ArrayList<Integer>();



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.counter_layout, container, false);
        mHelper = new TaskDbHelper(this.getActivity());

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_counters);
        instructionTextView = (TextView) view.findViewById(R.id.tv_instruction_counter);

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);

        mRecyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration mDivider = new DividerItemDecoration(mRecyclerView.getContext(),
                layoutManager.getOrientation());
        mRecyclerView.addItemDecoration(mDivider);

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
        mRecyclerView.setHasFixedSize(true);

        mListAdapter = new ListAdapter(this);

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
        mRecyclerView.setAdapter(mListAdapter);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);


        //fab.setTranslationY(fab.getHeight() + 512);
        fab.setScaleX(0);
        fab.setScaleY(0);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
                fab.animate().scaleX(1).setInterpolator(new DecelerateInterpolator(2)).start();
                fab.animate().scaleY(1).setInterpolator(new DecelerateInterpolator(2)).start();
                //fab.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();

            }}, 300);
        fab.setOnClickListener(this);
        //getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        updateUI();

        return view;
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.fab:
                fabClick(v);
        }
    }

    @Override
    public void onClick(String obj) {
        return;
    }

    @Override
    public void onValueChanged(String obj, int num) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.execSQL("UPDATE " + TaskContract.TaskEntry.TABLE
                + " SET " +TaskContract.TaskEntry.COL_NUM_TITLE +" = "+ String.valueOf(num) +
                " WHERE "+ TaskContract.TaskEntry.COL_TASK_TITLE + " = '" + obj + "';" );
        db.close();

        updateUI();
    }
    public void fabClick(View view) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View alertView = inflater.inflate(R.layout.activity_alert_dialog, null);

        final EditText etToCount = (EditText) alertView.findViewById(R.id.et_to_count);
        final EditText etNumber = (EditText) alertView.findViewById(R.id.et_number);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(alertView)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String object_dirty = etToCount.getText().toString();
                        String object = object_dirty.replaceAll("'","\"");

                        int number;
                        try {
                            number = Integer.parseInt(etNumber.getText().toString());
                        }catch (Exception e){
                            number = 0;
                        }


                        SQLiteDatabase db = mHelper.getWritableDatabase();
                        //Cursor temp_cursor = db.rawQuery("SELECT MAX("+ TaskContract.TaskEntry._ID+ ") FROM "
                        //        + TaskContract.TaskEntry.TABLE, null);


                        ContentValues values = new ContentValues();
                        values.put(TaskContract.TaskEntry.COL_TASK_TITLE, object);
                        values.put(TaskContract.TaskEntry.COL_NUM_TITLE, number);
                        db.insertWithOnConflict(TaskContract.TaskEntry.TABLE,
                                null,
                                values,
                                SQLiteDatabase.CONFLICT_REPLACE);
                        db.close();
                        updateUI();
                        mRecyclerView.smoothScrollToPosition(mListAdapter.getItemCount()-1);

                        dialog.dismiss();

                        //Intent intent = new Intent(getBaseContext() , MainActivity.class);
                        //startActivity(intent);


                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }

    public void deleteObj(View view) {
        View parent = (View) view.getParent();
        TextView taskTextView = (TextView) parent.findViewById(R.id.tv_object_data);
        String task = String.valueOf(taskTextView.getText());
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.delete(TaskContract.TaskEntry.TABLE,
                TaskContract.TaskEntry.COL_TASK_TITLE + " = ?",
                new String[]{task});
        db.close();
        updateUI();
    }

    private void updateUI() {
        ArrayList<String> objList = new ArrayList<>();
        ArrayList<Integer> numList = new ArrayList<>();
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(TaskContract.TaskEntry.TABLE,
                new String[]{TaskContract.TaskEntry._ID, TaskContract.TaskEntry.COL_TASK_TITLE, TaskContract.TaskEntry.COL_NUM_TITLE},
                null, null, null, null, null);
        while (cursor.moveToNext()) {
            int idx = cursor.getColumnIndex(TaskContract.TaskEntry.COL_TASK_TITLE);
            int idx_num = cursor.getColumnIndex(TaskContract.TaskEntry.COL_NUM_TITLE);
            objList.add(cursor.getString(idx));
            numList.add(cursor.getInt(idx_num));
        }

        mListAdapter.setData(objList, numList);
        cursor.close();
        db.close();

        if (objList.size() > 0) {
            instructionTextView.setVisibility(View.INVISIBLE);
        }else {
            instructionTextView.setVisibility(View.VISIBLE);
        }
    }

    public String getTitle(){
        String title = "Counter";
        return title;
    }
}
