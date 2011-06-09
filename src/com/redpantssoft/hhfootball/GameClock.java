package com.redpantssoft.hhfootball;

import android.os.Bundle;

/**
 * Implements a Game Clock for Handheld football.
 *
 * The game clock is externally driven, meaning the ${@code #tick()}
 * method needs to be called to increment the clock. The intent is
 * allow the clock to be run from the main UI thread. It should be
 * called once every 1/10 of a second.
 *
 * The {@code #GameClockHandler} interface allows for callbacks, onto the UI thread,
 * to update the view associated with the clock .
 *
 * @author Jeff Clyne
 */
public class GameClock {

    /**
     * Interface to update the UI thread of events that have happened
     * with the game clock.
     */
    public interface GameClockHandler {
        /**
         * Called to indicate that the clock display needs
         * to be updated. This will be called when the clock
         * is initialized, manually configured {@code #set_period} or
         * {@code #resetClock}, or after a {@code tick}.
         *
         * @param secs  Value of clock in seconds
         * @param period  Current Period of the game
         */
        public void updateClockDisplay(float secs,Period period);

        /**
         * Called when the game clock expires. The clock can expire
         * while a play is active, so the intent is to allow for a buzzer
         * or horn to signal to the user that it has expired. Once
         * a play is dead, the clock can be check manually for expiration
         * or the game activity can store a flag.
         */
        public void handleClockExpired();
    }

    /**
     * Enumerated type that defines the current period. It's main purpose
     * is to provide states that correspond to the end of a period when
     * the game is still live and to translate these states into the correct
     * integer for the scoreboard.
     */
    enum Period {
        FIRST_QUARTER,
        END_OF_FIRST_QUARTER,
        SECOND_QUARTER,
        HALFTIME,
        THIRD_QUARTER,
        END_OF_THIRD_QUARTER,
        FOURTH_QUARTER,
        GAME_OVER;

        public int toInt() {
            switch (this) {
                default:
                case FIRST_QUARTER:
                case END_OF_FIRST_QUARTER:
                    return 1;
                case SECOND_QUARTER:
                case HALFTIME:
                    return 2;
                case THIRD_QUARTER:
                case END_OF_THIRD_QUARTER:
                    return 3;
                case FOURTH_QUARTER:
                case GAME_OVER:
                    return 4;
            }
        }
    }

    /**
     * The current value of the clock in seconds
     */
    private float clockSecs = 0;

    /**
     * The current period of the game
     */
    private Period period;

    /**
     * The length of each period, in seconds
     */
    private final long periodLength;

    /**
     * Flag indicating whether or not the clock is running
     */
    private boolean running = false;

    /**
     * Reference to the current handler for this Game Clock
     */
    private GameClockHandler handler;


    /**
     * @param periodLength The length of each period, in seconds
     * @param handler Reference to a handler for this Game Clock
     */
    public GameClock(long periodLength, GameClockHandler handler) {
        super();

        this.handler = handler;
        this.periodLength = periodLength;
        period = Period.FIRST_QUARTER;
        clockSecs = periodLength;
        handler.updateClockDisplay(clockSecs, period);
    }

     /**
     * @param bundle Bundle to deserialize the game clock state
     * @param handler Reference to a handler for this Game Clock
     */
    public GameClock(Bundle bundle, GameClockHandler handler) {
        super();

        this.handler = handler;
        clockSecs = bundle.getFloat("clockSecs");
        running = bundle.getBoolean("running");
        period = Period.values()[bundle.getInt("period")];
        periodLength = bundle.getLong("periodLength");
        handler.updateClockDisplay(clockSecs, period);
    }

    /**
     * Serializes the state into a Bundle
     *
     * @return Bundle containing serialized state
     */
    public Bundle serialize() {
        Bundle bundle = new Bundle();
        bundle.putFloat("clockSecs", clockSecs);
        bundle.putBoolean("running", running);
        bundle.putInt("period", period.ordinal());
        bundle.putLong("periodLength", periodLength);
        return bundle;
    }

    /**
     * Called be the UI thread to update the clock. This should be called
     * whether or not the clock is running.
     */
    public void tick() {
        if (running) {
            clockSecs -= 0.1;
            if (clockSecs <= 0) {
                clockSecs = 0;
                stop();
                handler.handleClockExpired();
                period = Period.values()[period.ordinal() + 1];
            }
        handler.updateClockDisplay(clockSecs, period);
        }

    }

    /**
     * Resets the clock to the period length
     */
    public void resetClock() {
        running = false;
        clockSecs = periodLength;

        handler.updateClockDisplay(clockSecs, period);
    }

    /**
     *  Sets the current game period
     */
    public void setPeriod() {
        if (period != Period.GAME_OVER) {
            running = false;
            clockSecs = periodLength * 60;
            period = Period.values()[period.ordinal() + 1];
        }

        handler.updateClockDisplay(clockSecs, period);
    }

    /**
     * Starts the Game Clock
     */
    public synchronized void start() {
        running = true;
    }

    /**
     * Stops the Game Clock
     */
    public synchronized void stop() {
        running = false;
    }

    /**
     * @return The current Game Period
     */
    public final Period period() {
        return period;
    }

    /**
     * @return  True if the game clock has expired
     */
    public boolean expired() {
        return clockSecs <= 0;
    }

    /**
     * @return  Current value of the game clock in Seconds
     */
    public float timeLeftSecs() {
        return clockSecs;
    }
}