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

    public final static int SERVER_PORT = 56625;

    private Button broadcastButton;
    private Button startServerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        processViews();
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
                        SendIpBroadcast();
                    }
                    if(v == startServerButton)
                    {

                    }
                }
            }

            Listener listener = new Listener();
            broadcastButton.setOnClickListener(listener);
            startServerButton.setOnClickListener(listener);
        }
    }

    private void SendIpBroadcast()
    {
        String ip = getIPAddress(true);

        Network.SendBroadcast(ip, SERVER_PORT, this);
    }


    public static String getIPAddress(boolean useIPv4) {
        try
        {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }



}
