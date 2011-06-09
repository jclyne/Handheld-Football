package com.redpantssoft;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to simplify handling of sound effects that use the MediaPlayer interface
 * <p/>
 * A SoundFxManager object is initialized with the context of the current activity and
 * handles interactions with the AudioManager on the context's behalf, including getting
 * the correct media volume
 * <p/>
 * It maintains a table of sound effects, that can be added to and removed from dynamically.
 * All sound effects are stored in the project as raw resources and are added via their
 * resource ID. Each effect can be played at a different volume, specified as the percentage
 * of AudioManager Stream volume. Each effect can be individually paused as well. Once an effect
 * is added to the SoundFxManager, it can be played or stopped by referencing its user-specified
 * key in the table.
 */
public class SoundFxManager {
    /**
     * Logging Tag
     */
    private static final String TAG = "SoundFxManager";

    /**
     * Class to represent a specific sound effect in the the sound effect
     * table. Each effect can have a specific volume and can be paused. The
     * effect should be loaded from a raw resource with the id 'resid'
     */
    private class SoundEffect {
        /**
         * MediaPlayer instance for the effect
         */
        final MediaPlayer player;

        /**
         * Resource id of the sound resource played by the effect
         */
        final int resid;

        /**
         * Current volume of the effect. There is a volume set for the
         * AudioManager stream that the MediaPlayer uses. Each MediaPlayer
         * can play at its own volume. This setting will be multiplied by the
         * stream volume. A value of 1 makes the volume the same.
         */
        private float volume = 1;

        /**
         * Indicates if the effect is paused
         */
        private boolean paused = false;

        /**
         * @param resid  resource id of the audio file
         * @param player instance of a MediaPlayer to handle playback
         */
        SoundEffect(int resid, MediaPlayer player) {
            this.player = player;
            this.resid = resid;
        }

        /**
         * @return current effect volume
         */
        float getVolume() {
            return volume;
        }

        /**
         * @param volume new volume value for the effect
         */
        void setVolume(float volume) {
            this.volume = volume;
        }

        /**
         * @return returns the boolean flag indicating if the effect is paused
         */
        boolean isPaused() {
            return paused;
        }

        /**
         * @param paused new value of the paused flag for the effect
         */
        void setPaused(boolean paused) {
            this.paused = paused;
        }
    }

    /**
     * Current context of the owning activity
     */
    private final Context context;

    /**
     * Reference to the owning context's audio manager. This is mainly used
     * to get the system volume
     */
    private AudioManager audioManager;

    /**
     * Constant representing the audio manager stream used by the MediaPlayer
     */
    private final int AUDIO_MANAGER_STREAM = AudioManager.STREAM_MUSIC;

    /**
     * Table that maps an integer key to a sound effect. This the table of
     * effects managed by the SoundFXManager
     */
    private Map<Integer, SoundEffect> sfxTable;

    /**
     * Flag to indicate if all sound effects are muted
     */
    private boolean mute = false;

    /**
     * Flag to indicate if all sound effects are paused
     */
    private boolean paused = false;


    /**
     * @param activity reference to the current owning activity
     */
    public SoundFxManager(Activity activity) {
        this.context = activity;
        initialize(activity);
    }


    /**
     * Initializes the sound effects table as well as the audio manager reference and
     * preferred stream for hardware volume control
     *
     * @param activity reference to the current owning activity
     */
    private void initialize(Activity activity) {
        sfxTable = new HashMap<Integer, SoundEffect>();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        activity.setVolumeControlStream(AUDIO_MANAGER_STREAM);

        Log.i(TAG, "SoundFxManager initialized");
    }

    /**
     * Returns the current AudioManager stream volume. This corresponds
     * to the system volume set by the  hardware volume control. It is returned
     * as a percentage of maximum volume. It will return 0 if the soundFxManager
     * is muted.
     *
     * @return current system volume as a percentage of the maximum volume
     */
    private float getSystemVolume() {
        if (mute)
            return 0;

        return (float) audioManager.getStreamVolume(AUDIO_MANAGER_STREAM) /
                (float) audioManager.getStreamMaxVolume(AUDIO_MANAGER_STREAM);
    }

    /**
     * Adds a sound effect to the Manager's effect table.
     * <p/>
     * If an effect with the same key already exists, that effect
     * will be replaced with the new sound file.
     *
     * @param key   desired key to reference this effect
     * @param resid resource id of the sound file to load
     * @see #playSfx(int, boolean)
     * @see #setSfxVolume(int, float)
     * @see #stopSfx(int)
     * @see #release()
     */
    public void addSfx(int key, int resid) {
        if (sfxTable.containsKey(key)) {
            SoundEffect stream = sfxTable.get(key);
            if (stream.resid != resid) {
                Log.i(TAG, String.format("Unloading existing sfx: %d", stream.resid));
                stream.player.release();
            } else
                return;
        }


        // Note, using the create method will create a MediaPlayer in the prepared
        // state, not idle state.
        //  http://developer.android.com/reference/android/media/MediaPlayer.html#StateDiagram
        SoundEffect stream = new SoundEffect(resid, MediaPlayer.create(context, resid));
        sfxTable.put(key, stream);

        Log.i(TAG, String.format("Added new sfx: %d", stream.resid));
    }

