package com.arasio.tictactoe;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// show up-button on the action bar
		getActionBar().setDisplayHomeAsUpEnabled(true);
		// simple addPreferencesFromResource has been deprecated
		getFragmentManager().beginTransaction().replace(android.R.id.content, new GamePreferenceFragment()).commit();
    }

    public static class GamePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

	// add behaviour for action bar up-button
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: 
			onBackPressed();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
