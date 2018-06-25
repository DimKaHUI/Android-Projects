package com.dmitrymon.cipherbox;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;

public class FileProcessingActivity extends Activity
{
    // Data for decrypting
    public static final String ACTION_DECRYPT = "com.dmitrymon.cipherbox.ACTION_DECRYPT";
    public static final String ACTION_ENCRYPT = "com.dmitrymon.cipherbox.ACTION_ENCRYPT";
    public static final String DATA_PASSWORD = "com.dmitrymon.cipherbox.DATA_PROCESSING_PASSWORD";
    public static final String DATA_IV = "com.dmitrymon.cipherbox.DATA_IV";
    public static final String DATA_NAME_STRING = "com.dmitrymon.cipherbox.DATA_NAME_STRING";
    public static final String DATA_DEST_PATH = "com.dmitrymon.cipherbox.DATA_STORAGE_PATH";
    public static final String DATA_IS_PERMANENT = "com.dmitrymon.cipherbox.DATA_IS_PERMANENT";
    public static final String DATA_CIPHER_NAMES = "com.dmitrymon.cipherbox.DATA_CIPHER_NAMES";

    public static final String FILENAME_ECNRYPTED_PREFIX = ".cb_";

    public static final int BLOCK_SIZE = 1048576; // 1 Mb

    private byte[] keyBytes;
    private byte[] ivBytes;
    private File lastEncryptedFile;

