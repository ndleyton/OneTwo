package com.nicue.onetwo.Utils;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.os.Vibrator;

import com.nicue.onetwo.Utils.Pools.SimplePool;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;


public class TouchDisplayView extends View {

    //private long startTime = 0L;
    private boolean fingersDown = false;
    private boolean alreadyChosen = false;
    private boolean choosingOrder = false;
    //private int fingers = 0;
    private Random random = new Random();
    private int chosenColor = 0;
    private int chosenId = -1;
    private int[] randomArray = {};

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
                randomArray = intArrayToN(mTouches.size());
                shuffleArray(randomArray);
                chosenId = randomArray[0];
                chosenColor = COLORS[chosenId % COLORS.length];
                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0,20,10,50};
                v.vibrate(pattern, -1);
                invalidate();
            }
        }
    }

    public int[] intArrayToN(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; ++i) {
            a[i] = i;
        }
        return a;
    }
    private static void shuffleArray(int[] array)
    {
        int index;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            if (index != i)
            {
                array[index] ^= array[i];
                array[i] ^= array[index];
                array[index] ^= array[i];
            }
        }
    }


    // Hold data for active touch pointer IDs
    private SparseArray<TouchHistory> mTouches;

    // Is there an active touch?
    private boolean mHasTouch = false;

    /**
     * Holds data related to a touch pointer,
     * object pool using {} and recycle to reuse
     * existing objects.
     */
    static final class TouchHistory {


        public float x;
        public float y;
        public float pressure = 0f;

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


        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN: {
                //fingers = 1;
                // first pressed gesture has started

                int id = event.getPointerId(0);

                TouchHistory data = TouchHistory.obtain(event.getX(0), event.getY(0),
                        event.getPressure(0));

                //Store the data under its pointer identifier. The pointer number stays consistent
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
                randomArray = new int[0];
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
            randomArray = new int[0];
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
     * helper methods and variables required for drawing.
     */

    // radius of active touch circle in dp
    private static final float CIRCLE_RADIUS_DP = 75f;

    // calculated radiuses in px
    private float mCircleRadius;
    private int mScreenWidth;

    private Paint mCirclePaint = new Paint();
    private Paint mTextPaint = new Paint();
    private Paint mStrokePaint = new Paint();
    private Paint mTransStrokePaint = new Paint();

    private static final int BACKGROUND_ACTIVE = Color.WHITE;


    private void initialisePaint() {

        // Calculate radiuses i n px from dp based on screen density
        float density = getResources().getDisplayMetrics().density;
        mCircleRadius = CIRCLE_RADIUS_DP * density;
        //we want to know the screen width to make the white text remain in the screen
        Configuration configuration = getContext().getResources().getConfiguration();
        mScreenWidth = configuration.smallestScreenWidthDp * (int) density ;
        //mCircleHistoricalRadius = CIRCLE_HISTORICAL_RADIUS_DP * density;

        // Setup text paint for circle label
        mTextPaint.setTextSize(30f*density);
        mStrokePaint.setTextSize(30f*density);
        mTextPaint.setColor(Color.WHITE);
        mStrokePaint.setColor(Color.WHITE);
        mTransStrokePaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(6f* density);
        mTransStrokePaint.setStyle(Paint.Style.STROKE);
        mTransStrokePaint.setStrokeWidth(6f* density);

    }

    protected void drawCircle(Canvas canvas, int id, TouchHistory data) {
        // select the color based on the id
        int color = COLORS[id % COLORS.length];
        mCirclePaint.setColor(color);
        boolean drawBig = true;


        float radius = data.pressure * mCircleRadius;
        float half_r = radius / 2f;


        if(alreadyChosen){
            int place = indexInArray(randomArray, id) +1;
            if (chosenId == id){
                //We draw he circle first to be under the text
                canvas.drawCircle(data.x, (data.y) - half_r, radius + 10,
                        mStrokePaint);
                //Here we can add more circles for more visibility of chosen one;
                mTransStrokePaint.setAlpha(125);
                canvas.drawCircle(data.x, (data.y) - half_r, radius + 50,
                        mTransStrokePaint);
                mTransStrokePaint.setAlpha(50);
                canvas.drawCircle(data.x, (data.y) - half_r, radius + 90,
                        mTransStrokePaint);
                if ((data.x + radius + 20)<mScreenWidth ) {
                    canvas.drawText("Chosen", data.x + radius, data.y
                            - radius, mTextPaint);
                }else{
                    canvas.drawText("Chosen", data.x - radius -100, data.y
                            - radius - 90, mTextPaint);
                }

                drawBig = false;
            }else{ // With this line we are giving it a random order
                if (choosingOrder) {
                    if ((data.x + radius + 20)<mScreenWidth ) {
                        canvas.drawText(String.valueOf(place), data.x + radius + 20, data.y
                                - radius - 20, mTextPaint);
                    }else{
                        canvas.drawText(String.valueOf(place), data.x - radius-40, data.y
                                        - radius -20, mTextPaint);
                    }

                    // uncomment this to add a second indicator of order
                    //canvas.drawText(String.valueOf(place), data.x - radius-20, data.y
                    //        + radius +20, mTextPaint);
                    drawBig = false;
                }
            }
        }

        canvas.drawCircle(data.x, (data.y) - half_r, radius,
                mCirclePaint);

        if(drawBig) {
            mCirclePaint.setAlpha(125);
            canvas.drawCircle(data.x, (data.y) - half_r, radius * 2f,
                    mCirclePaint);
        }

    }

    public int indexInArray(int[] arr, int n){
        for(int i=0; i<arr.length;i++){
            if (arr[i]==n){
                return i;
            }
        }
        return -1;
    }

    public void setChoosingOrder(boolean b){
        choosingOrder = b;
    }
    public boolean getChoosingOrder(){
        return choosingOrder;
    }

}
