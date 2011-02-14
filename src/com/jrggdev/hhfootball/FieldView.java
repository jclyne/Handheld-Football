package com.jrggdev.hhfootball;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * FieldView: a custom view that implements a hand held football
 * field. It constructs a grid of field locations and stores bitmaps for all
 * possible entities, referred to as tiles at the locations. It can handle
 * collision detection as well.
 * 
 */
public class FieldView extends View
{
	// Logging Tag
	private static final String TAG = "FieldView";
	
	/**
	 * These parameters handle the dimensions of the field
	 */

	/**
	 * Stores the square dimension, in pixels, of the tiles. All new tile
	 * bitmaps should be initially saved as 32x32. When the field is resized,
	 * the bitmaps will scaled appropriately
	 */
	private int mTileSize = 32;

	/** Represents the dimensions of the field tile grid */
	private int mXTileCount = 10;
	private int mYTileCount = 3;

	/**
	 * Represents the bounding rectangle of the field view, represented in
	 * pixels relative the the view coordinates.
	 */
	private Rect mViewRect;

	/**
	 * Bounding rectangle of the end zones
	 */
	private Rect mHomeEndzoneRect;
	private Rect mVistorEndzoneRect;
	
	/**
	 * Bounding rectangle for the field of play
	 */
	private Rect mFieldRect;
	
	/**
	 * Represents the pixel width of the field lines
	 */
	private static int mFieldLineWidth = 1;
	
	/**
	 * Bitmap that is holds the current field, scaled for the size of the top level view
	 */
	private Bitmap mFieldBitmap;
	private Drawable mFieldBackground;
	private Drawable mHomeEndzoneBackground;
	private Drawable mVisitorEndzoneBackground;

	/**
	 * A hash of Drawables, specified by the subclasser in setTile, of all the possible image
	 * values a tile can have. These will be later be converted into scaled bitmaps that represent
	 * the computed tilesize
	 */
	private Drawable[] mTileArray;
	
	/**
	 * A hash that represents the scaled bitmaps the mTileArray field
	 */
	private Bitmap[] mTileBitmapArray;

	/**
	 * A two-dimensional array of integers in which the number represents the
	 * index of the tile image that should be drawn and that corresponding coordinate on the field
	 */
	private int[][] mTileGrid;

	/**
	 * A paint that is used to  dynamically draw the generated field bitmaps 
	 */
	private final Paint mPaint = new Paint();

