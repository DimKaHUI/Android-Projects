package com.dmitrymon.cipherbox;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

public abstract class LoginActivity extends Activity
{
    public static final String ACTION_LOGIN = BuildConfig.APPLICATION_ID + ".ACTION_LOGIN";

    // Data for logging in
    public static final String DATA_IV = BuildConfig.APPLICATION_ID + ".DATA_IV";
    public static final String DATA_PASSWORD = BuildConfig.APPLICATION_ID + ".DATA_PASSWORD";

    public static final String ACTION_GET_LOGIN_DATA = BuildConfig.APPLICATION_ID + ".ACTION_GET_LOGIN_DATA";

    @NonNull
    public static LoginActivity getPreferredLoginActivity(Activity invoker)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(invoker);

        // FIXME Default value is not grabbed from sharedPreferences
        String value = prefs.getString(invoker.getString(R.string.pref_login_method_key), "Password");
        if(value.equals("Password"))
            return new TextLoginActivity();
        if(value.equals("Graphic"))
        {
            return new GraphicalLoginActivity();
        }
        return new TextLoginActivity();
    }


    public void requestLoginData(Activity invoker, int requestCode)
    {
        Intent intent = new Intent(invoker, getClass());
        intent.setAction(ACTION_GET_LOGIN_DATA);
        invoker.startActivityForResult(intent, requestCode);
    }
}
