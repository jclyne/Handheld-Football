package com.jrggdev;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

public class SoundManager
{
	private static final String TAG ="SoundManager";
	private class SoundPoolStream
	{
		public MediaPlayer player;
		public int resid;
		public float volume;
		public boolean paused=false;
	}
	
	private Context mContext;
	private AudioManager  mAudioManager;
	private SoundPool mSoundPool;
	private HashMap<Integer,SoundPoolStream> mSfxTable;
	private boolean mMute=false;
	private boolean mPaused=false;
	
	public SoundManager(Context context)
	{
		mContext = context;
		mSfxTable=new HashMap<Integer,SoundPoolStream>();
		mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

		Log.i(TAG,"SoundManager initialized");
	}

	public float getStreamVolume()
	{
		if (mMute)
			return 0;
		else
			return (float)mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / 
				(	float) mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	}
	
	public void addSfx(int key, int resid)
	{
		if (mSfxTable.containsKey(key))
		{
			SoundPoolStream stream = mSfxTable.get(key);
			if (stream.resid != resid)
			{
				Log.i(TAG,String.format("Unloading existing sfx: %d",stream.resid));
				release(stream);
			}
			else
				return;				
		}
		
		
		SoundPoolStream stream= new SoundPoolStream();
		stream.resid=resid;
		stream.player= MediaPlayer.create(mContext,resid);
		stream.volume= 1;
		mSfxTable.put(key, stream);
		
		Log.i(TAG,String.format("Added new sfx: %d",stream.resid));
	}
	
	public void setSfxVolume(int key, float volume)
	{
		try
		{
			SoundPoolStream stream=mSfxTable.get(key);
			stream.volume=volume;
		}
		catch (NullPointerException e)
		{
			return;
		}
	}
	
	public void playSfx(int key,boolean loop)
	{
		try
		{
			SoundPoolStream stream=mSfxTable.get(key);
			float vol = getStreamVolume()*stream.volume;
			stream.player.stop();
			try 
			{
				stream.player.prepare();
			} catch (IllegalStateException e) 
			{
			} catch (IOException e) 
			{
				return;
			}
			stream.player.setVolume(vol, vol);
			stream.player.setLooping(loop);
			stream.player.start();
		}
		catch (NullPointerException e)
		{
			return;
		}
	}
	
	public void stopSfx(int key)
	{
		try
		{
			SoundPoolStream stream=mSfxTable.get(key);			
			try 
			{
				stream.player.stop();
			} catch (IllegalStateException e) 
			{
			}
			
		}
		catch (NullPointerException e)
		{
			return;
		}
	}
	
	
	private void release(SoundPoolStream stream)
	{
		stream.player.release();
		stream.player=null;
		stream=null;
	}
	
	public void release(int key)
	{
		try
		{
			release(mSfxTable.get(key));
			mSfxTable.remove(key);
		}
		catch (NullPointerException e)
		{
			return;
		}
	}
	
	public void release()
	{
		for (Iterator<SoundPoolStream> i=mSfxTable.values().iterator(); i.hasNext();)
		{
			release(i.next());
		}
		mSfxTable.clear();
	}
	
	public void setMute(boolean mute)
	{
		mMute=mute;
		
		for (Iterator<SoundPoolStream> i=mSfxTable.values().iterator(); i.hasNext();)
		{
			SoundPoolStream stream=i.next();
			if (stream.player.isPlaying() || stream.paused )
			{
				float level = getStreamVolume() * stream.volume;
				stream.player.setVolume(level,level);
			}
		}
		
	}
	
	public void pause()
	{
		if (!mPaused)
		{
			mPaused=true;
			for (Iterator<SoundPoolStream> i=mSfxTable.values().iterator(); i.hasNext();)
			{
				SoundPoolStream stream=i.next();
				if (stream.player.isPlaying())
				{
					stream.paused=true;
					stream.player.pause();
				}
			}
		}
	}
	
	public void resume()
	{
		if (mPaused)
		{
			mPaused=false;
			for (Iterator<SoundPoolStream> i=mSfxTable.values().iterator(); i.hasNext();)
			{
				SoundPoolStream stream=i.next();
				if (stream.paused)
				{
					stream.paused=false;
					stream.player.start();
				}
			}
		}

	}

}
