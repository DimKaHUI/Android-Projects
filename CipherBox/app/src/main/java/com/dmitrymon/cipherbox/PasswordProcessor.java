package com.dmitrymon.cipherbox;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class PasswordProcessor
{
    public static byte[] GetKey(String password)
    {
        try
        {
            return GetStringHash(password);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] GetIV(String iv)
    {
        try
        {
            return GetStringHash(iv);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private static byte[] GetStringHash(String str) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] strBytes = str.getBytes();
        byte[] hash = md.digest(strBytes);
        Log.i("HASH","Source string: " + str + ", bytes: " + ToString(strBytes) + ", hash: " + ToString(hash));
        return hash;
    }

    private static String ToString(byte[] bytes)
    {
        StringBuilder builder = new StringBuilder();

        for (byte b : bytes)
        {
            builder.append(b & 0xFF).append(' ');
        }

        return builder.toString();
    }


}
