package com.example.deepak.phonetics;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Declare our Text Variables and assign them views from the layout

        TextView answerLabel = (TextView) findViewById(R.id.Text_box);
        Button getAnswerButton = (Button) findViewById(R.id.LAN_BTN);

      //  getAnswerButton.setOnClickListener();
    }
}

