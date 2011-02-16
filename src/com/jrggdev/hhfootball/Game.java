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
import android.widget.TextView;

import com.jrggdev.Coordinate;
import com.jrggdev.Percentage;
import com.jrggdev.SoundManager;
import com.jrggdev.TextViewAnimator;
import com.jrggdev.Timer;
import com.jrggdev.hhfootball.GameClock.Period;


/**
 * HHFootball: a simple game that everyone can enjoy.
 * 
 * This is an implementation of the classic Game "HHFootball", in which you
 * control a serpent roaming around the garden looking for apples. Be careful,
 * though, because when you catch one, not only will you become longer, but
 * you'll move faster. Running into yourself or the walls will end the game.
 * 
 */
public class Game extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, GameClock.GameClockHandler
{
	private static String TAG = "HHFootball";
	
	/** Child view definitions */
	private FieldView mFieldView;
	private TextView mClockView;
	private TextView mPeriodView;
	private TextView mDriveView;
	private TextView mFieldPosView;
	private TextViewAnimator mInfoView;
	private TextView mHomeScoreView;
	private TextView mVisitorScoreView;
	private KickMeter mKickMeter;

	/** Menus Items */
	private static final int MENU_NEW_GAME=0;
	private static final int MENU_SETTINGS=1;
	private static final int MENU_QUIT=2;
	
	private SoundManager mSoundManager;
	private static final int AUDIO_QUARTERBACK=1;
	private static final int AUDIO_TACKLE=2;
	private static final int AUDIO_WHISTLE=3;
	private static final int AUDIO_CROWD=4;
	private static final int AUDIO_CROWD_BOO=5;
	private static final int AUDIO_CROWD_CHEER=6;
	private static final int AUDIO_TOUCHDOWN=7;
	private static final int AUDIO_KICK=8;
	private static final int AUDIO_CATCH=9;
	private static final int AUDIO_FIRST_DOWN=10;
	private static final int AUDIO_BUZZER=11;
	
	/** Defines whether the game is paused or not */
		
	/** Game States */
	protected enum State 
	{ 
		PLAY_LIVE,PLAY_DEAD,PRE_SNAP,PRE_KICKOFF,KICK,PASS,GAME_OVER;
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
	
	/**
	 * Represents time left in the period in milliseconds
	 */
	private int mPeriodLengthMins=4;
	
	private enum Difficulty
	{
		easy(25,10,0,90),
		medium(25,10,25,80),
		hard(50,10,50,70);
		
		private Percentage mDefenderMoves;
		private Percentage mReceiverMoves;
		private Percentage mDefenderTackles;
		private Percentage mFieldGoalIsGood;
		
		private Difficulty(int perDefenderMoves, int perReceiverMoves, int perDefenderTackles,int perFieldGoalIsGood)
		{
			mDefenderMoves = new Percentage(perDefenderMoves);
			mReceiverMoves = new Percentage(perReceiverMoves);
			mDefenderTackles= new Percentage(perDefenderTackles);
			mFieldGoalIsGood= new Percentage(perFieldGoalIsGood);
		}
		
		public Percentage perDefenderMoves() {  return mDefenderMoves; }
		public Percentage perReceiverMoves() {  return mReceiverMoves; }
		public Percentage perDefenderTackles() {  return mDefenderTackles; }
		public Percentage perFieldGoalIsGood() {  return mFieldGoalIsGood; }
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
	private static final int mKickoffPos=30;
	private static final int mFreeKickPos=20;
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
	private int mKickPower;
	private Coordinate mBallPos;
	
