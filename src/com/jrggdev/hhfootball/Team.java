package com.jrggdev.hhfootball;

import java.util.Random;

import android.os.Bundle;

abstract class Team implements Iterable<Player>
{
	public static final int SIDE_HOME=0;
	public static final int SIDE_VISITOR=1;
	
	public static final int ORIENTATION_LEFT=0;
	public static final int ORIENTATION_RIGHT=1;
	
	private int mSize;
	protected Player[] mPlayers;
	private int mSide;
	private int mOrientation;
	
	private Random rand = new Random();
	
	public Team(int size,int side,int orientation)
	{
		mSize=size;
		mSide=side;
		mOrientation=orientation;
		mPlayers=new Player[size];
	}
	
	public Team(Bundle bundle)
	{
		mSize=bundle.getInt("mSize");
		mSide=bundle.getInt("mSide");
		mOrientation=bundle.getInt("mOrientation");
		mPlayers=new Player[mSize];
	}
	
	public PlayerIterator iterator() { return new PlayerIterator(mPlayers,mSize); }
	public Player getPlayer(int idx) { assert(idx< mSize); return mPlayers [idx]; }
	public Player getRandomPlayer()
	{
		return getPlayer(rand.nextInt(mSize));
	}
	
	public Player findPlayer(Player player)
	{
		return findPlayer(player.x,player.y);
	}
	
	public Player findPlayer(int x, int y)
	{
		for (int i=0;i<size();i++)
		{
			if ( mPlayers[i].equals(x,y) )
			{
				return mPlayers[i];
			}
		}
		return null;
	}
	
	public void setSide(int side) { mSide= side; }
	public final int side() { return mSide; }
	
	public void setOrientation(int orientation) { mOrientation= orientation; }
	public final int orientation() { return mOrientation; }
	
	public final int size() { return mSize; }
	
	public Bundle serialize()
	{
		Bundle bundle=new Bundle();
		bundle.putInt("mSize", mSize);
		bundle.putInt("mSide", mSide);
		bundle.putInt("mOrientation", mOrientation);
		for (int i=0;i<size();i++)
			bundle.putBundle("player"+i, mPlayers[i].serialize());
			
		return bundle;
	}
	
	protected abstract int[][] getPreSnapFormation();
}

class Offense extends Team
{
	private static final int QUARTERBACK=0;
	private static final int RECEIVER=1;
	
	private static final int[][] preSnapFormation= {{2,1},{-1,-1}}; 
	public Offense(int side,int orientation)
	{
		super(2,side,orientation);
		mPlayers[QUARTERBACK]=new Quarterback(this);
		mPlayers[RECEIVER]=new Receiver(this);
	}
	
	public Offense(Bundle bundle)
	{
		super(bundle);
		mPlayers[QUARTERBACK]=new Quarterback(this,bundle.getBundle("player"+QUARTERBACK));
		mPlayers[RECEIVER]=new Receiver(this,bundle.getBundle("player"+RECEIVER));
	}
	
	public final Quarterback quarterback() { return (Quarterback)mPlayers[QUARTERBACK]; }
	public final Receiver receiver() { return (Receiver)mPlayers[RECEIVER]; }
	
	protected int[][] getPreSnapFormation()
	{
		return preSnapFormation;
	}
	
	public Bundle serialize()
	{
		Bundle bundle = super.serialize();
		return bundle;
	}
}

class Defense extends Team
{
	private static final int[][] preSnapFormation= {{6,0},{6,1},{6,2},{4,1},{2,0},{0,2}}; 
	
	public Defense(int side,int orientation)
	{
		super(6,side,orientation);
		for ( int i=0;i<size();i++)
			mPlayers[i]=new DefensivePlayer(this);
	}
	
	public Defense(Bundle bundle)
	{
		super(bundle);
		for ( int i=0;i<size();i++)
			mPlayers[i]=new DefensivePlayer(this,bundle.getBundle("player"+i));
		
	}
	
	DefensivePlayer getDefender(int idx) { assert(idx < size()); return (DefensivePlayer) mPlayers[idx]; }
	
	protected int[][] getPreSnapFormation()
	{
		return preSnapFormation;
	}
	
	public Bundle serialize()
	{
		Bundle bundle =  super.serialize();
		return bundle;
	}
}