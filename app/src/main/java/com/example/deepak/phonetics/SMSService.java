package com.example.deepak.phonetics;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/**
 * Created by deepak on 6/11/2017.
 */

public class SMSService extends Service implements TextToSpeech.OnInitListener{

        SharedPreferences mPreferences;
        Float pit;
        Float rate;
        public SMSService() {
        }

        public static TextToSpeech mTts;

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public void onStart(Intent intent, int startId) {
            // TODO Auto-generated method stub
            //   mPreferences = getSharedPreferences(Mysettings.PREF_NAME, Service.MODE_PRIVATE);
            mPreferences = getSharedPreferences("Vinod_Call_Reader", Service.MODE_PRIVATE);
            pit = Float.parseFloat(mPreferences.getString("pit","0.8"));
            rate = Float.parseFloat(mPreferences.getString("rate","1.1"));
            mTts = new TextToSpeech(this, this);
            super.onStart(intent, startId);
        }

        public void onInit(int status) {
            // TODO Auto-generated method stub
            if (status == TextToSpeech.SUCCESS) {
                if (mTts.isLanguageAvailable(Locale.UK) >= 0)

                    Toast.makeText( SMSService.this,
                            "Sucessfull intialization of Text-To-Speech engine Mytell ",
                            Toast.LENGTH_LONG).show();
                mTts.setLanguage(Locale.UK);

                mTts.setPitch(pit);
                mTts.setSpeechRate(rate);

            } else if (status == TextToSpeech.ERROR) {
                Toast.makeText(SMSService.this,
                        "Unable to initialize Text-To-Speech engine",
                        Toast.LENGTH_LONG).show();
            }
        }}



