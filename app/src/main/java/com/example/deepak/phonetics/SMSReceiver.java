package com.example.deepak.phonetics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.widget.Toast;

/**
 * Created by deepak on 6/11/2017.
 */
    public class SMSReceiver  extends BroadcastReceiver {
        String phone;

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle!=null){
                Object[] pdus = (Object[])bundle.get("pdus");
                for(int i=0;i<pdus.length;i++){
                    byte[] pdu = (byte[])pdus[i];
                    SmsMessage message = SmsMessage.createFromPdu(pdu);
                    String text = message.getDisplayMessageBody();
                    String sender = getContactName(message.getOriginatingAddress(),context);

                    // SMSService.mTts.speak("you have a Message"+text+"from "+sender, TextToSpeech.QUEUE_FLUSH,null);

                    Toast.makeText(context,"Message :"+text+"Sender:"+sender,Toast.LENGTH_LONG).show();
                }
            }
        }



        private String getContactName(String phone,Context context){
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
            String projection[] = new String[]{ContactsContract.Data.DISPLAY_NAME};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if(cursor.moveToFirst()){
                return cursor.getString(0);
            }else {
                return "unknown number";
            }
        }

    }


