package com.dmitrymon.dbdirl;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Protocol
{
    public static final String ROOT_ELEMENT = "DBD_PROTOCOL";
    public static final int CURRENT_VERSION = 1;
    public final static int SERVER_PORT = 56625;

    private String localIp;
    private Context context;
    private boolean isServer;

    private TextDataCallback ipReceiveCallback;
    private ParameterlessCallback generatorFinishedCallback;
    private ParameterlessCallback generatorAnswerCallback;


    public void setIpReceiveCallback(TextDataCallback ipReceiveCallback)
    {
        this.ipReceiveCallback = ipReceiveCallback;
    }

    public void setGeneratorAnswerCallback(ParameterlessCallback generatorAnswerCallback)
    {
        this.generatorAnswerCallback = generatorAnswerCallback;
    }

    public void setGeneratorFinishedCallback(ParameterlessCallback generatorFinishedCallback)
    {
        this.generatorFinishedCallback = generatorFinishedCallback;
    }

    public enum Method
    {
        IP_BROADCAST, GENERATOR_FINISHED, GENERATOR_ANSWER
    }

    public static class ProtocolEntry
    {
        public int version;
        public String sender;
        public Method method;
        String methodData;
    }

    public interface ParameterlessCallback
    {
        void Callback(String sender);
    }

    public interface TextDataCallback
    {
        void Callback(String sender, String data);
    }

    public static ProtocolEntry ParseJson(String json)
    {
        ProtocolEntry entry = null;

        try
        {
            entry = new ProtocolEntry();
            JSONObject jsonObject = new JSONObject(json);

            JSONObject rootJson = jsonObject.getJSONObject(ROOT_ELEMENT);
            entry.version  = rootJson.getInt("version");
            entry.sender = rootJson.getString("sender");
            entry.method = Method.valueOf(rootJson.getString("method"));
            entry.methodData = rootJson.getString("method_data");

        } catch (JSONException e)
        {
            e.printStackTrace();
        }

        return entry;
    }

    public static String EntryToJson(ProtocolEntry entry)
    {
        JSONObject root = null;

        JSONObject container = new JSONObject();
        try
        {
            root = new JSONObject();
            container.put("version", entry.version);
            container.put("sender", entry.sender);
            container.put("method", entry.method.toString());
            container.put("method_data", entry.methodData);

            root.put(ROOT_ELEMENT, container);

        } catch (JSONException e)
        {
            e.printStackTrace();
        }

        return root.toString();
    }

    public Protocol(Context context, boolean isServer)
    {
        this.localIp = Network.getIPAddress(true);
        this.context = context;
        this.isServer = isServer;
    }

    private class NetworkMessageReceivedCallback implements Network.MessageCallback
    {

        @Override
        public void Receive(String result)
        {
            ProtocolEntry entry = ParseJson(result);

            if(entry.version != CURRENT_VERSION)
            {
                Log.e("Protocol", "Version mismatch! Current version is " + CURRENT_VERSION + ", their version is " + entry.version);
            }

            String methodData = entry.methodData;
            Method method = entry.method;
            String senderIp = entry.sender;

            switch (method)
            {
                case IP_BROADCAST:
                    ipReceiveCallback.Callback(senderIp, methodData);
                    break;
                case GENERATOR_FINISHED:
                    generatorFinishedCallback.Callback(senderIp);
                    break;
                case GENERATOR_ANSWER:
                    generatorAnswerCallback.Callback(senderIp);
                    break;
            }

            StartListening();
        }
    }

    public void StartListening()
    {
        Network.receiveMessage(SERVER_PORT, new NetworkMessageReceivedCallback());
    }

    public void SendIpBroadcast()
    {
        ProtocolEntry entry = new ProtocolEntry();
        entry.version = CURRENT_VERSION;
        entry.sender = Network.getIPAddress(true);
        entry.method = Method.IP_BROADCAST;
        entry.methodData = entry.sender;
        Network.sendBroadcast(EntryToJson(entry), SERVER_PORT, context);
    }

    public void SendGeneratorAnswer(String server)
    {
        ProtocolEntry entry = new ProtocolEntry();
        entry.version = CURRENT_VERSION;
        entry.sender = Network.getIPAddress(true);
        entry.method = Method.GENERATOR_ANSWER;
        entry.methodData = "";
        Network.sendMessage(EntryToJson(entry), server, SERVER_PORT);
    }

    public void sendGeneratorFinish(String server)
    {
        ProtocolEntry entry = new ProtocolEntry();
        entry.version = CURRENT_VERSION;
        entry.sender = Network.getIPAddress(true);
        entry.method = Method.GENERATOR_FINISHED;
        entry.methodData = "";
        Network.sendMessage(EntryToJson(entry), server, SERVER_PORT);
    }
}
