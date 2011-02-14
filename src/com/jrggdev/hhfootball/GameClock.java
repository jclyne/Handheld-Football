package com.jrggdev.hhfootball;

import android.os.Bundle;
import android.widget.TextView;

public class GameClock
{
	enum Period
	{
		FIRST_QUARTER,
		END_OF_FIRST_QUARTER,
		SECOND_QUARTER,
		HALFTIME,
		THIRD_QUARTER,
		END_OF_THIRD_QUARTER,
		FOURTH_QUARTER,
		GAME_OVER;
		
		public int toInt()
		{
			switch (this)
			{
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
	
	private float mClock=0;
	private boolean mRunning=false;
	private Period mPeriod;
	private long mPeriodLength;
	
	public interface GameClockHandler
	{
		public void updateClockDisplay(float secs);
		public void updatePeriodDisplay(Period period);
		public void handleClockExpired();
	}
	private GameClockHandler mHandler;
	
	
	public GameClock(long periodLength,GameClockHandler handler)
	{
		super();
		
		mHandler=handler;
		mPeriodLength = periodLength;
		mPeriod=Period.FIRST_QUARTER;
		resetClock();
	}
	
	public GameClock(Bundle bundle,GameClockHandler handler)
	{
		super();

		mHandler=handler;
		mClock=bundle.getFloat("mClock");
		mRunning=bundle.getBoolean("mRunning");
		mPeriod=Period.values()[bundle.getInt("mPeriod")];
		mPeriodLength=bundle.getLong("mPeriodLength");
	}
	
	public Bundle serialize()
	{
		Bundle bundle = new Bundle();
		bundle.putFloat("mClock",mClock);
		bundle.putBoolean("mRunning",mRunning);
		bundle.putInt("mPeriod",mPeriod.ordinal());
		bundle.putLong("mPeriodLength",mPeriodLength);
		return bundle;
	}

	public void tick()
	{
		if (mRunning)
		{
			mClock -= 0.1;
			if (mClock <= 0)
			{
				mClock = 0;
				stop();
				mHandler.handleClockExpired();
				mPeriod=Period.values()[mPeriod.ordinal()+1];
			}
		}
		
		mHandler.updatePeriodDisplay(mPeriod);
		mHandler.updateClockDisplay(mClock);
	}
	
	public void resetClock()
	{
		stop();
		mClock=mPeriodLength*60;
		mHandler.updateClockDisplay(mClock);
	}
	
	public void set_period()
	{
		if (mPeriod != Period.GAME_OVER)
		{
			mPeriod=Period.values()[mPeriod.ordinal()+1];
			mHandler.updatePeriodDisplay(mPeriod);
			resetClock();
		}
	}
	
	public void start()
	{
		if (mRunning)
			return;
		
		mRunning=true;
	}
	
	public void stop()
	{
		if (!mRunning)
			return; 
		
		mRunning=false;
	}
	
	public Period period()
	{
		return mPeriod;
	}
	
	public boolean expired()
	{
		return mClock <= 0;
	}
	
	public float timeLeftSecs()
	{
		return mClock;
	}
	
//	public void updateClockDisplay(int mins, float secs)
//	{
//		mClockView.setText(String.format("%02d:%04.1f",mins,secs));
//	}
//	
//	public void updatePeriodDisplay(Period period)
//	{
//		mPeriodView.setText(String.format("%d",period()));
//	}
}