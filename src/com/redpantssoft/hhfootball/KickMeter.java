package com.redpantssoft.hhfootball;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ProgressBar;

/**
 * Implements a KickMeter widget that used to get kick power.
 * <p/>
 * It is based on a progress bar and uses a background thread to
 * update the power meter while waiting for user input on the main
 * thread.  It is invisible until the meter is enabled. Correct usage
 * is to set the minPower/max values, enable the meter, wait for user
 * input to disable it, then retrieve the power with {@link #getPowerValue()}
 * <p/>
 * The border size and color are configurable with styleable attributes
 *
 * @author Jeff Clyne
 */
public class KickMeter extends ProgressBar implements Runnable {
    /**
     * Static Logging Tag
     */
    private static final String TAG = "KickMeter";


    /**
     * Minimum value of the power meter
     */
    private int minPower = 10;
    /**
     * Maximum value of the power meter
     */
    private int maxPower = 70;

    /**
     * Smallest delay (fastest refresh) for the
     * linear meter refresh increase
     */
    private int minDelay = 10;

    /**
     * Largest delay (slowest refresh) for the
     * linear meter refresh increase
     */
    private int maxDelay = 100;

    /**
     * Flag indicating whether the power meter is enabled
     */
    private boolean meterEnabled = false;

    /**
     * Updated thread from the power meter
     */
    private Thread meterThread;

    /**
     * Paint used to render the meter's border
     */
    private Paint borderPaint;

