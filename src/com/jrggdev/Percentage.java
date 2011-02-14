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
	
	public final int getPercentage()
	{
		return mPercentage;
	}
	
	public boolean test()
	{
		return (rand.nextInt(100) < mPercentage);
	}
	
	public boolean test(int adjust)
	{
		int percentage = mPercentage+adjust;
		if (percentage >100)
			percentage=100;
		
		return (rand.nextInt(100) < percentage);
	}
}