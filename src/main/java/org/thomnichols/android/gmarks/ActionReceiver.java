/* This file is part of GMarks. Copyright 2010, 2011 Thom Nichols
 *
 * GMarks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GMarks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GMarks.  If not, see <http://www.gnu.org/licenses/>.
 */
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

			// Schedule the service startup for 60s after boot
            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            		SystemClock.elapsedRealtime() + 60*1000, serviceLauncher);

//		     Intent serviceLauncher = new Intent(context, BackgroundService.class);
//		     context.startService(serviceLauncher);
		  }
	}
}
