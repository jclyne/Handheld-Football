package com.jrggdev;

import java.util.LinkedList;
import java.util.Queue;

import android.view.animation.Animation;
import android.widget.TextView;


public class TextViewAnimator implements Animation.AnimationListener,Timer.TimerHandler
{
	TextView mTextView;
	Animation mInAnim;
	Animation mOutAnim;
	
	Timer mTimer;
	
	class TextDisplay
	{
		private String mText;
		private int mDisplayMillis;
		
		TextDisplay(String text )
		{
			mText = text;
			mDisplayMillis = 0;
		}
		
		TextDisplay(String text, int displayMillis )
		{
			mText = text;
			mDisplayMillis = displayMillis;
		}
		
		public final String getText() { return mText; }
		public final int getDisplayMillis() { return mDisplayMillis; }
	}
	Queue<TextDisplay> mTextQueue;
	
	public TextViewAnimator(TextView textView,Animation inAnim,Animation outAnim)
	{
		mTextView=textView;
		mInAnim=inAnim;
		mOutAnim=outAnim;
		
		mTextQueue = new LinkedList();
		mInAnim.setAnimationListener(this);
		mOutAnim.setAnimationListener(this);
		mTimer=new Timer(this);
	}
	
	public void onAnimationEnd(Animation animation) 
	{
		if (animation == mOutAnim)
		{
			mTextView.setText("");
			if (! mTextQueue.isEmpty() )
			{
				mTextView.startAnimation(mInAnim);
				mTextView.invalidate();
				
			}
		}
	}

	public void onAnimationRepeat(Animation animation) 
	{
		
	}

	public void onAnimationStart(Animation animation) 
	{
		if (animation == mInAnim)
		{
			TextDisplay disp=mTextQueue.remove();
			mTextView.setText(disp.getText());
			if (disp.getDisplayMillis() > 0)
				mTimer.start(disp.getDisplayMillis());
		}
	}
	
	public boolean HandleTimer() 
	{
		clearText();
		return false;
	}

	public void setText(String text) 
	{
		mTextQueue.add(new TextDisplay(text,0));
		mTextView.startAnimation(mInAnim);
		mTextView.invalidate();
	}
	
	public void setText(String text,int millis) 
	{
		mTextQueue.add(new TextDisplay(text,millis));
		mTextView.startAnimation(mInAnim);
		mTextView.invalidate();
	}
	
	public void clearText() 
	{
		mTextView.startAnimation(mOutAnim);
		mTextView.invalidate();
	}
}
