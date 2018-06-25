package com.dmitry.wifimanagercontroller;

import android.Manifest;
import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Icon;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.security.Permission;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class WifiManagerService extends Service
{
    public final int CheckInterval = 5000;
    public final int ScanInterval = 30000;
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_START = "com.dmitry.wifimanagercontroller.action.START";
    public static final String ACTION_STOP = "com.dmitry.wifimanagercontroller.action.STOP";

    public static final String HIGHEST_PRIOR = "com.dmitry.wifimanagercontroller.extra.HIGHEST_PRIOR";
    public static final String LOWEST_PRIOR = "com.dmitry.wifimanagercontroller.extra.LOWEST_PRIOR";
    public static int NOTIFICATION_ID = 6032403;

    private Thread checkerThread;
    private Thread scannerThread;
    private boolean scanNeeded;
    private boolean loop = true;
    private String highPriority;
    private String lowPriority;
    private List<ScanResult> scanResults;
    private WifiManager wifiManager;
    private boolean resultsChecked = true;
    private Notification note;
    private boolean permissionGranted = false;



    private Notification CreateNotification()
    {
        Notification.Builder noteBuilder = new Notification.Builder(getApplicationContext());
        noteBuilder.setContentText("In work");
        noteBuilder.setContentTitle("WifiController Service");
        Icon actionIcon = Icon.createWithResource(getApplicationContext(), R.mipmap.ic_launcher);

        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(actionIcon, "Stop", null);
        Intent intent = new Intent(this, WifiManagerService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        noteBuilder.setContentIntent(pendingIntent);
        noteBuilder.addAction(actionBuilder.build());

        note = noteBuilder.build();
        return note;
    }

    @Override
    public void onCreate()
    {
        Notification note = CreateNotification();
        synchronized (note)
        {
            note.notify();
        }
        startForeground(NOTIFICATION_ID, note);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        if(intent.getAction().equals(ACTION_START))
        {
            Log.v("WIFI_MANAGER","SERVICE START COMMAND RECEIVED");
            highPriority = intent.getStringExtra(HIGHEST_PRIOR);
            lowPriority = intent.getStringExtra(LOWEST_PRIOR);
            StartWifiChecking();
        }
        if(intent.getAction().equals(ACTION_STOP))
        {
            Log.v("WIFI_MANAGER", "SERVICE STOP COMMAND RECEIVED");
            StopWifiChecking();
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        stopForeground(true);
        StopWifiChecking();
        Log.v("WIFI_MANAGER", "SERVICE DESTROYED!");
    }

    private void StartWifiChecking()
    {
        if(checkerThread == null)
        {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            loop = true;
            checkerThread = new Thread(new WifiChecking());
            checkerThread.start();
            scanNeeded = true;
            scannerThread = new Thread(new WifiScanner());
            scannerThread.start();
        }
    }

    private void StopWifiChecking()
    {
        if(checkerThread != null)
        {
            loop = false;
        }
    }

    private void ConnectToHigh()
    {
        List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
        for(int i = 0; i < networks.size(); i++)
        {
            if(networks.get(i).SSID.replace("\"", "").equals(highPriority))
            {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i, true);
                wifiManager.reconnect();
                break;
            }
        }
    }

    private int GetHighestIndex()
    {
        //List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
        if(scanResults != null)
        {
            for (int i = 0; i < scanResults.size(); i++)
            {
                Log.v("WIFI_MANAGER", scanResults.get(i).SSID);
                if (scanResults.get(i).SSID.replace("\"", "").equals(highPriority))
                    return i;
            }
        }
        else
            Log.w("WIFI_MANAGER", "NO_SCAN_RESULTS");

        if(scanResults != null && scanResults.size() > 0)
            resultsChecked = true;

        return -1;
    }

    class WifiChecking implements Runnable
    {

        @Override
        public void run()
        {
            while(loop)
            {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ssid = wifiInfo.getSSID().replace("\"", "");

                if(ssid.equals(lowPriority))
                {
                    Log.w("WIFI_MANAGER","NOW CONNECTED TO LOW_PRIOR SSID");
                    int highest = GetHighestIndex();
                    if(highest != -1)
                    {
                        Log.w("WIFI_MANAGER", "SITUATION OCCURED!");
                    }
                }
                else
                {
                    Log.w("WIFI_MANAGER","Not equal :(");
                }

                try
                {
                    Thread.sleep(CheckInterval);
                }
                catch (InterruptedException ex)
                {
                    Log.e("WIFI_MANAGER", "Process interrupted");
                }
            }
        }
    }

    class WifiScanner implements Runnable
    {

        @Override
        public void run()
        {
            while (true)
            {
                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                scanResults = wifiManager.getScanResults();
                if(scanResults != null)
                    Log.v("WIFI_MANAGER", "Scanning iteration: " + scanResults.size());
                else
                    Log.v("WIFI_MANAGER", "Scanning iteration: null");
                if(scanResults == null || resultsChecked)
                {
                    /*boolean started = wifiManager.startScan();
                    resultsChecked = false;
                    if (!started)
                    {
                        Log.e("WIFI_MANAGER", "Scanning not started :(");
                    }
                    else
                    {
                        Log.v("WIFI_MANAGER", "Scanning started");
                    }*/
                }
                try
                {
                    Thread.sleep(ScanInterval);
                }

                catch (InterruptedException ex)
                {
                    Log.e("WIFI_MANAGER", "Scanner interrupted");
                }
            }
        }
    }
}
