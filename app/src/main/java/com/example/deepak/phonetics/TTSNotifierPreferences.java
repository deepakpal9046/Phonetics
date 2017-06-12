package com.example.deepak.phonetics;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;

public class TTSNotifierPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Activity ctx = this;
		addPreferencesFromResource(R.xml.preferences);
		if (!TTSDispatcher.isTtsBetaInstalled(this)) {
			TTSDispatcher.notifyTTSBeta(ctx);
		}
		Intent svc = new Intent(this, TTSNotifierService.class);
		startService(svc);
		// Assign the buttons
		Preference customPref;
		customPref = (Preference) findPreference("btnTestTTS");
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				TTSNotifierService.waitForSpeechInitialised();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				TTSNotifierService.setLanguage(prefs.getBoolean("cbxChangeLanguage", false), prefs.getString("txtLanguage", "English"));
				TTSNotifierService.speak(TTSNotifierService.myLanguage.getTxtTest(), false);
				return true;
			}
		});
		customPref = (Preference) findPreference("btnInstallTTSBeta");
		if (TTSDispatcher.isTtsBetaInstalled(ctx)) {
			customPref.setEnabled(false);
		} else {
			customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					TTSDispatcher.installTTSBeta(ctx);
					return true;
				}
			});
		}
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		TTSNotifierService.setLanguage(prefs.getBoolean("cbxChangeLanguage", false), prefs.getString("txtLanguage", "English"));
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		TTSNotifierService.setLanguage(prefs.getBoolean("cbxChangeLanguage", false), prefs.getString("txtLanguage", "English"));
	}
}