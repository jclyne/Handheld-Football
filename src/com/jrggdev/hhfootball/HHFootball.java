package com.jrggdev.hhfootball;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;

import com.jrggdev.Timer;

public class HHFootball extends Activity
{
	private static final int ABOUT_DIALOG=1;
	private AlertDialog mAboutDialog;
	
	private MediaPlayer mSplashSound;
	Timer T;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		
		mAboutDialog = new AlertDialog.Builder(this) 
				.setMessage("ABOUT")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			     })
			     .create();

		mSplashSound= MediaPlayer.create(this, R.raw.splash);
		((LinearLayout)findViewById(R.id.logoview)).setLayoutAnimationListener(new AnimationListener(){
			public void onAnimationEnd(Animation animation)
			{
			}

			public void onAnimationRepeat(Animation animation)
			{				
			}

			public void onAnimationStart(Animation animation)
			{		
				mSplashSound.start();
			}
		});
		
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case ABOUT_DIALOG:
				return mAboutDialog;
		}
		return super.onCreateDialog(id);
	}

	public void onNewGame(View view)
	{
		Intent intent = new Intent();
		intent.setClass(this,Game.class);
		startActivity(intent);
	}
	
	public void onSettings(View view)
	{
		Intent intent = new Intent();
		intent.setClass(this,Settings.class);
		startActivity(intent);
	}
	
	public void onAbout(View view)
	{
		showDialog(ABOUT_DIALOG);
	}
	
	public void onExit(View view)
	{
		finish();
	}

}