    /**
     * Constructor used to inflate from XML, includes optional style
     *
     * @param context  Context of the owning activity
     * @param attrs    Attributes from the XML tag inflating the view
     * @param defStyle Style to apply to this view
     */
    public KickMeter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setVisibility(INVISIBLE);
        initializeBorderFromAttributeSet(attrs);
    }

    /**
     * Constructor used to inflate from XML, includes optional style
     *
     * @param context Context of the owning activity
     * @param attrs   Paint used to render the meter's border
     */
    public KickMeter(Context context, AttributeSet attrs) {
        super(context, attrs);

        setVisibility(INVISIBLE);
        initializeBorderFromAttributeSet(attrs);
    }

    /**
     * Initializes  the border paint from the XML attributes inflating the view
     *
     * @param attrs Attributes from the XML tag inflating the view
     */
    private void initializeBorderFromAttributeSet(AttributeSet attrs) {
        // Initialize the border
        borderPaint = new Paint();

        // Initialize the border paint from the attribute set
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.KickMeter);

        int borderColor = a.getColor(R.styleable.KickMeter_borderColor, Color.WHITE);
        float borderWidth = a.getInt(R.styleable.KickMeter_borderSize, 0);

        Log.i(TAG, "Color: " + borderColor);
        borderPaint.setColor(borderColor);

        Log.i(TAG, "Size: " + borderWidth);
        borderPaint.setStrokeWidth(borderWidth);

        borderPaint.setStyle(Style.STROKE);

        a.recycle();
    }

    /**
     * Constructor for manual creation of the meter
     *
     * @param context Context of the owning activity
     */
    public KickMeter(Context context) {
        super(context);

        setVisibility(INVISIBLE);
    }

    /**
     * Serializes the kick meter into a Bundle
     *
     * @return Bundle of serialized kick meter state
     */
    public Bundle serialize() {
        Bundle bundle = new Bundle();
        bundle.putInt("minPower", minPower);
        bundle.putInt("maxPower", maxPower);
        bundle.putInt("minDelay", minDelay);
        bundle.putInt("maxDelay", maxDelay);
        bundle.putInt("borderColor", borderPaint.getColor());
        bundle.putFloat("borderWidth", borderPaint.getStrokeWidth());
        bundle.putBoolean("meterEnabled", meterEnabled);
        bundle.putInt("progress", getProgress());

        return bundle;
    }

    /**
     * Restores the kick meter from an existing bundle
     *
     * @param bundle Bundle containing serialized values
     */
    public void restore(Bundle bundle) {
        disable();

        setMinMaxPower(bundle.getInt("minPower"), bundle.getInt("maxPower"));
        setMinMaxDelay(bundle.getInt("minDelay"), bundle.getInt("maxDelay"));
        borderPaint.setColor(bundle.getInt("borderColor"));
        borderPaint.setStrokeWidth(bundle.getFloat("borderWidth"));
        if (bundle.getBoolean("meterEnabled")) {
            enable(bundle.getInt("progress"));
        }
    }

    /**
     * @param min New minimum meter value
     * @param max New maximum meter value
     */
    public void setMinMaxPower(int min, int max) {
        assert (min < max);
        minPower = min;
        maxPower = max;
        setMax(maxPower - minPower);
    }

    /**
     * @return Current maximum meter value
     */
    public int getMaxPower() {
        return maxPower;
    }

    /**
     * @return Current minimum meter value
     */
    public int getMinPower() {
        return minPower;
    }

    /**
     * @param min New minimum update delay
     * @param max New maximum update delay
     */
    public void setMinMaxDelay(int min, int max) {
        assert (min < max);
        minDelay = min;
        maxDelay = max;
    }

    /**
     * @return Current minimum update delay
     */
    public int getMinDelay() {
        return minDelay;
    }

    /**
     * @return Current maximum update delay
     */
    public int getMaxDelay() {
        return maxDelay;
    }

    /**
     * @param borderPaint Paint to use to draw the meter border
     */
    public void setBorderPaint(Paint borderPaint) {
        this.borderPaint = borderPaint;
    }

    /**
     * @return Flag indicating of the meter is enabled
     */
    @Override
    public synchronized boolean isEnabled() {
        return meterEnabled;
    }

    /**
     * Enables the meter, makes it visible, and sets the initial
     * progress to 0
     */
    public synchronized void enable() {
        enable(0);
    }

    /**
     * Enables the meter, makes it visible, and sets the initial
     * progress to 'progress'
     *
     * @param progress initial progress value after initialization
     */
    public synchronized void enable(int progress) {
        if (!meterEnabled) {
            if (progress > getMax() || progress < 0) {
                progress = 0;
            }
            setVisibility(VISIBLE);
            setProgress(progress);
            meterEnabled = true;
            meterThread = new Thread(this);
            meterThread.start();
        }
    }

    /**
     * Disables the meter, making it invisible. This should be driven
     * from user interaction (kick button). The power value can be
     * retrieved from {@link #getPowerValue()} once it is disabled.
     *
     * @return Flag indicating whether the meter was disabled.
     */
    public boolean disable() {
        synchronized (this) {
            if (meterEnabled) {
                setVisibility(INVISIBLE);
                meterEnabled = false;
                notifyAll();
            } else
                return false;
        }

        while (meterThread.isAlive()) {
            try {
                meterThread.join();
            } catch (InterruptedException ignored) {
            }
        }
        return true;
    }

    public synchronized int getPowerValue() {
        return getProgress() + minPower;
    }

    /**
     * Thread context to update the power meter.
     * <p/>
     * The power meter increases based on a delay factor
     * that is applied to the current value, thus making it
     * slowly update faster as the power increases. This makes
     * it more difficult, and risky, to go for the high power.
     */
    @Override
    public void run() {
        // Calculates a delay factor that should produce at linear increase in the
        //  meter update
        final float delayFactor = (float) ((maxDelay - minDelay)) / (float) (super.getMax());
        Log.d(TAG, String.format("delayFactor: %3.2f", delayFactor));
        synchronized (this) {
            while (meterEnabled) {
                int val = getProgress() + 1;
                if (val > super.getMax())
                    val = 0;
                setProgress(val);

                int wait_time = (int) ((float) (maxDelay) - val * delayFactor);

                long start = System.currentTimeMillis();
                while (wait_time > 0) {
                    try {
                        wait(wait_time);
                        break;
                    } catch (InterruptedException e) {
                        wait_time -= System.currentTimeMillis() - start;
                    }
                }
            }
        }
    }


    /**
     * Custom drawing routine to add a bounding rectangle to the meter.
     * This increases the visibility of bounds
     *
     * @param canvas Current canvas ro draw the view on to
     */
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth() - 1, getHeight() - 1, borderPaint);

    }
}
