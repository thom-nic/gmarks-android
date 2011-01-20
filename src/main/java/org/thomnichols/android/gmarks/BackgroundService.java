package org.thomnichols.android.gmarks;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

public class BackgroundService extends Service implements OnSharedPreferenceChangeListener {
	static final String TAG = "GMARKS BACKGROUND SVC";

	static final String KEY_SYNC_INTERVAL = "background_sync_interval";
	static final String KEY_BACKGROUND_SYNC_ENABLED = "background_sync_enabled";

	static final AtomicBoolean started = new AtomicBoolean(false);
	static final AtomicBoolean scheduled = new AtomicBoolean(false);
	int startID = -1;
	int DEFAULT_SYNC_INTERVAL = 60; // 1 hour in minutes
	boolean backgroundSyncEnabled = false;
	int backgroundSyncInterval = DEFAULT_SYNC_INTERVAL; // value is in minutes
	SharedPreferences syncPrefs;

	PendingIntent serviceLauncher = null; // created during onStart
	
	@Override
    public void onCreate() {
    	Log.d(TAG,"CREATED");
    	
    	Intent scheduledAction = new Intent(this, BackgroundService.class);
    	scheduledAction.setAction(Intent.ACTION_SYNC);
    	this.serviceLauncher = PendingIntent.getService(this, 0, scheduledAction, 0);

    	this.syncPrefs = PreferenceManager.getDefaultSharedPreferences(this);
//    	prefs.registerOnSharedPreferenceChangeListener(this);
    	this.backgroundSyncEnabled = syncPrefs.getBoolean(KEY_BACKGROUND_SYNC_ENABLED, false);
    	String intStr = syncPrefs.getString(KEY_SYNC_INTERVAL, ""+DEFAULT_SYNC_INTERVAL);
    	this.backgroundSyncInterval = Integer.parseInt(intStr); 
    	Log.d(TAG,"Enabled: " + backgroundSyncEnabled);
    	Log.d(TAG,"Interval: " + backgroundSyncInterval);
    }
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG,"STARTING: " + startId + " " + intent);
		super.onStart(intent, startId);
		this.startID = startId;
		
		if ( ! started.compareAndSet(false, true) )
			Log.w(TAG, "Already started!");
		
		final String action = intent.getAction();
		if ( Intent.ACTION_RUN.equals(action) ) {
			// schedule the recurring service then quit immediately.
			scheduleSync();
		}
		else if ( Intent.ACTION_SHUTDOWN.equals(action) ) {
			Log.d(TAG,"SHUTTING DOWN");
			// Disable recurring task then quit immediately
			unscheduleSync();
		}
		else if ( Intent.ACTION_SYNC.equals(action) ) {
			// this is the intent called by AlarmManager
			if ( backgroundSyncEnabled ) {
				Log.d(TAG,"STARTING BACKGROUND SYNC!");
				new BackgroundSyncTask(getApplicationContext()).execute();
				return; // don't stop the service yet.
			}
			else {
				Log.w(TAG,"Got SYNC action, but not enabled!");
				this.unscheduleSync();
			}
		}
		else if ( Intent.ACTION_CONFIGURATION_CHANGED.equals(action) ) {
			String key = intent.getStringExtra("key");
			if ( key != null ) {
				SharedPreferences sharedPreferences = 
					PreferenceManager.getDefaultSharedPreferences(this);
				this.onSharedPreferenceChanged(sharedPreferences, key);
			} else Log.w(TAG,"No 'key' extra for preference change!");
		}
		else Log.d(TAG, "Unknown action: " + action);
		this.stopSelf(this.startID);
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG,"DESTROY: " + this.startID);
		started.compareAndSet(true, false);
		super.onDestroy();
	}

	protected boolean scheduleSync() {
		return this.scheduleSync(true);
	}

	protected boolean scheduleSync(boolean first) {
		Log.d(TAG,"Scheduling task...");
		if ( ! this.backgroundSyncEnabled ) {
			Log.d(TAG, "Background sync not enabled!");
			return false;
		}
		if ( first && ! scheduled.compareAndSet(false, true) ) { 
			Log.d(TAG, "Already scheduled.");
			return false;
		}
		long intervalInMS = backgroundSyncInterval * 60 * 1000;
    	AlarmManager am = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
    	// sync doesn't need to happen while the phone is asleep.  Nor does it 
    	// need to occur any more frequently after it wakes up.  So set once
    	// and re-schedule the next occurrence when the operation completes.
    	long lastSync = syncPrefs.getLong(RemoteSyncTask.PREF_LAST_SYNC_ATTEMPT, 0);
    	long wakeUp = lastSync + intervalInMS; 
    	am.set( AlarmManager.RTC, wakeUp, serviceLauncher );
    	Log.d(TAG,"Scheduled for " + wakeUp);
        return true;
	}
	
	protected void unscheduleSync() {
		Log.d(TAG,"Stopping recurring task...");
		AlarmManager am = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        am.cancel(serviceLauncher);
        if ( ! scheduled.compareAndSet(true, false) )
        	Log.d(TAG, "Already unscheduled!");
	}

    /**
     * The remote sync task. 
     */
    class BackgroundSyncTask extends RemoteSyncTask {
    	WakeLock wakeLock;
    	BackgroundSyncTask(Context ctx) {
			super(ctx);
			this.showToast = false;
		}
    	
    	@Override protected void onPreExecute() {
    		// don't let the phone fall asleep during sync
    		final PowerManager powerManager = 
    			(PowerManager)getSystemService(Context.POWER_SERVICE);
    		this.wakeLock = powerManager.newWakeLock(
    				PowerManager.PARTIAL_WAKE_LOCK, BackgroundService.TAG);
    		wakeLock.acquire();
    		super.onPreExecute();
    	}
    	
		@Override protected void onPostExecute(Integer result) {
			Log.d(BackgroundService.TAG,"SYNC Post-execute complete!");
			super.onPostExecute(result);
			this.wakeLock.release();
			// schedule the next occurrence:
			BackgroundService.this.scheduleSync(false);
            // Done with our work...  stop the service!
            BackgroundService.this.stopSelf(startID);    
    	}
    };
    
    /**
     * This isn't actually likely to be notified of changes since most often
     * when preferences change it will be shut down.  Not to mention, the 
     * same instance variables (enabled and interval) are already pulled
     * from the preferences during onCreate so they're not likely (ever?) to 
     * change when this is called.  The net effect is basically to re-call a 
     * schedule or unschedule whenever this a CONFIGURATION_CHANGE intent is sent.
     */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if ( KEY_BACKGROUND_SYNC_ENABLED.equals(key) ) {
			// if enabled/ disabled, start or stop the recurring task
			boolean enabled = sharedPreferences.getBoolean(KEY_BACKGROUND_SYNC_ENABLED, false);
			if ( ! enabled ) {
				this.backgroundSyncEnabled = false;
				unscheduleSync();
			}
			else {
				this.backgroundSyncEnabled = true;
				scheduleSync();
			}
		}
		else if ( KEY_SYNC_INTERVAL.equals(key) ) {
			String intStr = sharedPreferences.getString(
					KEY_SYNC_INTERVAL, ""+DEFAULT_SYNC_INTERVAL);
			this.backgroundSyncInterval = Integer.parseInt(intStr);
			scheduleSync(false);  // this will overwrite a previously-scheduled intent
		} else Log.w(TAG,"Unknown key: " + key);
	}

    @Override
    public IBinder onBind(Intent intent) {
    	Log.d(TAG,"onBind called");
    	return null;
    }
}
