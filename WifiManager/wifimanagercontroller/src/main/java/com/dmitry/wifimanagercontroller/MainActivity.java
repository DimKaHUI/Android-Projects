package com.dmitry.wifimanagercontroller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static com.dmitry.wifimanagercontroller.WifiManagerService.ACTION_START;
import static com.dmitry.wifimanagercontroller.WifiManagerService.ACTION_STOP;
import static com.dmitry.wifimanagercontroller.WifiManagerService.HIGHEST_PRIOR;
import static com.dmitry.wifimanagercontroller.WifiManagerService.LOWEST_PRIOR;

public class MainActivity extends Activity
{

    public final int REQUEST_CODE = 1111;

    /*private static final String ACTION_START = "com.dmitry.wifimanager.action.START";
    private static final String ACTION_STOP = "com.dmitry.wifimanager.action.STOP";
    private static final String HIGHEST_PRIOR = "com.dmitry.wifimanager.extra.HIGHEST_PRIOR";
    private static final String LOWEST_PRIOR = "com.dmitry.wifimanager.extra.LOWEST_PRIOR";
*/
    EditText hightPriorBox;
    EditText lowPriorBox;
    Button startButton;
    Button stopButton;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupLocalValues();
    }

    private void setupLocalValues()
    {
        hightPriorBox = findViewById(R.id.highEditText);
        lowPriorBox = findViewById(R.id.lowEditText);

        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new StartButtonListener());

        stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new StopButtonListener());
    }

    public boolean ProcessPermissions()
    {
        int setWifiState = checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE);
        int getWifiState  = checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE);
        int getLocation  = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int getFineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        boolean granted = true;
        if(setWifiState == PackageManager.PERMISSION_DENIED)
        {
            Log.v("WIFI_MANAGER", "No permission for setting wifi state");
            granted = false;
        }
        if(getWifiState == PackageManager.PERMISSION_DENIED)
        {
            Log.v("WIFI_MANAGER", "No permission for reading wifi state");
            granted = false;
        }
        if(getLocation == PackageManager.PERMISSION_DENIED)
        {
            Log.v("WIFI_MANAGER", "No permission for reading location");
            granted = false;
        }
        if(getFineLocation == PackageManager.PERMISSION_DENIED)
        {
            Log.v("WIFI_MANAGER", "No permission for reading fine location");
            granted = false;
        }
        return granted;
    }


        public void GetPermission()
        {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_CODE);
        }

    class StartButtonListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            Log.v("Button pressed", "Start button pressed");

            boolean hasPermissions = ProcessPermissions();
            if(!hasPermissions)
            {
                GetPermission();
            }

            Context context = getApplicationContext();
            Intent intent = new Intent(context, WifiManagerService.class);
            intent.setAction(ACTION_START);
            intent.putExtra(HIGHEST_PRIOR, hightPriorBox.getText().toString());
            intent.putExtra(LOWEST_PRIOR, lowPriorBox.getText().toString());

            ComponentName name = context.startService(intent);

            /*
            String hText = hightPriorBox.getText().toString();
            String lText = lowPriorBox.getText().toString();
            WifiManagerService.startManager(getApplicationContext(), hText, lText);*/
        }
    }

    class StopButtonListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            Log.v("Button pressed", "Stop button pressed");
            Context context = getApplicationContext();
            Intent intent = new Intent(context, WifiManagerService.class);
            intent.setAction(ACTION_STOP);

            context.startService(intent);
        }
    }
}
