package org.thomnichols.android.gmarks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class ActionReceiver extends BroadcastReceiver {

	static final String TAG = "GMARKS RECEIVER";
	PendingIntent intent;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG,"Got intent: " + intent);
		
		if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Intent action = new Intent(context, BackgroundService.class);
			action.setAction( Intent.ACTION_RUN );
			PendingIntent serviceLauncher = PendingIntent.getService(
					context, 0, action, 0);

			// Schedule the service startup for 20s after boot
            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            		SystemClock.elapsedRealtime() + 60*1000, serviceLauncher);

//		     Intent serviceLauncher = new Intent(context, BackgroundService.class);
//		     context.startService(serviceLauncher);
		  }
	}
}