	/** 
	 * Constructor
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public FieldView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setFocusable(true);
		mTileGrid = new int[mXTileCount][mYTileCount];
	}

	/**
	 * Constructor
	 * @param context
	 * @param attrs
	 */
	public FieldView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setFocusable(true);
		mTileGrid = new int[mXTileCount][mYTileCount];
	}

	/**
	 * Handles scales the field bitmaps to fit in to the size of the top level view
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		Log.i(TAG,"onSizeChanged, resizing field dimensions");
		/*
		 * First calculate the size of the tiles. This is based on the size on
		 * pixel dimensions available and the desired number of tiles. The grid
		 * dimensions remain fixed across resizing
		 */
		int tilew = (int) Math.floor(w / (mXTileCount + 1)); // We add 1 for the
																// end zones
		int tileh = (int) Math.floor(h / mYTileCount);
		if (tilew < tileh)
		{
			mTileSize = tilew;
		}
		else
		{
			mTileSize = tileh;
		}
		
		/*
		 * Once the tilesize is known, the bounding rectangle for the entire field
		 * can be computed. This will center the rectangle in the top level view
		 */
		int mXOffset = ((w - (mTileSize * (mXTileCount + 1))) / 2);
		int mYOffset = ((h - (mTileSize * mYTileCount)) / 2);

		mViewRect = new Rect(mXOffset, mYOffset, mXOffset + (mXTileCount + 1)
				* mTileSize, mYOffset + mYTileCount * mTileSize);

		/*
		 * With the fieldview rectangle computed, the endzone and playing field
		 * rectangles are computed. These rectangle coordinates are relative to the 
		 * top level view rectangle, mViewRect.
		 */
		mHomeEndzoneRect = new Rect(0, 0, mTileSize / 2, mViewRect.height() - 1);
		mFieldRect = new Rect(mHomeEndzoneRect.right, 0, mHomeEndzoneRect.right
				+ (mXTileCount * mTileSize), mViewRect.height() - 1);
		mVistorEndzoneRect = new Rect(mFieldRect.right, 0, mFieldRect.right
				+ mTileSize / 2, mViewRect.height() - 1);
	
		/*
		 * Now draw the field into mFieldBitmap. 
		 */
		drawFieldBitmap();
		for (int i = 0; i < mTileBitmapArray.length; i++)
		{
			if (mTileBitmapArray[i] != null)
			{
				mTileBitmapArray[i] = scaleTileBitmap(mTileArray[i]);
			}
		}
		clearTiles();
		
		
		dumpFieldDimensions();
	}

	/**
	 * Handles drawing the field bitmap and the grid of tile images in the field of play
	 */
	@Override
	public void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		canvas.drawBitmap(mFieldBitmap, mViewRect.left, mViewRect.top,
				mPaint);

		for (int x = 0; x < mXTileCount; x++)
		{
			for (int y = 0; y < mYTileCount; y++)
			{
				if (mTileGrid[x][y] > 0)
				{
					canvas.drawBitmap(mTileBitmapArray[mTileGrid[x][y]],
							mViewRect.left+mFieldRect.left + (x * mTileSize)+mFieldLineWidth,
							mViewRect.top+mFieldRect.top + (y * mTileSize)+mFieldLineWidth, mPaint);
				}
			}
		}
	}
	
	/**
	 * Constructs the mFieldBitmap, according to the mViewRect size, and
	 * an associated canvas to draw into the bitmap. It then calls the drawing
	 * routines that actually draw on the canvas
	 * @pre mViewRect != NULL
	 * @pre mFieldRect != NULL
	 * @pre mHomeEndzoneRect != NULL
	 * @pre mVistorEndzoneRect != NULL
	 */
	private void drawFieldBitmap()
	{
		mFieldBitmap = Bitmap.createBitmap(mViewRect.width(),
				mViewRect.height(), Bitmap.Config.ARGB_8888);
		
		Canvas canvas = new Canvas(mFieldBitmap);

		drawFieldofPlay(canvas, mFieldRect,mFieldBackground);

		// Draw the home end zone
		drawEndZone(canvas, mHomeEndzoneRect, mHomeEndzoneBackground);
		drawEndZone(canvas, mVistorEndzoneRect, mVisitorEndzoneBackground);

	}

	/**
	 * Draws the field of play grid onto the supplied canvas
	 * @param canvas Canvas to draw into
	 * @param rect Bounding rectangle of the field of play within
	 * the canvas
	 */
	private void drawFieldofPlay(Canvas canvas, Rect rect,Drawable background)
	{
		Paint paint = new Paint();
		
		if (background == null)
		{
			paint.setARGB(255, 70, 180, 70);
			paint.setStyle(Style.FILL);
			canvas.drawRect(rect, paint);
		}
		else
		{
			background.setBounds(rect);
			background.draw(canvas);
		}

		paint.setARGB(255, 255, 255, 255);
		paint.setStrokeWidth(mFieldLineWidth);
		paint.setStyle(Style.STROKE);
		canvas.drawRect(rect, paint);

		for (int x = 0; x <= mXTileCount; x++)
		{
			canvas.drawLine(rect.left + x * mTileSize, 0, rect.left + x
					* mTileSize, rect.width() - 1, paint);
			for (int y = 1; y < mYTileCount; y++)
			{
				canvas.drawLine(rect.left + (x * mTileSize) - (mTileSize / 4),
						rect.top + y * mTileSize, rect.left + (x * mTileSize)
								+ (mTileSize / 4), rect.top + y * mTileSize,
						paint);
			}
		}
	}

	/**
	 * Draws an end zone onto the supplied canvas
	 * @param canvas Canvas to draw into
	 * @param rect Bounding rectangle of the field of play within
	 * the canvas
	 */
	private void drawEndZone(Canvas canvas, Rect rect,Drawable background)
	{
		Paint paint = new Paint();
		
		if (background == null)
		{
			paint.setARGB(255, 255, 0, 0);
			paint.setStyle(Style.FILL);
			canvas.drawRect(rect, paint);
			
			paint.setARGB(255, 255, 255, 255);
			paint.setStrokeWidth(mFieldLineWidth);
			paint.setStyle(Style.STROKE);
			canvas.drawRect(rect, paint);

			int linecnt = rect.height() / rect.width();
			if (linecnt > 0)
			{
				for (int y = 0; y < linecnt; y++)
				{
					canvas.drawLine(rect.left, y * rect.width(),
							rect.left + rect.width(),
							y * rect.width() + rect.width(), paint);
				}
			}
		}
		else
		{
			background.setBounds(rect);
			background.draw(canvas);
			
			paint.setARGB(255, 255, 255, 255);
			paint.setStrokeWidth(mFieldLineWidth);
			paint.setStyle(Style.STROKE);
			canvas.drawRect(rect, paint);
		}
	}

	/**
	 * Generates a bitmap to fit into the field of play grid from the specified drawable. This
	 * should be called when a new tile image is set and when the view gets resized
	 * @param tile Drawable to draw into the bitmap
	 * @return generated bitmap
	 */
	private Bitmap scaleTileBitmap(Drawable tile)
	{
		Bitmap bitmap = Bitmap.createBitmap(mTileSize - mFieldLineWidth * 2,
				mTileSize - mFieldLineWidth * 2, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		tile.setBounds(0, 0, mTileSize - mFieldLineWidth * 2, mTileSize
				- mFieldLineWidth * 2);
		tile.draw(canvas);
		return bitmap;
	}

	public void setFieldBackground(Drawable drawable)
	{
		mFieldBackground=drawable;
	}
	
	public void setEndZoneBackground(Drawable homeEndZone, Drawable visitorEndZone)
	{
		mHomeEndzoneBackground=homeEndZone;
		mVisitorEndzoneBackground=visitorEndZone;
	}

	/**
	 * Function to set the specified Drawable as the tile for a particular
	 * integer key.
	 * 
	 * @param key Key to associated specified drawable to a field of play grid coordinate
	 * @param tile Drawable associated with specified key
	 */
	public void loadTile(int key, Drawable tile)
	{
		mTileArray[key] = tile;
		mTileBitmapArray[key] = scaleTileBitmap(tile);
	}

	/**
	 * Clears the field of play by setting each tile in the grid to empty
	 * 
	 */
	public void clearTiles()
	{
		for (int x = 0; x < mXTileCount; x++)
		{
			for (int y = 0; y < mYTileCount; y++)
			{
				setTile(0, x, y);
			}
		}
	}
	
	/**
	 * Returns the number of tiles long (between the end zones) the playing field is
	 */
	public int getFieldLength()
	{
		return mXTileCount;
	}
	
	/**
	 * Returns the number of tiles wide (between the boundaries) the playing field is
	 */
	public int getFieldWidth()
	{
		return mYTileCount;
	}

	/**
	 * Resets the internal array of drawables used for drawing thetiles, and sets the
	 * maximum index of tiles to be inserted. This needs to be called before setTile can be called
	 * 
	 * @param tilecount
	 */

	public void resetTiles(int tilecount)
	{
		mTileArray = new Drawable[tilecount];
		mTileBitmapArray = new Bitmap[tilecount];
	}
	
	/**
	 * Used to indicate that a particular tile (set with loadTile and referenced
	 * by an integer) should be drawn at the given x/y coordinates during the
	 * next invalidate/draw cycle.
	 * 
	 * @param tileindex
	 * @param x
	 * @param y
	 */
	public void setTile(int tileindex, int x, int y)
	{
		mTileGrid[x][y] = tileindex;

	}

	/**
	 * Routine that dumps the current field dimensions and bounding rectangles to
	 * stdout
	 */
	public void dumpFieldDimensions()
	{
		Log.i(TAG,"mViewRect: "+mViewRect.toShortString());
		Log.i(TAG,"mHomeEndzoneRect: "+mHomeEndzoneRect.toShortString());
		Log.i(TAG,"mVistorEndzoneRect: "+mVistorEndzoneRect.toShortString());
		Log.i(TAG,"mFieldRect: "+mFieldRect.toShortString());
		Log.i(TAG,"mTileSize: "+mTileSize);
		Log.i(TAG,"mXTileCount: "+mXTileCount);
		Log.i(TAG,"mYTileCount: "+mYTileCount);
		Log.i(TAG,"mFieldLineWidth: "+mFieldLineWidth);
		
	}
	

}
