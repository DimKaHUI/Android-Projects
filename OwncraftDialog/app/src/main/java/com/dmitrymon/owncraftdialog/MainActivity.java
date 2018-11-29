package com.dmitrymon.owncraftdialog;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

public class MainActivity extends AppCompatActivity
{

    public static String ACTION_START_CONVERSATION = "com.dmitrymon.owncraftdialog.ACTION_START_CONVERSATION";


    private final int PERMISSION_REQUEST_ID = 123;

    private WatsonApi watson;

    private boolean wasLoaded;

    private boolean conversationEnded;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        processViews();

        if(!wasLoaded)
        {
            Intent intent = getIntent();
            processIntent(intent);
        }
    }

    private void startListener()
    {
        if(!isListenerRunning(OwncraftServerListener.class))
        {
            Intent startServiceIntent = new Intent(getApplicationContext(), OwncraftServerListener.class);
            startServiceIntent.setAction(OwncraftServerListener.ACTION_START_FROM_ACTIVITY);

            startService(startServiceIntent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        processIntent(intent);
    }

    private boolean isListenerRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void processIntent(Intent intent)
    {
        if(wasLoaded)
        {
            Log.i(getPackageName(), "Received new intent! Ignore");
            return;
        }

        if(!Objects.equals(intent.getAction(), ACTION_START_CONVERSATION))
        {
            onJobDone();
            return;
        }

        wasLoaded = true;

        processPermissions();
    }



    void processViews()
    {
        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new ButtonListener());
        sendButton.setEnabled(false);
    }

    void processPermissions()
    {
        boolean allGranted = true;
        if(checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
        {
            // No internet permission, requesting needed
            requestPermissions(new String[]{Manifest.permission.INTERNET}, PERMISSION_REQUEST_ID);
            allGranted = false;
        }

        if(allGranted)
            processPermissionsOk();
    }

    void processPermissionsOk()
    {
        if(watson == null)
        {
            watson = new WatsonSdkCaller(getApplicationContext());

            watson.SetSendUserInputCallback(new SendCallback());
            watson.SetStartSessionCallback(new StartSessionCallback());

            watson.StartSession();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        boolean permissionsOk = true;
        if(requestCode == PERMISSION_REQUEST_ID)
        {
            for(int i = 0; i < permissions.length; i++)
            {
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                {
                    Toast msg = Toast.makeText(getApplicationContext(), permissions[i] + getString(R.string.permission_error), Toast.LENGTH_SHORT);
                    msg.show();
                    permissionsOk = false;
                }
            }
        }

        if(permissionsOk)
            processPermissionsOk();
    }

    private void sendCollectedData()
    {
        OwncraftServerSender sender = new OwncraftServerSender(new SenderCallback());

        WatsonSdkCaller caller = (WatsonSdkCaller)watson;

        EntityCollector collector = caller.getEntityCollector();

        sender.sendData(collector.getData());

    }

    private class SenderCallback implements OwncraftServerSender.Callback
    {

        @Override
        public void OnFinish()
        {
            Toast.makeText(getApplicationContext(), "Data was successfully collected and delivered to Owncraft!", Toast.LENGTH_SHORT).show();
        }
    }


    private class ButtonListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            if(v == findViewById(R.id.sendButton))
            {
                if(!conversationEnded)
                    sendMessageToWatson();
                else
                    onJobDone();
            }
        }
    }

    private void onJobDone()
    {
        startListener();
        finish();
    }

    private void addDialogMessage(String sender, String message)
    {

        TextView view = new TextView(this);

        String result = sender + "\n" + message;

        view.setText(result);
        view.setGravity(Gravity.BOTTOM | Gravity.END);
        view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        view.setTextColor(Color.BLACK);

        LinearLayout list = findViewById(R.id.dialogView);
        //list.addView(view);
        list.addView(view);
    }

    private void sendMessageToWatson()
    {
        EditText textBox = findViewById(R.id.editText);
        String message = textBox.getText().toString();

        addDialogMessage(getString(R.string.user_name_ui), message);

        ChangeSendButtonState(false);
        watson.SendUserInput(message);
    }

    private void ChangeSendButtonState(boolean enabled)
    {
        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setEnabled(enabled);
    }

    private class SendCallback extends WatsonApi.WatsonCallback
    {
        @Override
        public void onSuccess()
        {
            EditText textBox = findViewById(R.id.editText);
            textBox.setText("");
            addDialogMessage(getString(R.string.server_name), watson.ReadWatsonAnswer());
            ChangeSendButtonState(true);

            WatsonSdkCaller caller = (WatsonSdkCaller)watson;

            EntityCollector collector = caller.getEntityCollector();

            if(collector.isAlertDataCollected() || collector.isAuxDataCollected() || (collector.isHadActivityCollected() && collector.getHadActivity()))
            {
                conversationEnded = true;
                if(!collector.getHadActivity())
                {
                    sendCollectedData();
                }

                addDialogMessage(getString(R.string.server_name), "Dialog ended! Press OK to exit");
            }
        }

        @Override
        public void onFail()
        {
            addDialogMessage(getString(R.string.server_name), getString(R.string.service_error));
            ChangeSendButtonState(true);
        }
    }

    private class StartSessionCallback extends WatsonApi.WatsonCallback
    {
        @Override
        public void onSuccess()
        {

            addDialogMessage("Owncraft health", getString(R.string.service_ok));

            EditText textBox = findViewById(R.id.editText);
            textBox.setText("");
            addDialogMessage(getString(R.string.server_name), watson.ReadWatsonAnswer());


            ChangeSendButtonState(true);
        }

        @Override
        public void onFail()
        {
            addDialogMessage(getString(R.string.server_name), getString(R.string.service_error));
            ChangeSendButtonState(false);
        }
    }
}
