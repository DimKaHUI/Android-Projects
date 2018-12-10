package com.dmitrymon.dbdirl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class GeneratorActivity extends AppCompatActivity
{

    private InetAddress serverAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generator);

        if(serverAddress == null)
        {
            ReceiveServerIp();
        }
    }

    private void ReceiveServerIp()
    {
        Network.ReceiveBroadcast(ServerActivity.SERVER_PORT, this, new BroadcastReceiver());
    }

    private class BroadcastReceiver implements Network.MessageCallback
    {

        @Override
        public void Receive(String result)
        {
            try
            {
                serverAddress = InetAddress.getByName(result);
            } catch (UnknownHostException e)
            {
                e.printStackTrace();
            }
        }
    }

}
