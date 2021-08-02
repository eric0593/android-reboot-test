package com.odmsz.reboottest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;


public class BootReceiver extends BroadcastReceiver{
	static final String boot_broadcast = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context,Intent intent){
		KeyguardManager keyguardManager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
		KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("");
		keyguardLock.disableKeyguard();
		Intent boot_intent = new Intent(context,MainActivity.class);
		boot_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(boot_intent);
	}
}