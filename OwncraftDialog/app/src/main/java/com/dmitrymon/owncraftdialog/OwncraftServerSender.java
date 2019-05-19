package com.dmitrymon.owncraftdialog;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class OwncraftServerSender
{

    public interface Callback
    {
        void OnFinish(boolean success);
    }

    private Callback callback;

    public OwncraftServerSender(Callback callback)
    {
        this.callback = callback;
    }


    public int sendData(String data)
    {
        SendData sender = new SendData();
        sender.execute(data);

        return sender.getResponse();
    }


    private class SendData extends AsyncTask<String, Void, Void>
    {

        private int response;

        public int getResponse()
        {
            return response;
        }

        @Override
        protected Void doInBackground(String... strings)
        {
            try
            {

                URL url = new URL(OwncraftServerListener.DATA_UPLOAD_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                Log.i("JSON", strings[0]);
                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                os.writeBytes(strings[0]);

                os.flush();
                os.close();

                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG" , conn.getResponseMessage());

                conn.disconnect();
                response = conn.getResponseCode();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v)
        {
            callback.OnFinish(response == 200);
        }
    }
}
