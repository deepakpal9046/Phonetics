package com.example.deepak.phonetics;

import java.io.IOException;
import java.util.Locale;

//import com.google.tts.TextToSpeechBeta;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsMessage;
import android.util.Log;

public class TTSNotifierService extends Service {

	public volatile static TTSNotifierLanguage myLanguage = null;
	public static final int MY_TTS_DATA_CHECK_CODE = 31337;
	private static TextToSpeech  tts;

	private static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
	private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private static final String ACTION_BATTERY_LOW = "android.intent.action.ACTION_BATTERY_LOW";
	private static final String ACTION_MEDIA_BAD_REMOVAL = "android.intent.action.ACTION_MEDIA_BAD_REMOVAL";
	private static final String ACTION_BOOT_COMPLETED = "android.intent.action.ACTION_BOOT_COMPLETED";
	private static final String ACTION_PROVIDER_CHANGED = "android.intent.action.ACTION_PROVIDER_CHANGED";
	private static final String ACTION_MEDIA_MOUNTED = "android.intent.action.ACTION_MEDIA_MOUNTED";
	private static final String ACTION_MEDIA_UNMOUNTED = "android.intent.action.ACTION_MEDIA_UNMOUNTED";
	private static final String ACTION_PICK_WIFI_NETWORK = "android.net.wifi.PICK_WIFI_NETWORK";
	private static final String ACTION_WIFI_STATE_CHANGE = "android.net.wifi.STATE_CHANGE";
	private static final String ACTION_SUPPLICANT_CONNECTION_CHANGE_ACTION  = "android.net.wifi.supplicant.CONNECTION_CHANGE";

	private static final int MEDIUM_THREADWAIT = 300;
	private static final int SHORT_THREADWAIT = 50;

	public volatile static TTSDispatcher myTts = null;
	public volatile static boolean ttsReady = false;
	private volatile MediaPlayer myRingTonePlayer = null;
	private volatile boolean stopRingtone = false;

	private Context context;	private TelephonyManager mTelephonyManager;

    private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;
	private SharedPreferences mPrefs;
	private AudioManager mAudioManager;
	private Thread mRingtoneThread;

	// State
	private boolean silentMode = false;
	private volatile int oldStreamRingtoneVolume = 0;
	private volatile int oldStreamMusicVolume = 0;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.v("TTSNotifierService", "onCreate()");
		context = getApplicationContext();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		HandlerThread thread;
		setLanguage(mPrefs.getBoolean("cbxChangeLanguage", false), mPrefs.getString("txtLanguage", "English"));
		if (mPrefs.getBoolean("cbxRunWithHighPriority", false))
			thread = new HandlerThread("HandleIntentTTSNotifier", Process.THREAD_PRIORITY_URGENT_AUDIO);
		else
			thread = new HandlerThread("HandleIntentTTSNotifier", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.v("TTSNotifierService", "onStart()");
		if (myTts == null) {
			myTts = new TTSDispatcher(context);
		}
		if (mPrefs.getBoolean("cbxChangeLanguage", false))
			setLanguageTts(myLanguage.getLocale());
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.sendMessage(msg);
	}

	@Override
	public void onDestroy() {
		stopRingtone = true;
		ttsReady = false;
		if (tts != null)
			tts.shutdown();
		tts = null;
		if (myRingTonePlayer != null)
			myRingTonePlayer.release();
		myRingTonePlayer = null;
		mServiceLooper.quit();
	} 

	private final class ServiceHandler extends Handler {

		public ServiceHandler(Looper serviceLooper) {
			super(serviceLooper);
		}

