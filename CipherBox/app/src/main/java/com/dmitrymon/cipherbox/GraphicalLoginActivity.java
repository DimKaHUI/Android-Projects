package com.dmitrymon.cipherbox;

import android.os.Bundle;

public class GraphicalLoginActivity extends LoginActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graphical_login);
    }
}
