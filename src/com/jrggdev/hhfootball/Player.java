package com.jrggdev.hhfootball;

import java.util.Iterator;
import java.util.NoSuchElementException;

import android.os.Bundle;

import com.jrggdev.Coordinate;


abstract class Player
{
	private Coordinate mPos;
	private boolean mFlashing=false;
	private Team mTeam;
	
	enum Position{
		Quarterback,
		Receiver,
		Defender
	}
	
	public Player(Team team)
	{
		mPos=new Coordinate(-1,-1);
		mTeam=team;
	}
	
	public Player(Team team, int newX, int newY)
	{
		mPos=new Coordinate(newX,newY);
		mTeam=team;
	}
	
	public Player(Team team,Bundle bundle)
	{
		mPos=new Coordinate(bundle.getInt("xpos"),bundle.getInt("ypos"));
		mFlashing=bundle.getBoolean("mFlashing");
		mTeam=team;
	}
	
	public Player(Player copy)
	{
		mPos=new Coordinate(copy.pos());
		mFlashing=copy.mFlashing;
	}

	boolean isVisibile()
	{
		return ( mPos.x >= 0 && mPos.y >= 0);
	}
	
	public Coordinate pos() 
	{
		return mPos;
	}
	
	@Override
	public String toString()
	{
		return mPos.toString();
	}
	
	public boolean equals(Player other)
	{
		try 
		{
			return equals(other.mPos.x,other.mPos.y);
		} 
		catch (NullPointerException e) 
		{
			return false;
		}
	}
	
	public boolean equals(int x, int y)
	{
		return mPos.equals(x,y);
	}

	public final boolean isFlashing() { return mFlashing; }
	public void setFlashing(boolean flashing) { mFlashing = flashing; }
	public Team team() { return mTeam; }
	public abstract Position position();
	
	public void set(int newX, int newY)
	{		
		mPos.x = newX;
		mPos.y = newY;
	}
	
	public Bundle serialize()
	{
		Bundle bundle = new Bundle();
		bundle.putInt("xpos", mPos.x);
		bundle.putInt("ypos", mPos.y);
		bundle.putBoolean("mFlashing", mFlashing);
		
		return bundle;
	}
}

class PlayerIterator implements Iterator<Player>
{
	private int idx=0;
	private int mSize;
	private Player[] mPlayers;
	
	public PlayerIterator(Player[] players,int size)
	{
		mPlayers=players;
		mSize= size;
	}
	
	public boolean hasNext()  { return idx < mSize; }

	public Player next() 
	{
	    if(idx == mSize)
	    	throw new NoSuchElementException();

	    return mPlayers[idx++];
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
}

abstract class OffensivePlayer extends Player
{

	public OffensivePlayer(Team team)
	{
		super(team);
	}

	public OffensivePlayer(Team team, int newX, int newY)
	{
		super(team, newX, newY);
	}
	

	public OffensivePlayer(Player copy)
	{
		super(copy);
	}
	
	public OffensivePlayer(Team team, Bundle bundle)
	{
		super(team, bundle);
	}
	
	public Bundle serialize()
	{
		Bundle bundle =  super.serialize();
		return bundle;
	}
	
}

class Quarterback extends OffensivePlayer
{
	public Quarterback(Team team)
	{
		super(team);
	}
	
	
	public Quarterback(Team team, int newX, int newY)
	{
		super(team, newX, newY);
	}
	
	public Quarterback(Player copy)
	{
		super(copy);
	}
	
	public Quarterback(Team team, Bundle bundle)
	{
		super(team, bundle);
	}
	
	public Position position() 
	{ 
		return Position.Quarterback; 
	}
	
	public Bundle serialize()
	{
		Bundle bundle =  super.serialize();
		return bundle;
	}
	
}

class Receiver extends OffensivePlayer
{
	public Receiver(Team team)
	{
		super(team);
	}
	
	public Receiver(Team team, int newX, int newY)
	{
		super(team, newX, newY);
	}
	
	public Receiver(Player copy)
	{
		super(copy);
	}
	
	public Receiver(Team team, Bundle bundle)
	{
		super(team, bundle);
	}
	
	public Position position()
	{ 
		return Position.Receiver; 
	}
	
	public Bundle serialize()
	{
		Bundle bundle =  super.serialize();
		return bundle;
	}
	
}

class DefensivePlayer extends Player
{
	public DefensivePlayer(Team team)
	{
		super(team);
	}
	
	public DefensivePlayer(Team team, int newX, int newY)
	{
		super(team, newX, newY);
	}
	
	public DefensivePlayer(Player copy)
	{
		super(copy);
	}
	
	public DefensivePlayer(Team team, Bundle bundle)
	{
		super(team, bundle);
	}
	
	public Position position() 
	{ 
		return Position.Defender; 
	}
	
	public Bundle serialize()
	{
		Bundle bundle =  super.serialize();
		return bundle;
	}
	
}