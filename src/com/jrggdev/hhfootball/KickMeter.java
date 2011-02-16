package com.jrggdev.hhfootball;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ProgressBar;

public class KickMeter extends ProgressBar implements Runnable
{
	private String TAG="KickMeter";
	private int mMin=10;
	private int mMaxDelay=100;
	private int mMinDelay=10;
	
	private boolean enabled;
	private Thread mThread;
	
	public KickMeter(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		enabled=false;
	}

	public KickMeter(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		enabled=false;
	}

	public KickMeter(Context context)
	{
		super(context);
		enabled=false;
	}
	
	
	public Bundle serialize()
	{
		Bundle bundle = new Bundle();
		bundle.putInt("mMin", mMin);
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
			enable(bundle.getInt("progress"));
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

	public synchronized boolean isEnabled()
	{
		return enabled;
	}
	
	public synchronized void enable()
	{
		enable(0);
	}
	
	public synchronized void enable(int progress)
	{
		if (!enabled)
		{
			setVisibility(VISIBLE);
			setProgress(progress);
			enabled=true;
			mThread = new Thread(this);
			mThread.start();
		}
	}
	
	public boolean disable()
	{
		synchronized(this)
		{
			if (enabled)
			{
				setVisibility(INVISIBLE);
				enabled=false;
				notifyAll();
			}
			else
				return false;
		}
		
		while (mThread.isAlive())
		{
			try
			{
				mThread.join();
			}
			catch (InterruptedException e)
			{
				continue;
			}
		}
		return true;
	}
	
	public synchronized int getPowerValue()
	{
		return getProgress()+mMin;
	}
	
	public void run()
	{
		float delayFactor=(float)((mMaxDelay-mMinDelay))/(float)(super.getMax());
		Log.d(TAG,String.format("delayFactor: %3.2f",delayFactor));
		synchronized (this)
		{
			while (enabled)
			{
				if (!enabled)
					return;
				
				int val=getProgress()+1;
				if (val > super.getMax())
					val = 0;
				setProgress(val);
				
				int wait_time = (int) ((float)(mMaxDelay) - (float)(val*delayFactor));
				
				long start=System.currentTimeMillis();
				while (wait_time > 0)
				{
					try{
						this.wait(wait_time);
						break;
					}
					catch (InterruptedException e){
						wait_time-=System.currentTimeMillis()-start;
					}
				}
			}
		}
	}
}
