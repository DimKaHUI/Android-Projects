package com.dmitrymon.dbdirl;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

public class ServerActivity extends AppCompatActivity
{

    private Button broadcastButton;
    private Button startServerButton;

    private Protocol protocol;

    private ArrayList<String> clientList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        processProtocol();

        processViews();
    }

    private void processProtocol()
    {
        if(protocol == null)
        {
            protocol = new Protocol(this, true);
            protocol.setGeneratorAnswerCallback(new Protocol.ParameterlessCallback()
            {
                @Override
                public void Callback(String sender)
                {
                    if(!clientList.contains(sender))
                    {
                        clientList.add(sender);
                        Log.e("Server", "Generator responded!");
                    }
                }
            });

            protocol.setGeneratorFinishedCallback(new Protocol.ParameterlessCallback()
            {
                @Override
                public void Callback(String sender)
                {
                    clientList.remove(sender);
                    Log.e("Server", "Generator finished!");
                }
            });
        }
    }

    private void processViews()
    {
        if(broadcastButton == null )
        {

            broadcastButton = findViewById(R.id.broadcastButton);
            startServerButton = findViewById(R.id.startServer);

            class Listener implements View.OnClickListener
            {

                @Override
                public void onClick(View v)
                {
                    if (v == broadcastButton)
                    {
                        protocol.SendIpBroadcast();
                    }
                    if(v == startServerButton)
                    {
                        protocol.StartListening();
                    }
                }
            }

            Listener listener = new Listener();
            broadcastButton.setOnClickListener(listener);
            startServerButton.setOnClickListener(listener);
        }
    }



}
