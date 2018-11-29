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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
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

        handlerService.postDelayed(runnableService, 1000);
    }

    @Override
    public void onDestroy()
    {
        handlerService.removeCallbacksAndMessages(null);
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
            URL url = null;

            try
            {

                String result = new JSONObject()
                        .put("alertData", "True")
                        .put("auxData", "False")
                        .put("name", "Test name")
                        .put("age", "25")
                        .put("pain", "True")
                        .put("location", "Test city")
                        .put("drinking", "False")
                        .put("smoking", "False")
                        .toString();

                url = new URL(OwncraftServerListener.DATA_UPLOAD_URL);

                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

                OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

                writer.write(result);
                writer.flush();
                writer.close();
                outputStream.close();

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
                e.printStackTrace();
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
