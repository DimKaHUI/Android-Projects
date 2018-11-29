package com.dmitrymon.owncraftdialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.Objects;

public class AutorunRecevier extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED) || Objects.equals(intent.getAction(), Intent.ACTION_LOCKED_BOOT_COMPLETED))
        {
            Toast toast = Toast.makeText(context.getApplicationContext(), "Autorun broadcast received!", Toast.LENGTH_LONG);
            toast.show();

            // Starting service

            Intent startServiceIntent = new Intent(context, OwncraftServerListener.class);
            startServiceIntent.setAction(OwncraftServerListener.ACTION_START_FROM_AUTORUN);

            context.startService(startServiceIntent);
        }
    }
}
