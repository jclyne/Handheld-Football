package com.redpantssoft;

import android.os.Bundle;

/**
 * Represents a coordinate in 2d space
 *
 * All access to the x,y values are public.
 */
public class Coordinate {
    /**
     * X value of the coordinate
     */
    public int x;
    /**
     * Y Value of the coordinate
     */
    public int y;

    /**
     * Constructor
     *
     * @param x initial X position
     * @param y initial y position
     */
    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Copy Constructor
     *
     * @param pos initial position to set
     */
    public Coordinate(Coordinate pos) {
        this.x = pos.x;
        this.y = pos.y;
    }

    /**
     * Deserializing constructor
     *
     * @param bundle  Bundle to deserialize the coordinate values
     */
    public Coordinate(Bundle bundle) {
        x = bundle.getInt("x");
        y = bundle.getInt("y");
    }

    /**
     * Serializes the coordinate to the specified Bundle
     *
     * @return  the serialized Bundle
     */
    public Bundle serialize() {
        Bundle bundle = new Bundle();
        bundle.putInt("x", x);
        bundle.putInt("y", y);

        return bundle;
    }

    /**
     * Test for equality with specified x,y coordinate
     *
     * @param x value of X to compare against
     * @param y value of Y to compare against
     * @return boolean representing equality
     */
    public boolean equals(int x, int y) {
        return (x == this.x && y == this.y);
    }

    /**
     * Converts coordinate to a suitable string representation
     * @return  string representation of the coordinate
     */
    @Override
    public String toString() {
        return "Coordinate: [" + x + "," + y + "]";
    }

}
