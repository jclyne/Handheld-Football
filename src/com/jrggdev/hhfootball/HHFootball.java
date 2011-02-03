package com.jrggdev.hhfootball;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

public class HHFootball extends Activity
{
	private static final int ABOUT_DIALOG=1;
	private AlertDialog mAboutDialog;
	private boolean mQuiet;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		SharedPreferences settings = 
				PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mQuiet=settings.getBoolean("sound", mQuiet);
		mAboutDialog = new AlertDialog.Builder(this) 
				.setMessage("ABOUT")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			     }).create();
		
		setContentView(R.layout.splash);
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
