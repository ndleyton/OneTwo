package com.nicue.onetwo.Utils;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.os.Vibrator;

import com.nicue.onetwo.Utils.Pools.SimplePool;

import java.util.Random;

/**
 * View that shows touch events and their history. This view demonstrates the
 * use of {@link #onTouchEvent(android.view.MotionEvent)} and {@link android.view.MotionEvent}s to keep
 * track of touch pointers across events.
 */
public class TouchDisplayView extends View {

    //private long startTime = 0L;
    private boolean fingersDown = false;
    private boolean alreadyChosen = false;
    //private int fingers = 0;
    private Random random = new Random();
    private int chosenColor = 0;
    private int chosenId = -1;

    private final int[] COLORS = {
            0xFF03A9F4, 0xFF009688, 0xFF8BC34A, 0xFFF44336, 0xFFFF9800,
            0xFFFF5722, 0xFF795548, 0xFFAFB3A0, 0xFFE91E63, 0xFF9C27B0};


    //A Handler to check if all fingers have been pressed down for x time
    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        public void run() {
            checkGlobalVariable();
        }
    };

    public void checkGlobalVariable(){
        if (fingersDown){
            if (!alreadyChosen) {
                alreadyChosen = true;
                chosenId = random.nextInt(mTouches.size());
                chosenColor = COLORS[chosenId % COLORS.length];
                //Log.d("Checking fingers", "Done");
                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0,20,10,50};
                v.vibrate(pattern, -1);
                invalidate();
            }
        }
    }


    // Hold data for active touch pointer IDs
    private SparseArray<TouchHistory> mTouches;

    // Is there an active touch?
    private boolean mHasTouch = false;

    /**
     * Holds data related to a touch pointer, including its current position,
     * pressure and historical positions. Objects are allocated through an
     * object pool using {} and {@link #recycle()} to reuse
     * existing objects.
     */
    static final class TouchHistory {


        public float x;
        public float y;
        public float pressure = 0f;
        public String label = null;

        private static final int MAX_POOL_SIZE = 10;
        private static final SimplePool<TouchHistory> sPool =
                new SimplePool<TouchHistory>(MAX_POOL_SIZE);

        public static TouchHistory obtain(float x, float y, float pressure) {
            TouchHistory data = sPool.acquire();
            if (data == null) {
                data = new TouchHistory();
            }

            data.setTouch(x, y, pressure);

            return data;
        }

        public TouchHistory() {
            // initialise history array
            //for (int i = 0; i < HISTORY_COUNT; i++) {
            //   history[i] = new PointF();
            //}
        }

        public void setTouch(float x, float y, float pressure) {
            this.x = x;
            this.y = y;
            this.pressure = (this.pressure + pressure)/2f;
            this.pressure = Math.max(0.25f,this.pressure); // low limit
            this.pressure = Math.min(this.pressure, 0.5f);  //high limit
        }

        public void recycle() {
            sPool.release(this);
        }


    }

    public TouchDisplayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // SparseArray for touch events, indexed by touch id
        mTouches = new SparseArray<TouchHistory>(10);

        initialisePaint();
    }

    // BEGIN_INCLUDE(onTouchEvent)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getAction();

        /*
         * Switch on the action. The action is extracted from the event by
         * applying the MotionEvent.ACTION_MASK. Alternatively a call to
         * event.getActionMasked() would yield in the action as well.
         */
        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN: {
                //fingers = 1;
                // first pressed gesture has started

                /*
                 * Only one touch event is stored in the MotionEvent. Extract
                 * the pointer identifier of this touch from the first index
                 * within the MotionEvent object.
                 */
                int id = event.getPointerId(0);

                TouchHistory data = TouchHistory.obtain(event.getX(0), event.getY(0),
                        event.getPressure(0));
                //data.label = "id: " + 0;

                /*
                 * Store the data under its pointer identifier. The pointer
                 * number stays consistent for the duration of a gesture,
                 * accounting for other pointers going up or down.
                 */
                mTouches.put(id, data);

                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(20);

                mHasTouch = true;

                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {

                fingersDown = true;
                //fingers ++ ;

                /*
                 * A non-primary pointer has gone down, after an event for the
                 * primary pointer (ACTION_DOWN) has already been received.
                 */
                int index = event.getActionIndex();
                int id = event.getPointerId(index);

                TouchHistory data = TouchHistory.obtain(event.getX(index), event.getY(index),
                        event.getPressure(index));
                //data.label = "id: " + id;

                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(20);

                /*
                 * Store the data under its pointer identifier. The index of
                 * this pointer can change over multiple events, but this
                 * pointer is always identified by the same identifier for this
                 * active gesture.
                 */
                mTouches.put(id, data);
                handler.postDelayed(runnable, 1500);

                break;
            }

            case MotionEvent.ACTION_UP: {
                //fingers = 0;
                /*
                 * Final pointer has gone up and has ended the last pressed
                 * gesture.
                 */

                int id = event.getPointerId(0);
                TouchHistory data = mTouches.get(id);
                mTouches.remove(id);
                data.recycle();

                mHasTouch = false;

                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                fingersDown = false;
                alreadyChosen = false;
                //fingers --;
                handler.removeCallbacks(runnable);
                /*
                 * A non-primary pointer has gone up and other pointers are
                 * still active.
                 */

                int index = event.getActionIndex();
                int id = event.getPointerId(index);

                TouchHistory data = mTouches.get(id);
                mTouches.remove(id);
                data.recycle();

                break;
            }

            case MotionEvent.ACTION_MOVE: {

                for (int index = 0; index < event.getPointerCount(); index++) {
                    // get pointer id for data stored at this index
                    int id = event.getPointerId(index);

                    // get the data stored externally about this pointer.
                    TouchHistory data = mTouches.get(id);

                    // add previous position to history
                    //data.addHistory(data.x, data.y);

                    //add new values
                    data.setTouch(event.getX(index), event.getY(index),
                            event.getPressure(index));

                }

                break;
            }
        }

        // trigger redraw on UI thread
        this.postInvalidate();

        return true;
    }

    // END_INCLUDE(onTouchEvent)

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Canvas background color depends on whether there is an active touch
        if (mHasTouch) {
            canvas.drawColor(BACKGROUND_ACTIVE);
            if (alreadyChosen){
                canvas.drawColor(chosenColor);
            }
        }else{
            alreadyChosen = false;
        }

        // loop through all active touches and draw them
        for (int i = 0; i < mTouches.size(); i++) {

            // get the pointer id and associated data for this index
            int id = mTouches.keyAt(i);
            TouchHistory data = mTouches.valueAt(i);

            // draw the data and its history to the canvas
            drawCircle(canvas, id, data);
        }
    }

    /*
     * Below are only helper methods and variables required for drawing.
     */

    // radius of active touch circle in dp
    private static final float CIRCLE_RADIUS_DP = 75f;
    // radius of historical circle in dp
    //private static final float CIRCLE_HISTORICAL_RADIUS_DP = 7f;

    // calculated radiuses in px
    private float mCircleRadius;
    //private float mCircleHistoricalRadius;

    private Paint mCirclePaint = new Paint();
    private Paint mTextPaint = new Paint();

    private static final int BACKGROUND_ACTIVE = Color.WHITE;

    // inactive border
    private static final float INACTIVE_BORDER_DP = 15f;
    private static final int INACTIVE_BORDER_COLOR = 0xFFffd060;
    private Paint mBorderPaint = new Paint();
    private float mBorderWidth;

    ;

    private void initialisePaint() {

        // Calculate radiuses i n px from dp based on screen density
        float density = getResources().getDisplayMetrics().density;
        mCircleRadius = CIRCLE_RADIUS_DP * density;
        //mCircleHistoricalRadius = CIRCLE_HISTORICAL_RADIUS_DP * density;

        // Setup text paint for circle label
        mTextPaint.setTextSize(30f);
        mTextPaint.setColor(Color.BLACK);

        // Setup paint for inactive border
        mBorderWidth = INACTIVE_BORDER_DP * density;
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setColor(INACTIVE_BORDER_COLOR);
        mBorderPaint.setStyle(Paint.Style.STROKE);

    }

    protected void drawCircle(Canvas canvas, int id, TouchHistory data) {
        // select the color based on the id
        int color = COLORS[id % COLORS.length];
        mCirclePaint.setColor(color);


        float radius = data.pressure * mCircleRadius;
        float half_r = radius / 2f;


        if(alreadyChosen){
            if (chosenId == id){
                canvas.drawText("Chosen", data.x + radius, data.y
                        - radius, mTextPaint);
                canvas.drawCircle(data.x, (data.y) - half_r, radius + 5,
                        mTextPaint);
            }
        }


        canvas.drawCircle(data.x, (data.y) - half_r, radius,
                mCirclePaint);


        mCirclePaint.setAlpha(125);
        canvas.drawCircle(data.x, (data.y) - half_r, radius*2f,
                mCirclePaint);




    }

}
