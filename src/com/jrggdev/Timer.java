package com.jrggdev;

import android.os.Handler;
import android.os.Message;

public class Timer extends Handler
{
	private boolean mRunning=false;
	private int mIntervalMillis=0;
	private TimerHandler mHandler;
	private static int GLOBAL_MESSAGE_ID=0;
	private int mMessageId;
	
	public interface TimerHandler
	{
		public boolean HandleTimer();
	}
	
	public Timer(TimerHandler handler)
	{
		mMessageId=GLOBAL_MESSAGE_ID++;
		mHandler=handler;
	}
	
	public Timer(int intervalMillis, TimerHandler handler)
	{
		mMessageId=GLOBAL_MESSAGE_ID++;
		mHandler=handler;
		mIntervalMillis=intervalMillis;
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		if ( (msg.what == mMessageId) && mRunning)
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
		removeMessages(mMessageId);
		sendMessageDelayed(obtainMessage(mMessageId), mIntervalMillis);
	}
	
	public void stop()
	{
		mRunning=false;
	}	
}