	enum GameState
	{
		KICKOFF,
		PUNT,
		KICK_RETURN,
		DRIVE_IN_PROGRESS,
		TURNOVER_ON_DOWNS,
		INCOMPLETE,
		INTERCEPTION,
		FUMBLE,
		TOUCHDOWN,
		TOUCHBACK,
		FIELD_GOAL_ATTEMPT,
		FIELD_GOAL_MAKE,
		FIELD_GOAL_MISS,
		SAFETY,
		FREEKICK;
		
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
		mClockView=(TextView)findViewById(R.id.scoreboard_clock);
		mPeriodView=(TextView)findViewById(R.id.scoreboard_period);
		mDriveView = (TextView)findViewById(R.id.drive_view);
		mFieldPosView = (TextView)findViewById(R.id.fieldpos_view);
		mHomeScoreView = (TextView)findViewById(R.id.scoreboard_home);
		mVisitorScoreView = (TextView)findViewById(R.id.scoreboard_visitor);
		mKickMeter= (KickMeter)findViewById(R.id.kick_meter);
		mInfoView = new TextViewAnimator((TextView)findViewById(R.id.info_view),
											AnimationUtils.loadAnimation(this, R.anim.scroll_in),
											AnimationUtils.loadAnimation(this, R.anim.scroll_out));
		
		
		mSoundManager=new SoundManager(this);
		
		// Get the current settings values
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mSoundManager.setMute(settings.getBoolean("sound", true) == false);
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
	protected void onDestroy() 
	{
		mSoundManager.release();
		super.onDestroy();
	}

	@Override
	protected void onPause()
	{
		Log.i(TAG,"Activity Paused");
		super.onPause();
		mGameClock.stop();
		mAiUpdater.stop();
		mGameUpdater.stop();
		mSoundManager.pause();
	}

	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG,"Activity Resumed");
		mSoundManager.resume();
		switch (mState)
		{
			case KICK:
				mGameClock.start();
				break;
			case PLAY_LIVE:
			case PASS:
				mGameClock.start();
				mAiUpdater.start();
				break;
			default:
				break;	
		}
		
		updateDriveStatus();
		updateScoreBoard();
		mGameUpdater.start();
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
		map.putBundle("mKickMeter",mKickMeter.serialize());
		map.putInt("mPeriodLengthMins",mPeriodLengthMins);
		map.putInt("mPeriod",mPeriod);
		map.putInt("mLineOfScrimmage",mLineOfScrimmage);
		map.putInt("mFieldPos",mFieldPos);
		map.putInt("mSeriesDown",mSeriesDown);
		map.putInt("mFirstDownPos",mFirstDownPos);
		map.putInt("mKickPower",mKickPower);
		map.putBundle("mBallPos", mBallPos.serialize());
		map.putBundle("mOffense", mOffense.serialize());
		map.putBundle("mDefense", mDefense.serialize());
		
		outState.putBundle(TAG, map);
	}

	public void onSharedPreferenceChanged(SharedPreferences settings, String key)
	{
		if (key.equals("sound"))
		{
			mSoundManager.setMute(settings.getBoolean("sound", true) == false);
		}
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
		mGameClock=new GameClock(savedState.getBundle("mGameClock"),this);
		mKickMeter.restore(savedState.getBundle("mKickMeter"));
		mPeriod=savedState.getInt("mPeriod");
		mPeriodLengthMins=savedState.getInt("mPeriodLengthMins");
		mLineOfScrimmage=savedState.getInt("mLineOfScrimmage");
		mFieldPos=savedState.getInt("mFieldPos");
		mSeriesDown=savedState.getInt("mSeriesDown");
		mFirstDownPos=savedState.getInt("mFirstDownPos");
		mKickPower=savedState.getInt("mKickPower");
		mBallPos=new Coordinate(savedState.getBundle("mBallPos"));
		mOffense=new Offense(savedState.getBundle("mOffense"));
		mDefense=new Defense(savedState.getBundle("mDefense"));
		initAudio();
	}
	