    private File extracted;
    private boolean viewerStarted = false;
    boolean cipherNames = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_processing);

        processIntent(getIntent());
    }

    File DecryptFileName(File file)
    {
        try
        {
            Cryptor cryptor = new Cryptor(keyBytes, ivBytes, Cryptor.Mode.DECRYPTING);
            String sourceFileName = file.getName();
            String replFileName = sourceFileName.replace(FILENAME_ECNRYPTED_PREFIX, "");
            if(sourceFileName.startsWith(FILENAME_ECNRYPTED_PREFIX))
            {
                try
                {
                    byte[] name = android.util.Base64.decode(replFileName, android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE);
                    if (name != null)
                    {
                        name = cryptor.doFinal(name);
                        if (name != null)
                        {
                            File renamed = new File(file.getParentFile(), new String(name));
                            if (file.renameTo(renamed))
                                file = renamed;
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log.e("ERROR", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
        catch (Cryptor.InvalidKeySize invalidKeySize)
        {
            invalidKeySize.printStackTrace();
        }
        return file;
    }

    public static String GetDecryptedName(File file, byte[] keyBytes, byte[] ivBytes)
    {
        try
        {
            String sourceFileName = file.getName();
            String result = sourceFileName.replace(FILENAME_ECNRYPTED_PREFIX, "");
            if(sourceFileName.startsWith(FILENAME_ECNRYPTED_PREFIX))
            {
                Cryptor cryptor = new Cryptor(keyBytes, ivBytes, Cryptor.Mode.DECRYPTING);
                byte[] name = android.util.Base64.decode(result, android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE);
                if (name != null)
                {
                    byte[] decrypted = cryptor.doFinal(name);
                    if (decrypted != null)
                        result = new String(decrypted);
                    else
                        result = file.getName();
                }
            }
            return result;
        }
        catch (Cryptor.InvalidKeySize invalidKeySize)
        {
            invalidKeySize.printStackTrace();
            return file.getName();
        }
        catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
            return file.getName();
        }
    }

    File EncryptFileName(File file)
    {
        try
        {
            Cryptor cryptor = new Cryptor(keyBytes, ivBytes, Cryptor.Mode.ENCRYPTING);
            byte[] plain = file.getName().getBytes();
            byte[] encrypted = cryptor.doFinal(plain);
            String encoded = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE);
            File renamed = new File(file.getParentFile(), FILENAME_ECNRYPTED_PREFIX + encoded);
            if(file.renameTo(renamed))
                file = renamed;
        }
        catch (Cryptor.InvalidKeySize invalidKeySize)
        {
            invalidKeySize.printStackTrace();
        }
        return file;
    }

    public static void inputFile(Context context, File sourceFile, File destFile, byte[] key, byte[] iv, boolean cipherNames)
    {
        Intent intent = new Intent(context, FileProcessingActivity.class);
        intent.setAction(ACTION_ENCRYPT);
        intent.putExtra(DATA_DEST_PATH, destFile.getPath());
        intent.putExtra(DATA_PASSWORD, key);
        intent.putExtra(DATA_IV, iv);
        intent.putExtra(DATA_NAME_STRING, sourceFile.getPath());
        intent.putExtra(DATA_CIPHER_NAMES, cipherNames);
        context.startActivity(intent);
    }

    public static void outputFile(Activity context, File info, byte[] key, byte[] iv, boolean cipherNames)
    {
        Intent intent = new Intent(context, FileProcessingActivity.class);
        intent.setAction(FileProcessingActivity.ACTION_DECRYPT);
        intent.putExtra(DATA_NAME_STRING, info.getPath());
        intent.putExtra(DATA_PASSWORD, key);
        intent.putExtra(DATA_IV, iv);
        intent.putExtra(DATA_CIPHER_NAMES, cipherNames);
        context.startActivity(intent);
    }

    public static void outputFilePermanent(Activity context, File fileSource, File fileDestination, int requestCode, byte[] key, byte[] iv,  boolean cipherNames)
    {
        Intent intent = new Intent(context, FileProcessingActivity.class);
        intent.setAction(FileProcessingActivity.ACTION_DECRYPT);
        intent.putExtra(DATA_NAME_STRING, fileSource.getPath());
        intent.putExtra(DATA_DEST_PATH, fileDestination.getPath());
        intent.putExtra(DATA_IS_PERMANENT, true);
        intent.putExtra(DATA_PASSWORD, key);
        intent.putExtra(DATA_IV, iv);
        intent.putExtra(DATA_CIPHER_NAMES, cipherNames);
        context.startActivityForResult(intent, requestCode);
    }

    private void processIntent(Intent intent)
    {
        keyBytes = intent.getByteArrayExtra(DATA_PASSWORD);
        ivBytes = intent.getByteArrayExtra(DATA_IV);
        String destinationPath = intent.getStringExtra(DATA_DEST_PATH);
        String filePath = intent.getStringExtra(DATA_NAME_STRING);
        cipherNames = intent.getBooleanExtra(DATA_CIPHER_NAMES, cipherNames);

        if(intent.getAction().equals(ACTION_ENCRYPT))
        {
            addFileToStorage(new File(filePath), new File(destinationPath));
        }
        else if(intent.getAction().equals(ACTION_DECRYPT))
        {
            boolean isPermanent = intent.getBooleanExtra(DATA_IS_PERMANENT, false);
            if(!isPermanent)
                extractFileFromStorage(filePath, null);
            else
                extractFileFromStorage(filePath, new File(destinationPath));
        }
        else if(intent.getAction().equals(Intent.ACTION_SEND))
        {
            Log.w("", "Some file was received!: " + intent.getDataString());

        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    private void addFileToStorage(File sourceFile, File destinationFile)
    {
        TransmitterParams params = new TransmitterParams();

        if(cipherNames)
            destinationFile = EncryptFileName(destinationFile);

        try
        {
            params.writer = new FileOutputStream(destinationFile);
            params.reader = new FileInputStream(sourceFile);
            params.mode = 1;
            lastEncryptedFile = sourceFile;
            new TransmitTask().execute(params);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

    }

    class SafeDeleteTask extends AsyncTask<File, Void, Void>
    {

        boolean success = true;
        File toDelete;
        @Override
        protected Void doInBackground(File... files)
        {
            toDelete = files[0];
            try
            {
                byte[] block = new byte[1024 * 1024];
                for (int i = 0; i < block.length; i++)
                    block[i] = (byte) 0xFF;

                int offset = 0;
                int fileLen = (int) toDelete.length();
                FileOutputStream writer = new FileOutputStream(toDelete);

                while (offset < fileLen)
                {
                    writer.write(block, 0, block.length);
                    offset += block.length;
                }
            }
            catch (IOException ex)
            {
                success = false;
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            if(!toDelete.delete())
            {
                Toast t = Toast.makeText(getApplicationContext(), "File not deleted. Do it manually", Toast.LENGTH_SHORT);
                t.show();
            }

            if(!success)
            {
                Toast t = Toast.makeText(getApplicationContext(), "IOException occured!", Toast.LENGTH_SHORT);
                t.show();
            }
            finish();
        }
    }


    class TransmitTask extends AsyncTask<TransmitterParams, Integer, Void>
    {

        int mode;
        boolean success = true;
        boolean shouldOpen;

        // TODO Correct offsets of storage
        @Override
        protected Void doInBackground(TransmitterParams... params)
        {
            try
            {
                TransmitterParams param = params[0];
                shouldOpen = param.shouldOpen;
                mode = param.mode;

                FileInputStream reader = param.reader;

                OutputStream writer = param.writer;


                Cryptor cryptor;
                if (mode == 1)
                {
                    cryptor = new Cryptor(keyBytes, ivBytes, Cryptor.Mode.ENCRYPTING);
                } else
                {
                    cryptor = new Cryptor(keyBytes, ivBytes, Cryptor.Mode.DECRYPTING);
                }

                long fileLength = reader.getChannel().size();
                long blockSize = BLOCK_SIZE;
                long count = fileLength / blockSize;
                long offset = 0;
                long thresold = blockSize * count;

                if(thresold == fileLength)
                {
                    thresold -= blockSize;
                }

                double progress = 0;
                double progressIncrement = (double)blockSize / fileLength * 100.0f;

                for(; offset < thresold; offset += blockSize)
                {
                    byte[] block = new byte[(int)blockSize];
                    reader.read(block, 0, (int)blockSize);

                    byte[] encryptedBlock = cryptor.update(block);

                    if(encryptedBlock == null)
                    {
                        success = false;
                        break;
                    }

                    writer.write(encryptedBlock, 0, encryptedBlock.length);

                    progress += progressIncrement;
                    publishProgress((int)progress);
                }

                long delta = fileLength - thresold;

                byte[] finalBlock = new byte[(int)delta];
                reader.read(finalBlock, 0, finalBlock.length);
                byte[] encryptedFinalBlock =  cryptor.doFinal(finalBlock);
                if(encryptedFinalBlock == null)
                    success = false;
                else
                    writer.write(encryptedFinalBlock, 0, encryptedFinalBlock.length);

                publishProgress(100);
                reader.close();
                writer.close();
            }
            catch (FileNotFoundException ex)
            {
                Log.e(FileProcessingActivity.class.getName(), ex.getMessage());
                handleFileNotFound();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (Cryptor.InvalidKeySize ex)
            {
                handleInvalidKeySize();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            setProgressBarValue(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result)
        {
            // Mode 1 - Adding to storage
            if(!success)
            {
                showMessageWrongKey();
                deleteFile(extracted);
            }
            if(mode == 0)
            {
                if(success && shouldOpen)
                    showExtractedFile();
                else if(!shouldOpen)
                    finish();
            }
            if(mode == 1 && success)
            {
                deleteFile(lastEncryptedFile);
            }

            //finish();
        }
    }

    private void setProgressBarValue(int percent)
    {
        ProgressBar bar = findViewById(R.id.progressBar);
        bar.setProgress(percent);
    }

    private void deleteFile(File file)
    {
        /*if(lastEncryptedFile != null && lastEncryptedFile.exists())
            lastEncryptedFile.delete();  */
        new SafeDeleteTask().execute(file);
    }

    private void showMessageWrongKey()
    {
        Toast t = Toast.makeText(this,"Wrong password!", Toast.LENGTH_SHORT);
        t.show();
    }

    private String getFileMime(File file)
    {
        String type = null;

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        String name = file.getName();
        int index = name.lastIndexOf('.');
        if(index != -1)
        {
            String part = name.substring(index + 1);
            type = mimeTypeMap.getMimeTypeFromExtension(part);
            Log.v("TYPE GETTER", "Extension: " + part + ". Type: " + type);
        }

        if(type == null)
            type = "*/*";
        return type;
    }

    private void extractFileFromStorage(String encryptedFilePath, File destFile)
    {
        TransmitterParams param = new TransmitterParams();
        File encryptedFile = new File(encryptedFilePath);

        if(destFile == null)
        {
            //destFile = new File(Environment.getExternalStorageDirectory() + "/" + encryptedFile.getName());
            File externalDirectory = Environment.getExternalStorageDirectory();
            String name = GetDecryptedName(encryptedFile, keyBytes, ivBytes);
            destFile = new File(externalDirectory, name);
        }
        else
        {
            param.shouldOpen = false;
            String name = GetDecryptedName(destFile, keyBytes, ivBytes);
            destFile = new File(destFile.getParentFile(), name);
        }

        extracted = destFile;

        try
        {
            if(!destFile.exists())
            {
                Log.e("TEST", destFile.getPath());
                destFile.createNewFile();
            }
            param.writer = new FileOutputStream(destFile);
            param.reader = new FileInputStream(encryptedFile);
            param.mode = 0;
            new TransmitTask().execute(param);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onResume()
    {
        // TODO File deleting after opening
        super.onResume();
        if(viewerStarted)
            new SafeDeleteTask().execute(extracted);
    }

    private void showExtractedFile()
    {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        //Uri uri = Uri.fromFile(extracted);

        if(cipherNames)
        {
            extracted = DecryptFileName(extracted);
        }

        Uri uri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                extracted);

        String type = getFileMime(extracted);
        intent.setDataAndType(uri, type);

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        //intent.addCategory(Intent.CATEGORY_DEFAULT);

        try
        {
            //if (intent.resolveActivity(getPackageManager()) != null)
            startActivity(intent);
            viewerStarted = true;
            //else
                //showMessageNoActivity();
        }
        catch (Exception ex)
        {
            String extra = "uri: " + uri.toString() + "\n";
            //extra += "type: " + type;
            ErrorSenderActivity.sendErrorReport(this, ex, "uri: " + extra);
            showMessageNoActivity();
        }
    }

    private void handleFileNotFound()
    {
        // TODO Handle file not found exception
        Log.e(FileProcessingActivity.class.getName(), "File not found!");
        setResult(RESULT_CANCELED);
        finish();
    }

    private void handleIoException()
    {
        // TODO handleIoException
        Log.e(FileProcessingActivity.class.getName(), "IO Exception!");
        setResult(RESULT_CANCELED);
        finish();
    }

    private void showMessageNoActivity()
    {
        Toast toast = Toast.makeText(this, "No activity to open this file!", Toast.LENGTH_LONG);
        toast.show();
    }

    private void handleInvalidKeySize()
    {
        // TODO handleInvalidKeySize
        Log.e(FileProcessingActivity.class.getName(), "Invalid key size!");
        setResult(RESULT_CANCELED);
        finish();
    }


    private class TransmitterParams
    {
        FileInputStream reader;
        FileOutputStream writer;
        int mode;
        boolean shouldOpen = true;
    }
}
