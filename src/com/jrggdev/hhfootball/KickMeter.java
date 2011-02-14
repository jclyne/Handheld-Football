package com.jrggdev.hhfootball;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.jrggdev.Coordinate;
import com.jrggdev.Timer;
import com.jrggdev.Timer.TimerHandler;

public class KickMeter extends ProgressBar implements TimerHandler
{
	private int mMin=10;
	private int mSpeed=0;
		
	private Timer mTimer;
	
	public KickMeter(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		mTimer = new Timer(mSpeed,this);
	}

	public KickMeter(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mTimer = new Timer(mSpeed,this);
	}

	public KickMeter(Context context)
	{
		super(context);
		mTimer = new Timer(mSpeed,this);
	}
	
	public Bundle serialize()
	{
		Bundle bundle = new Bundle();
		bundle.putInt("mMin", mMin);
		bundle.putInt("mSpeed", mSpeed);
		bundle.putInt("mMax", getMax());
		bundle.putBoolean("enabled", isEnabled());
		bundle.putInt("progress", getProgress());
		
		return bundle;
	}
	
	public void restore(Bundle bundle)
	{
		disable();
		setMinMax(bundle.getInt("mMin"),bundle.getInt("mMax"));
		if (bundle.getBoolean("enabled"))
		{
			setProgress(bundle.getInt("progress"));
			setVisibility(VISIBLE);
			mTimer.start(bundle.getInt("mSpeed"));
		}
		
	}
	
	public int getMin()
	{
		return mMin;
	}

	public int getMax()
	{
		return super.getMax()+mMin;
	}

	public void setMinMax(int min, int max)
	{
		assert(min < max);
		mMin = min;
		super.setMax(max-mMin);
	}

	public boolean isEnabled()
	{
		return getVisibility() == VISIBLE;
	}
	
	public void enable()
	{
		setProgress(0);
		setVisibility(VISIBLE);
		mTimer.start(mSpeed);
	}
	
	public void disable()
	{
		setVisibility(INVISIBLE);
		mTimer.stop();
	}
	
	public int getPowerValue()
	{
		return getProgress()+mMin;
	}
	
	public boolean HandleTimer()
	{
		int val=getProgress()+1;
		if (val > super.getMax())
			val = 0;
		setProgress(val);
		
		// Make the meter progressively faster as the power increases
//		mTimer.start(mSpeed - ((mSpeed*val)/super.getMax()));
		return true;
	}
}
