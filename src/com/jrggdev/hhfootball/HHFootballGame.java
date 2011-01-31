package com.jrggdev.hhfootball;

import java.util.Random;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * HHFootball: a simple game that everyone can enjoy.
 * 
 * This is an implementation of the classic Game "HHFootball", in which you
 * control a serpent roaming around the garden looking for apples. Be careful,
 * though, because when you catch one, not only will you become longer, but
 * you'll move faster. Running into yourself or the walls will end the game.
 * 
 */
public class HHFootballGame extends Activity
{
	private static String TAG = "HHFootball";
	
	/** Child view definitions */
	private HHFootballFieldView mFieldView;
	private TextView mStatusView;
	private TextView mInfoView;
	private TextView mPlayClockView;
	private TextView mPeriodView;
	private TextView mHomeScoreView;
	private TextView mVisitorScoreView;

	/** Menus Items */
	private static final int MENU_NEW_GAME=0;
	private static final int MENU_EXIT=1;
	
	/** Defines whether the game is paused or not */
	
	private boolean mPaused = false;
	
	/** Game States */
	protected enum State 
	{ 
		GAME_OVER,PLAY_LIVE,PLAY_DEAD,PRE_SNAP,PASS,KICKOFF;
	}
	private State mState=State.GAME_OVER;
	
	private int mHomeScore=0;
	private int mVisitorScore=0;
	
	/**
	 * Labels for the drawables that will be loaded into the TileView class
	 */
	private static final int HOME_LEFT = 1;
	private static final int HOME_RIGHT = 2;
	private static final int VISITOR_LEFT = 3;
	private static final int VISITOR_RIGHT = 4;
	private static final int FOOTBALL = 5;
	
	private static final int[][] mBitmapLookup = {{HOME_LEFT,HOME_RIGHT},{VISITOR_LEFT,VISITOR_RIGHT}};
	
	/**
	 * Represents time left in the period in milliseconds
	 */
	private static int mPeriodLengthMins=2;
	private class PlayClock
	{
		private float mCounter=0;
		private boolean mRunning=false;
		private TextView mTextView;
		
		
		public PlayClock(TextView textView)
		{
			super();
			mTextView=textView;
		}
		
		public PlayClock(TextView textView,float counter)
		{
			super();
			mTextView=textView;
			mCounter=counter;
		}
		
		public void tick()
		{
			if (mRunning)
			{
				mCounter -= 0.1;
				if (mCounter <= 0)
				{
					mCounter = 0;
					stop();
				}

				mTextView.setText(PlayClock.this.toString());
			}
		}
		
		public void set(long timeMins)
		{
			set(timeMins,0);
		}
		
		public void set(long timeMins, long timeSecs)
		{
			stop();
			mCounter=timeMins*60 + timeSecs;
			mTextView.setText(toString());
		}
		
		public void start()
		{
			if (mRunning)
				return;
			
			mRunning=true;
		}
		
		public void stop()
		{
			if (!mRunning)
				return; 
			
			mRunning=false;
		}
		
		public boolean expired()
		{
			return mCounter <= 0;
		}
		
		public float timeLeftSecs()
		{
			return mCounter;
		}
		
		public String toString()
		{
			int mins=(int)Math.floor(mCounter/60);
			float secs=mCounter- (mins*60);
			
			return String.format("%02d:%04.1f",mins,secs);
		}
	}
	private PlayClock mPlayClock;
	
	private static int mNumberofPeriods=4;
	private int mPeriod;
	
	/**
	 * Game field settings
	 */
	private int mFieldPos = 0;
	private static int mTouchbackPos=20;
	private static int mDownsPerSeries=4;
	private int mSeriesDown = 1;
	private static int mYardsForFirstDown=10;
	private int mLineOfScrimmage=0;
	private int mFirstDownPos = 0;
	private int mStartingXPos=3;
	private boolean mChangeOfPossesion=false;
	private boolean mKickOff=false;
	

	/**
	 * mQuarterback: current Player of the Quarterback mReceiver: current
	 * coordiante of the Wide Reciever mDefense: List of Players for the
	 * defensive players
	 */
	private Offense mOffense;
	private Defense mDefense;
	
	
	private class Percentage
	{
		private Random rand = new Random();
		private int mPercentage;
		public Percentage(int percentage)
		{
			assert ( percentage >0 && percentage <=100 );
			mPercentage=percentage;
		}
		
