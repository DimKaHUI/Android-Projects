package com.dmitrymon.dbdirl;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ServerActivity extends AppCompatActivity
{

    public final int SERVER_PORT = 56625;

    private NetworkListener listener;

    private Button broadcastButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        StartListener();
    }

    private void processViews()
    {
        if(broadcastButton == null )
        {
            broadcastButton = findViewById(R.id.broadcastButton);

            class Listener implements View.OnClickListener
            {

                @Override
                public void onClick(View v)
                {
                    if (v == broadcastButton)
                    {
                        BroadcastSelfIp();
                    }
                }
            }

            Listener listener = new Listener();
            broadcastButton.setOnClickListener(listener);
        }
    }

    private void StartListener()
    {
        listener = new NetworkListener(SERVER_PORT);
        listener.StartDataListener(new StringReceiver());

        Toast.makeText(this, "Listener started!", Toast.LENGTH_SHORT).show();
    }

    private class StringReceiver implements NetworkListener.StringDataHandler
    {
        @Override
        public void OnDataReceived(String data)
        {
            Log.e("Server", data);
        }
    }

    private void BroadcastSelfIp()
    {
        try
        {
            InetAddress address = InetAddress.getLocalHost();
            Log.e("Server", "Is on ip: " + address.getHostAddress());
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
    }

    class BroadcastingClient
    {
        private DatagramSocket socket = null;

        public void sendBroadcast(String message)
        {
            @SuppressLint("StaticFieldLeak") AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>()
            {
                @Override
                protected Void doInBackground(String... strings)
                {
                    try
                    {
                        broadcast(strings[0], InetAddress.getByName("255.255.255.255"));
                    } catch (UnknownHostException e)
                    {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(message);
        }

        private void broadcast(String message, InetAddress address)
        {
            try
            {
                socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] buffer = message.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
                socket.send(packet);
                socket.close();
            } catch (SocketException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }

}
