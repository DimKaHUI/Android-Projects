package com.dmitrymon.dbdirl;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkListener
{
    private ServerSocket serverSocket;
    private int port;

    public NetworkListener(int port)
    {
        this.port = port;
    }


    public void StartListener()
    {
        try
        {
            serverSocket = new ServerSocket(port);
            ConnectionWaiter waiter = new ConnectionWaiter();
            waiter.execute();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public void StartDataListener(StringDataHandler handler)
    {
        try
        {
            StringSocketReceiver receiver = new StringSocketReceiver(handler);
            serverSocket = new ServerSocket(port);
            ConnectionWaiter waiter = new ConnectionWaiter();
            waiter.setAsyncCallback(receiver);
            waiter.execute();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private class StringSocketReceiver implements ConnectionWaiterCallback
    {

        StringDataHandler handler;

        public StringSocketReceiver(StringDataHandler handler)
        {
            this.handler = handler;
        }

        @Override
        public void OnConnectionEstablished(Socket socket)
        {
            String data = ReadStringData(socket);
            handler.OnDataReceived(data);
        }
    }

    private String ReadStringData(Socket socket)
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            InputStream is = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while((line = reader.readLine()) != null)
                sb.append(line);
            is.close();
            return sb.toString();

        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public interface StringDataHandler
    {
        void OnDataReceived(String data);
    }

    public interface ConnectionWaiterCallback
    {
        void OnConnectionEstablished(Socket socket);
    }

    private class ConnectionWaiter extends AsyncTask<Void, Void, Void>
    {
        private ConnectionWaiterCallback postExecuteCallback;
        private ConnectionWaiterCallback asyncExecuteCallback;
        private Socket socket;

        public void setPostExecuteCallback(ConnectionWaiterCallback callback)
        {
            postExecuteCallback = callback;
        }

        public void setAsyncCallback(ConnectionWaiterCallback callback)
        {
            asyncExecuteCallback = callback;
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            try
            {
                socket = serverSocket.accept();

                if(asyncExecuteCallback != null)
                    asyncExecuteCallback.OnConnectionEstablished(socket);

            } catch (IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            if(postExecuteCallback != null)
                postExecuteCallback.OnConnectionEstablished(socket);
        }
    }
}