		public boolean test()
		{
			return (rand.nextInt(100) < mPercentage);
		}
	}
	private Percentage mDefenderMoves= new Percentage(25);
	private Percentage mReceiverMoves= new Percentage(10);
	private Percentage mDefenderTackles= new Percentage(25);
	
	
	private static final int mGameRefreshRate=100;
	private Timer mGameUpdater = new Timer(mGameRefreshRate,new Timer.TimerHandler() {
		private boolean mFlashToggle=false;
		public boolean HandleTimer() { 
			mFlashToggle=!mFlashToggle;
			HHFootballGame.this.updateGame(mFlashToggle);
			return true;
		} 
	});
	
	private int mAiUpdateRate=250;
	private Timer mAiUpdater = new Timer(mAiUpdateRate,new Timer.TimerHandler() {
		public boolean HandleTimer() { 			
			return HHFootballGame.this.OnUpdateGameAI(); 
		} 
	});
	
	
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
        menu.add(0, MENU_EXIT, 0, R.string.menu_exit);

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
            case MENU_EXIT:
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
		
		mFieldView = (HHFootballFieldView)findViewById(R.id.hhfootballview);		
		mStatusView = (TextView)findViewById(R.id.status_view);
		mInfoView = (TextView)findViewById(R.id.info_view);
		mPlayClockView = (TextView)findViewById(R.id.scoreboard_clock);
		mPeriodView = (TextView)findViewById(R.id.scoreboard_period);
		mHomeScoreView = (TextView)findViewById(R.id.scoreboard_home);
		mVisitorScoreView = (TextView)findViewById(R.id.scoreboard_visitor);
				
