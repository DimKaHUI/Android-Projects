package com.dmitrymon.owncraftdialog;

import android.os.AsyncTask;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class OwncraftServerSender
{

    public interface Callback
    {
        void OnFinish();
    }

    private Callback callback;

    public OwncraftServerSender(Callback callback)
    {
        this.callback = callback;
    }

    public void sendData(String data)
    {
        SendData sender = new SendData();
        sender.execute(data);
    }


    private class SendData extends AsyncTask<String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... strings)
        {
            try
            {
                URL url = new URL(OwncraftServerListener.DATA_UPLOAD_URL);
                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

                OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

                writer.write(strings[0]);
                writer.flush();
                writer.close();
                outputStream.close();

                urlConnection.connect();
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
            callback.OnFinish();
        }
    }
}
