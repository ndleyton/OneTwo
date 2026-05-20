package com.nicue.onetwo.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import com.nicue.onetwo.R;
import com.nicue.onetwo.utils.Pools.SimplePool;
import java.util.Random;

public class TouchDisplayView extends View {

    // private long startTime = 0L;
    private boolean fingersDown = false;
    private boolean alreadyChosen = false;
    private boolean choosingOrder = false;
    // private int fingers = 0;
    public static final long SELECTION_REVEAL_DURATION_MS = 650L;
    private static final int SELECTED_TOUCH_ALPHA = 255;
    private static final int DIMMED_TOUCH_ALPHA = 105;
    private static final int TOUCH_HALO_ALPHA = 125;
    private static final float ORDER_LABEL_OFFSET_DP = 22f;
    private int chosenColor = 0;
    private int chosenId = -1;
    private int[] randomArray = {};
    private float selectionRevealProgress = 1f;
    private float selectionRevealCenterX = 0f;
    private float selectionRevealCenterY = 0f;
    private float selectionRevealMaxRadius = 0f;
    private ValueAnimator selectionRevealAnimator;

    private float pulseValue = 0f;
    private ValueAnimator pulseAnimator;

    private int backgroundColorOverride = getResources().getColor(R.color.overrideBackground);

    private final int[] COLORS = {
        0xFF6750A4, // M3 Purple
        0xFF0061A4, // M3 Blue
        0xFF006A6A, // M3 Teal
        0xFF386A20, // M3 Green
        0xFF8B5000, // M3 Orange
        0xFFBA1A1A, // M3 Red
        0xFF984061, // M3 Pink
        0xFF4A6572, // M3 Blue Gray
        0xFF525E75, // M3 Steel
        0xFF7D5260 // M3 Maroon
    };

    public interface OnSelectionListener {
        void onSelectionMade();
    }

    private OnSelectionListener mSelectionListener;

    public void setOnSelectionListener(OnSelectionListener listener) {
        this.mSelectionListener = listener;
    }

    // A Handler to check if all fingers have been pressed down for x time
    private final Handler handler = new Handler();
    private final Runnable runnable =
            new Runnable() {
                public void run() {
                    checkGlobalVariable();
                }
            };

