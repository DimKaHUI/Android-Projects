package com.dmitrymon.owncraftdialog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import static com.dmitrymon.owncraftdialog.WatsonApi.*;

public class MainActivity extends AppCompatActivity
{

    private final int PERMISSION_REQUEST_ID = 123;

    private WatsonApi watson;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        processPermissions();

        processViews();
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


    private class ButtonListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            if(v == findViewById(R.id.sendButton))
            {
                sendMessageToWatson();
            }
        }
    }

    private void addDialogMessage(String sender, String message)
    {
        TextView textView = findViewById(R.id.dialogTextView);

        String text = textView.getText().toString();
        text += sender;
        text += '\n';
        text += message;
        text += '\n';
        text += '\n';

        textView.setText(text);
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
            ChangeSendButtonState(true);
        }

        @Override
        public void onFail()
        {
            addDialogMessage("Owncraft health", getString(R.string.service_error));
            ChangeSendButtonState(false);
        }
    }
}
