package com.jrggdev.hhfootball;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import junit.framework.Assert;


/**
 * Simple class containing two integer values and a comparison function. There's
 * probably something I should use instead, but this was quick and easy to
 * build.
 * 
 */
abstract class Player
{
	public int x;
	public int y;

	private boolean mFlashing=false;
	private Team mTeam;
	
	enum Position{
		Quarterback,
		Receiver,
		Defender
	}
	
	public Player(Team team)
	{
		x = -1;
		y = -1;
		mTeam=team;
	}
	
	public Player(Team team, int newX, int newY)
	{
		x = newX;
		y = newY;
		mTeam=team;
	}
	
	public Player(Player copy)
	{
		x = copy.x;
		y = copy.y;
		mFlashing=copy.mFlashing;
	}

	@Override
	public String toString()
	{
		return "Player: [" + x + "," + y + "]";
	}
	
	public boolean equals(Player other)
	{
		try 
		{
			return equals(other.x,other.y);
		} 
		catch (NullPointerException e) 
		{
			return false;
		}
	}
	
	public boolean equals(int x, int y)
	{
		return (x == this.x && y == this.y);
	}

	public final boolean isFlashing() { return mFlashing; }
	public void setFlashing(boolean flashing) { mFlashing = flashing; }
	public Team team() { return mTeam; }
	public abstract Position position();
	
	public void set(int newX, int newY)
	{		
		x = newX;
		y = newY;
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
	
	public Position position() 
	{ 
		return Position.Quarterback; 
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
	
	public Position position()
	{ 
		return Position.Receiver; 
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
	
	public Position position() 
	{ 
		return Position.Defender; 
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
	
	
	protected abstract int[][] getPreSnapFormation();
}

class Offense extends Team
{
	private int QUARTERBACK=0;
	private int RECEIVER=1;
	
	private static int[][] preSnapFormation= {{2,1},{-1,-1}}; 
	public Offense(int side,int orientation)
	{
		super(2,side,orientation);
		mPlayers[QUARTERBACK]=new Quarterback(this);
		mPlayers[RECEIVER]=new Receiver(this);
	}
	
	public final Quarterback quarterback() { return (Quarterback)mPlayers[QUARTERBACK]; }
	public final Receiver receiver() { return (Receiver)mPlayers[RECEIVER]; }
	
	protected int[][] getPreSnapFormation()
	{
		return preSnapFormation;
	}
}

class Defense extends Team
{
	private static int[][] preSnapFormation= {{6,0},{6,1},{6,2},{4,1},{2,0},{0,2}}; 
	
	public Defense(int side,int orientation)
	{
		super(6,side,orientation);
		for ( int i=0;i<size();i++)
			mPlayers[i]=new DefensivePlayer(this);
	}
	
	DefensivePlayer getDefender(int idx) { assert(idx < size()); return (DefensivePlayer) mPlayers[idx]; }
	
	protected int[][] getPreSnapFormation()
	{
		return preSnapFormation;
	}
}