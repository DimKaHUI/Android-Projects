package com.dmitrymon.cipherbox;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordProcessor
{
    public static byte[] GetKey(String password)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(password.getBytes());
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
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(iv.getBytes());
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
