package com.example.deepak.phonetics;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import java.util.Locale;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by deepak on 6/11/2017.
 */

public class Settings extends AppCompatActivity{

    private static Speaker speaker;
    TextToSpeech t1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_setting);

        Button speechTest = (Button) findViewById(R.id.speechTest);
        Button setpreferences = (Button) findViewById(R.id.setPreferences);


      /*  speechTest.setOnClickListener(new View.OnClickListener() {
            @Override
            //On click function
            public void onClick(View view) {
                //Create the intent to start another activity
            //    Intent intent = new Intent(view.getContext(), Call_set.class);
              //  startActivity(intent);
                speaker.allow(true);
                speaker.speak("Welcome to Phonetics.");

            }
        });*/

        setpreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            //On click function
            public void onClick(View view) {
                //Create the intent to start another activity
                Intent intent = new Intent(view.getContext(), Settings.class);
                startActivity(intent);
            }
        });


        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        speechTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = "Welcome to Phonetics.";//ed1.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });


    }
    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }


}
