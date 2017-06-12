package com.example.deepak.phonetics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TTSNotifierReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		intent.setClass(context, TTSNotifierService.class);
		intent.putExtra("result", getResultCode());

		//intent.setAction("android.intent.action.MAIN");
		//intent.addCategory("android.intent.category.LAUNCHER"); 
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		TTSNotifierService.beginStartingService(context, intent);
	}

}
