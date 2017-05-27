package com.example.deepak.phonetics;

/**
 * Created by deepak on 5/24/2017.
 */
import java.lang.reflect.Method;
//import com.android.internal.telephony.ITelephony;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class PhoneCallReceiver extends BroadcastReceiver {
    Context context = null;
    private static final String TAG = "Phone call";
    private ITelephony telephonyService;

    @Override
    public void onReceive(Context context, Intent intent) {                                         // 2
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);                         // 3
        String msg = "Phone state changed to " + state;

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {                                   // 4
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);  // 5
            msg += ". Incoming number is " + incomingNumber;

            // TODO This would be a good place to "Do something when the phone rings" ;-)

        }

        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();

    }
}
