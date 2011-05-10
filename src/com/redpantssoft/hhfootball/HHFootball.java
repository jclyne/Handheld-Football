package com.redpantssoft.hhfootball;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Main activity Splash Screen for Handheld Football Game
 * <p/>
 * This activity provides a set of controls to start a new game,
 * change the settings, get information about the game and ways to
 * contact the developer.
 * <p/>
 * The initial display is animated with background football audio
 *
 * @author Jeff clyne
 */
public class HHFootball extends Activity {
    /**
     * ID for the About dialog
     */
    private static final int ABOUT_DIALOG = 1;
    /**
     * ID for the Help dialog
     */
    private static final int HOW_TO_PLAY_DIALOG = 2;
    /**
     * ID for the Website Display error dialog
     */
    private static final int WEBSITE_ERROR_DIALOG = 3;
    /**
     * ID for the Email generation error dialog
     */
    private static final int EMAIL_ERROR_DIALOG = 4;

    /**
     * Sound to be played when activity splash starts animating
     */
    private MediaPlayer mSplashSound;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hhfootlball_layout);

        // Set the Volume controls to always handle the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Setup a media player to play the splash sound. Set up an animation listener
        //  so it will be played when the animation starts
        mSplashSound = MediaPlayer.create(this, R.raw.splash);
        ((LinearLayout) findViewById(R.id.logoview)).setLayoutAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                mSplashSound.start();
            }
        });

    }

    /**
     * Handler for activity dialog creation
     *
     * Use {@link #showDialog(int)} to show the specific dialog
     * Supported Dialog IDs are:
     *  {@link #ABOUT_DIALOG},
     *  {@link #HOW_TO_PLAY_DIALOG},
     *  {@link #WEBSITE_ERROR_DIALOG},
     *  {@link #EMAIL_ERROR_DIALOG},
     *
     *
     * @param id    ID of the specified dialog to create
     * @return      Newly created dialog
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ABOUT_DIALOG:
                return buildAboutDialog();

            case HOW_TO_PLAY_DIALOG:
                return buildHowToPlayDialog();

            case WEBSITE_ERROR_DIALOG:
                return buildShowWebsiteErrorDialog();

            case EMAIL_ERROR_DIALOG:
                return buildSendDevEmailErrorDialog();

        }
        return super.onCreateDialog(id);
    }

    /**
     * Handler for "New Game" button
     *
     * Will issue and specific intent for the HHFootball GAME activity
     *
     * @param view    Reference to the "New Game" button
     */
    public void onNewGame(View view) {
        Intent intent = new Intent();
        intent.setClass(this, Game.class);
        startActivity(intent);
    }

    /**
     * Handler for "Settings" button
     *
     * Will issue and specific intent for the HHFootball SETTINGS activity
     *
     * @param view     Reference to the "Settings" button
     */
    public void onSettings(View view) {
        Intent intent = new Intent();
        intent.setClass(this, Settings.class);
        startActivity(intent);
    }

    /**
     * Handler for "About" button
     *
     * Will show the About dialog
     *
     * @param view     Reference to the "About" button
     */
    public void onAbout(View view) {
        showDialog(ABOUT_DIALOG);
    }

    /**
     * Handler for the "Exit" button
     *
     * Will finish the current activity and exit
     *
     * @param view     Reference to the "Exit" button
     */
    public void onExit(View view) {
        finish();
    }

    /**
     * Builds an About Dialog
     *
     * @return newly created About Dialog
     */
    private Dialog buildAboutDialog() {
        ImageView title = new ImageView(this);
        title.setImageResource(R.drawable.splash_about_normal);
        return new AlertDialog.Builder(this)
                .setCustomTitle(title)
                .setItems(R.array.about_items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        switch (id) {
                            case 0:
                                showDialog(HOW_TO_PLAY_DIALOG);
                                break;

                            case 1:
                                Intent webIntent = new Intent(android.content.Intent.ACTION_VIEW);
                                webIntent.setData(Uri.parse(getString(R.string.dev_website_url)));
                                try {
                                    startActivity(webIntent);
                                } catch (ActivityNotFoundException e) {
                                    showDialog(WEBSITE_ERROR_DIALOG);
                                }

                                break;

                            case 2:
                                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                                emailIntent.setType("plain/text");
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.dev_email_address));
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.dev_email_subject));

                                try {
                                    startActivity(emailIntent);
                                } catch (ActivityNotFoundException e) {
                                    showDialog(EMAIL_ERROR_DIALOG);
                                }

                                break;

                            default:
                                dialog.cancel();
                        }

                    }
                })
                .create();
    }

    /**
     * Build a Help Dialog
     *
     * @return newly created Help Dialog
     */
    private Dialog buildHowToPlayDialog() {
        ImageView helpTitle = new ImageView(this);
        helpTitle.setImageResource(R.drawable.how_to_play);
        return new AlertDialog.Builder(this)
                .setCustomTitle(helpTitle)
                .setView(getLayoutInflater().inflate(R.layout.help_layout, null))
                .setInverseBackgroundForced(true)
                .setCancelable(false)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();
    }

    /**
     * Build an Error Dialog on failure to launch website
     *
     * @return newly created Error Dialog
     */
    private Dialog buildSendDevEmailErrorDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dev_email_error_title))
                .setMessage(getString(R.string.dev_email_error_msg))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();
    }

    /**
     * Build an Error Dialog on failure to generate email
     *
     * @return newly created Error Dialog
     */
    private Dialog buildShowWebsiteErrorDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dev_website_error_title))
                .setMessage(getString(R.string.dev_website_error_msg))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();
    }
}
