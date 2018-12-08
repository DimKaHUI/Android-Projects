package com.dmitrymon.dbdirl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class GeneratorActivity extends AppCompatActivity
{

    NetworkListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generator);

        StartListener();
    }

    private void StartListener()
    {
        listener = new NetworkListener(ServerActivity.SERVER_PORT);
        listener.StartDataListener(new StringReceiver());

        Toast.makeText(this, "Listener started!", Toast.LENGTH_SHORT).show();
    }

    private class StringReceiver implements NetworkListener.StringDataHandler
    {
        @Override
        public void OnDataReceived(String data)
        {
            Log.e("Client", data);
        }
    }

}
