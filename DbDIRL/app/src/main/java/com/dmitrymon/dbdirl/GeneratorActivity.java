package com.dmitrymon.dbdirl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class GeneratorActivity extends AppCompatActivity
{

    private String serverAddress;
    private Protocol protocol;
    private Button finishGeneratorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generator);

        processProtocol();
        processViews();
    }

    private void processProtocol()
    {
        if(protocol == null)
        {
            protocol = new Protocol(this, false);

            protocol.setIpReceiveCallback(new Protocol.TextDataCallback()
            {
                @Override
                public void Callback(String sender, String data)
                {
                    serverAddress = data;
                    Toast.makeText(getApplicationContext(), "Server ip received: " + serverAddress, Toast.LENGTH_SHORT).show();
                    protocol.SendGeneratorAnswer(serverAddress);
                }
            });

            protocol.StartListening();
        }
    }

    private void processViews()
    {
        if(finishGeneratorButton == null)
        {
            finishGeneratorButton = findViewById(R.id.finishGeneratorButton);
            finishGeneratorButton.setOnClickListener(new View.OnClickListener()
            {
<<<<<<< HEAD
                serverAddress = InetAddress.getByName(result);
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            } catch (UnknownHostException e)
            {
                e.printStackTrace();
            }
=======
                @Override
                public void onClick(View v)
                {
                    protocol.sendGeneratorFinish(serverAddress);
                }
            });
>>>>>>> 130de89ea1fd4921b79e2c75586c3f16c181f9bd
        }
    }
}
