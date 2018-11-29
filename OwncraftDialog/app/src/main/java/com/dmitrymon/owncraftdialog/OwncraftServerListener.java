package com.dmitrymon.owncraftdialog;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class OwncraftServerListener extends Service
{
    public static String ACTION_START_FROM_AUTORUN = "com.dmitrymon.owncraftdialog.ACTION_START_FROM_AUTORUN";
    public static String ACTION_START_FROM_ACTIVITY = "com.dmitrymon.owncraftdialog.ACTION_START_FROM_ACTIVITY";
    public static String ACTION_START_FROM_SELF = "com.dmitrymon.owncraftdialog.ACTION_START_FROM_SELF";

    public static String ACTION_FORCE_STOP = "com.dmitrymon.owncraftdialog.ACTION_FORCE_STOP";

    public static String ALERT_CHECK_URL = "http://195.19.40.201:31168/get_health_alert";
    public static String DATA_UPLOAD_URL = "http://195.19.40.201:31168/update_health_data";
    public static String ALERT_RESET_URL = "http://195.19.40.201:31168/reset_health_alert";
    public static String ALERT_SET_URL = "http://195.19.40.201:31168/set_health_alert";

    private final int DELAY_MS = 5000;

    private boolean forceStop;

    private Handler handler;

    public OwncraftServerListener()
    {
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        if(intent == null)
        {
            stopSelf();
            return Service.START_STICKY;
        }
        else if(Objects.equals(intent.getAction(), ACTION_START_FROM_AUTORUN))
        {

        }
        else if(intent.getAction().equals(ACTION_FORCE_STOP))
        {
            forceStop = true;
            Log.e("Listener","Force stop received!");
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        else if(intent.getAction().equals(ACTION_START_FROM_SELF))
        {
            Log.e("Listener", "SELF RESTART");
        }
        else if (Objects.equals(intent.getAction(), ACTION_START_FROM_ACTIVITY))
        {

        }
        else
            stopSelf();


        initListener();

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void initListener()
    {

        Toast toast = Toast.makeText(getApplicationContext(), "Owncraft dialog initiated!", Toast.LENGTH_LONG);
        toast.show();

        handler = new Handler();

        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                readServerData();
                handler.postDelayed(this, DELAY_MS);
            }
        };

        handler.postDelayed(runnable, DELAY_MS);
    }

    private void readServerData()
    {
        Log.i("OWNCRAFT_DIALOG", "Periodic action!");

        checkServerData();
    }

    private void checkServerData()
    {
        // TODO Read server data

        FetchDataInBackground task = new FetchDataInBackground();
        task.setCallback(new Callback());
        task.execute();
    }

    class Callback
    {
        void OnFinish(JSONObject json)
        {
            onServerDataReceived(json);
        }
    }

    private static class FetchDataInBackground extends AsyncTask<Void, Void, Void>
    {

        private Callback callback;
        private JSONObject jsonObj;

        void setCallback(Callback callback)
        {
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            try
            {
                String json = getJsonFromServer(ALERT_CHECK_URL);

                jsonObj = new JSONObject(json);

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
            callback.OnFinish(jsonObj);
        }

        static String getJsonFromServer(String url) throws IOException
        {

            BufferedReader inputStream = null;

            URL jsonUrl = new URL(url);
            URLConnection dc = jsonUrl.openConnection();

            dc.setConnectTimeout(5000);
            dc.setReadTimeout(5000);

            inputStream = new BufferedReader(new InputStreamReader(
                    dc.getInputStream()));

            // read the JSON results into a string
            String jsonResult = inputStream.readLine();

            resetServerValue();
            return jsonResult;
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
    }

    private void onServerDataReceived(JSONObject obj)
    {

        try
        {
            String value = obj.getString("health_alert");
            if(value.equals("true"))
            {
                startConversation();
            }

            Log.v("Server answered", "Server responce: " + value);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (NullPointerException ex)
        {
            //ex.printStackTrace();
            Log.e("Listener","JSON Was lot downloaded. Check connection");
        }

    }

    private void startConversation()
    {
        Intent startConversation = new Intent(getApplicationContext(), MainActivity.class);
        startConversation.setAction(MainActivity.ACTION_START_CONVERSATION);
        startConversation.setFlags(FLAG_ACTIVITY_NEW_TASK);

        startActivity(startConversation);

        stopSelf();
    }

    private void seltRestart()
    {
        Intent selfIntent = new Intent(getApplicationContext(), this.getClass());
        selfIntent.setAction(ACTION_START_FROM_SELF);
        selfIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        Log.e("Service", "Restarting service!");
        startService(selfIntent);
    }

    @Override
    public void onDestroy()
    {
        if(handler != null)
            handler.removeCallbacksAndMessages(null);


        super.onDestroy();

        if(!forceStop)
            seltRestart();
    }
}
