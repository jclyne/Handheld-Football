package com.jrggdev;

import java.util.Random;

public class Percentage
{
	private Random rand = new Random();
	private int mPercentage;
	public Percentage(int percentage)
	{
		assert ( percentage >0 && percentage <=100 );
		mPercentage=percentage;
	}
	
	public boolean test()
	{
		return (rand.nextInt(100) < mPercentage);
	}
}