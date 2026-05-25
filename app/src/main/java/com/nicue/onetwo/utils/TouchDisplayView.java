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

    private ValueAnimator countdownAnimator;
    private float countdownProgress = 0f;
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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

                    if (countdownAnimator != null) {
                        countdownAnimator.cancel();
                    }
                    countdownProgress = 0f;
                    countdownAnimator = ValueAnimator.ofFloat(0f, 1f);
                    countdownAnimator.setDuration(1500L);
                    countdownAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
                    countdownAnimator.addUpdateListener(
                            new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    countdownProgress = (float) animation.getAnimatedValue();
                                    invalidate();
                                }
                            });
                    countdownAnimator.start();

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

        if (mHasTouch) {
            postInvalidateOnAnimation();
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

        mGlowPaint.setStyle(Paint.Style.FILL);
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
                if (pulseAnimator != null) {
                    drawPulsingConcentricCircles(canvas, data.x, data.y, radius, half_r);
                }
                drawBig = false;
            } else { // With this line we are giving it a random order
                if (choosingOrder) {
                    int place = indexInArray(randomArray, id) + 1;
                    drawSingleRefinedOrderBadge(canvas, String.valueOf(place), data, radius, half_r, color);
                    drawBig = false;
                }
            }
        }

        canvas.drawCircle(data.x, (data.y) - half_r, radius, mCirclePaint);

        // Continuous wave/ripple effect radiating outwards from each active finger
        if (!alreadyChosen) {
            long time = System.currentTimeMillis();
            float ripplePhase = (time % 1600L) / 1600f;
            float rippleRadius = radius + (ripplePhase * radius * 1.2f);
            int rippleAlpha = (int) (130 * (1f - ripplePhase));

            mTransStrokePaint.setColor(color);
            mTransStrokePaint.setAlpha(rippleAlpha);
            float density = getResources().getDisplayMetrics().density;
            mTransStrokePaint.setStrokeWidth(3f * density);
            canvas.drawCircle(data.x, data.y - half_r, rippleRadius, mTransStrokePaint);
        }

        // Draw concentric progress/charging arc around each finger while held
        if (fingersDown && !alreadyChosen) {
            float density = getResources().getDisplayMetrics().density;
            float arcRadius = radius + (12f * density);
            android.graphics.RectF arcBounds = new android.graphics.RectF(
                data.x - arcRadius,
                (data.y - half_r) - arcRadius,
                data.x + arcRadius,
                (data.y - half_r) + arcRadius
            );

            mStrokePaint.setColor(color);
            mStrokePaint.setAlpha(255);
            mStrokePaint.setStrokeWidth(4f * density);

            float sweepAngle = 360f * countdownProgress;
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, mStrokePaint);
        }

        if (drawBig) {
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            int colorStart = Color.argb(haloAlpha, red, green, blue);
            int colorEnd = Color.argb(0, red, green, blue);

            android.graphics.RadialGradient gradient = new android.graphics.RadialGradient(
                    data.x, data.y - half_r, radius * 2.2f,
                    colorStart, colorEnd,
                    android.graphics.Shader.TileMode.CLAMP);
            mGlowPaint.setShader(gradient);
            canvas.drawCircle(data.x, data.y - half_r, radius * 2.2f, mGlowPaint);
        }
    }

    private void drawSingleRefinedOrderBadge(
            Canvas canvas, String label, TouchHistory data, float radius, float half_r, int circleColor) {
        float density = getResources().getDisplayMetrics().density;
        float labelDistance = radius + mOrderLabelOffset;

        float centerX = data.x;
        float centerY = data.y - half_r;

        float topDist = data.y;
        float bottomDist = getHeight() - data.y;
        float leftDist = data.x;
        float rightDist = getWidth() - data.x;

        float minDist = topDist;
        int closestEdge = 0; // 0 = top, 1 = right, 2 = bottom, 3 = left

        if (rightDist < minDist) {
            minDist = rightDist;
            closestEdge = 1;
        }
        if (bottomDist < minDist) {
            minDist = bottomDist;
            closestEdge = 2;
        }
        if (leftDist < minDist) {
            minDist = leftDist;
            closestEdge = 3;
        }

        float badgeX = centerX;
        float badgeY = centerY;
        float rotation = 0f;

        switch (closestEdge) {
            case 2: // bottom
                badgeY = centerY - labelDistance;
                rotation = 0f;
                break;
            case 0: // top
                badgeY = centerY + labelDistance;
                rotation = 180f;
                break;
            case 3: // left
                badgeX = centerX + labelDistance;
                rotation = 90f;
                break;
            case 1: // right
                badgeX = centerX - labelDistance;
                rotation = -90f;
                break;
        }

        // Clamp the badge coordinates so they don't clip off the screen boundaries
        float badgeRadius = 18f * density;
        float minX = badgeRadius + 4f * density;
        float maxX = getWidth() - badgeRadius - 4f * density;
        float minY = badgeRadius + 4f * density;
        float maxY = getHeight() - badgeRadius - 4f * density;

        badgeX = clamp(badgeX, minX, maxX);
        badgeY = clamp(badgeY, minY, maxY);

        drawBadgeLabel(canvas, label, badgeX, badgeY, rotation, circleColor);
    }

    private void drawBadgeLabel(
            Canvas canvas, String label, float badgeX, float badgeY, float rotation, int circleColor) {
        float density = getResources().getDisplayMetrics().density;
        float badgeRadius = 18f * density;

        canvas.save();
        canvas.rotate(rotation, badgeX, badgeY);

        // 1. Draw badge background (white circle with subtle drop shadow)
        Paint badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBgPaint.setColor(Color.WHITE);
        badgeBgPaint.setStyle(Paint.Style.FILL);
        badgeBgPaint.setShadowLayer(4f * density, 0, 2f * density, 0x3F000000);
        canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBgPaint);

        // 2. Draw badge border (colored outline matching the finger's color)
        Paint badgeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeStrokePaint.setColor(circleColor);
        badgeStrokePaint.setStyle(Paint.Style.STROKE);
        badgeStrokePaint.setStrokeWidth(2f * density);
        canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeStrokePaint);

        // 3. Draw the number in the center of the badge
        Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeTextPaint.setTextSize(14f * density);
        badgeTextPaint.setColor(circleColor);
        badgeTextPaint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);

        // Center text vertically
        float textHeight = badgeTextPaint.descent() - badgeTextPaint.ascent();
        float textOffset = (textHeight / 2) - badgeTextPaint.descent();
        canvas.drawText(label, badgeX, badgeY + textOffset, badgeTextPaint);

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
        selectionRevealAnimator.addListener(
                new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (alreadyChosen) {
                            startPulseAnimation();
                        }
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
        pulseAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
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
        // Option 3: Filled Translucent Ripples (Layered Soundwaves) - Pronounced, Starts Smaller
        mGlowPaint.setShader(null); // Clear any gradient shader to use solid color
        mGlowPaint.setColor(Color.WHITE);

        // Dynamic scale factor: starts smaller (40% of base) and expands to the pronounced size (135% of base)
        float scaleFactor = 0.4f + (pulseValue * 0.95f);
        float scaledPulseOffset = mPulseOffset * 1.6f;

        for (int i = 2; i >= 0; i--) { // Draw largest first so smaller ones layer on top
            float baseOffset = (i + 1) * mPulseBaseOffset * scaleFactor;
            float pulseOffset = pulseValue * scaledPulseOffset;
            float circleRadius = radius + baseOffset + pulseOffset;

            // Calculate expansion progress fraction from 0.0 to 1.0
            float maxRange = radius + mPulseFadeRange * 1.35f;
            float progress = (circleRadius - radius) / maxRange;
            if (progress > 1f) progress = 1f;
            if (progress < 0f) progress = 0f;

            // More pronounced opacity (max alpha 40, ~16% opacity per layer, accumulating in center)
            int alpha = (int) (40 * (1f - progress));
            if (alpha < 0) alpha = 0;

            mGlowPaint.setAlpha(alpha);
            canvas.drawCircle(x, y - half_r, circleRadius, mGlowPaint);
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
        if (countdownAnimator != null) {
            countdownAnimator.cancel();
            countdownAnimator = null;
        }
        countdownProgress = 0f;
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

    public float getSelectionRevealCenterX() {
        return selectionRevealCenterX;
    }

    public float getSelectionRevealCenterY() {
        return selectionRevealCenterY;
    }
}