    public void checkGlobalVariable() {
        if (fingersDown) {
            if (!alreadyChosen) {
                alreadyChosen = true;
                randomArray = intArrayToN(mTouches.size());
                shuffleArray(randomArray);
                chosenId = randomArray[0];
                chosenColor = COLORS[chosenId % COLORS.length];
                updateSelectionRevealBounds();
                startSelectionRevealAnimation();
                startPulseAnimation();
                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0, 20, 10, 50};
                v.vibrate(pattern, -1);
                invalidate();

                if (mSelectionListener != null) {
                    mSelectionListener.onSelectionMade();
                }
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

    private static void shuffleArray(int[] array) {
        int index;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            if (index != i) {
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
     * Holds data related to a touch pointer, object pool using {} and recycle to reuse existing
     * objects.
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

        public TouchHistory() {}

        public void setTouch(float x, float y, float pressure) {
            this.x = x;
            this.y = y;
            this.pressure = (this.pressure + pressure) / 2f;
            this.pressure = Math.max(0.25f, this.pressure); // low limit
            this.pressure = Math.min(this.pressure, 0.5f); // high limit
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

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(runnable);
        resetSelection();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    // BEGIN_INCLUDE(onTouchEvent)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                {
                    // fingers = 1;
                    // first pressed gesture has started

                    int id = event.getPointerId(0);

                    TouchHistory data =
                            TouchHistory.obtain(event.getX(0), event.getY(0), event.getPressure(0));

                    // Store the data under its pointer identifier. The pointer number stays
                    // consistent
                    mTouches.put(id, data);

                    Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(20);

                    mHasTouch = true;

                    break;
                }

            case MotionEvent.ACTION_POINTER_DOWN:
                {
                    fingersDown = true;
                    // fingers ++ ;

                    /*
                     * A non-primary pointer has gone down, after an event for the
                     * primary pointer (ACTION_DOWN) has already been received.
                     */
                    int index = event.getActionIndex();
                    int id = event.getPointerId(index);

                    TouchHistory data =
                            TouchHistory.obtain(
                                    event.getX(index), event.getY(index), event.getPressure(index));
                    // data.label = "id: " + id;

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

            case MotionEvent.ACTION_UP:
                {
                    // fingers = 0;
                    /*
                     * Final pointer has gone up and has ended the last pressed
                     * gesture.
                     */

                    int id = event.getPointerId(0);
                    TouchHistory data = mTouches.get(id);
                    mTouches.remove(id);
                    data.recycle();

                    mHasTouch = false;
                    resetSelection();

                    break;
                }

            case MotionEvent.ACTION_POINTER_UP:
                {
                    fingersDown = false;
                    resetSelection();
                    // fingers --;
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

            case MotionEvent.ACTION_MOVE:
                {
                    for (int index = 0; index < event.getPointerCount(); index++) {
                        // get pointer id for data stored at this index
                        int id = event.getPointerId(index);

                        // get the data stored externally about this pointer.
                        TouchHistory data = mTouches.get(id);

                        // add new values
                        data.setTouch(
                                event.getX(index), event.getY(index), event.getPressure(index));
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
            if (alreadyChosen) {
                canvas.drawColor(chosenColor);
                drawSelectionReveal(canvas);
            } else {
                canvas.drawColor(backgroundColorOverride);
            }
        } else {
            resetSelection();
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
    private float mPulseBaseOffset;
    private float mPulseOffset;
    private float mPulseFadeRange;
    private float mOrderLabelOffset;

    private Paint mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTransStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mRevealPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private void initialisePaint() {

        // Calculate radiuses i n px from dp based on screen density
        float density = getResources().getDisplayMetrics().density;
        mCircleRadius = CIRCLE_RADIUS_DP * density;
        mPulseBaseOffset = 35f * density;
        mPulseOffset = 20f * density;
        mPulseFadeRange = 180f * density;
        mOrderLabelOffset = ORDER_LABEL_OFFSET_DP * density;

        // Setup text paint for circle label
        mTextPaint.setTextSize(18f * density);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mStrokePaint.setColor(Color.WHITE);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(3f * density);

        mTransStrokePaint.setColor(Color.WHITE);
        mTransStrokePaint.setStyle(Paint.Style.STROKE);
        mTransStrokePaint.setStrokeWidth(3f * density);
    }

    protected void drawCircle(Canvas canvas, int id, TouchHistory data) {
        // select the color based on the id
        int color = COLORS[id % COLORS.length];
        int touchAlpha =
                alreadyChosen && chosenId != id ? DIMMED_TOUCH_ALPHA : SELECTED_TOUCH_ALPHA;
        int haloAlpha = alreadyChosen && chosenId != id ? DIMMED_TOUCH_ALPHA / 2 : TOUCH_HALO_ALPHA;
        mCirclePaint.setColor(color);
        mCirclePaint.setAlpha(touchAlpha);
        boolean drawBig = true;

        float radius = data.pressure * mCircleRadius;
        float half_r = radius / 2f;

        if (alreadyChosen) {
            if (chosenId == id) {
                drawPulsingConcentricCircles(canvas, data.x, data.y, radius, half_r);
                drawBig = false;
            } else { // With this line we are giving it a random order
                if (choosingOrder) {
                    int place = indexInArray(randomArray, id) + 1;
                    mTextPaint.setColor(Color.WHITE);
                    drawOrderLabels(canvas, String.valueOf(place), data.x, data.y - half_r, radius);
                    drawBig = false;
                }
            }
        }

        canvas.drawCircle(data.x, (data.y) - half_r, radius, mCirclePaint);

        if (drawBig) {
            mCirclePaint.setAlpha(haloAlpha);
            canvas.drawCircle(data.x, (data.y) - half_r, radius * 2f, mCirclePaint);
        }
    }

    private void drawOrderLabels(
            Canvas canvas, String label, float centerX, float centerY, float radius) {
        float textSize = mTextPaint.getTextSize();
        float halfTextWidth = mTextPaint.measureText(label) / 2f;
        float labelDistance = radius + mOrderLabelOffset;
        float baselineCenterOffset = textSize / 3f;

        float minX = halfTextWidth;
        float maxX = getWidth() - halfTextWidth;
        float minBaselineY = textSize;
        float maxBaselineY = getHeight() - baselineCenterOffset;

        drawRotatedOrderLabel(
                canvas,
                label,
                clamp(centerX, minX, maxX),
                clamp(centerY - labelDistance + baselineCenterOffset, minBaselineY, maxBaselineY),
                0f,
                baselineCenterOffset);
        drawRotatedOrderLabel(
                canvas,
                label,
                clamp(centerX + labelDistance, minX, maxX),
                clamp(centerY + baselineCenterOffset, minBaselineY, maxBaselineY),
                90f,
                baselineCenterOffset);
        drawRotatedOrderLabel(
                canvas,
                label,
                clamp(centerX, minX, maxX),
                clamp(centerY + labelDistance + baselineCenterOffset, minBaselineY, maxBaselineY),
                180f,
                baselineCenterOffset);
        drawRotatedOrderLabel(
                canvas,
                label,
                clamp(centerX - labelDistance, minX, maxX),
                clamp(centerY + baselineCenterOffset, minBaselineY, maxBaselineY),
                -90f,
                baselineCenterOffset);
    }

    private void drawRotatedOrderLabel(
            Canvas canvas,
            String label,
            float x,
            float baselineY,
            float degrees,
            float baselineCenterOffset) {
        canvas.save();
        canvas.rotate(degrees, x, baselineY - baselineCenterOffset);
        canvas.drawText(label, x, baselineY, mTextPaint);
        canvas.restore();
    }

    private float clamp(float value, float min, float max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(value, max));
    }

    private void startSelectionRevealAnimation() {
        if (selectionRevealAnimator != null) {
            selectionRevealAnimator.cancel();
        }
        selectionRevealProgress = 0f;
        selectionRevealAnimator = ValueAnimator.ofFloat(0f, 1f);
        selectionRevealAnimator.setDuration(SELECTION_REVEAL_DURATION_MS);
        selectionRevealAnimator.setInterpolator(new DecelerateInterpolator());
        selectionRevealAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        selectionRevealProgress = (Float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
        selectionRevealAnimator.start();
    }

    private void drawSelectionReveal(Canvas canvas) {
        if (selectionRevealProgress >= 1f) {
            return;
        }

        if (selectionRevealMaxRadius <= 0f) {
            return;
        }

        float revealRadius = selectionRevealMaxRadius * (1f - selectionRevealProgress);

        mRevealPaint.setColor(backgroundColorOverride);
        canvas.drawCircle(
                selectionRevealCenterX, selectionRevealCenterY, revealRadius, mRevealPaint);
    }

    private void updateSelectionRevealBounds() {
        TouchHistory chosenTouch = mTouches.get(chosenId);
        if (chosenTouch == null) {
            selectionRevealMaxRadius = 0f;
            return;
        }

        float radius = chosenTouch.pressure * mCircleRadius;
        selectionRevealCenterX = chosenTouch.x;
        selectionRevealCenterY = chosenTouch.y - (radius / 2f);
        selectionRevealMaxRadius =
                maxDistanceToCorner(selectionRevealCenterX, selectionRevealCenterY);
    }

    private float maxDistanceToCorner(float x, float y) {
        float topLeft = distance(x, y, 0f, 0f);
        float topRight = distance(x, y, getWidth(), 0f);
        float bottomLeft = distance(x, y, 0f, getHeight());
        float bottomRight = distance(x, y, getWidth(), getHeight());
        return Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight));
    }

    private float distance(float startX, float startY, float endX, float endY) {
        float xDiff = startX - endX;
        float yDiff = startY - endY;
        return (float) Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1200L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        pulseValue = (Float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
        pulseAnimator.start();
    }

    private void drawPulsingConcentricCircles(
            Canvas canvas, float x, float y, float radius, float half_r) {
        for (int i = 0; i < 3; i++) {
            float baseOffset = (i + 1) * mPulseBaseOffset;
            float pulseOffset = pulseValue * mPulseOffset;
            float circleRadius = radius + baseOffset + pulseOffset;

            // Fade out as they get larger
            int alpha = (int) (160 * (1.0f - (circleRadius - radius) / (radius + mPulseFadeRange)));
            if (alpha < 0) alpha = 0;

            mTransStrokePaint.setAlpha(alpha);
            canvas.drawCircle(x, y - half_r, circleRadius, mTransStrokePaint);
        }
    }

    private void resetSelection() {
        alreadyChosen = false;
        chosenId = -1;
        randomArray = new int[0];
        selectionRevealProgress = 1f;
        selectionRevealCenterX = 0f;
        selectionRevealCenterY = 0f;
        selectionRevealMaxRadius = 0f;
        if (selectionRevealAnimator != null) {
            selectionRevealAnimator.cancel();
            selectionRevealAnimator = null;
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        pulseValue = 0f;
    }

    public int indexInArray(int[] arr, int n) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == n) {
                return i;
            }
        }
        return -1;
    }

    public void setChoosingOrder(boolean b) {
        choosingOrder = b;
    }

    public boolean getChoosingOrder() {
        return choosingOrder;
    }
}
