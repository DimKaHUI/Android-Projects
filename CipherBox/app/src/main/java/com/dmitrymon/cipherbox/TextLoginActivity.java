package com.dmitrymon.cipherbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class TextLoginActivity extends LoginActivity
{
    private Button loginButton;
    private EditText passwordInput;
    private EditText ivInput;

    private boolean exportData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        processIntent(getIntent());

        setupLayout();
    }

    private void setupLayout()
    {
        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new ButtonListener());
        passwordInput = findViewById(R.id.passwordInput);
        ivInput = findViewById(R.id.ivInput);
    }

    private class ButtonListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            if(v == loginButton)
            {
                sendData();
            }
        }
    }

    private void sendData()
    {
        Intent intent;

        if(!exportData)
        {
            intent = new Intent(this, MenuActivity.class);
        }
        else
        {
            intent = new Intent();
        }

        String password = passwordInput.getText().toString();
        String iv = ivInput.getText().toString();
        intent.putExtra(DATA_PASSWORD, PasswordProcessor.GetKey(password));
        intent.putExtra(DATA_IV, PasswordProcessor.GetIV(iv));

        if(!exportData)
        {
            intent.setAction(ACTION_LOGIN);
            startActivity(intent);
        }
        else
        {
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void processIntent(Intent intent)
    {
        String action = intent.getAction();
        if(action == null)
            return;
        if(action.equals(ACTION_GET_LOGIN_DATA))
        {
            exportData = true;
        }
    }

}
