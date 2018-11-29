package com.dmitrymon.owncraftdialog;

import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class TestActivity extends AppCompatActivity
{

    Handler handler = new Handler();

    Runnable runnable = new Runnable()
    {

        @Override
        public void run()
        {
            boolean value = isListenerRunning(OwncraftServerListener.class);

            String labelText;
            if(value)
                labelText = "Service running";
            else
                labelText = "Service stopped";

            TextView textView = findViewById(R.id.infoLabel);
            textView.setText(labelText);

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Button setButton = findViewById(R.id.buttonSet);
        setButton.setOnClickListener(new Listener());
        Button resetButton = findViewById(R.id.buttonReset);
        resetButton.setOnClickListener(new Listener());

        handler.postDelayed(runnable, 1000);
    }

    @Override
    public void onDestroy()
    {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private class Listener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            if (v == findViewById(R.id.buttonSet))
                new SendCommand().execute(true);

            if (v == findViewById(R.id.buttonReset))
                new SendCommand().execute(false);

        }
    }

    private static class SendCommand extends AsyncTask<Boolean, Void, Void>
    {

        @Override
        protected Void doInBackground(Boolean... booleans)
        {
            if(booleans[0])
            {
                setServerValue();
            }
            else
            {
                resetServerValue();
            }

            return null;
        }
    }

    static void resetServerValue()
    {
        try
        {
            URL url = new URL(OwncraftServerListener.ALERT_RESET_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("POST");

            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();

            Log.e("URL Test", "RESET command sent! Response code: " + responseCode);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    static void setServerValue()
    {
        try
        {
            URL url = new URL(OwncraftServerListener.ALERT_SET_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("POST");

            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();

            Log.e("URL Test", "SET command sent! Response code: " + responseCode);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private boolean isListenerRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
