package com.dmitrymon.dbdirl;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

public class Network
{
    public static final String MULTICAST_GROUP = "224.0.0.1";

    public interface MessageCallback
    {
        void Receive(String result);
    }

    public static MulticastSocket socket;

    public static void sendBroadcast(final String message, final int port, final Context context) // TODO Rework, should be multicast
    {
        new AsyncTask<Void, Void, Void>()
        {

            @Override
            protected Void doInBackground(Void... voids)
            {
                try
                {
                    if (socket != null && socket.isBound())
                    {
                        socket.close();
                    }

                    socket = new MulticastSocket(port);

                    InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                    socket.joinGroup(group);

                    byte[] buf = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);

                    socket.send(packet);

                    Log.e("Network", "Multicasting from: " + getIPAddress(true));

                    socket.leaveGroup(group);

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

    public static void receiveMessage(final int port, final MessageCallback callback)
    {
        new AsyncTask<Void, Void, Void>()
        {

            String result;
            @Override
            protected Void doInBackground(Void... voids)
            {
                try
                {
                    if(socket != null && socket.isBound())
                    {
                        socket.close();
                    }

                    byte[] b = new byte[1024];
                    DatagramPacket dgram = new DatagramPacket(b, b.length);
                    socket = new MulticastSocket(port); // must bind receive side

                    socket.joinGroup(InetAddress.getByName(MULTICAST_GROUP));

                    socket.receive(dgram);

                    result = new String(dgram.getData());

                    socket.leaveGroup(InetAddress.getByName(MULTICAST_GROUP));
                    socket.close();

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

    public static void sendMessage(final String message, final String ip, final int port)
    {
        new AsyncTask<Void, Void, Void>()
        {

            @Override
            protected Void doInBackground(Void... voids)
            {
                try
                {
                    if(socket != null && socket.isBound())
                    {
                        socket.close();
                    }

                    InetAddress address = InetAddress.getByName(ip);
                    socket = new MulticastSocket(port);

                    socket.connect(address, port);
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.send(packet);
                    socket.close();
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

    private static InetAddress getBroadcastAddress(Context context) throws IOException
    {
        /*WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);*/

        return InetAddress.getByName("192.168.1.255");
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
