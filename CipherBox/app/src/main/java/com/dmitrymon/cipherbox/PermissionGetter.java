package com.dmitrymon.cipherbox;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.security.Permission;

public class PermissionGetter extends Activity
{

    public static final String ACTION_GET_PERMISSION = "com.dmirtymon.cipherbox.ACTION_GET_PERMISSION";
    public static final String DATA_PERMISSION_STRING = "com.dmirtymon.cipherbox.DATA_PERMISSION_STRING";
    public static final String DATA_ANSWER = "com.dmirtymon.cipherbox.ANSWER";
    static final int REQUEST_CODE = 500;

    TextView label;
    String permission;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_getter);

        setupLayout();
        processIntent(getIntent());
    }

    private void setupLayout()
    {
        label = findViewById(R.id.textView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        if(requestCode == REQUEST_CODE)
        {
            if(permissions.length > 0)
            {
                if(permissions[0].equals(permission))
                {
                    if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        makeAnswer(true);
                    else
                        makeAnswer(false);
                }
            }
        }
    }


    private void makeAnswer(boolean positive)
    {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra(DATA_ANSWER, positive);
        setResult(RESULT_OK, intent);
        finish();
    }


    private void processIntent(Intent intent)
    {
        if (intent.getAction() == ACTION_GET_PERMISSION)
        {
            //outsideRequestCode = intent.
            permission = intent.getStringExtra(DATA_PERMISSION_STRING);
            requestPermissions(new String[]{permission}, REQUEST_CODE);
        }
    }
}
