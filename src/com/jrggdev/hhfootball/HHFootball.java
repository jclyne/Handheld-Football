package com.jrggdev.hhfootball;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class HHFootball extends Activity
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
	}
	
	public void onNewGame(View view)
	{
		Intent intent = new Intent();
		intent.setClass(this,Game.class);
		startActivity(intent);
	}
	
	public void onSettings(View view)
	{
		
	}
	
	public void onAbout(View view)
	{
		
	}
	
	public void onExit(View view)
	{
		finish();
	}

}
