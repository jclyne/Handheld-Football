package com.redpantssoft;

import java.util.Random;

/**
 * Represents a percentage test.
 *
 * A percentage object is instantiated with a specified
 * percentage value. The test methods should return true
 * with an approximate frequency of the specified percentage.
 * For instance, a Percentage object set to 50% should return
 * true from test half of the times it is called.
 */
public class Percentage {
    /**
     * Random number generator
     */
    private final Random rand = new Random();

    /**
     * Specified percentage value
     */
    private int percentage;

    /**
     * Constructor
     *
     * @param percentage desired percentage value, must be an unsigned
     * integer between 1 and 100 inclusive.
     */
    public Percentage(int percentage) {
        assert (percentage > 0 && percentage <= 100);
        this.percentage = percentage;
    }

    /**
     * Accessor for the specified percentage
     *
     * @return percentage value
     */
    public final int getPercentage() {
        return percentage;
    }

    /**
     * Test method, should return true at a frequency
     * equal to the specified percentage
     *
     * @return boolean representing hit or miss
     */
    public boolean test() {
        return (rand.nextInt(100) < percentage);
    }

    /**
     * Test method that allows for a temporary tweak of the
     * specified percentage for that test only.
     *
     * @param adjust percentage adjustment to apply, can
     * be a positive or negative number
     * @return boolean representing hir or miss
     */
    public boolean test(int adjust) {
        int percentage = this.percentage + adjust;
        if (percentage > 100)
            percentage = 100;

        return (rand.nextInt(100) < percentage);
    }
}