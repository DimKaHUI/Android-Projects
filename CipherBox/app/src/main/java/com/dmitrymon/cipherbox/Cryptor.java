package com.dmitrymon.cipherbox;

import android.util.Log;

import java.io.BufferedInputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Cryptor
{
    public  static final String ALGORITHM_NAME = "AES";
    public  static final String ALGORITHM_MODE = "CBC";
    public  static final String ALGORITHM_PADDING = "PKCS5Padding";

    private Cipher cipher;

    public class WrongKey extends Exception
    {

    }

    public class InvalidKeySize extends Exception
    {
        String msg;
        public InvalidKeySize(String msg)
        {
            this.msg = msg;
        }


        @Override
        public String getMessage()
        {
            return msg;
        }
    }

    public enum Mode
    {
        DECRYPTING, ENCRYPTING
    }

    public Cryptor(byte[] keyBytes, byte[] ivBytes, Mode mode) throws InvalidKeySize
    {
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM_NAME);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        try
        {
            cipher = Cipher.getInstance(ALGORITHM_NAME + "/" + ALGORITHM_MODE + "/" + ALGORITHM_PADDING);
            switch (mode)
            {
                case DECRYPTING:
                    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
                    break;
                case ENCRYPTING:
                    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
                    break;
            }
        }
        catch (NoSuchAlgorithmException ex)
        {
            // TODO Suppressing impossible error
            ex.printStackTrace();
        }
        catch (NoSuchPaddingException ex)
        {
            // TODO Suppressing impossible error
            ex.printStackTrace();
        }
        catch (InvalidKeyException ex)
        {
            throw new InvalidKeySize(ex.getMessage());
        }
        catch (InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
            throw new InvalidKeySize(e.getMessage());
        }
    }

    public int getOutputSize(int inputLen)
    {
        return cipher.getOutputSize(inputLen);
    }

    public byte[] update(byte block[])
    {
        return cipher.update(block);
    }

    public byte[] doFinal(byte[] lastBlock)
    {
        try
        {
            return cipher.doFinal(lastBlock);
        }
        catch (IllegalBlockSizeException ex)
        {
            // TODO Suppressing impossible error
            Log.e("Cryptor","Illegal block size exception");
        }
        catch (BadPaddingException ex)
        {
            // TODO Suppressing impossible error
            Log.e("Cryptor","Bad padding exception");
        }

        return null;
    }
}
