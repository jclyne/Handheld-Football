package com.jrggdev.hhfootball;

import android.os.Handler;
import android.os.Message;

public class Timer extends Handler
{
	private boolean mRunning=false;
	private int mIntervalMillis=0;
	private TimerHandler mHandler;
	
	public interface TimerHandler
	{
		public boolean HandleTimer();
	}
	
	public Timer(TimerHandler handler)
	{
		
		mHandler=handler;
	}
	
	public Timer(int intervalMillis, TimerHandler handler)
	{
		
		mHandler=handler;
		mIntervalMillis=intervalMillis;
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		if (mRunning)
		{
			mRunning=false;
			if ( mHandler.HandleTimer())
			{
				start(mIntervalMillis);
			}

		}
	}

	public void start(int intervalMillis)
	{
		mIntervalMillis=intervalMillis;
		start();
	}
	
	public void start()
	{
		mRunning=true;
		removeMessages(0);
		sendMessageDelayed(obtainMessage(0), mIntervalMillis);
	}
	
	public void stop()
	{
		mRunning=false;
	}	
}
