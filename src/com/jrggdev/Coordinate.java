package com.jrggdev;

import android.os.Bundle;

public class Coordinate
{
	public int x;
	public int y;
	
	public Coordinate(int x, int y)
	{
		this.x=x;
		this.y=y;
	}

	public Coordinate(Coordinate pos)
	{
		this.x=pos.x;
		this.y=pos.y;
	}
	
	public Coordinate(Bundle bundle)
	{
		x=bundle.getInt("x");
		y=bundle.getInt("y");
	}
	
	public Bundle serialize()
	{
		Bundle bundle = new Bundle();
		bundle.putInt("x", x);
		bundle.putInt("y", y);
		
		return bundle;
	}
	
	public boolean equals(int x, int y)
	{
		return (x == this.x && y == this.y);
	}
	
	@Override
	public String toString()
	{
		return "Coordinate: [" + x + "," + y + "]";
	}
	
}
