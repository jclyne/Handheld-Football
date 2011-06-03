package com.redpantssoft;

import android.os.Handler;
import android.os.Message;

/**
 * Implementation of  a timer based on the Android send message/handler api.
 *
 * A user implements the Timer.TimerHandler interface which
 * takes care of handling the timer expiration.  It can
 * be optionally re-armed for a continuously repeating timer
 *
 * @author Jeff clyne
 */
public class Timer extends Handler {

    /**
     * Represents a global message ID for timer messages
     */
    private static final int TIMER_MESSAGE_ID = 1024;

    /**
     * Flag that indicates whether or not the timer is armed
     */
    private boolean running = false;

    /**
     * Specifies the current timer interval. This can be
     * initialized at timer creation and changed any time
     * the timer is started.
     */
    private int intervalMillis = 0;

    /**
     * Reference to the timer handler, set at timer creation
     */
    private final TimerHandler handler;

    /**
     * Interface to handle the expiration of a timer
     */
    public interface TimerHandler {
        /**
         * Method should be implemented to handle expiration of a timer
         * @return true to re-arm the timer
         */
        public boolean HandleTimer();
    }

    /**
     * Constructor
     *
     * @param handler reference to the timer handler
     */
    public Timer(TimerHandler handler) {
        this.handler = handler;
    }

    /**
     * Constructor
     * @param intervalMillis  initial value of timer interval
     * @param handler  reference to the timer handler
     */
    public Timer(int intervalMillis, TimerHandler handler) {
        this.handler = handler;
        this.intervalMillis = intervalMillis;
    }

    /**
     * Starts the timer with a specified millisecond interval
     *
     * @param intervalMillis timer interval to arm timer with
     */
    public void start(int intervalMillis) {
        this.intervalMillis = intervalMillis;
        start();
    }

    /**
     * Starts the timer with the previously specified interval
     */
    public void start() {
        running = true;
        removeMessages(TIMER_MESSAGE_ID,this);
        sendMessageDelayed(obtainMessage(TIMER_MESSAGE_ID, this), intervalMillis);
    }

    /**
     * Stops the current timer
     */
    public void stop() {
        running = false;
    }

    /**
     * Implementation of the android.handler.handleMessage method. This
     * is called to handle messages delivered to the looper message queue.
     *
     * @param msg   message being delivered from looper thread
     */
    @Override
    public void handleMessage(Message msg) {
        if (running && (msg.what == TIMER_MESSAGE_ID) && (msg.obj == this)) {
            running = false;
            if (handler.HandleTimer()) {
                start(intervalMillis);
            }
        }
    }


}
