package com.nicue.onetwo;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import com.nicue.onetwo.db.TaskContract;
import com.nicue.onetwo.db.TaskDbHelper;

public class MainActivity extends AppCompatActivity implements ListAdapter.ListAdapterOnClickHandler {

    private RecyclerView mRecyclerView;
    private TaskDbHelper mHelper;
    private ListAdapter mListAdapter;

    private ArrayList<String> mObjects = new ArrayList<String>();
    private ArrayList<Integer> mObjectsNumbers = new ArrayList<Integer>();
    private int cuenta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHelper = new TaskDbHelper(this);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_counters);

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

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

        updateUI();


    }

    @Override
    public void onClick(String object) {
        return;
    }

    @Override
    public void onValueChanged(String obj, int num) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.execSQL("UPDATE " + TaskContract.TaskEntry.TABLE
                + " SET " +TaskContract.TaskEntry.COL_NUM_TITLE +" = "+ String.valueOf(num) +
                " WHERE "+ TaskContract.TaskEntry.COL_TASK_TITLE + " = '" + obj + "';" );
        db.close();
        SQLiteDatabase db_2 = mHelper.getWritableDatabase();
        Cursor reading = db_2.rawQuery("SELECT * FROM Objects;", null);

        updateUI();
    }

    public void fabClick(View view) {

        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.activity_alert_dialog, null);

        final EditText etToCount = (EditText) alertView.findViewById(R.id.et_to_count);
        final EditText etNumber = (EditText) alertView.findViewById(R.id.et_number);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(alertView)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String object_dirty = etToCount.getText().toString();
                        String object = object_dirty.replaceAll("'","\"");
                        Log.d("Se ingresa", object);

                        int number;
                        try {
                            number = Integer.parseInt(etNumber.getText().toString());
                        }catch (Exception e){
                            number = 0;
                        }

                        //mObjects.add(object);
                        //mObjectsNumbers.add(number);

                        //mListAdapter.setData(mObjects, mObjectsNumbers);

                        Log.d("Tag Lista",mObjects.toString());


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

                        dialog.dismiss();

                        //Intent intent = new Intent(getBaseContext() , MainActivity.class);
                        //startActivity(intent);


                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

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
            Log.d("Lista: ", String.valueOf(objList));
        }

        mListAdapter.setData(objList, numList);
        cursor.close();
        db.close();
    }
}


