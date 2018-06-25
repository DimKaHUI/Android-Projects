package com.dmitry.wifimanager;

import android.app.AlertDialog;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class WifiManager extends IntentService
{
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_START = "com.dmitry.wifimanager.action.START";
    private static final String ACTION_STOP = "com.dmitry.wifimanager.action.STOP";

    private static final String HIGHEST_PRIOR = "com.dmitry.wifimanager.extra.HIGHEST_PRIOR";
    private static final String LOWEST_PRIOR = "com.dmitry.wifimanager.extra.LOWEST_PRIOR";

    public WifiManager()
    {
        super("WifiManager");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startManager(Context context, String highPriorSsid, String lowPriorSsid)
    {
        Intent intent = new Intent(context, WifiManager.class);
        intent.setAction(ACTION_START);
        intent.putExtra(HIGHEST_PRIOR, highPriorSsid);
        intent.putExtra(LOWEST_PRIOR, lowPriorSsid);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void stopManager(Context context)
    {
        Intent intent = new Intent(context, WifiManager.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            final String action = intent.getAction();
            if (ACTION_START.equals(action))
            {
                final String hight = intent.getStringExtra(HIGHEST_PRIOR);
                final String low = intent.getStringExtra(LOWEST_PRIOR);
                handleActionStart(hight, low);
            }
            else if (ACTION_STOP.equals(action))
            {
                handleActionStop();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionStart(String highPriority, String lowPriority)
    {
        // TODO: Handle action Start
        Log.v("WifiManager", "Service start message received!");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Service started");
        builder.create().show();
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionStop()
    {
        Log.v("WifiManager", "Service stop message received!");
        stopSelf();
    }
}
