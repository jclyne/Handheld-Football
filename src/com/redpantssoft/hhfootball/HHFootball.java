package com.redpantssoft.hhfootball;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.redpantssoft.Timer;

public class HHFootball extends Activity implements DialogInterface.OnClickListener
{
	private static final int ABOUT_DIALOG=1;
	private static final int HELP_DIALOG=2;
	private static final int EMAIL_ERROR_DIALOG=3;
	
	private AlertDialog mAboutDialog;
	
	private MediaPlayer mSplashSound;
	Timer T;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		
		ImageView title = new ImageView(this);
		title.setImageResource(R.drawable.splash_about_normal);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mAboutDialog = new AlertDialog.Builder(this) 
				.setCustomTitle(title)
				.setItems(R.array.about_items, this)
			    .create();

		mSplashSound= MediaPlayer.create(this, R.raw.splash);
		((LinearLayout)findViewById(R.id.logoview)).setLayoutAnimationListener(new AnimationListener(){
			public void onAnimationEnd(Animation animation){}
			public void onAnimationRepeat(Animation animation){	}
			public void onAnimationStart(Animation animation){mSplashSound.start();}
		});
		
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case ABOUT_DIALOG:
				return mAboutDialog;
				
			case HELP_DIALOG:
				ImageView title = new ImageView(this);
				title.setImageResource(R.drawable.how_to_play);
				return new AlertDialog.Builder(this) 
						.setCustomTitle(title)
						.setView(getLayoutInflater().inflate(R.layout.help_layout,null))
						.setInverseBackgroundForced(true)
						.setCancelable(false)
						.setPositiveButton("Done", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                dialog.cancel();
					           }
					     }) .create();
				
			case EMAIL_ERROR_DIALOG:
				return new AlertDialog.Builder(this) 
						.setTitle(getString(R.string.dev_email_error_title))
						.setMessage(getString(R.string.dev_email_error_msg))
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                dialog.cancel();
					           }
					     }) .create();
				
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

	public void onClick(DialogInterface dialog, int id)
	{
		switch (id)
        {
        	case 0:
        		showDialog(HELP_DIALOG);
        		break;
        	case 1:
        		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        		emailIntent.setType( "plain/text");
        		emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.dev_email_address)});
        		emailIntent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.dev_email_subject));
        		try{
					startActivity(emailIntent);
				}
				catch (ActivityNotFoundException e){
					showDialog(EMAIL_ERROR_DIALOG);
				}
        		break;
        	default:
        		dialog.cancel();
        }
		
	}

}
