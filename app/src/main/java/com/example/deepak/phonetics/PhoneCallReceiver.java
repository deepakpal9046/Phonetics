package com.example.deepak.phonetics;

/**
 * Created by deepak on 5/24/2017.
 */
import java.lang.reflect.Method;
//import com.android.internal.telephony.ITelephony;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import java.util.Locale;
import android.util.Log;
import android.widget.Toast;

public class PhoneCallReceiver extends BroadcastReceiver {

    Context context = null;
    private static Speaker speaker;
    private static final String TAG = "Phone call";
    private ITelephony telephonyService;
    String name="";
    TextToSpeech ttobj;
    @Override
    public void onReceive(Context context, Intent intent) {                                         // 2
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);                         // 3
        String msg = "Phone state changed to " + state;

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {                                   // 4
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);  // 5
            msg += ". Incoming number is " + incomingNumber;

            // TODO This would be a good place to "Do something when the phone rings" ;-)
            name=getContactDisplayNameByNumber(incomingNumber,context);
            msg +="And Name is Mr. "+name;

            String msg1 = "Incoming Call From Mr."+ name;
           // String toSpeak = msg1.toString();

          ttobj=new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    ttobj.setLanguage(Locale.UK);
                }
            }
        });

        //    Toast.makeText(context.getApplicationContext(), msg1,Toast.LENGTH_SHORT).show();
                Toast.makeText(context, msg1, Toast.LENGTH_LONG).show();
                ttobj.speak(msg1, TextToSpeech.QUEUE_FLUSH, null);
               // speaker.allow(true);
              //  speaker.speak(msg1);



        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();

    }

    public String getContactDisplayNameByNumber(String number,Context context) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        name = "Incoming call from";

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, null, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                // this.id =
                // contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                // String contactId =
                // contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }else{
                name = "Unknown number";
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

}
