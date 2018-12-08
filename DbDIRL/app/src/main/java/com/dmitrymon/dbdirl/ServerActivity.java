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
import java.util.Collections;
import java.util.List;

public class ServerActivity extends AppCompatActivity
{

    public final static int SERVER_PORT = 56625;

    private NetworkListener listener;

    private Button broadcastButton;
    private Button startServerButton;
    private boolean serverStarted = false;

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
                        BroadcastSelfIp();
                    }
                    if(v == startServerButton)
                    {
                        if(!serverStarted)
                        {
                            StartListener();
                            serverStarted = true;
                        }
                        else
                        {
                            listener.StopListener();
                            serverStarted = false;
                        }
                    }
                }
            }

            Listener listener = new Listener();
            broadcastButton.setOnClickListener(listener);
            startServerButton.setOnClickListener(listener);
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
        String ip = getIPAddress(true);

        BroadcastingClient broadcastingClient = new BroadcastingClient();
        broadcastingClient.sendBroadcast(ip);
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



    class BroadcastingClient
    {
        private DatagramSocket socket = null;


        @SuppressLint("StaticFieldLeak")
        public void sendBroadcast(String message)
        {
            Log.e("Server", "Starting broadcast async task...");

            new AsyncTask<String, Void, Void>()
            {
                @Override
                protected Void doInBackground(String... strings)
                {
                    try
                    {
                        Log.e("Server", "Broadcasting: " + strings[0]);
                        broadcast(strings[0], InetAddress.getByName("255.255.255.255"));
                    } catch (UnknownHostException e)
                    {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void v)
                {
                    Log.e("Server", "Broadcasting task finished!" );
                }

            }.execute(message);

            Log.e("Server", "Broadcasting task launched...");
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
