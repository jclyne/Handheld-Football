package com.jrggdev.hhfootball;

import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jrggdev.Percentage;
import com.jrggdev.TextViewAnimator;
import com.jrggdev.Timer;

/**
 * HHFootball: a simple game that everyone can enjoy.
 * 
 * This is an implementation of the classic Game "HHFootball", in which you
 * control a serpent roaming around the garden looking for apples. Be careful,
 * though, because when you catch one, not only will you become longer, but
 * you'll move faster. Running into yourself or the walls will end the game.
 * 
 */
public class Game extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private static String TAG = "HHFootball";
	
	/** Child view definitions */
	private FieldView mFieldView;
	private TextView mDriveView;
	private TextView mFieldPosView;
	private TextViewAnimator mInfoView;
	private TextView mHomeScoreView;
	private TextView mVisitorScoreView;
	private ProgressBar mKickMeter;

	/** Menus Items */
	private static final int MENU_NEW_GAME=0;
	private static final int MENU_SETTINGS=1;
	private static final int MENU_QUIT=2;
	
	/** Defines whether the game is paused or not */
		
	/** Game States */
	protected enum State 
	{ 
		PLAY_LIVE,PLAY_DEAD,PRE_SNAP,PASS;
	}

	/**
	 * Labels for the drawables that will be loaded into the TileView class
	 */
	private static final int HOME_LEFT = 1;
	private static final int HOME_RIGHT = 2;
	private static final int VISITOR_LEFT = 3;
	private static final int VISITOR_RIGHT = 4;
	private static final int FOOTBALL = 5;
	
	private static final int[][] mBitmapLookup = {{HOME_LEFT,HOME_RIGHT},{VISITOR_LEFT,VISITOR_RIGHT}};
	
	private boolean mQuiet=false;
	/**
	 * Represents time left in the period in milliseconds
	 */
	private int mPeriodLengthMins=2;
	
	private enum Difficulty
	{
		easy(25,10,5),
		medium(25,10,25),
		hard(50,10,50);
		
		private Percentage mDefenderMoves;
		private Percentage mReceiverMoves;
		private Percentage mDefenderTackles;
		
		private Difficulty(int perDefenderMoves, int perReceiverMoves, int perDefenderTackles)
		{
			mDefenderMoves = new Percentage(perDefenderMoves);
			mReceiverMoves = new Percentage(perReceiverMoves);
			mDefenderTackles= new Percentage(perDefenderTackles);
		}
		
		public Percentage perDefenderMoves() {  return mDefenderMoves; }
		public Percentage perReceiverMoves() {  return mReceiverMoves; }
		public Percentage perDefenderTackles() {  return mDefenderTackles; }

	}
	
	private Difficulty mDifficulty=Difficulty.medium;

	private static final int mGameRefreshRate=100;
	private Timer mGameUpdater = new Timer(mGameRefreshRate,new Timer.TimerHandler() {
		private boolean mFlashToggle=false;
		public boolean HandleTimer() { 
			mFlashToggle=!mFlashToggle;
			Game.this.updateGame(mFlashToggle);
			return true;
		} 
	});
	
	private int mAiUpdateRate=250;
	private Timer mAiUpdater = new Timer(mAiUpdateRate,new Timer.TimerHandler() {
		public boolean HandleTimer() { 			
			return Game.this.OnUpdateGameAI(); 
		} 
	});
	
	private static final int mInfoDuration=1500;
	
	/**
	 * Game settings
	 */
	
	private static final int mTouchbackPos=20;
	private static final int mDownsPerSeries=4;
	private static final int mYardsForFirstDown=10;
	private static final int mStartingXPos=3;
	
	private State mState;
	private int mHomeScore;
	private int mVisitorScore;
	private GameClock mGameClock;
	private int mPeriod;
	private int mFieldPos;
	private int mSeriesDown;
	private int mLineOfScrimmage;
	private int mFirstDownPos;
	
	enum GameState
	{
		KICKOFF,
		FREE_KICK,
		DRIVE_IN_PROGRESS,
		TURNOVER_ON_DOWNS,
		INCOMPLETE,
		INTERCEPTION,
		FUMBLE,
		TOUCHDOWN,
		FIELD_GOAL,
		SAFETY;
		
		public boolean isTurnover()
		{
			switch (this)
			{
				case TURNOVER_ON_DOWNS:
				case INTERCEPTION:
				case FUMBLE:
					return true;
				default:
					return false;
			}
		}
	}
	
	private GameState mGameState;
	
	/**
	 * mQuarterback: current Player of the Quarterback mReceiver: current
	 * coordiante of the Wide Reciever mDefense: List of Players for the
	 * defensive players
	 */
	private Offense mOffense;
	private Defense mDefense;
	
    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     * 
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_NEW_GAME, 0, R.string.menu_new_game);
        menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings);
        menu.add(0, MENU_QUIT, 0, R.string.menu_quit);

        return true;
    }

    /**
     * Invoked when the user selects an item from the Menu.
     * 
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NEW_GAME:
            	startNewGame();
            	return true;
            	
            case MENU_SETTINGS:
            	Intent intent = new Intent();
        		intent.setClass(this,Settings.class);
        		startActivity(intent);
            	return true;
            	
            case MENU_QUIT:
                finish();
                return true;
        }

        return false;
    }
    
	/**
	 * Called when Activity is first created. Turns off the title bar, sets up
	 * the content views, and fires up the HHFootballView.
	 * 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.hhfootball_layout);
		
		mFieldView = (FieldView)findViewById(R.id.hhfootballview);		
		mDriveView = (TextView)findViewById(R.id.drive_view);
		mFieldPosView = (TextView)findViewById(R.id.fieldpos_view);
		mHomeScoreView = (TextView)findViewById(R.id.scoreboard_home);
		mVisitorScoreView = (TextView)findViewById(R.id.scoreboard_visitor);
				
		mKickMeter= (ProgressBar)findViewById(R.id.kick_meter);
		assert(mKickMeter.isIndeterminate() == false);
		mInfoView = new TextViewAnimator((TextView)findViewById(R.id.info_view),
											AnimationUtils.loadAnimation(this, R.anim.scroll_in),
											AnimationUtils.loadAnimation(this, R.anim.scroll_out));
		
		// Get the current settings values
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mQuiet=settings.getBoolean("sound", mQuiet);
		mPeriodLengthMins=Integer.parseInt(settings.getString("period_length", 
											String.valueOf(mPeriodLengthMins)));
		mDifficulty = Difficulty.valueOf(settings.getString("difficulty", mDifficulty.name()));
		
		settings.registerOnSharedPreferenceChangeListener(this);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPostCreate(android.os.Bundle)
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) 
	{
		super.onPostCreate(savedInstanceState);
		Resources r = getBaseContext().getResources();

		mFieldView.setFieldBackground(r.getDrawable(R.drawable.field));
		mFieldView.setEndZoneBackground(r.getDrawable(R.drawable.home_endzone),
										r.getDrawable(R.drawable.home_endzone) );
		
		mFieldView.resetTiles(6);
		mFieldView.loadTile(HOME_LEFT, r.getDrawable(R.drawable.home_left));
		mFieldView.loadTile(HOME_RIGHT, r.getDrawable(R.drawable.home_right));
		mFieldView.loadTile(VISITOR_LEFT, r.getDrawable(R.drawable.visitor_left));
		mFieldView.loadTile(VISITOR_RIGHT, r.getDrawable(R.drawable.visitor_right));
		mFieldView.loadTile(FOOTBALL, r.getDrawable(R.drawable.football));
		
		if (savedInstanceState != null)
		{
			// We are being restored
			Bundle map = savedInstanceState.getBundle(TAG);
			if (map != null)
			{
				restoreState(map);
			}
		}
		else
		{
			startNewGame();
		}
	}

	
	@Override
	protected void onPause()
	{
		Log.i(TAG,"Activity Paused");
		super.onPause();
		mGameClock.stop();
		mAiUpdater.stop();
		mGameUpdater.stop();
	}

	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG,"Activity Resumed");
		mGameUpdater.start();
		updateDriveStatus();
		switch (mState)
		{
			case PLAY_LIVE:
			case PASS:
				mGameClock.start();
				mAiUpdater.start();
				return;
			default:
				return;	
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// Store the game state
		Bundle map = new Bundle();
		
		map.putInt("mState",mState.ordinal());
		map.putInt("mGameState",mGameState.ordinal());
		map.putInt("mHomeScore",mHomeScore);
		map.putInt("mVisitorScore",mVisitorScore);
		map.putBundle("mGameClock",mGameClock.serialize());
		map.putInt("mPeriodLengthMins",mPeriodLengthMins);
		map.putInt("mPeriod",mPeriod);
		map.putInt("mLineOfScrimmage",mLineOfScrimmage);
		map.putInt("mFieldPos",mFieldPos);
		map.putInt("mSeriesDown",mSeriesDown);
		map.putInt("mFirstDownPos",mFirstDownPos);

		map.putBundle("offense", mOffense.serialize());
		map.putBundle("defense", mDefense.serialize());
		
		outState.putBundle(TAG, map);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences settings, String key)
	{
		if (key.equals("sound"))
		{
			mQuiet=settings.getBoolean("sound", mQuiet);
		}
//		else if (key.equals("period_length"))
//		{
//			mPeriodLengthMins=Integer.parseInt(settings.getString("period_length", 
//												String.valueOf(mPeriodLengthMins)));
//		}
		else if (key.equals("difficulty"))
		{
			mDifficulty = Difficulty.valueOf(settings.getString("difficulty", mDifficulty.name()));
		}
		
	}
	

	/**
	 * Restore game state if our process is being relaunched
	 * 
	 * @param savedState
	 *            a Bundle containing the game state
	 */
	public void restoreState(Bundle savedState)
	{
		mState=State.values()[savedState.getInt("mState")];
		mGameState=GameState.values()[savedState.getInt("mGameState")];
		mHomeScore=savedState.getInt("mHomeScore");
		mVisitorScore=savedState.getInt("mVisitorScore");
		mGameClock=new GameClock(savedState.getBundle("mGameClock"),
				(TextView)findViewById(R.id.scoreboard_clock),
				(TextView)findViewById(R.id.scoreboard_period));
		mPeriod=savedState.getInt("mPeriod");
		mPeriodLengthMins=savedState.getInt("mPeriodLengthMins");
		mLineOfScrimmage=savedState.getInt("mLineOfScrimmage");
		mFieldPos=savedState.getInt("mFieldPos");
		mSeriesDown=savedState.getInt("mSeriesDown");
		mFirstDownPos=savedState.getInt("mFirstDownPos");
		mOffense=new Offense(savedState.getBundle("offense"));
		mDefense=new Defense(savedState.getBundle("defense"));
	}
	
	
	/**
	 * Returns the number of tiles long (between the end zones) the playing field is
	 */
	public int getFieldLength()
	{
		return mFieldView.getFieldLength();
	}
	
	/**
	 * Returns the number of tiles wide (between the boundaries) the playing field is
	 */
	public int getFieldWidth()
	{
		return mFieldView.getFieldWidth();
	}
	
	public void startNewGame()
	{
		mOffense = new Offense(Team.SIDE_HOME,Team.ORIENTATION_LEFT);
		mDefense = new Defense(Team.SIDE_VISITOR,Team.ORIENTATION_RIGHT);
		mGameClock = new GameClock(mPeriodLengthMins,
				(TextView)findViewById(R.id.scoreboard_clock),
				(TextView)findViewById(R.id.scoreboard_period));
		mHomeScore=0;
		mVisitorScore=0;
		mGameState=GameState.KICKOFF;
		mGameUpdater.start();
		initPreSnap();
	}
	

	private void initPreSnap()
	{
		handleNewPeriod();	
		
		mLineOfScrimmage=mFieldPos;
		switch (mGameState)
		{
			case KICKOFF:
				handleKickoff();
				break;
			case TURNOVER_ON_DOWNS:
			case INTERCEPTION:
			case FUMBLE:
				handleChangeOfPossesion();
				break;
		}
		
		arrangePreSnapFormation(mOffense);
		arrangePreSnapFormation(mDefense);
		mState=State.PRE_SNAP;
		mInfoView.clearText();
		updateScoreBoard();
		updateDriveStatus();
	}
	
	private void arrangePreSnapFormation(Team team)
	{
		int[][] preSnapFormation= team.getPreSnapFormation();
		for ( int idx=0;idx<team.size();idx++)
		{
			Player player = team.getPlayer(idx);
			player.setFlashing(false);
			if (preSnapFormation[idx][0] == -1 && preSnapFormation[idx][1] == -1)
			{
				player.set(-1,-1);
			}
			else
			{		
				if (team.orientation()==Team.ORIENTATION_RIGHT)
					player.set(preSnapFormation[idx][0],preSnapFormation[idx][1]);
				else
					player.set(getFieldLength()-1-preSnapFormation[idx][0],preSnapFormation[idx][1]);
			}
		}
	}
	
	private void setFirstDownPos()
	{
		if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
		{
			mFirstDownPos=mFieldPos+mYardsForFirstDown;
			if (mFirstDownPos > 100)
				mFirstDownPos=100;
		}
		else
		{
			mFirstDownPos=mFieldPos-mYardsForFirstDown;
			if (mFirstDownPos < 0)
				mFirstDownPos=0;
		}
	}
	
	private boolean checkFirstDown()
	{
		return ( (mOffense.orientation() == Team.ORIENTATION_RIGHT) ? 
					(mFieldPos >= mFirstDownPos) :
						(mFieldPos <= mFirstDownPos));
	}
	
	private int swapFieldPosOrientation(int pos)
	{
		if (pos > 50)
			return 50-(pos-50);
		else
			return 50+(50-pos);
	}
	private void swapOrientation()
	{
		int tmp = mOffense.orientation();
		mOffense.setOrientation(mDefense.orientation());
		mDefense.setOrientation(tmp);
	}
	
	private void swapSides()
	{
		int tmp = mOffense.side();
		mOffense.setSide(mDefense.side());
		mDefense.setSide(tmp);
	}
	
	private void handleNewPeriod()
	{
		if (mGameClock.expired())
		{
			switch (mGameClock.period())
			{
				case END_OF_FIRST_QUARTER:
				case END_OF_THIRD_QUARTER:	
					// End of quarter, just swap sides
					mFieldPos=swapFieldPosOrientation(mFieldPos);
					mLineOfScrimmage=mFieldPos;
					mFirstDownPos=swapFieldPosOrientation(mFirstDownPos);
					swapOrientation();
					mGameClock.set_period();
					mInfoView.setText(getString(R.string.info_change_sides),mInfoDuration);
					break;
				case HALFTIME:	
					mOffense.setSide(Team.SIDE_VISITOR);
					mOffense.setOrientation(Team.ORIENTATION_LEFT);
					mDefense.setSide(Team.SIDE_HOME);
					mDefense.setOrientation(Team.ORIENTATION_RIGHT);
					mGameState=GameState.KICKOFF;
					mGameClock.set_period();
					mInfoView.setText(getString(R.string.info_kickoff),mInfoDuration);
					break;
				case GAME_OVER:
					return;
				default:
					break;
			}
		}
	}
	
	private void handleFirstDown()
	{
		mSeriesDown = 1;
		setFirstDownPos();
	}
	
	private void handleChangeOfPossesion()
	{
		swapSides();
		swapOrientation();
		mSeriesDown = 1;
		setFirstDownPos();
		mGameState=GameState.DRIVE_IN_PROGRESS;
	}
	
	private void handlePlayDead()
	{
		mGameClock.stop();
		mAiUpdater.stop();
		mState = State.PLAY_DEAD;
			
		switch (mGameState)
		{
			case TOUCHDOWN:
				mInfoView.setText(getString(R.string.info_touchdown),mInfoDuration);
				mGameState=GameState.KICKOFF;
				break;
				
			case INTERCEPTION:
				mInfoView.setText(getString(R.string.info_interception),mInfoDuration);
				break;
				
			case INCOMPLETE:
				mInfoView.setText(getString(R.string.info_incomplete),mInfoDuration);
				mGameState=GameState.DRIVE_IN_PROGRESS;
				
			case DRIVE_IN_PROGRESS:
				if (checkFirstDown())
				{
					handleFirstDown();
					mInfoView.setText(getString(R.string.info_first_down),mInfoDuration);
				}
				else if (mSeriesDown == mDownsPerSeries)
				{
					// Turnover on downs
					mGameState=GameState.TURNOVER_ON_DOWNS;
					mInfoView.setText(getString(R.string.info_turnover_on_downs),mInfoDuration);
				}
				else
				{
					switch (++mSeriesDown)
					{
						case 2:
							mInfoView.setText(getString(R.string.info_second_down),mInfoDuration);
							break;
							
						case 3:
							mInfoView.setText(getString(R.string.info_third_down),mInfoDuration);
							break;
									
						case 4:
							mInfoView.setText(getString(R.string.info_fourth_down),mInfoDuration);
							break;
					}
				}
				break;
		}
		
		switch (mGameClock.period())
		{
			case END_OF_FIRST_QUARTER:
				mInfoView.setText(getString(R.string.info_end_of_first_quarter),mInfoDuration);
				break;
			case HALFTIME:
				mInfoView.setText(getString(R.string.info_halftime));
				break;
			case END_OF_THIRD_QUARTER:
				mInfoView.setText(getString(R.string.info_end_of_third_quarter),mInfoDuration);
				break;
			case GAME_OVER:
				mInfoView.setText(getString(R.string.info_game_over));
				mGameUpdater.stop();
				break;
		}
	}
	
	private void handleTouchBack()
	{
		mFieldPos=(mOffense.orientation() == Team.ORIENTATION_RIGHT)?mTouchbackPos:100-mTouchbackPos;
		mLineOfScrimmage=mFieldPos;
		mSeriesDown = 1;
		setFirstDownPos();
	}
	
	private void handleKickoff()
	{
		swapSides();
		swapOrientation();
		handleTouchBack();
		mGameState=GameState.DRIVE_IN_PROGRESS;
	}
	
	private void handleTouchDown()
	{
		if (mOffense.side() == Team.SIDE_HOME)
		{
			mHomeScore+=7;
		}
		else
		{
			mVisitorScore+=7;
		}
		
		mGameState=GameState.TOUCHDOWN;
		updateScoreBoard();
		handlePlayDead();
	}
	
	private Random recRand=new Random();
	private void handleSnap()
	{
		mInfoView.clearText();
		mState = State.PLAY_LIVE;
		mGameClock.start();
		mOffense.receiver().set((mOffense.orientation() == Team.ORIENTATION_RIGHT)?
								mOffense.quarterback().x+2:
									mOffense.quarterback().x-2, recRand.nextInt(3));
		mAiUpdater.start();
	}
	
	private void moveBallCarrierLeft()
	{
		int newX=mOffense.quarterback().x;
		
		if (mOffense.quarterback().x > 0)
		{
			newX-=1;
		}
		else if (mOffense.orientation() == Team.ORIENTATION_LEFT)
		{
			newX=getFieldLength() - 1;
		}
		else
			return;
		
		DefensivePlayer tackler = (DefensivePlayer)mDefense.findPlayer(newX,mOffense.quarterback().y);
		if (tackler == null)
		{
			mOffense.quarterback().x=newX;
			mFieldPos-=1;
			if (mOffense.orientation() == Team.ORIENTATION_LEFT)
			{
				if (mFieldPos < mLineOfScrimmage)
				{
					mOffense.receiver().set(-1,-1);
				}
				
				if (mFieldPos <= 0)
				{
					handleTouchDown();
				}
			}
		}
		else
		{
			tackler.setFlashing(true);
			mOffense.quarterback().setFlashing(true);
			handlePlayDead();
		}
	}
	public void onLeft(View view)
	{
		switch (mState)
		{
			case PRE_SNAP:
				if (mOffense.orientation() != Team.ORIENTATION_RIGHT)
					return;
				handleSnap();
				
			case PLAY_LIVE:
				moveBallCarrierLeft();
				return;
			default:
				return;
		}		
	}

	private void moveBallCarrierRight()
	{
		int newX=mOffense.quarterback().x;
		
		if (mOffense.quarterback().x < getFieldLength()-1)
		{
			newX+=1;
		}
		else if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
		{
			newX=0;
		}
		else
			return;
		
		DefensivePlayer tackler = (DefensivePlayer)mDefense.findPlayer(newX,mOffense.quarterback().y);
		if (tackler == null)
		{
			mOffense.quarterback().x=newX;
			mFieldPos+=1;
			if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
			{
				if (mFieldPos > mLineOfScrimmage)
				{
					mOffense.receiver().set(-1,-1);
				}
				
				if (mFieldPos >= 100)
				{
					handleTouchDown();
				}
			}
		}
		else
		{
			tackler.setFlashing(true);
			mOffense.quarterback().setFlashing(true);
			handlePlayDead();
		}
	}
	
	public void onRight(View view)
	{	
		switch (mState)
		{
			case PRE_SNAP:
				if (mOffense.orientation() != Team.ORIENTATION_LEFT)
					return;
				handleSnap();
			case PLAY_LIVE:
				moveBallCarrierRight();
				return;
			default:
				return;
		}
	}

	private void moveBallCarrierUp()
	{
		int newY=mOffense.quarterback().y;
		
		if (mOffense.quarterback().y > 0)
		{
			newY -= 1;
		}
		else
			return;
		
		DefensivePlayer tackler = (DefensivePlayer)mDefense.findPlayer(mOffense.quarterback().x,newY);
		if (tackler == null)
		{
			mOffense.quarterback().y=newY;
		}
		else
		{
			tackler.setFlashing(true);
			mOffense.quarterback().setFlashing(true);
			handlePlayDead();
		}
	}
	
	public void onUp(View view)
	{	
		switch (mState)
		{
			case PLAY_LIVE:
				moveBallCarrierUp();
				break;
			default:
				return;
		}
	}

	private void moveBallCarrierDown()
	{
		int newY=mOffense.quarterback().y;
		
		if (mOffense.quarterback().y < mFieldView.getFieldWidth() - 1)
		{
			newY += 1;
		}
		else
			return;
		
		DefensivePlayer tackler = (DefensivePlayer)mDefense.findPlayer(mOffense.quarterback().x,newY);
		if (tackler == null)
		{
			mOffense.quarterback().y=newY;
		}
		else
		{
			tackler.setFlashing(true);
			mOffense.quarterback().setFlashing(true);
			handlePlayDead();
		}
	}
	
	public void onDown(View view)
	{	
		switch (mState)
		{
			case PLAY_LIVE:
				moveBallCarrierDown();
				break;
			default:
				return;
		}
	}
	
	private int passX;
	public void onPass(View view)
	{	
		switch (mState)
		{
			case PLAY_LIVE:
				mState=State.PASS;	
				passX=mOffense.quarterback().x;
				break;
			default:
				return;
		}
	}
	
	public void onHuddle(View view)
	{	
		switch (mState)
		{
			case PLAY_DEAD:
				initPreSnap();
				break;
			default:
				return;
		}
	}
	
	public void onKick(View view)
	{	
	}
	
	private Player findPlayer(int x, int y)
	{
		Player player = mOffense.findPlayer(x,y);
		if (player == null)
			player = mDefense.findPlayer(x,y);
		
		return player;
	}
	
	private void handleCompletion()
	{
		Player quarterback = mOffense.quarterback();
		Player receiver = mOffense.receiver();
		
		mFieldPos+=(receiver.x - quarterback.x);
		quarterback.set(receiver.x,receiver.y);
		receiver.set(-1,-1);
		mState=State.PLAY_LIVE;	
		
		if (mOffense.orientation() == Team.ORIENTATION_LEFT)
		{
			if (mFieldPos <= 0)
			{
				handleTouchDown();
			}
		}
		else
		{
			if (mFieldPos >= 100)
			{
				handleTouchDown();
			}
		}
	}
	
	private void handleInterception(Player defender)
	{
		mFieldPos+=(defender.x - mOffense.quarterback().x);
		defender.setFlashing(true);
		mGameState=GameState.INTERCEPTION;
		handlePlayDead();
	} 
	
	private void handlePass()
	{
		Player quarterback = mOffense.quarterback();
		Player receiver = mOffense.receiver();
		int newX = (mOffense.orientation() == Team.ORIENTATION_RIGHT)?passX+1:passX-1;
		
		// Check for incomplete pass
		if (newX < 0 || newX >= getFieldLength())
		{
			mGameState=GameState.INCOMPLETE;
			mFieldPos=mLineOfScrimmage;
			handlePlayDead();
			return;
		}
		
		Player defender = mDefense.findPlayer(newX,quarterback.y);
		if (defender != null)
		{
			if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
			{
				if (defender.x >= mStartingXPos)
					handleInterception(defender);
			}
			else 
			{
				if (defender.x <= getFieldLength()-1-mStartingXPos)
					handleInterception(defender);
			}
		}
		
		if (receiver.equals(newX,quarterback.y))
		{
			handleCompletion();
			return;
		}

		passX=newX;	
	}
	
	protected boolean OnUpdateGameAI()
	{
		switch (mState)
		{
			case PLAY_LIVE:
				Game.this.OnMoveDefense(); 
				Game.this.OnMoveReceiver(); 
				return (mState==State.PLAY_LIVE);
			case PASS:
				handlePass();
				return true;
			default:
				return false;
		}
	}
	
	protected void OnMoveReceiver()
	{
		if ( !mDifficulty.perReceiverMoves().test() || 
				mOffense.receiver().x == -1 ||
					mOffense.receiver().y == -1)
			return;
		
		movePlayerRelativePosition(mOffense.receiver(),
										(mOffense.orientation() == Team.ORIENTATION_RIGHT)?getFieldLength()-1:0,
										mOffense.quarterback().y);	
	}

	
	private DefensivePlayer selectDefenderToMove()
	{
		OffensivePlayer ballCarrier=mOffense.quarterback();
		for (PlayerIterator i=mDefense.iterator();i.hasNext();)
		{
			DefensivePlayer defender = (DefensivePlayer)i.next();
			// Look for a defender that can make the tackle. Based on the difficulty
			//  setting, there is a percentage chance that the player will make the
			//  tackle. The more players surrounding the ball carrier, the better
			//  the chance
			if ((Math.abs(defender.x-ballCarrier.x) + 
			      Math.abs(defender.y-ballCarrier.y)) == 1)
			{
				if (mDifficulty.perDefenderTackles().test())
					return defender;
			}
		}
		
		return (DefensivePlayer)mDefense.getRandomPlayer();
	}
	
	private Random bRand=new Random();
	protected void OnMoveDefense() 
	{
		if ( ! mDifficulty.perDefenderMoves().test())
		{
			Log.d(TAG,"onMoveDefense, not moving any defenders");
			return;
		}
		
		DefensivePlayer defender = selectDefenderToMove();
		OffensivePlayer ballCarrier=mOffense.quarterback();


		// Make the tackle if possible
	    if ((Math.abs(defender.x-ballCarrier.x) + 
			      Math.abs(defender.y-ballCarrier.y)) == 1)
		{
	    	defender.setFlashing(true);
	    	ballCarrier.setFlashing(true);
			handlePlayDead();
	    	return;
		}
		
	    movePlayerRelativePlayer(defender,ballCarrier);
	}

	protected void movePlayerRelativePlayer(Player player,Player other)
	{
		movePlayerRelativePosition(player,other.x,other.y);
	}
	
	protected void movePlayerRelativePosition(Player player,int x, int y)
	{
		int xOffset= player.x-x;
	    int yOffset= player.y-y;
	    
	    int newX=( (xOffset== 0) ? player.x :
	    			( (xOffset < 0) ? player.x+1: player.x-1) );
	    	    
		int newY=( (yOffset== 0) ? player.y :
					( (yOffset < 0) ? player.y+1:player.y-1) );
				
		// If we can't make the tackle pick a direction to try moving first.
		//  we will try both directions before giving up
		boolean selector = bRand.nextBoolean();
		for (int i=0;i<2;i++)
		{
			int dx=selector?newX:player.x;
			int dy=selector?player.y:newY;
			
			if (player.equals(dx,dy))
				continue;
			
			if (findPlayer(dx,dy) == null)
	    	{
				
	    		player.set(dx,dy);
	    		return;
	    	}
			selector=!selector;
		}
		
		Log.d(TAG,"movePlayerRelativePosition, no possible moves for specified player");
		return;
	}

	private void updateScoreBoard()
	{
		mHomeScoreView.setText(String.format("%d",mHomeScore));
		mVisitorScoreView.setText(String.format("%d",mVisitorScore));
	}

	private String driveStatusToString()
	{
		String status="";
		switch (mSeriesDown)
		{
			case 1:status= "1st and"; break;
			case 2:status= "2nd and"; break;
			case 3:status= "3rd and"; break;
			case 4: status= "4th and"; break;
		}
		
		if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
		{
			status+= String.format(" %d",mFirstDownPos-mFieldPos);
		}
		else
		{
			status+= String.format(" %d",mFieldPos-mFirstDownPos);
		}
		return status;
	}
	
	private String fieldPosToString()
	{
		String status= "";
		if (mFieldPos>50)
		{
			if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
			{
				status+= String.format(" Opp");
			}
			else
			{
				status+= String.format(" Own");
			}
			status+= String.format(" %d",50-(mFieldPos-50));
		}
		else
		{
			if (mFieldPos<50)
			{
				if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
				{
					status+= String.format(" Own");
				}
				else
				{
					status+= String.format(" Opp");
				}
			}
			status+= String.format(" %d",mFieldPos);
		}
		
		return status;
	}
	
	private void updateDriveStatus()
	{
		mDriveView.setText(driveStatusToString());
		mFieldPosView.setText(fieldPosToString());
	}


	private void updateGame(boolean flash)
	{
		mGameClock.tick();
		updateField(flash);
	}
	
	private void setPlayerTile(Player player, int bitmapIdx,boolean flash)
	{
		try
		{
			if ( !player.isFlashing() || flash )
				mFieldView.setTile(bitmapIdx, player.x, player.y);
		}
		catch (NullPointerException e)
		{
			return;
		}
		catch (IndexOutOfBoundsException e)
		{
			return;
		}
	}
	
	private void updatePlayerTiles(boolean flash)
	{
		mFieldView.clearTiles();	
		for (PlayerIterator i=mOffense.iterator();i.hasNext();)
			setPlayerTile(i.next(),mBitmapLookup[mOffense.side()][mOffense.orientation()],flash);
		for (PlayerIterator i=mDefense.iterator();i.hasNext();)
			setPlayerTile(i.next(),mBitmapLookup[mDefense.side()][mDefense.orientation()],flash);
	}
	
	private void updateField(boolean flash)
	{
		switch (mState)
		{
			case PLAY_LIVE:
			case PLAY_DEAD:
			case PRE_SNAP:
				updatePlayerTiles(flash);
				break;
				
			case PASS:
				updatePlayerTiles(flash);
				mFieldView.setTile(FOOTBALL,passX,mOffense.quarterback().y);
				break;
				
			default:
				break;
		}
		
		mFieldView.invalidate();
	}
}
