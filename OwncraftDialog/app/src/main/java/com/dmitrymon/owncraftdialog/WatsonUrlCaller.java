package com.dmitrymon.owncraftdialog;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import java.io.IOError;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class WatsonUrlCaller extends WatsonApi
{
    private Activity invoker;
    private HttpsURLConnection connection;

    public WatsonUrlCaller(Activity invoker)
    {
        this.invoker = invoker;
    }

    private void createNetworkConnection()
    {
        try
        {
            URL watsonEndpoint = new URL("");
            connection =  (HttpsURLConnection) watsonEndpoint.openConnection();
            connection.setRequestProperty("Owncraft-Dialog", "owncraft-dialog-v1.0");
        }
        catch (MalformedURLException ex)
        {
            Context context = invoker.getApplicationContext();
            Toast.makeText(context, "Error: URL is malformed.", Toast.LENGTH_SHORT).show();
        }
        catch (IOException ex)
        {
            Context context = invoker.getApplicationContext();
            Toast.makeText(context, "Error: impossible to connect. Check your internet connection", Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
        }
    }

    @Override
    public void SendUserInput(String text)
    {

    }

    @Override
    public String ReadWatsonAnswer()
    {
        return null;
    }
}
