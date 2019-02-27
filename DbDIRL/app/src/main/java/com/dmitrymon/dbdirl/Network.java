package com.dmitrymon.dbdirl;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Network
{
    public interface MessageCallback
    {
        void Receive(String result);
    }

    public static void SendBroadcast(final String message, final int port, final Context context)
    {
        new AsyncTask<Void, Void, Void>()
        {

            @Override
            protected Void doInBackground(Void... voids)
            {
                try
                {
                    DatagramSocket socket = new DatagramSocket(port);

                    socket.connect(getBroadcastAddress(context), port);
                    socket.setBroadcast(true);
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.send(packet);
                } catch (SocketException e)
                {
                    e.printStackTrace();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }

    public static void ReceiveBroadcast(final int port, final Context context, final MessageCallback callback)
    {
        new AsyncTask<Void, Void, Void>()
        {

            String result;
            @Override
            protected Void doInBackground(Void... voids)
            {
                try
                {
                    DatagramSocket socket = new DatagramSocket(port);

                    socket.connect(getBroadcastAddress(context), port);
                    socket.setBroadcast(true);
                    byte[] buffer = new byte[12];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    result = new String(buffer);

                } catch (SocketException e)
                {
                    e.printStackTrace();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v)
            {
                callback.Receive(result);
            }

        }.execute();
    }

    private static InetAddress getBroadcastAddress(Context context) throws IOException
    {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);
    }
}
