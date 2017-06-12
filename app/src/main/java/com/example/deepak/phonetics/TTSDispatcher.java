package com.example.deepak.phonetics;

import java.util.HashMap;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

//import com.google.tts.TextToSpeechBeta;

public class TTSDispatcher {

	private volatile static boolean useOriginal = false;
	private volatile static TextToSpeech myTts = null;
    TextToSpeech ttsInitListener;
	
	public TTSDispatcher(Context context) {
        if (!isTtsInstalled(context)) {
            Toast.makeText(context, "No TTS engine found! Install 'eSpeak TTS' from the Play store", Toast.LENGTH_LONG).show();
        }

        ttsInitListener = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    ttsInitListener.setLanguage(Locale.UK);
                }
            }
        });
	/*	if (isTtsBetaInstalled(context)) {
			try {
				TextToSpeech.OnInitListener ttsInitListener = new TextToSpeech.OnInitListener() {
						@Override
						public void onInit(int arg0, int arg1) {
							Log.v("TTSNotifierService", "TTS INIT DONE");
							TTSNotifierService.setLanguageTts(TTSNotifierService.myLanguage.getLocale());
							myTts.speak("", 0, null);
							TTSNotifierService.ttsReady = true;
						}
					};
					myTts = new TextToSpeech(context, ttsInitListener);
					useOriginal = false;
			} catch (Exception e) { e.printStackTrace(); }
		} else {
			try {
				TextToSpeech.OnInitListener ttsInitListener = new TextToSpeech.OnInitListener() {
						@Override
						public void onInit(int arg0) {
							Log.v("TTSNotifierService", "TTS INIT DONE");
							TTSNotifierService.setLanguageTts(TTSNotifierService.myLanguage.getLocale());
							TTSNotifierService.myTts.speak("", 0, null);
							TTSNotifierService.ttsReady = true;
						}
					};
				myTts = new TextToSpeech(context, ttsInitListener);
				useOriginal = true;
			} catch (Exception e) { e.printStackTrace(); }
		}
	}  */
    }
	


	public static boolean isTtsBetaInstalled(Context ctx) {
		return false;
	}
	
	public static boolean isTtsBetaInstalled2(Context ctx) {
		try {
			PackageManager pm = ctx.getPackageManager();
	        Intent intent = new Intent("com.google.intent.action.START_TTS_SERVICE_BETA");
	        intent.addCategory("com.google.intent.category.TTS_BETA");
	        ResolveInfo info = pm.resolveService(intent, 0);
	        return info != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unused" })
	public static boolean isTtsInstalled(Context ctx) {
		if (isTtsBetaInstalled(ctx)) return true;
		try {
			String classToLoad = "android.speech.tts.TextToSpeech";
		    Class<?> c = Class.forName(classToLoad, false, ctx.getClass().getClassLoader());
		    Class[] args = new Class[1];
	    } catch (Exception e) {
	       	return false;
		}
		return true;
	}
	
	public void speak(String str, int i, HashMap<String, String> object) {
		myTts.speak(str, i, object);
	}
	public boolean isSpeaking() {
		return myTts.isSpeaking();
	}
	public void setLanguage(Locale languageShortName) {
		myTts.setLanguage(languageShortName);
	}
	public void stop() {
		myTts.stop();
	}
	public void shutdown() {
		myTts.shutdown();
		myTts = null;
	}

	public static void installTTSBeta(Activity ctx) {
		try {
			Toast.makeText(ctx, "Please install 'TTS Extended 3.1'.", Toast.LENGTH_LONG).show();
			Uri u = Uri.parse("http://eyes-free.googlecode.com/files/tts_3.1_market.apk");
			Intent i = new Intent(Intent.ACTION_VIEW, u);
			ctx.startActivityForResult(i, 0);
			ctx.finish();
		} catch (Exception e) {
			Toast.makeText(ctx, "Failed to launch a browser. Please install the 'TTS Extended 3.1' application.", Toast.LENGTH_LONG).show();
		}
	}



	public static void notifyTTSBeta(Activity ctx) {
		Toast.makeText(ctx, "Install 'tts_3.1_market.apk' from http://code.google.com/p/eyes-free/downloads/ store to improve TTS quality", Toast.LENGTH_LONG).show();
	}

}