	private void initAudio()
	{
		mSoundManager.addSfx(AUDIO_CROWD, R.raw.crowd);
		mSoundManager.addSfx(AUDIO_CROWD_BOO, R.raw.crowd_boo);
		mSoundManager.addSfx(AUDIO_CROWD_CHEER, R.raw.crowd_cheer);
		mSoundManager.addSfx(AUDIO_QUARTERBACK, R.raw.quarterback);
		mSoundManager.addSfx(AUDIO_TACKLE, R.raw.tackle);
		mSoundManager.addSfx(AUDIO_WHISTLE, R.raw.whistle);
		mSoundManager.addSfx(AUDIO_KICK, R.raw.kick);
		mSoundManager.addSfx(AUDIO_CATCH, R.raw.ball_catch);
		mSoundManager.addSfx(AUDIO_TOUCHDOWN,R.raw.touchdown);
		mSoundManager.addSfx(AUDIO_FIRST_DOWN,R.raw.firstdown);
		mSoundManager.addSfx(AUDIO_BUZZER,R.raw.buzzer);
		mSoundManager.setSfxVolume(AUDIO_CROWD, 0.1f);
		mSoundManager.playSfx(AUDIO_CROWD,true);
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
		mBallPos = new Coordinate(mOffense.quarterback().pos());
		mGameClock = new GameClock(mPeriodLengthMins,this);
		mHomeScore=0;
		mVisitorScore=0;
		mGameState=GameState.KICKOFF;
		initAudio();		
		mGameUpdater.start();
		initPreSnap();
	}
	