    /**
     * Configures the volume for a specific effect.
     * <p/>
     * The volume should be a float the represents the percentage of the
     * system volume to play the effect at. It cannot be played higher than
     * the system volume, but may be lower based on the percentage. This is
     * useful for looping sounds that need to be quieter in the background.
     *
     * @param key    key of effect to modify volume on
     * @param volume volume as a percentage of system volume
     * @return true if the operation succeeded, false otherwise. All errors are logged.
     */
    public boolean setSfxVolume(int key, float volume) {
        assert (volume >= 0 && volume <= 1);

        if (!sfxTable.containsKey(key)) {
            Log.e(TAG, String.format("Undefined sfx key '%s' in 'setSfxVolume'", key));
            return false;
        }

        sfxTable.get(key).setVolume(volume);
        return true;
    }

    /**
     * Plays a specific sound effect
     * <p/>
     * The effect can be optionally looped indefinitely. The effect can be
     * stopped with {@link #stopSfx(int)}, regardless of whether it is looped
     * or not.
     *
     * @param key  key of effect to modify volume on
     * @param loop flag to indicate whether the effect should be played once or looped
     * @return true if the operation succeeded, false otherwise. All errors are logged.
     * @see #stopSfx(int)
     */
    public boolean playSfx(int key, boolean loop) {

        if (!sfxTable.containsKey(key)) {
            Log.e(TAG, String.format("Undefined sfx key '%s' in 'playSfx'", key));
            return false;
        }

        SoundEffect effect = sfxTable.get(key);

        // This is required to maintain the MediaPlayer state machine
        //  http://developer.android.com/reference/android/media/MediaPlayer.html#StateDiagram
        effect.player.stop();
        try {
            effect.player.prepare();
        } catch (IllegalStateException ignored) {
        } catch (IOException e) {
            Log.e(TAG, String.format("Error in playSfx"), e);
            return false;
        }

        //Apply the effect specific volume
        float vol = getSystemVolume() * effect.getVolume();
        effect.player.setVolume(vol, vol);

        effect.player.setLooping(loop);
        effect.player.start();
        return true;
    }

    /**
     * Stops a currently playing sound effect
     *
     * @param key of effect to stop
     * @return true if the operation succeeded, false otherwise. All errors are logged.
     * @see #playSfx(int, boolean)
     */
    public boolean stopSfx(int key) {

        if (!sfxTable.containsKey(key)) {
            Log.e(TAG, String.format("Undefined sfx key '%s' in 'stopSfx'", key));
            return false;
        }

        SoundEffect effect = sfxTable.get(key);
        try {
            effect.player.stop();
        } catch (IllegalStateException ignored) {
        }

        return true;
    }


    /**
     * Releases a sound effect from the manager's table
     *
     * @param key key of effect to release
     * @return true if the operation succeeded, false otherwise. All errors are logged.
     */
    public boolean release(int key) {

        if (!sfxTable.containsKey(key)) {
            Log.e(TAG, String.format("Undefined sfx key '%s' in 'release'", key));
            return false;
        }

        sfxTable.get(key).player.release();
        sfxTable.remove(key);

        return true;
    }

    /**
     * Releases all the sound effects in the manager's table
     */
    public void release() {
        for (SoundEffect stream : sfxTable.values()) {
            stream.player.release();
        }
        sfxTable.clear();
    }

    /**
     * Mutes all currently playing sound effects
     *
     * @param mute flag to enable or disable muting
     */
    public void setMute(boolean mute) {
        this.mute = mute;

        for (SoundEffect stream : sfxTable.values()) {
            if (stream.player.isPlaying() || stream.isPaused()) {
                float level = getSystemVolume() * stream.getVolume();
                stream.player.setVolume(level, level);
            }
        }

    }

    /**
     * Pauses all currently playing sound effects
     *
     * @see #resume()
     */
    public void pause() {
        if (!paused) {
            paused = true;
            for (SoundEffect stream : sfxTable.values()) {
                if (stream.player.isPlaying()) {
                    stream.setPaused(true);
                    stream.player.pause();
                }
            }
        }
    }

    /**
     * Resumes the active sound effects if they are paused
     *
     * @see #pause()
     */
    public void resume() {
        if (paused) {
            paused = false;
            for (SoundEffect stream : sfxTable.values()) {
                if (stream.isPaused()) {
                    stream.setPaused(false);
                    stream.player.start();
                }
            }
        }
    }
}

