package com.dmitrymon.cipherbox;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileProcessingActivity extends Activity
{
    // Data for decrypting
    public static final String ACTION_DECRYPT = BuildConfig.APPLICATION_ID + ".ACTION_DECRYPT";
    public static final String ACTION_ENCRYPT = BuildConfig.APPLICATION_ID + ".ACTION_ENCRYPT";
    public static final String ACTION_DELETE_FILE = BuildConfig.APPLICATION_ID + ".ACTION_DELETE_FILE";
    public static final String DATA_PASSWORD = BuildConfig.APPLICATION_ID + ".DATA_PROCESSING_PASSWORD";
    public static final String DATA_IV = BuildConfig.APPLICATION_ID + ".DATA_IV";
    public static final String DATA_NAME_STRING = BuildConfig.APPLICATION_ID + ".DATA_NAME_STRING";
    public static final String DATA_DEST_PATH = BuildConfig.APPLICATION_ID + ".DATA_STORAGE_PATH";
    public static final String DATA_IS_PERMANENT = BuildConfig.APPLICATION_ID + ".DATA_IS_PERMANENT";
    public static final String DATA_CIPHER_NAMES = BuildConfig.APPLICATION_ID + ".DATA_CIPHER_NAMES";

    public static final String FILENAME_ENCRYPTED_PREFIX = ".cb_";

    public static int BLOCK_SIZE = 256 * 1024;
    public static int BLOCK_SIZE_DELETION = 256 * 1024;

    private static int LOGIN_REQUEST_CODE = 123;
    private byte[] keyBytes;
    private byte[] ivBytes;
    private File lastEncryptedFile;

    private File extracted;
    private File sourceFile;
    private boolean viewerStarted = false;
    private boolean cipherNames = false;
    private boolean hideUnsafeDeletionOption = true;
    private ProgressBar bar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_processing);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        bar = findViewById(R.id.progressBar);

        getPreferences();
        processIntent(getIntent());
    }

    private void getPreferences()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getString(R.string.pref_hide_unsafe_deletion_option_key);
        hideUnsafeDeletionOption = sharedPref.getBoolean(key, true);
    }

    static File DecryptFileName(File file, byte[] keyBytes, byte[] ivBytes)
    {
        try
        {
            Cryptor cryptor = new Cryptor(keyBytes, ivBytes, Cryptor.Mode.DECRYPTING);
            String sourceFileName = file.getName();
            String replFileName = sourceFileName.replace(FILENAME_ENCRYPTED_PREFIX, "");
            if(sourceFileName.startsWith(FILENAME_ENCRYPTED_PREFIX))
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

    static String GetDecryptedName(File file, byte[] keyBytes, byte[] ivBytes)
    {
        String sourceFileName = file.getName();
        String result = sourceFileName.replace(FILENAME_ENCRYPTED_PREFIX, "");
        try
        {
            if(sourceFileName.startsWith(FILENAME_ENCRYPTED_PREFIX))
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
            //return result;
        }
        catch (Cryptor.InvalidKeySize invalidKeySize)
        {
            invalidKeySize.printStackTrace();
        }
        /*catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
        }*/
        return result;
    }

    static String GetEncryptedName(File file, byte[] keyBytes, byte[] ivBytes)
    {
        String fileName = file.getName();
        if(fileName.startsWith(FILENAME_ENCRYPTED_PREFIX))
            return file.getName();
        try
        {
            Cryptor cryptor = new Cryptor(keyBytes, ivBytes, Cryptor.Mode.ENCRYPTING);
            byte[] plain = file.getName().getBytes();
            byte[] encrypted = cryptor.doFinal(plain);
            String encoded = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE);
            file = new File(file.getParentFile(), FILENAME_ENCRYPTED_PREFIX + encoded);
        }
        catch (Cryptor.InvalidKeySize invalidKeySize)
        {
            invalidKeySize.printStackTrace();
        }
        return file.getName();
    }

    static File EncryptFileName(File file, byte[] keyBytes, byte[] ivBytes)
    {
        String fileName = file.getName();
        if(fileName.startsWith(FILENAME_ENCRYPTED_PREFIX))
            return file;
        try
        {
            Cryptor cryptor = new Cryptor(keyBytes, ivBytes, Cryptor.Mode.ENCRYPTING);
            byte[] plain = file.getName().getBytes();
            byte[] encrypted = cryptor.doFinal(plain);
            String encoded = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE);
            File renamed = new File(file.getParentFile(), FILENAME_ENCRYPTED_PREFIX + encoded);
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

    public static void deleteFile(Activity invoker, File file)
    {
        Intent intent = new Intent(invoker, FileProcessingActivity.class);
        intent.setAction(ACTION_DELETE_FILE);
        intent.putExtra(DATA_NAME_STRING, file.getPath());
        invoker.startActivity(intent);
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

    private boolean checkPermissions()
    {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void processIntent(Intent intent)
    {
        String action = intent.getAction();
        if(action == null)
            return;
        keyBytes = intent.getByteArrayExtra(DATA_PASSWORD);
        ivBytes = intent.getByteArrayExtra(DATA_IV);
        String destinationPath = intent.getStringExtra(DATA_DEST_PATH);
        String filePath = intent.getStringExtra(DATA_NAME_STRING);
        cipherNames = intent.getBooleanExtra(DATA_CIPHER_NAMES, cipherNames);


        if(!checkPermissions())
        {
            showMessageNoPermissions();
            return;
        }

        switch (action)
        {
            case ACTION_ENCRYPT:
                addFileToStorage(new File(filePath), new File(destinationPath));
                break;
            case ACTION_DECRYPT:
                boolean isPermanent = intent.getBooleanExtra(DATA_IS_PERMANENT, false);
                if (!isPermanent)
                    extractFileFromStorage(filePath, null);
                else
                    extractFileFromStorage(filePath, new File(destinationPath));
                break;
            case Intent.ACTION_VIEW:
                Uri content = intent.getData();
                if (content != null)
                    addFileFromUri(content);
                else
                    finish();
                break;

            case Intent.ACTION_SEND:
                onActionSend(intent);
                break;

            case ACTION_DELETE_FILE:
                File file = new File(filePath);
                requestFileDeletion(file, R.string.deletion_dialog_on_permanent);
                break;
        }
    }

    private void onActionSend(Intent intent)
    {
        String message = "";
        ClipData data = intent.getClipData();
        message += "Data loaded\n";
        boolean success = true;

        if(data == null)
        {
            success = false;
            message += "Data is null";
        }
        else
        {
            ClipData.Item item = data.getItemAt(0);
            if(item == null)
            {
                success = false;
                message += "item is null";
            }
            else
            {
                Uri fileUri = item.getUri();
                if (fileUri != null)
                {
                    addFileFromUri(fileUri);
                    message += "INVOCATION";
                }
                else
                {
                    success = false;
                    message += "uri is null";
                }
            }
        }

        if(!success)
        {
            showMessageNotFile();
            //ErrorSenderActivity.sendErrorReport(this, new Exception(), message); // TODO Remove error sender
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    private void addFileFromUri(Uri uri)
    {
        Log.v("FileProcessingActivity", uri.toString());
        String path = uri.getPath();
        sourceFile = new File(path);

        LoginActivity.getPreferredLoginActivity(this).requestLoginData(this, LOGIN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if(requestCode == LOGIN_REQUEST_CODE)
        {
            if(resultCode == RESULT_OK)
            {
                keyBytes = data.getByteArrayExtra(TextLoginActivity.DATA_PASSWORD);
                ivBytes = data.getByteArrayExtra(TextLoginActivity.DATA_IV);
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                String storagePath = sharedPref.getString(getString(R.string.pref_storage_path_key), getString(R.string.pref_default_path));
                File extStorage = new File(Environment.getExternalStorageDirectory(), storagePath);
                File destination = new File(extStorage, sourceFile.getName());

                addFileToStorage(sourceFile, destination);
            }
        }
    }

    private void addFileToStorage(File srcFile, File destinationFile)
    {
        TransmitterParams params = new TransmitterParams();

        if(cipherNames)
            destinationFile = EncryptFileName(destinationFile, keyBytes, ivBytes);

        try
        {
            params.writer = new FileOutputStream(destinationFile);
            params.reader = new FileInputStream(srcFile);
            params.mode = 1;
            lastEncryptedFile = srcFile;
            new TransmitTask().execute(params);
        }
        catch (FileNotFoundException e)
        {
            showMessageFileNotFound();
            ErrorSenderActivity.sendErrorReport(this, e, "");
        }
    }

    @SuppressLint("StaticFieldLeak")
    class SafeDeleteTask extends AsyncTask<File, Integer, Void>
    {

        boolean success = true;
        File toDelete;
        @Override
        protected Void doInBackground(File... files)
        {
            toDelete = files[0];
            try
            {
                byte[] block = new byte[BLOCK_SIZE_DELETION];
                for (int i = 0; i < block.length; i++)
                    block[i] = (byte) 0xFF;

                long offset = 0;
                long fileLen = toDelete.length();
                FileOutputStream writer = new FileOutputStream(toDelete);

                setProgressBarValue(0);
                float increment = (float)block.length / fileLen * 100;
                float progress = 0;

                while (offset < fileLen)
                {
                    writer.write(block, 0, block.length);
                    offset += block.length;
                    progress += increment;
                    publishProgress((int)progress);
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
        protected void onProgressUpdate(Integer... progress)
        {
            setProgressBarValue(progress[0]);
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
            if(success)
                finish();
        }
    }

    @SuppressLint("StaticFieldLeak")
    class TransmitTask extends AsyncTask<TransmitterParams, Integer, Void>
    {
        int mode;
        boolean success = true;
        boolean shouldOpen;

        private FileInputStream reader;
        private OutputStream writer;

        @Override
        protected Void doInBackground(TransmitterParams... params)
        {
            try
            {
                TransmitterParams param = params[0];
                shouldOpen = param.shouldOpen;
                mode = param.mode;

                reader = param.reader;

                writer = param.writer;


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
                long threshold = blockSize * count;

                if(threshold == fileLength)
                {
                    threshold -= blockSize;
                }

                double progress = 0;
                double progressIncrement = (double)blockSize / fileLength * 100.0f;

                for(; offset < threshold; offset += blockSize)
                {
                    byte[] block = new byte[(int)blockSize];
                    int bytesWereRead = reader.read(block, 0, (int)blockSize);
                    if(bytesWereRead != blockSize)
                    {
                        throw new IOException("Possible data corruption.");
                    }

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

                long delta = fileLength - threshold;

                byte[] finalBlock = new byte[(int)delta];
                int bytesWereRead = reader.read(finalBlock, 0, finalBlock.length);

                if(bytesWereRead != finalBlock.length)
                {
                    throw new IOException("Possible data corruption.");
                }

                byte[] encryptedFinalBlock = cryptor.doFinal(finalBlock);
                if(encryptedFinalBlock == null)
                    success = false;
                else
                    writer.write(encryptedFinalBlock, 0, encryptedFinalBlock.length);

                publishProgress(100);
                reader.close();
                writer.close();
            }
            catch (IOException e)
            {
                showMessage(e);
            }
            catch (Cryptor.InvalidKeySize ex)
            {
                showMessageInvalidKeySize();
            }
            finally
            {
                try
                {
                    reader.close();
                    writer.close();
                }
                catch (IOException e)
                {
                    showMessage(e);
                }
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

            }
            if(mode == 0)
            {
                if(success && shouldOpen)
                    showExtractedFile();
                if(success && !shouldOpen)
                {
                    requestFileDeletion(sourceFile, R.string.deletion_dialog_on_permanent);
                }
            }
            if(mode == 1 && success)
            {
                requestFileDeletion(lastEncryptedFile, R.string.deletion_dialog_on_adding);
            }
        }
    }

    private void setProgressBarValue(int percent)
    {
        bar.setProgress(percent);
    }

    private void deleteFileSafely(File file)
    {
        TextView label = findViewById(R.id.progressBarLabel);
        label.setText(R.string.file_processor_deleting);
        new SafeDeleteTask().execute(file);
    }

    private void deleteFileUnsafely(File file)
    {
        boolean deleted = file.delete();
        if(!deleted)
            Log.e("DELETING", "File not deleted");
        finish();
    }

    private void requestFileDeletion(final File file, int messageId)
    {
        class PositiveListener implements DialogInterface.OnClickListener
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                deleteFileSafely(file);
            }
        }

        class NeutralListener implements DialogInterface.OnClickListener
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                deleteFileUnsafely(file);
            }
        }

        class NegativeListener implements DialogInterface.OnClickListener
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                finish();
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.deletion_dialog_yes, new PositiveListener());
        if(!hideUnsafeDeletionOption)
            builder.setNeutralButton(R.string.deletion_dialog_yes_unsafe, new NeutralListener());
        builder.setNegativeButton(R.string.deletion_dialog_no, new NegativeListener());
        builder.setCancelable(false);
        builder.setMessage(messageId);
        builder.setTitle(R.string.deletion_dialog_title);
        AlertDialog dialog = builder.create();
        dialog.show();
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
            param.shouldOpen = true;
        }
        else
        {
            param.shouldOpen = false;
            String name = GetDecryptedName(destFile, keyBytes, ivBytes);
            destFile = new File(destFile.getParentFile(), name);
        }

        extracted = destFile;
        sourceFile = encryptedFile;

        try
        {
            if(!destFile.exists())
            {
                Log.e("TEST", destFile.getPath());
                boolean isFileCreated = destFile.createNewFile();
                if(!isFileCreated)
                    throw new IOException("Impossible to create file");
            }
            param.writer = new FileOutputStream(destFile);
            param.reader = new FileInputStream(encryptedFile);
            param.mode = 0;
            new TransmitTask().execute(param);
        }
        catch (FileNotFoundException e)
        {
            showMessageFileNotFound();
        }
        catch (IOException ex)
        {
            showMessage(ex);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(viewerStarted)
            requestFileDeletion(extracted, R.string.deletion_dialog_on_tmp);
    }

    private void showExtractedFile()
    {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        //Uri uri = Uri.fromFile(extracted);

        if(cipherNames)
        {
            extracted = DecryptFileName(extracted, keyBytes, ivBytes);
        }

        Uri uri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                extracted);

        String type = getFileMime(extracted);
        intent.setDataAndType(uri, type);

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try
        {
            startActivity(intent);
            viewerStarted = true;
        }
        catch (Exception ex)
        {
            String extra = "uri: " + uri.toString() + "\n";
            //extra += "type: " + type;
            ErrorSenderActivity.sendErrorReport(this, ex, "uri: " + extra);
            showMessageNoActivity();
        }
    }

    private class TransmitterParams
    {
        FileInputStream reader;
        FileOutputStream writer;
        int mode;
        boolean shouldOpen = true;
    }



    // Error messages

    class onErrorFinishListener implements ErrorDialogBuilder.onFinishListener
    {
        @Override
        public void onFinish()
        {
            finish();
        }
    }

    private void showMessage(String message)
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, message, new onErrorFinishListener());
        builder.showDialog();
    }

    private void showMessage(Exception exception)
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, exception.getMessage(), new onErrorFinishListener());
        builder.showDialog();
    }

    private void showMessageWrongKey()
    {
        class onWrongKeyListener implements ErrorDialogBuilder.onFinishListener
        {
            @Override
            public void onFinish()
            {
                deleteFileSafely(extracted);
            }
        }
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, getString(R.string.message_wrong_key), new onWrongKeyListener());
        builder.showDialog();
    }

    private void showMessageNotFile()
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, getString(R.string.message_not_a_file), new onErrorFinishListener());
        builder.showDialog();
    }

    private void showMessageNoPermissions()
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, getString(R.string.message_permissions_not_granted), new onErrorFinishListener());
        builder.showDialog();
    }

    private void showMessageFileNotFound()
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, getString(R.string.message_file_not_found), new onErrorFinishListener());
        builder.showDialog();
    }

    private void showMessageNoActivity()
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, getString(R.string.message_no_activity), new onErrorFinishListener());
        builder.showDialog();
    }

    private void showMessageInvalidKeySize()
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, "Invalid key size", new onErrorFinishListener());
        builder.showDialog();
    }

}