	private void initPreSnap()
	{
		
		handleNewPeriod();	
		
		mLineOfScrimmage=mFieldPos;
		mInfoView.clearText();
		
		
		switch (mGameState)
		{
			case KICKOFF:
				mState=State.PRE_KICKOFF;
				mFieldPos=(mOffense.orientation() == Team.ORIENTATION_RIGHT)?mKickoffPos:100-mKickoffPos;
				arrangePreSnapFormation(mOffense);
				arrangePreSnapFormation(mDefense);
				mInfoView.setText(getString(R.string.info_kickoff),mInfoDuration);
				updateDriveStatus();
				mKickMeter.setMinMax(20,75);
				mKickMeter.enable();
				return;
				
			case FREEKICK:
				mState=State.PRE_KICKOFF;
				mFieldPos=(mOffense.orientation() == Team.ORIENTATION_RIGHT)?mFreeKickPos:100-mFreeKickPos;
				arrangePreSnapFormation(mOffense);
				arrangePreSnapFormation(mDefense);
				mInfoView.setText(getString(R.string.info_freekick),mInfoDuration);
				updateDriveStatus();
				mKickMeter.setMinMax(20,75);
				mKickMeter.enable();
				return;
				
			case TOUCHBACK:
				handleTouchBack();
				break;
			case FIELD_GOAL_MISS:
			case TURNOVER_ON_DOWNS:
			case INTERCEPTION:
			case FUMBLE:
				handleChangeOfPossesion();
				break;
				
		}
		
		mState=State.PRE_SNAP;
		arrangePreSnapFormation(mOffense);
		arrangePreSnapFormation(mDefense);
		mSoundManager.playSfx(AUDIO_QUARTERBACK,true);
		updateScoreBoard();
		updateDriveStatus();
	}
	
	
	private void arrangePreSnapFormation(Team team)
	{
		int[][] formation;
		switch (mGameState)
		{
			case KICKOFF: 
			case FREEKICK:
				formation = team.getKickoffFormation();
				break;
				
			default:
			case DRIVE_IN_PROGRESS: 
				formation = team.getPreSnapFormation();
				break;
		}
		
		for ( int idx=0;idx<team.size();idx++)
		{
			Player player = team.getPlayer(idx);
			player.setFlashing(false);
			if (formation[idx][0] == -1 || formation[idx][1] == -1)
			{
				player.set(-1,-1);
			}
			else
			{		
				if (team.orientation()==Team.ORIENTATION_RIGHT)
					player.set(formation[idx][0],formation[idx][1]);
				else
					player.set(getFieldLength()-1-formation[idx][0],formation[idx][1]);
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
	
	private void handleTouchBack()
	{
		swapSides();
		swapOrientation();
		mFieldPos=(mOffense.orientation() == Team.ORIENTATION_RIGHT)?mTouchbackPos:100-mTouchbackPos;
		mLineOfScrimmage=mFieldPos;
		mSeriesDown = 1;
		setFirstDownPos();
		mGameState=GameState.DRIVE_IN_PROGRESS;
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
			case KICK_RETURN:
				mSoundManager.playSfx(AUDIO_TACKLE,false);
				handleFirstDown();
				mGameState=GameState.DRIVE_IN_PROGRESS;
				break;
				
			case TOUCHBACK:
				mInfoView.setText(getString(R.string.info_touchback),mInfoDuration);
				break;
				
			case TOUCHDOWN:
				mSoundManager.playSfx(AUDIO_TOUCHDOWN,false);
				mInfoView.setText(getString(R.string.info_touchdown));
				mGameState=GameState.KICKOFF;
				break;
				
			case SAFETY:
				mInfoView.setText(getString(R.string.info_safety),mInfoDuration);
				mGameState=GameState.FREEKICK;
				break;
				
			case FIELD_GOAL_MAKE:
				mInfoView.setText(getString(R.string.info_field_goal_make));
				mGameState=GameState.KICKOFF;
				break;
				
			case FIELD_GOAL_MISS:
				mFieldPos=mLineOfScrimmage;
				mInfoView.setText(getString(R.string.info_field_goal_miss));
				break;
				
			case INTERCEPTION:
				mInfoView.setText(getString(R.string.info_interception),mInfoDuration);
				break;
				
			case INCOMPLETE:
				mInfoView.setText(getString(R.string.info_incomplete),mInfoDuration);
				
			case DRIVE_IN_PROGRESS:
				if (mGameState==GameState.DRIVE_IN_PROGRESS)
				{
					mSoundManager.playSfx(AUDIO_TACKLE,false);
				}
				else
				{
					mGameState=GameState.DRIVE_IN_PROGRESS;
				}
				
				if (isSafety())
				{
					handleSafety();
				}
				else if (checkFirstDown())
				{
					handleFirstDown();
					mSoundManager.playSfx(AUDIO_FIRST_DOWN,false);
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
		
		try {Thread.sleep(200);} 
			catch (InterruptedException e) {}
		
		mSoundManager.playSfx(AUDIO_WHISTLE,false);
		switch (mGameClock.period())
		{
			case END_OF_FIRST_QUARTER:
				mInfoView.setText(getString(R.string.info_end_of_first_quarter),mInfoDuration);
				break;
			case HALFTIME:
				mInfoView.setText(getString(R.string.info_halftime),mInfoDuration);
				break;
			case END_OF_THIRD_QUARTER:
				mInfoView.setText(getString(R.string.info_end_of_third_quarter),mInfoDuration);
				break;
			case GAME_OVER:
				mState = State.GAME_OVER;
				mInfoView.setText(getString(R.string.info_game_over));
				mSoundManager.release();
				mGameUpdater.stop();
				break;
		}
	}
	

	private void handleKickReception()
	{
		mSoundManager.playSfx(AUDIO_CATCH,false);
		swapSides();
		swapOrientation();
		mGameState=GameState.KICK_RETURN;
		mState=State.PLAY_LIVE;
		
		for (PlayerIterator i=mOffense.iterator();i.hasNext();)
			i.next().set(-1,-1);
		
		for (PlayerIterator i=mDefense.iterator();i.hasNext();)
			i.next().set(-1,-1);
		
		mOffense.quarterback().set(mBallPos.x,mBallPos.y);	
		mAiUpdater.start();
	}
	
	private void onHandleKick()
	{
		int newX;
		boolean inEndZone=false;
		
		if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
		{
			newX = mBallPos.x+1;
			if (newX >= getFieldLength())
				newX=0;		
			
			mFieldPos++;
			inEndZone = mFieldPos >= 100;
		}
		else
		{
			newX = mBallPos.x-1;
			if (newX < 0)
				newX=getFieldLength()-1;	
			
			mFieldPos--;
			inEndZone = mFieldPos <= 0;
		}
		
		if (inEndZone)
		{
			switch (mGameState)
			{
				case FIELD_GOAL_ATTEMPT:
					handleFieldGoal();
					return;
				case PUNT:
				case KICKOFF:
				case FREEKICK:
				default:
					mGameState=GameState.TOUCHBACK;
					handlePlayDead();
					return;
			}
		}
		
		if (--mKickPower  == 0)
		{
			switch (mGameState)
			{
				case FIELD_GOAL_ATTEMPT:
					mGameState=GameState.FIELD_GOAL_MISS;
					handlePlayDead();
					return;
				case PUNT:
				case KICKOFF:
				case FREEKICK:
				default:
					handleKickReception();
					return;
			}
		}

		mBallPos.x=newX;
	}
	
	private void handleTouchDown()
	{
		if (mOffense.side() == Team.SIDE_HOME)
		{
			mHomeScore+=7;
			mSoundManager.playSfx(AUDIO_CROWD_CHEER, false);
		}
		else
		{
			mVisitorScore+=7;
			mSoundManager.playSfx(AUDIO_CROWD_BOO, false);
		}
		
		mGameState=GameState.TOUCHDOWN;
		updateScoreBoard();
		handlePlayDead();
	}
	
	private void handleSafety()
	{
		if (mDefense.side() == Team.SIDE_HOME)
		{
			mHomeScore+=2;
			mSoundManager.playSfx(AUDIO_CROWD_CHEER, false);
		}
		else
		{
			mVisitorScore+=2;
			mSoundManager.playSfx(AUDIO_CROWD_BOO, false);
		}
		
		mGameState=GameState.SAFETY;
		updateScoreBoard();
		handlePlayDead();
	}
	
	private void handleFieldGoal()
	{
		Log.i(TAG,String.format("Field Goal percentage %d%%",mDifficulty.perFieldGoalIsGood().getPercentage()+mKickPower));
		if (mDifficulty.perFieldGoalIsGood().test(mKickPower))
		{
			Log.i(TAG,"Field goal is good");
			if (mOffense.side() == Team.SIDE_HOME)
			{
				mHomeScore+=3;
				mSoundManager.playSfx(AUDIO_CROWD_CHEER, false);
			}
			else
			{
				mVisitorScore+=3;
				mSoundManager.playSfx(AUDIO_CROWD_BOO, false);
			}
			
			mGameState=GameState.FIELD_GOAL_MAKE;
			updateScoreBoard();
		}
		else
		{
			Log.i(TAG,"Field goal miss");
			mGameState=GameState.FIELD_GOAL_MISS;
		}
		
		handlePlayDead();
	}
	
	private Random recRand=new Random();
	private void handleSnap()
	{
		if (mKickMeter.isEnabled())
			return;
		
		mSoundManager.stopSfx(AUDIO_QUARTERBACK);
		mInfoView.clearText();
		mState = State.PLAY_LIVE;
		mGameClock.start();
		mOffense.receiver().set((mOffense.orientation() == Team.ORIENTATION_RIGHT)?
								mOffense.quarterback().pos().x+2:
									mOffense.quarterback().pos().x-2, recRand.nextInt(3));
		mAiUpdater.start();
	}
	
	private boolean ballAcrossLineOfScrimmage()
	{
		if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
		{
			return (mFieldPos > mLineOfScrimmage);
		}
		else
		{
			return (mFieldPos < mLineOfScrimmage);
		}
	}
	
	private boolean isTouchDown()
	{
		if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
		{
			return (mFieldPos >= 100);
		}
		else
		{
			return (mFieldPos <= 0);
		}
	}
	private boolean isSafety()
	{
		if (mOffense.orientation() == Team.ORIENTATION_LEFT)
		{
			return (mFieldPos >= 100);
		}
		else
		{
			return (mFieldPos <= 0);
		}
	}
		
	private void moveBallCarrierLeft()
	{
		int newX=mOffense.quarterback().pos().x;
		
		if (mOffense.quarterback().pos().x > 0)
		{
			newX-=1;
		}
		else if (mOffense.orientation() == Team.ORIENTATION_LEFT)
		{
			newX=getFieldLength() - 1;
		}
		else
			return;
		
		DefensivePlayer tackler = (DefensivePlayer)mDefense.findPlayer(newX,mOffense.quarterback().pos().y);
		if (tackler == null)
		{
			mOffense.quarterback().pos().x=newX;
			mFieldPos-=1;
			if (ballAcrossLineOfScrimmage())
				mOffense.receiver().set(-1,-1);
						
			if (isTouchDown())
				handleTouchDown();
				
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
		if (mKickMeter.isEnabled())
			return;
		
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
		int newX=mOffense.quarterback().pos().x;
		
		if (mOffense.quarterback().pos().x < getFieldLength()-1)
		{
			newX+=1;
		}
		else if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
		{
			newX=0;
		}
		else
			return;
		
		DefensivePlayer tackler = (DefensivePlayer)mDefense.findPlayer(newX,mOffense.quarterback().pos().y);
		if (tackler == null)
		{
			mOffense.quarterback().pos().x=newX;
			mFieldPos+=1;
			if (ballAcrossLineOfScrimmage())
				mOffense.receiver().set(-1,-1);
						
			if (isTouchDown())
				handleTouchDown();
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
		if (mKickMeter.isEnabled())
			return;
		
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
		int newY=mOffense.quarterback().pos().y;
		
		if (mOffense.quarterback().pos().y > 0)
		{
			newY -= 1;
		}
		else
			return;
		
		DefensivePlayer tackler = (DefensivePlayer)mDefense.findPlayer(mOffense.quarterback().pos().x,newY);
		if (tackler == null)
		{
			mOffense.quarterback().pos().y=newY;
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
		if (mKickMeter.isEnabled())
			return;
		
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
		int newY=mOffense.quarterback().pos().y;
		
		if (mOffense.quarterback().pos().y < mFieldView.getFieldWidth() - 1)
		{
			newY += 1;
		}
		else
			return;
		
		DefensivePlayer tackler = (DefensivePlayer)mDefense.findPlayer(mOffense.quarterback().pos().x,newY);
		if (tackler == null)
		{
			mOffense.quarterback().pos().y=newY;
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
		if (mKickMeter.isEnabled())
			return;
		
		switch (mState)
		{
			case PLAY_LIVE:
				moveBallCarrierDown();
				break;
			default:
				return;
		}
	}
	
	public void onPass(View view)
	{	
		if (mKickMeter.isEnabled())
			return;
		
		switch (mState)
		{
			case PLAY_LIVE:
				mState=State.PASS;	
				mBallPos=new Coordinate(mOffense.quarterback().pos());
				break;
			default:
				return;
		}
	}
	
	public void onHuddle(View view)
	{	
		if (mKickMeter.isEnabled())
			return;
		
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
		if (mKickMeter.disable())
		{
			mKickPower=mKickMeter.getPowerValue();
			Log.i(TAG,String.format("KickMeter kick power = %d",mKickPower));
			mBallPos=new Coordinate(mOffense.quarterback().pos());
			mSoundManager.playSfx(AUDIO_KICK,false);
			mInfoView.clearText();
			mState = State.KICK;
			mGameClock.start();
		}
		else
		{
			switch (mState)
			{
				case PRE_SNAP:
					mSoundManager.stopSfx(AUDIO_QUARTERBACK);
					mInfoView.clearText();
					mKickMeter.setMinMax(5,50);
					mGameState = GameState.FIELD_GOAL_ATTEMPT;
					mKickMeter.enable();
					mAiUpdater.stop();
					mGameClock.stop();
					break;
					
				case PLAY_LIVE:
					// Punt
					if (ballAcrossLineOfScrimmage() == false)
					{
						mInfoView.clearText();
						mKickMeter.setMinMax(10,60);
						mKickMeter.enable();
						mGameState = GameState.PUNT;
						mAiUpdater.stop();
						mGameClock.stop();
					}
					break;
			}
		}
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
		
		mSoundManager.playSfx(AUDIO_CATCH,false);
		mFieldPos+=(receiver.pos().x - quarterback.pos().x);
		quarterback.set(receiver.pos().x,receiver.pos().y);
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
		mFieldPos+=(defender.pos().x - mOffense.quarterback().pos().x);
		if (mFieldPos > 100)
			mFieldPos=100-mTouchbackPos;
		else if (mFieldPos < 0)
			mFieldPos=mTouchbackPos;
		
		defender.setFlashing(true);
		mGameState=GameState.INTERCEPTION;
		handlePlayDead();
	} 
	
	private void onHandlePass()
	{
		Player quarterback = mOffense.quarterback();
		Player receiver = mOffense.receiver();
		int newX = (mOffense.orientation() == Team.ORIENTATION_RIGHT)?mBallPos.x+1:mBallPos.x-1;
		
		// Check for incomplete pass
		if (newX < 0 || newX >= getFieldLength())
		{
			mGameState=GameState.INCOMPLETE;
			mFieldPos=mLineOfScrimmage;
			handlePlayDead();
			return;
		}
		
		Player defender = mDefense.findPlayer(newX,quarterback.pos().y);
		if (defender != null)
		{
			if (mOffense.orientation() == Team.ORIENTATION_RIGHT)
			{
				if (defender.pos().x >= mStartingXPos)
					handleInterception(defender);
			}
			else 
			{
				if (defender.pos().x <= getFieldLength()-1-mStartingXPos)
					handleInterception(defender);
			}
		}
		
		if (receiver.equals(newX,quarterback.pos().y))
		{
			handleCompletion();
			return;
		}

		mBallPos.x=newX;	
	}
	
	protected boolean OnUpdateGameAI()
	{
		switch (mState)
		{
			case PLAY_LIVE:
				Game.this.onMoveDefense(); 
				Game.this.onMoveReceiver(); 
				return (mState==State.PLAY_LIVE);
			case PASS:
				return true;
			case KICK:
				return true;

			default:
				return false;
		}
	}
	
	protected void onMoveReceiver()
	{
		if ( !mDifficulty.perReceiverMoves().test() || 
				mOffense.receiver().pos().x == -1 ||
					mOffense.receiver().pos().y == -1)
			return;
		
		movePlayerRelativePosition(mOffense.receiver(),
										(mOffense.orientation() == Team.ORIENTATION_RIGHT)?getFieldLength()-1:0,
										mOffense.quarterback().pos().y);	
	}

	
	private DefensivePlayer selectDefenderToMove()
	{
		OffensivePlayer ballCarrier=mOffense.quarterback();
		for (PlayerIterator i=mDefense.iterator();i.hasNext();)
		{
			DefensivePlayer defender = (DefensivePlayer)i.next();
			// If the defender is not visible, make them visible first, this 
			// happens on kick returns
			if(defender.isVisibile())
			{
				// Look for a defender that can make the tackle. Based on the difficulty
				//  setting, there is a percentage chance that the player will make the
				//  tackle. The more players surrounding the ball carrier, the better
				//  the chance
				if ((Math.abs(defender.pos().x-ballCarrier.pos().x) + 
				      Math.abs(defender.pos().y-ballCarrier.pos().y)) == 1)
				{
					if (mDifficulty.perDefenderTackles().test())
						return defender;
				}
			}
			else
				return defender;

		}
		
		return (DefensivePlayer)mDefense.getRandomPlayer();
	}
	
	
	protected void onMoveDefense() 
	{
		if ( ! mDifficulty.perDefenderMoves().test())
		{
			Log.d(TAG,"onMoveDefense, not moving any defenders");
			return;
		}
		
		DefensivePlayer defender = selectDefenderToMove();
		OffensivePlayer ballCarrier=mOffense.quarterback();

		// Make the tackle if possible
	    if ((Math.abs(defender.pos().x-ballCarrier.pos().x) + 
			      Math.abs(defender.pos().y-ballCarrier.pos().y)) == 1)
		{
	    	defender.setFlashing(true);
	    	ballCarrier.setFlashing(true);
			handlePlayDead();
	    	return;
		}
		
	    if (defender.isVisibile())
	    {
	    	movePlayerRelativePlayer(defender,ballCarrier);
	    }
	    else
	    {
	    	int y = bRand.nextInt(getFieldWidth());
	    	int x = bRand.nextInt(getFieldLength());
	    	
	    	while ( (mOffense.findPlayer(x,y)!=null) || (mDefense.findPlayer(x,y)!=null) )
	    	{
	    		Log.d(TAG,String.format("onMoveDefense, pos make visible occupied at %d %d",x,y));
	    		y = bRand.nextInt(getFieldWidth());
		    	x = bRand.nextInt(getFieldLength());
	    	}
	    	Log.d(TAG,String.format("onMoveDefense, defender now visible at %d %d",x,y));
	    	defender.set(x,y);
	    	assert(!defender.equals(ballCarrier));
	    }
	}

	protected void movePlayerRelativePlayer(Player player,Player other)
	{
		movePlayerRelativePosition(player,other.pos().x,other.pos().y);
	}
	
	private Random bRand=new Random();
	protected void movePlayerRelativePosition(Player player,int x, int y)
	{
		int xOffset= player.pos().x-x;
	    int yOffset= player.pos().y-y;
	    
	    int newX=( (xOffset== 0) ? player.pos().x :
	    			( (xOffset < 0) ? player.pos().x+1: player.pos().x-1) );
	    	    
		int newY=( (yOffset== 0) ? player.pos().y :
					( (yOffset < 0) ? player.pos().y+1:player.pos().y-1) );
				
		// If we can't make the tackle pick a direction to try moving first.
		//  we will try both directions before giving up
		boolean selector = bRand.nextBoolean();
		for (int i=0;i<2;i++)
		{
			int dx=selector?newX:player.pos().x;
			int dy=selector?player.pos().y:newY;
			
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
		switch (mGameState)
		{
			case KICKOFF:
				mDriveView.setText(getString(R.string.info_kickoff));
				break;
			case PUNT:
				mDriveView.setText(getString(R.string.info_punt));
				break;
			case FREEKICK:
				mDriveView.setText(getString(R.string.info_freekick));
				break;
			case FIELD_GOAL_ATTEMPT:
				mDriveView.setText(getString(R.string.info_field_goal));
				break;
			default:
				mDriveView.setText(driveStatusToString());
		}
		mFieldPosView.setText(fieldPosToString());
	}


	private void updateGame(boolean flash)
	{
		mGameClock.tick();
		switch (mState)
		{
			case KICK:
				onHandleKick();
				break;
			case PASS:
				onHandlePass();
				break;
		}
			
		updateField(flash);
	}
	public void updateClockDisplay(float clock) 
	{
		int mins=(int)Math.floor(clock/60);
		float secs=clock- (mins*60);
		mClockView.setText(String.format("%02d:%04.1f",mins,secs));
	}

	public void updatePeriodDisplay(Period period) 
	{
		mPeriodView.setText(String.format("%d",period.toInt()));
	}

	public void handleClockExpired() 
	{
		mSoundManager.playSfx(AUDIO_BUZZER,false);
	}
	
	private void setPlayerTile(Player player, int bitmapIdx,boolean flash)
	{
		try
		{
			if ( !player.isFlashing() || flash )
				mFieldView.setTile(bitmapIdx, player.pos().x, player.pos().y);
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
			case PLAY_DEAD:
				switch (mGameState)
				{
					case KICKOFF:
					case FIELD_GOAL_MAKE:
					case FIELD_GOAL_MISS:
					case FREEKICK:
					case TOUCHBACK:
						mFieldView.clearTiles();
						break;
					default:
						updatePlayerTiles(flash);
							
				}
				break;
			case PLAY_LIVE:
			case PRE_SNAP:
				updatePlayerTiles(flash);
				break;
				
			case PASS:
				updatePlayerTiles(flash);
				mFieldView.setTile(FOOTBALL,mBallPos.x,mBallPos.y);
				break;
				
			case PRE_KICKOFF:
				updatePlayerTiles(flash);
				mFieldView.setTile(FOOTBALL,mOffense.quarterback().pos().x +
						((mOffense.orientation() == Team.ORIENTATION_RIGHT)?1:-1),mOffense.quarterback().pos().y);
				
				// animate the kick meter
				break;
				
			case KICK:
				mFieldView.clearTiles();
				mFieldView.setTile(FOOTBALL,mBallPos.x,mBallPos.y);
				updateDriveStatus();
				break;
				
			default:
				break;
		}
		
		mFieldView.invalidate();
	}
}
