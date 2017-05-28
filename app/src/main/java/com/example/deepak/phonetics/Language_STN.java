package com.example.deepak.phonetics;

/**
 * Created by deepak on 5/28/2017.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Language_STN extends Activity{

        /** Called when the activity is first created. */
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.language_setting);

            Button next = (Button) findViewById(R.id.LAN_BTN);
            next.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                }

            });
        }
}