		@Override
		public void handleMessage(Message msg) {
			Log.v("TTSNotifierService", "handleMessage()");

			Intent intent = (Intent) msg.obj;
			if (intent == null) return;
			String action = intent.getAction();

			readState();
			storeAndUpdateVolume();

			boolean cbxEnable = mPrefs.getBoolean("cbxEnable", false);
			boolean cbxObeySilentMode = mPrefs.getBoolean("cbxObeySilentMode", true);

			if (action == null) return;
			if (!cbxEnable) return;
			if (cbxObeySilentMode && silentMode) return;

			// When calling ignore other notifications
			if (!ACTION_PHONE_STATE.equals(action) && (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE))
				return;

			Log.v("TTSNotifierService", "Action: " + action);

			if (ACTION_PHONE_STATE.equals(action)) {
				handleACTION_PHONE_STATE(intent);
			} else if (ACTION_SMS_RECEIVED.equals(action)) {
				handleACTION_SMS_RECEIVED(intent);
			} else if (ACTION_BATTERY_LOW.equals(action)) {
				handleACTION_BATTERY_LOW(intent);
			} else if (ACTION_MEDIA_BAD_REMOVAL.equals(action)) {
				handleACTION_MEDIA_BAD_REMOVAL(intent);
			} else if (ACTION_BOOT_COMPLETED.equals(action)) {
				handleACTION_BOOT_COMPLETED(intent);
			} else if (ACTION_PROVIDER_CHANGED.equals(action)) {
				handleACTION_PROVIDER_CHANGED(intent);
			} else if (ACTION_MEDIA_MOUNTED.equals(action)) {
				handleACTION_MEDIA_MOUNTED(intent);
			} else if (ACTION_MEDIA_UNMOUNTED.equals(action)) {
				handleACTION_MEDIA_UNMOUNTED(intent);
			} else if (ACTION_PICK_WIFI_NETWORK.equals(action)) {
				handleACTION_PICK_WIFI_NETWORK(intent);
			} else if (ACTION_WIFI_STATE_CHANGE.equals(action)) {
				handleACTION_WIFI_STATE_CHANGE(intent);
			} else if (ACTION_SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)) {
				handleSUPPLICANT_CONNECTION_CHANGE_ACTION(intent);
			}
			// ACTION_PHONE_STATE is different because we are using a thread in it
			if (!ACTION_PHONE_STATE.equals(action))
				restoreVolume();
		}
	}

	public static void setLanguage(boolean changeLanguage, String language) {
		if (!changeLanguage)
			language = java.util.Locale.getDefault().getLanguage();
		if (language.equals("English")
				|| language.equals("en_US")
				|| language.equals("en_GB")
				|| language.equals("en_CA")
				|| language.equals("en_AU")
				|| language.equals("en_NZ")
				|| language.equals("en_SG"))
			myLanguage = new TTSNotifierLanguageEN();
		else if (language.equals("Nederlands")
				|| language.equals("nl_NL")
				|| language.equals("nl_BE"))
			myLanguage = new TTSNotifierLanguageNL();
		else if (language.equals("Franais")
				|| language.equals("Français")
				|| language.equals("fr_FR")
				|| language.equals("fr_BE")
				|| language.equals("fr_CA")
				|| language.equals("fr_CH"))
			myLanguage = new TTSNotifierLanguageFR();
		else if (language.equals("Deutsch")
				|| language.equals("de_DE")
				|| language.equals("de_AT")
				|| language.equals("de_CH")
				|| language.equals("de_LI"))
			myLanguage = new TTSNotifierLanguageDE();
		else 
			myLanguage = new TTSNotifierLanguageEN();
		setLanguageTts(myLanguage.getLocale());
	}



	public static void setLanguageTts(Locale languageShortName) {
		if (tts != null) {
			tts.setLanguage(languageShortName);
		}		
	}

	private void readState() {
		if (mAudioManager != null)
			silentMode = mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
	}

	private void storeAndUpdateVolume() {
		if (mPrefs.getBoolean("cbxChangeVolume", false) && mAudioManager != null && mRingtoneThread == null) {
			int intOptionsTTSVolume = Math.min(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), Math.max(0, Integer.parseInt(mPrefs.getString("intOptionsTTSVolume", "14"))));
			oldStreamMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			oldStreamRingtoneVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < intOptionsTTSVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > intOptionsTTSVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
		}
	}

	private void restoreVolume() {
		if (mPrefs.getBoolean("cbxChangeVolume", false) && mAudioManager != null) {
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > oldStreamMusicVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < oldStreamMusicVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_RING) < oldStreamRingtoneVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, 0);			
		}
	}

	public static void waitForSpeechInitialised() {
		while (!ttsReady) {
			try {
				Thread.sleep(MEDIUM_THREADWAIT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void waitForSpeechFinished() {
		try {
			Thread.sleep(MEDIUM_THREADWAIT);
		} catch (InterruptedException e) { }
		while (tts.isSpeaking()) {
			try {
				Thread.sleep(MEDIUM_THREADWAIT);
			} catch (InterruptedException e) { }
		}
		try {
			Thread.sleep(MEDIUM_THREADWAIT);
		} catch (InterruptedException e) { }
	}

	private void waitForRingTonePlayed() {
		do {
			try {
				Thread.sleep(MEDIUM_THREADWAIT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (myRingTonePlayer.isPlaying());
	}

	private void waitForRingToneThreadStopped() {
		while (stopRingtone) {
			try {
				Thread.sleep(SHORT_THREADWAIT);
			} catch (InterruptedException e) { 
				e.printStackTrace();
			}
		}
	}

	private void playRingtone(boolean waitForFinish) throws IllegalStateException, IOException {
		Log.v("TTSNotifierService", "playRingtone()" + myRingTonePlayer);
		if (myRingTonePlayer == null) return;
		myRingTonePlayer.start();
		if (waitForFinish)
			waitForRingTonePlayed();
	}

/*	public static void speak(String str, boolean waitForFinish) {
		Log.v("TTSNotifierService", "speak():" + myTts);
		if (myTts == null) return;
		waitForSpeechInitialised();
		tts.speak(str, 0, null);
		if (waitForFinish)
			waitForSpeechFinished();
	} */

	private void handleACTION_PHONE_STATE(Intent intent) {
		if (!mPrefs.getBoolean("cbxEnableIncomingCall", true)) return;
		final String txtOptionsIncomingCall;
		// Preferences
		if (mPrefs.getBoolean("cbxOptionsIncomingCallUserDefinedText", false))
			txtOptionsIncomingCall = mPrefs.getString("txtOptionsIncomingCall", myLanguage.getTxtOptionsIncomingCall());
		else
			txtOptionsIncomingCall = myLanguage.getTxtOptionsIncomingCall();
		final boolean cbxOptionsIncomingCallUseNoteField = mPrefs.getBoolean("cbxOptionsIncomingCallUseNoteField", false);
		final boolean cbxOptionsIncomingCallUseTTSRingtone = mPrefs.getBoolean("cbxOptionsIncomingCallUseTTSRingtone", false);
		final String txtOptionsIncomingCallRingtone = mPrefs.getString("txtOptionsIncomingCallRingtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
		final int intOptionsIncomingCallMinimalRingCountBeforeTTS = Integer.parseInt(mPrefs.getString("intOptionsIncomingCallMinimalRingCountBeforeTTS", "2"));
		final int intOptionsIncomingCallRingCountAfterTTS = Integer.parseInt(mPrefs.getString("intOptionsIncomingCallRingCountAfterTTS", "2"));
		final int intOptionsIncomingCallTTSRepeats = Integer.parseInt(mPrefs.getString("intOptionsIncomingCallTTSRepeats", "2"));
		final int intOptionsIncomingCallDelayBetweenRingtones = Integer.parseInt(mPrefs.getString("intOptionsIncomingCallDelayBetweenRingtones", "200"));
		final String phoneNr = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		// Logic
		if (mRingtoneThread != null)
			stopRingtone = true;
		if (tts != null)
			tts.stop();
		if (myRingTonePlayer != null) {
			myRingTonePlayer.stop();
			try {
				myRingTonePlayer.prepare();
			} catch (IllegalStateException e) {
				myRingTonePlayer = null;
			} catch (IOException e) {
				myRingTonePlayer = null;
			}
		}
		if (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
			waitForRingToneThreadStopped();
			mRingtoneThread = new Thread() {
				public void run() {
					try {
						if (cbxOptionsIncomingCallUseTTSRingtone) {
							while (mAudioManager.getStreamVolume(AudioManager.STREAM_RING) > 1)
								mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_LOWER, 0);
							if (myRingTonePlayer == null)
								myRingTonePlayer = MediaPlayer.create(context, Uri.parse(txtOptionsIncomingCallRingtone));
							int ringtoneState = 0;
							int nrTTS = 0;
							int ringCounter = 1;
							while (!stopRingtone) {
								Log.v("TTSNotifierService", "CALL STATE :" + ringtoneState);
								switch (ringtoneState) {
								case 0:
									playRingtone(true);
									break;
								case 1:
									if (nrTTS >= intOptionsIncomingCallTTSRepeats) {
										ringCounter += 1;
										ringtoneState = -1;
										try {
											Thread.sleep(intOptionsIncomingCallDelayBetweenRingtones);
										} catch (InterruptedException e) { }
									} else if (nrTTS == 0) {
										if (!ttsReady || ringCounter < intOptionsIncomingCallMinimalRingCountBeforeTTS) {
											ringCounter += 1;
											ringtoneState = -1;
											try {
												Thread.sleep(intOptionsIncomingCallDelayBetweenRingtones);
											} catch (InterruptedException e) { }
										}
									} else {
										if (!ttsReady || ringCounter < intOptionsIncomingCallRingCountAfterTTS) {
											ringCounter += 1;
											ringtoneState = -1;
											try {
												Thread.sleep(intOptionsIncomingCallDelayBetweenRingtones);
											} catch (InterruptedException e) { }
										}
									}
									break;
								case 2:
									tts.speak(String.format(txtOptionsIncomingCall, getContactNameFromNumber(phoneNr, cbxOptionsIncomingCallUseNoteField)),TextToSpeech.QUEUE_FLUSH, null);
									nrTTS += 1;
									break;
								case 3:
									ringtoneState = -1;
									ringCounter = 1;
									try {
										Thread.sleep(intOptionsIncomingCallDelayBetweenRingtones);
									} catch (InterruptedException e) { }
									break;
								}
								ringtoneState += 1;
							}
						} else {
							tts.speak(String.format(txtOptionsIncomingCall, getContactNameFromNumber(phoneNr, cbxOptionsIncomingCallUseNoteField)), TextToSpeech.QUEUE_FLUSH, null);
						}
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					mRingtoneThread = null;
					stopRingtone = false;
				}
			};
			mRingtoneThread.start();
		} else {
			restoreVolume();
		}
	}
	
	@SuppressWarnings("deprecation")
	private void handleACTION_SMS_RECEIVED(Intent intent) {
		if (!mPrefs.getBoolean("cbxEnableIncomingSMS", true)) return;
		final boolean cbxOptionsIncomingSMSUseNoteField = mPrefs.getBoolean("cbxOptionsIncomingSMSUseNoteField", false);
		String txtOptionsIncomingSMS;
		// Preferences
		if (mPrefs.getBoolean("cbxOptionsIncomingSMSOnlyAnnouncePerson", true))
			txtOptionsIncomingSMS = myLanguage.getTxtOptionsIncomingSMS();
		else if (mPrefs.getBoolean("cbxOptionsIncomingSMSUserDefinedText", true))
			txtOptionsIncomingSMS = mPrefs.getString("txtOptionsIncomingSMS", myLanguage.getTxtOptionsIncomingSMSBody());
		else
			txtOptionsIncomingSMS = myLanguage.getTxtOptionsIncomingSMSBody();
		// Logic
		SmsMessage[] messages = getMessagesFromIntent(intent);
		if (messages == null) return;
		SmsMessage sms = messages[0];
		if (sms.getMessageClass() != SmsMessage.MessageClass.CLASS_0 && !sms.isReplace()) {
			String body;
			if (messages.length == 1) {
				body = messages[0].getDisplayMessageBody();
			} else {
				StringBuilder bodyText = new StringBuilder();
				for (int i = 0; i < messages.length; i++) {
					bodyText.append(messages[i].getMessageBody());
				}   
				body = bodyText.toString();
			}
			String address = messages[0].getDisplayOriginatingAddress();
			tts.speak(String.format(txtOptionsIncomingSMS, getContactNameFromNumber(address, cbxOptionsIncomingSMSUseNoteField), body), TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	public void handleACTION_BATTERY_LOW(Intent intent) {
		if (!mPrefs.getBoolean("cbxEnableBatteryLow", true)) return;
		String txtOptionsBatteryLowWarningText;
		// Preferences
		if (mPrefs.getBoolean("cbxOptionsBatteryLowWarningUserDefinedText", true))
			txtOptionsBatteryLowWarningText = mPrefs.getString("txtOptionsBatteryLowWarningText", myLanguage.getTxtOptionsBatteryLowWarningText());
		else
			txtOptionsBatteryLowWarningText = myLanguage.getTxtOptionsBatteryLowWarningText();
		tts.speak(txtOptionsBatteryLowWarningText,TextToSpeech.QUEUE_FLUSH, null);
	}

	public void handleACTION_MEDIA_BAD_REMOVAL(Intent intent) {
		if (!mPrefs.getBoolean("cbxEnableBadMediaRemoval", true)) return;
		String txtOptionsMediaBadRemovalText;
		if (mPrefs.getBoolean("cbxOptionsMediaBadRemovalUserDefinedText", true))
			txtOptionsMediaBadRemovalText = mPrefs.getString("txtOptionsMediaBadRemovalText", myLanguage.getTxtOptionsMediaBadRemovalText());
		else
			txtOptionsMediaBadRemovalText = myLanguage.getTxtOptionsMediaBadRemovalText();
		tts.speak(txtOptionsMediaBadRemovalText, TextToSpeech.QUEUE_FLUSH, null);
	}

	public void handleACTION_BOOT_COMPLETED(Intent intent) { }

	public void handleACTION_PROVIDER_CHANGED(Intent intent) {
		if (!mPrefs.getBoolean("cbxEnableProviderChanged", true)) return;
		String txtOptionsProviderChangedText;
		if (mPrefs.getBoolean("cbxOptionsProviderChangedUserDefinedText", true))
			txtOptionsProviderChangedText = mPrefs.getString("txtOptionsProviderChangedText", myLanguage.getTxtOptionsProviderChangedText());
		else
			txtOptionsProviderChangedText = myLanguage.getTxtOptionsProviderChangedText();
		tts.speak(txtOptionsProviderChangedText,TextToSpeech.QUEUE_FLUSH, null);
	}

	@SuppressWarnings("unused")
	public void handleACTION_MEDIA_MOUNTED(Intent intent) {
		if (!mPrefs.getBoolean("cbxEnableMediaMounted", true)) return;
		if (true) return; // Disabled because TTS needs the SD card
		String txtOptionsMediaMountedText;
		if (mPrefs.getBoolean("cbxOptionsMediaMountedUserDefinedText", true))
			txtOptionsMediaMountedText = mPrefs.getString("txtOptionsMediaMountedText", myLanguage.getTxtOptionsMediaMountedText());
		else
			txtOptionsMediaMountedText = myLanguage.getTxtOptionsMediaMountedText();
		tts.speak(txtOptionsMediaMountedText,TextToSpeech.QUEUE_FLUSH, null);
	}

	public void handleACTION_MEDIA_UNMOUNTED(Intent intent) {
		if (!mPrefs.getBoolean("cbxEnableMediaUnMounted", true)) return;
		String txtOptionsMediaUnMountedText;
		if (mPrefs.getBoolean("cbxOptionsMediaUnMountedUserDefinedText", true))
			txtOptionsMediaUnMountedText = mPrefs.getString("txtOptionsMediaUnMountedText", myLanguage.getTxtOptionsMediaUnMountedText());
		else
			txtOptionsMediaUnMountedText = myLanguage.getTxtOptionsMediaUnMountedText();
		tts.speak(txtOptionsMediaUnMountedText,TextToSpeech.QUEUE_FLUSH, null);
	}

	public void handleACTION_PICK_WIFI_NETWORK(Intent intent) {
		// Preferences
		boolean cbxEnableWifiDiscovered = mPrefs.getBoolean("cbxEnableWifiDiscovered", true);
		String txtOptionsWifiDiscovered;
		if (mPrefs.getBoolean("cbxOptionsWifiDiscoveredUserDefinedText", false))
			txtOptionsWifiDiscovered = mPrefs.getString("txtOptionsWifiDiscovered", myLanguage.getTxtOptionsWifiDiscovered());
		else
			txtOptionsWifiDiscovered = myLanguage.getTxtOptionsWifiDiscovered();
		// Logic
		if (!cbxEnableWifiDiscovered) return;
		tts.speak(txtOptionsWifiDiscovered,TextToSpeech.QUEUE_FLUSH, null);
	}

	private void handleACTION_WIFI_STATE_CHANGE(Intent intent) {
		// Preferences
		boolean cbxEnableWifiConnect = mPrefs.getBoolean("cbxEnableWifiConnect", true);
		String txtOptionsWifiConnected;
		if (mPrefs.getBoolean("cbxOptionsWifiConnectedUserDefinedText", false))
			txtOptionsWifiConnected = mPrefs.getString("txtOptionsWifiConnected", myLanguage.getTxtOptionsWifiConnected());
		else
			txtOptionsWifiConnected = myLanguage.getTxtOptionsWifiConnected();
		NetworkInfo ni = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		if (ni == null) return;
		if (cbxEnableWifiConnect && ni.getState() == NetworkInfo.State.CONNECTED)
			tts.speak(txtOptionsWifiConnected, TextToSpeech.QUEUE_FLUSH, null);
	}

	private void handleSUPPLICANT_CONNECTION_CHANGE_ACTION(Intent intent) {
		// Preferences
		boolean cbxEnableWifiDisconnect = mPrefs.getBoolean("cbxEnableWifiDisconnect", true);
		String txtOptionsWifiDisconnected;
		if (mPrefs.getBoolean("cbxOptionsWifiDisconnectedUserDefinedText", true))
			txtOptionsWifiDisconnected = mPrefs.getString("txtOptionsWifiDisconnected", myLanguage.getTxtOptionsWifiDisconnected());
		else
			txtOptionsWifiDisconnected = myLanguage.getTxtOptionsWifiDisconnected();
		// Logic
		if (cbxEnableWifiDisconnect && !intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, true))
			tts.speak(txtOptionsWifiDisconnected, TextToSpeech.QUEUE_FLUSH, null);
	}

	private String getContactNameFromNumber(String number, boolean useNote) {
		String[] projection = new String[] {
				CommonDataKinds.Note.NOTE, Phone.DISPLAY_NAME };
		Uri contactUri = Uri.withAppendedPath(Phone.CONTENT_URI, Uri.encode(number));
		Cursor c = getContentResolver().query(contactUri,
				projection,
				Phone.NUMBER + "=?",
                new String[]{String.valueOf(number)},
                null);
		String name = myLanguage.getTxtUnknown();
		if (c.moveToFirst()) {
			String notes = c.getString(c.getColumnIndex(CommonDataKinds.Note.NOTE)); 
			String dname = c.getString(c.getColumnIndex(Phone.DISPLAY_NAME)); 		
			if(!useNote || notes == null || notes.length()==0 ) { 
				name = dname;
			} else {
				name = notes;
			}
		}
		return name;
	}

	@SuppressWarnings("deprecation")
	private SmsMessage[] getMessagesFromIntent(Intent intent)
	{
		SmsMessage retMsgs[] = null;
		Bundle bdl = intent.getExtras();
		try{
			Object pdus[] = (Object [])bdl.get("pdus");
			retMsgs = new SmsMessage[pdus.length];
			for(int n=0; n < pdus.length; n++)
			{
				byte[] byteData = (byte[])pdus[n];
				retMsgs[n] =
					SmsMessage.createFromPdu(byteData);
			}        
		}
		catch(Exception e)
		{
			Log.e("GetMessages", "fail", e);
		}
		return retMsgs;
	}

	public static void beginStartingService(Context context, Intent intent) {
		if (TTSDispatcher.isTtsInstalled(context))
			context.startService(intent);
	}
}
