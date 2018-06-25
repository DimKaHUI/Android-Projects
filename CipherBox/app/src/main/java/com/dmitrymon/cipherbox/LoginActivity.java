package com.dmitrymon.cipherbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity
{
    Button loginButton;
    EditText passwordInput;
    EditText ivInput;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupLayout();
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }

    private void setupLayout()
    {
        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new ButtonListener());
        passwordInput = findViewById(R.id.passwordInput);
        ivInput = findViewById(R.id.ivInput);
    }

    class ButtonListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            if(v == loginButton)
            {
                invokeMenuActivity();
            }
        }
    }

    void invokeMenuActivity()
    {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.setAction(MenuActivity.ACTION_LOGIN);
        String password = new StringBuilder(passwordInput.getText()).toString();
        String iv = new StringBuilder(ivInput.getText()).toString();
        //Log.v(LoginActivity.class.getName(), "Sending password: " + password);
        intent.putExtra(MenuActivity.DATA_PASSWORD, PasswordProcessor.GetKey(password));
        intent.putExtra(MenuActivity.DATA_IV, PasswordProcessor.GetIV(iv));
        startActivity(intent);
    }
}
