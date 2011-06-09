package com.redpantssoft.hhfootball;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Builds a Preference Activity for the HandHeld Football game
 */
public class Settings extends PreferenceActivity {

    /**
     * Called on activity creation to inflate the context from the
     * defined settings
     *
     * @param savedInstanceState  Saved instance state for activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

}