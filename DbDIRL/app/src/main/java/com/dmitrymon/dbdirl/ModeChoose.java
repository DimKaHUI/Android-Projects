package com.dmitrymon.dbdirl;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ModeChoose extends AppCompatActivity
{

    Button serverButton;
    Button generatorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_choose);

        processViews();
    }

    private void processViews()
    {
        if(serverButton == null || generatorButton == null)
        {
            serverButton = findViewById(R.id.serverButton);
            generatorButton = findViewById(R.id.generatorButton);

            class Listener implements View.OnClickListener
            {

                @Override
                public void onClick(View v)
                {
                    if (v == serverButton)
                    {
                        Intent intent = new Intent(getApplicationContext(), ServerActivity.class);
                        startActivity(intent);
                    }
                    if (v == generatorButton)
                    {
                        Toast.makeText(getApplicationContext(), "Not implemented!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            Listener listener = new Listener();
            serverButton.setOnClickListener(listener);
            generatorButton.setOnClickListener(listener);
        }
    }
}
