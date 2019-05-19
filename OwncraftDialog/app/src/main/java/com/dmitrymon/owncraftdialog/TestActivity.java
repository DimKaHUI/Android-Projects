package com.dmitrymon.owncraftdialog;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class TestActivity extends AppCompatActivity
{

    Handler handlerService = new Handler();

    Runnable runnableService = new Runnable()
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

            TextView textView = findViewById(R.id.serviceInfo);
            textView.setText(labelText);

            handlerService.postDelayed(this, 1000);
        }
    };

    Handler handlerConnection = new Handler();

    Runnable runnableConnection = new Runnable()
    {

        @Override
        public void run()
        {
            new ConnectionChecker().execute();
            handlerConnection.postDelayed(this, 1000);
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

        Button sendPost = findViewById(R.id.sendPost);
        sendPost.setOnClickListener(new Listener());

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new Listener());

        Button killServiceButton = findViewById(R.id.killServiceButton);
        killServiceButton.setOnClickListener(new Listener());

        handlerService.postDelayed(runnableService, 1000);
        handlerConnection.postDelayed(runnableConnection, 1000);
    }

    @Override
    public void onDestroy()
    {
        if(handlerService != null)
            handlerService.removeCallbacksAndMessages(null);

        if(handlerConnection != null)
            handlerConnection.removeCallbacksAndMessages(null);

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

            if(v == findViewById(R.id.sendPost))
                new SendPost().execute();

            if(v == findViewById(R.id.exitButton))
                finish();

            if(v == findViewById(R.id.killServiceButton))
                stopService();
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

    private class SendPost extends AsyncTask<Void, Void, Void>
    {

        int responseCode = -1;

        @Override
        protected Void doInBackground(Void... booleans)
        {

            try
            {

                String result = new JSONObject()
                        .put("alertData", "false")
                        .put("auxData", "true")
                        .put("name", "Joe")
                        .put("age", "54")
                        .put("pain", "false")
                        .put("location", "Test city")
                        .put("drinking", "true")
                        .put("smoking", "true")
                        .toString();

                OwncraftServerSender sender = new OwncraftServerSender(new SendCallback());
                responseCode = sender.sendData(result);

            } catch (JSONException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v)
        {
            Toast.makeText(getApplicationContext(), "Response code: " + responseCode, Toast.LENGTH_SHORT).show();
        }
    }

    private class SendCallback implements OwncraftServerSender.Callback
    {

        @Override
        public void OnFinish(boolean success)
        {
            Log.e("Test","Test data sent!");
        }
    }

    private class ConnectionChecker extends AsyncTask<Void, Void, Void>
    {

        int responseCode = -1;

        @Override
        protected Void doInBackground(Void... booleans)
        {
            URL url = null;

            try
            {

                url = new URL(OwncraftServerListener.ALERT_CHECK_URL);

                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

                urlConnection.connect();

                responseCode = urlConnection.getResponseCode();

                return null;

            } catch (MalformedURLException e)
            {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                //e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v)
        {
            //Toast.makeText(getApplicationContext(), "Response code: " + responseCode, Toast.LENGTH_SHORT).show();
            TextView textView = findViewById(R.id.connectionInfo);
            if(responseCode == -1)
            {
                textView.setText("Unavailable");
            }
            else
                textView.setText("Response code: " + responseCode);
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

    private void stopService()
    {
        Intent stopService = new Intent(this, OwncraftServerListener.class);
        stopService.setAction(OwncraftServerListener.ACTION_FORCE_STOP);
        startService(stopService);
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
