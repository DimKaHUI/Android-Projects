package com.dmitrymon.owncraftdialog;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ibm.watson.developer_cloud.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.DeleteSessionOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageInput;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageResponse;
import com.ibm.watson.developer_cloud.assistant.v2.model.SessionResponse;
import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.service.exception.NotFoundException;
import com.ibm.watson.developer_cloud.service.exception.RequestTooLargeException;
import com.ibm.watson.developer_cloud.service.exception.ServiceResponseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class WatsonSdkCaller extends WatsonApi
{
    private final String VERSION = "2018-09-20";
    private final String URL = "https://gateway.watsonplatform.net/assistant/api";
    private final String ASSISTANT_ID = "e818b527-90bc-4843-8706-73efaf0aa215";

    // Old connection chars
    private final String USERNAME = "2863bdca-4c11-4bb4-90d4-2c91acb742bb";
    private final String PASSWORD = "xph7tZ2V8WAA";

    private com.ibm.watson.developer_cloud.assistant.v2.Assistant watsonAssistantRuntime;
    private String sessionId;

    private Context context;

    private Queue<String> responseBuffer = new ArrayDeque<>();


    // Callbacks
    private WatsonCallback sendUserInputCallback;
    private WatsonCallback startSessionCallback;
    private WatsonCallback endSessionCallback;
    private WatsonCallback endOfConversation;

    private EntityCollector entityCollector;

    public EntityCollector getEntityCollector()
    {
        return entityCollector;
    }

    public WatsonSdkCaller(Context applicationContext)
    {
        context = applicationContext;

        entityCollector = new EntityCollector();

        establishConnectionOld();
    }

    private void establishConnectionOld()
    {
        //watsonAssistantRuntime = new Assistant(VERSION, USERNAME, PASSWORD);
        watsonAssistantRuntime = new com.ibm.watson.developer_cloud.assistant.v2.Assistant(VERSION, USERNAME, PASSWORD);
        watsonAssistantRuntime.setEndPoint(URL);
    }

    @Override
    public void StartSession()
    {
        new StartSessionTask().execute();
    }

    @Override
    public void EndSession()
    {
        new EndSessionTask().execute();
    }

    @Override
    public void SendUserInput(final String text)
    {
        SendUserInputTask task = new SendUserInputTask();
        task.execute(text);
    }

    @Override
    public String ReadWatsonAnswer()
    {
        //return responseBuffer.remove();
        String[] answers = parseTextAnswers();
        StringBuilder result = new StringBuilder();
        for(String answer : answers)
            result.append(answer).append('\n');

        return result.toString();
    }

    @Override
    public void SetSendUserInputCallback(WatsonCallback callback)
    {
        sendUserInputCallback = callback;
    }

    @Override
    public void SetStartSessionCallback(WatsonCallback callback)
    {
        startSessionCallback = callback;
    }

    @Override
    public void SetEndSessionCallback(WatsonCallback callback)
    {
        endSessionCallback = callback;
    }

    private boolean startSessionAsync()
    {
        try
        {
            CreateSessionOptions options;
            CreateSessionOptions.Builder soBuilder = new CreateSessionOptions.Builder();
            soBuilder.assistantId(ASSISTANT_ID);
            options = soBuilder.build();
            SessionResponse response = watsonAssistantRuntime.createSession(options).execute();

            sessionId = response.getSessionId();

            MessageOptions options1 = new MessageOptions.Builder().assistantId(ASSISTANT_ID).sessionId(sessionId).build();

            MessageResponse welcomeResponce = watsonAssistantRuntime.message(options1).execute();

            responseBuffer.add(welcomeResponce.toString());

            return true;
        }
        catch(NotFoundException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return false;
    }

    private boolean endSessionAsync()
    {
        try
        {
            DeleteSessionOptions dOptions;
            DeleteSessionOptions.Builder doBuilder = new DeleteSessionOptions.Builder();
            doBuilder.sessionId(sessionId);
            doBuilder.assistantId(ASSISTANT_ID);
            dOptions = doBuilder.build();
            watsonAssistantRuntime.deleteSession(dOptions).execute();

            return true;
        }
        catch(NotFoundException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return false;
    }

    private boolean sendUserInputAsync(String text)
    {

        try
        {

            MessageInput input = new MessageInput.Builder().text(text).build();

            MessageOptions options;
            MessageOptions.Builder moBuilder = new MessageOptions.Builder();
            moBuilder.assistantId(ASSISTANT_ID);
            moBuilder.sessionId(sessionId);
            moBuilder.input(input);
            options = moBuilder.build();
            MessageResponse response = watsonAssistantRuntime.message(options).execute();

            responseBuffer.add(response.toString());

            entityCollector.onNewMessage(new JSONObject(response.toString()));

            Log.e("New response", entityCollector.toString());

            return true;
        }
        catch (NotFoundException ex)
        {
            //Toast.makeText(context, "Method was not found!", Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
        }
        catch (RequestTooLargeException ex)
        {
            //Toast.makeText(context, "Request to Watson API was too large!", Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
        }
        catch (ServiceResponseException ex)
        {
            //String msg = "Service returned status code: " + ex.getStatusCode() + ": " + ex.getMessage();
            //Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return false;
    }

    private class SendUserInputTask extends AsyncTask<String, Void, Boolean>
    {

        @Override
        protected Boolean doInBackground(String... strings)
        {
            boolean result = sendUserInputAsync(strings[0]);
            return result;
        }

        protected void onPostExecute(Boolean res)
        {
            if(sendUserInputCallback != null)
            {
                if(res)
                    sendUserInputCallback.onSuccess();
                else
                    sendUserInputCallback.onFail();
            }
        }
    }

    private class StartSessionTask extends AsyncTask<Void, Void, Boolean>
    {

        @Override
        protected Boolean doInBackground(Void... voids)
        {
            boolean result = startSessionAsync();
            return result;
        }

        protected void onPostExecute(Boolean res)
        {
            if(sendUserInputCallback != null)
            {
                if(res)
                    startSessionCallback.onSuccess();
                else
                    startSessionCallback.onFail();
            }
        }
    }

    private class EndSessionTask extends AsyncTask<Void, Void, Boolean>
    {

        @Override
        protected Boolean doInBackground(Void... voids)
        {
            boolean result = endSessionAsync();
            return result;
        }

        protected void onPostExecute(Boolean res)
        {
            if(sendUserInputCallback != null)
            {
                if(res)
                    endSessionCallback.onSuccess();
                else
                    endSessionCallback.onFail();
            }
        }
    }

    private String[] parseTextAnswers()
    {
        ArrayList<String> answerList = new ArrayList<>();
        try
        {
            String jsonText = responseBuffer.remove();

            Log.w("Watson answer", jsonText);

            JSONObject obj = new JSONObject(jsonText);
            JSONObject output = obj.getJSONObject("output");
            JSONArray answerArray = output.getJSONArray("generic");

            for(int i = 0; i < answerArray.length(); i++)
            {
                JSONObject element = answerArray.getJSONObject(i);
                if(element.getString("response_type").equals("text"))
                    answerList.add(element.getString("text"));
            }
        }
        catch (JSONException ex)
        {
            ex.printStackTrace();
        }

        String[] result = new String[answerList.size()];
        answerList.toArray(result);
        return result;
    }
}
