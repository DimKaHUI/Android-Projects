package com.dmitrymon.owncraftdialog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{

    private final int PERMISSION_REQUEST_ID = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        processPermissions();
    }

    void processPermissions()
    {
        if(checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
        {
            // No internet permission, requesting needed
            requestPermissions(new String[]{Manifest.permission.INTERNET}, PERMISSION_REQUEST_ID);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if(requestCode == PERMISSION_REQUEST_ID)
        {
            for(int i = 0; i < permissions.length; i++)
            {
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                {
                    Toast msg = Toast.makeText(getApplicationContext(), permissions[i] + " was denied!", Toast.LENGTH_SHORT);
                    msg.show();
                }
            }
        }
    }
}