		mPlayClock = new PlayClock(mPlayClockView);
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
		mPaused=true;
		mPlayClock.stop();
		mAiUpdater.stop();
		mGameUpdater.stop();
	}

	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (mPaused)
		{
			Log.i(TAG,"Activity Resumed");
			mPaused=false;
			mGameUpdater.start();
			switch (mState)
			{
				case PLAY_LIVE:
				case PASS:
				case KICKOFF:
					mPlayClock.start();
					mAiUpdater.start();
					return;
				default:
					return;
				
			}
			
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// Store the game state
		Bundle map = new Bundle();
		
		map.putInt("mState",mState.ordinal());
		map.putInt("mHomeScore",mHomeScore);
		map.putInt("mVisitorScore",mVisitorScore);
		map.putFloat("mPlayClock",mPlayClock.timeLeftSecs());
		map.putInt("mPeriod",mPeriod);
		map.putInt("mLineOfScrimmage",mLineOfScrimmage);
		map.putInt("mFieldPos",mFieldPos);
		map.putInt("mSeriesDown",mSeriesDown);
		map.putInt("mFirstDownPos",mFirstDownPos);
//		map.putIntArray("mQuarterback", mQuarterback.to_list());
//		map.putIntArray("mReceiver", mReceiver.to_list());
//		map.putIntArray("mDefense", mDefense.to_array());
				
		outState.putBundle(TAG, map);
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
		mHomeScore=savedState.getInt("mHomeScore");
		mVisitorScore=savedState.getInt("mVisitorScore");
		mPlayClock=new PlayClock(mPlayClockView, savedState.getFloat("mPlayClock"));
		mPeriod=savedState.getInt("mPeriod");
		mLineOfScrimmage=savedState.getInt("mLineOfScrimmage");
		mFieldPos=savedState.getInt("mFieldPos");
		mSeriesDown=savedState.getInt("mSeriesDown");
		mFirstDownPos=savedState.getInt("mFirstDownPos");
//		mQuarterback = new Player(savedState.getIntArray("mQuarterback"));
//		mReceiver = new Player(savedState.getIntArray("mReceiver"));
//		mDefense = PlayerList.from_array(savedState.getIntArray("mDefense"));
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
	
	public Offense getOffense() { return mOffense; }
	public Defense getDefense() { return mDefense; }
	
	public void startNewGame()
	{
		mOffense = new Offense(Team.SIDE_HOME,Team.ORIENTATION_LEFT);
		mDefense = new Defense(Team.SIDE_VISITOR,Team.ORIENTATION_RIGHT);
		mHomeScore=0;
		mVisitorScore=0;
		mPeriod=1;
		mPlayClock.set(mPeriodLengthMins);
		mPeriodView.setText(String.format("%d",mPeriod));
		handleTouchBack();
		
		mGameUpdater.start();
		initPreSnap();
	}
	

	private void initPreSnap()
	{
		if (mPlayClock.expired())
			handleNewPeriod();	
		
		mLineOfScrimmage=mFieldPos;
		if (mKickOff)
			handleKickoff();
		else if (mChangeOfPossesion)
			handleChangeOfPossesion();

		
		arrangePreSnapFormation(mOffense);
		arrangePreSnapFormation(mDefense);
		mState=State.PRE_SNAP;
		mInfoView.setText("");
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
		if (mPeriod < mNumberofPeriods)
		{
			mState = State.PLAY_DEAD;
			
			mFieldPos=swapFieldPosOrientation(mFieldPos);
			mLineOfScrimmage=mFieldPos;
			mFirstDownPos=swapFieldPosOrientation(mFirstDownPos);

			mPeriod++;
			mPeriodView.setText(String.format("%d",mPeriod));
			mPlayClock.set(mPeriodLengthMins);
			
			swapOrientation();
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
		mChangeOfPossesion=false;
	}
	
	private void handlePlayDead()
	{
		mPlayClock.stop();
		mAiUpdater.stop();
		mState = State.PLAY_DEAD;
			
		// TODO HAndle turnover in the end zone
		// TODO Handle safety
		if (mPlayClock.expired())
		{
			if (mPeriod+1 >= mNumberofPeriods)
			{
				mState = State.GAME_OVER;
				mInfoView.setText("Game Over");
				return;
			}
		}
		if (checkFirstDown())
		{
			handleFirstDown();
			mInfoView.setText("First Down");
		}
		else if (mSeriesDown < mDownsPerSeries)
		{
			// Drive is still alive
			mSeriesDown++;
		}
		else if (mSeriesDown == mDownsPerSeries)
		{
			// Turnover on downs
			mChangeOfPossesion=true;
			mInfoView.setText("Turnover on Downs");
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
		mKickOff=false;
	}
	
	private void handleTouchDown()
	{
		mPlayClock.stop();
		mState=State.PLAY_DEAD;
		
		if (mOffense.side() == Team.SIDE_HOME)
		{
			mHomeScore+=7;
		}
		else
		{
			mVisitorScore+=7;
		}
		
		mInfoView.setText("Touchdown");
		mKickOff=true;
	}
	
	private Random recRand=new Random();
	private void handleSnap()
	{
		mState = State.PLAY_LIVE;
		mPlayClock.start();
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
		mState=State.PLAY_LIVE;	
		quarterback.set(receiver.x,receiver.y);
		receiver.set(-1,-1);
		mInfoView.setText("Complete");
	}
	
	private void handleInterception(Player defender)
	{
		mFieldPos+=(defender.x - mOffense.quarterback().x);
		defender.setFlashing(true);
		mChangeOfPossesion=true;
		handlePlayDead();
		mInfoView.setText("Interception");
	} 
	
	private void handlePass()
	{
		Player quarterback = mOffense.quarterback();
		Player receiver = mOffense.receiver();
		int newX = (mOffense.orientation() == Team.ORIENTATION_RIGHT)?passX+1:passX-1;
		
		// Check for incomplete pass
		if (newX < 0 || newX >= getFieldLength())
		{
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
				HHFootballGame.this.OnMoveDefense(); 
				HHFootballGame.this.OnMoveReceiver(); 
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
		if ( !mReceiverMoves.test() || mOffense.receiver().x == -1 || mOffense.receiver().y == -1)
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
				if (mDefenderTackles.test())
					return defender;
			}
		}
		
		return (DefensivePlayer)mDefense.getRandomPlayer();
	}
	
	private Random bRand=new Random();
	protected void OnMoveDefense() 
	{
		if ( ! mDefenderMoves.test())
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

	private void updateDriveStatus()
	{
		String status="";
		switch (mSeriesDown)
		{
		case 1:status= "1st down"; break;
		case 2:status= "2nd down"; break;
		case 3:status= "3rd down"; break;
		case 4: status= "4th down"; break;
		}
		
		if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
		{
			status+= String.format(" %d",mFirstDownPos-mFieldPos);
		}
		else
		{
			status+= String.format(" %d",mFieldPos-mFirstDownPos);
		}
		
		status+= " yds to go, Ball on";
		if (mFieldPos>50)
		{
			if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
			{
				status+= String.format(" opp");
			}
			else
			{
				status+= String.format(" own");
			}
			status+= String.format(" %d yard line",50-(mFieldPos-50));
		}
		else
		{
			if (mFieldPos<50)
			{
				if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
				{
					status+= String.format(" own");
				}
				else
				{
					status+= String.format(" opp");
				}
			}
			status+= String.format(" %d yard line",mFieldPos);
		}
		
		mStatusView.setText(status);
	}

	private void updateGame(boolean flash)
	{
		mPlayClock.tick();
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
				
			case GAME_OVER:
				return;
				
			case KICKOFF:
			default:
				break;
		}
		
		mFieldView.invalidate();
	}
}
