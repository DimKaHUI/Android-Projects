package com.dmitrymon.cipherbox;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MenuActivity extends Activity
{

    private LinearLayout linearLayout;
    private Button prefsButton;
    private Button addButton;
    private Button loginButton;
    private File[] fileList;
    private String[] decryptedFileNames;
    private File extracted;


    private static final int OPEN_DOCUMENT_REQUEST_CODE = 125;
    private static final int GET_PERMISSIONS_REQUEST_CODE = 126;
    private static final int DECRYPT_FILE_REQUEST_CODE = 128;
    private static final int OPEN_DIRECTORY_REQUEST_CODE = 129;
    private static final int PREFS_REQUEST_CODE = 130;
    private static final int LOGIN_REQUEST_CODE = 131;

    // Data for encryption/decryption
    private byte[] keyBytes = null;
    private byte[] ivBytes = null;
    private String storagePath;
    private boolean cipherFileNames;
    private boolean activityInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(!activityInitialized)
        {
            initializeActivity();
            activityInitialized = true;
        }
    }

    private void initializeActivity()
    {
        setupLayout();
        getPreferences();
        processIntent(getIntent()); // Only reads key and IV from intent

        boolean permissionsGranted = processPermission();
        boolean passwordNotNull = keyBytes != null;
        if(permissionsGranted && passwordNotNull)
        {
            processFileNames(new File(storagePath));
            readStorage();
        }
    }

    private void requestLoginData()
    {
        LoginActivity loginActivity = LoginActivity.getPreferredLoginActivity(this);
        loginActivity.requestLoginData(this, LOGIN_REQUEST_CODE);
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getPreferences();

        if(keyBytes != null)
            readStorage();
    }

    private boolean processPermission()
    {
        boolean success = true;
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            success = false;
            Intent intent = new Intent(this, PermissionGetter.class);
            intent.setAction(PermissionGetter.ACTION_GET_PERMISSION);
            intent.putExtra(PermissionGetter.DATA_PERMISSION_STRING, Manifest.permission.READ_EXTERNAL_STORAGE);
            startActivityForResult(intent, GET_PERMISSIONS_REQUEST_CODE);
        }

        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            success = false;
            Intent intent = new Intent(this, PermissionGetter.class);
            intent.setAction(PermissionGetter.ACTION_GET_PERMISSION);
            intent.putExtra(PermissionGetter.DATA_PERMISSION_STRING, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            startActivityForResult(intent, GET_PERMISSIONS_REQUEST_CODE);
        }
        return success;
    }

    private void setupLayout()
    {
        ButtonListener listener = new ButtonListener();
        prefsButton = findViewById(R.id.prefsButton);
        prefsButton.setOnClickListener(listener);
        linearLayout = findViewById(R.id.listOfFiles);
        addButton = findViewById(R.id.addToStorageButton);
        addButton.setOnClickListener(listener);
        addButton.setVisibility(View.INVISIBLE);
        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(listener);
    }

    private void processIntent(Intent intent)
    {
        switch (Objects.requireNonNull(intent.getAction()))
        {
            case LoginActivity.ACTION_LOGIN:
                keyBytes = intent.getByteArrayExtra(LoginActivity.DATA_PASSWORD);
                ivBytes = intent.getByteArrayExtra(LoginActivity.DATA_IV);
                break;
            default:
        }
    }

    private void getPreferences()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String storagePathKey = getString(R.string.pref_storage_path_key);
        String defaultPath = getString(R.string.pref_default_path);
        String storageFolder = sharedPref.getString(storagePathKey, defaultPath);
        storagePath = Environment.getExternalStorageDirectory() + "/" + storageFolder;
        String keyCipherFilenames = getString(R.string.pref_cipher_file_names_key);
        cipherFileNames = sharedPref.getBoolean(keyCipherFilenames, true);
    }

    private File[] getFilesFromStorage(File directory) throws Exception
    {
        if(!directory.exists())
        {
            boolean created = directory.mkdir();
            if(!created)
            {
                throw new Exception("Impossible to create directory");
            }
        }

        return directory.listFiles();
    }

    private void readStorage()
    {
        try
        {
            fileList = getFilesFromStorage(new File(storagePath));
        }
        catch (Exception ex)
        {
            showMessageImpossibleToCreateFolder();
            return;
        }

        linearLayout.removeAllViews();

        if (fileList != null && fileList.length != 0)
        {
            sortFilesByName();
            for (int i = 0; i < fileList.length; i++)
            {
                View element = generateView(fileList[i], decryptedFileNames[i]);
                linearLayout.addView(element);
            }
        }
    }

    private void processFileNames(File directory)
    {
        File[] files = directory.listFiles();
        if(cipherFileNames)
        {
            for (File file : files)
            {
                FileProcessingActivity.EncryptFileName(file, keyBytes, ivBytes);
            }
        }
        else
        {
            for (File file : files)
            {
                FileProcessingActivity.DecryptFileName(file, keyBytes, ivBytes);
            }
        }
    }

    View generateView(File fileInfo, String label)
    {
        final FileButton button = new FileButton(this, fileInfo);
        String name;
        name = label;

        button.setText(name);
        button.setOnClickListener(new ButtonListener());
        button.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                startActionMode(new FileItemCallback(button));
                return true;
            }
        });

        int width = linearLayout.getWidth();
        if(button.getWidth() > width)
            button.setWidth(width);

        return button;
    }

    void sortFilesByName()
    {
        // Using Shell sort
        String[] decryptedNames = new String[fileList.length];
        for(int i = 0; i < decryptedNames.length; i++)
            decryptedNames[i] = FileProcessingActivity.GetDecryptedName(fileList[i], keyBytes, ivBytes);

        class swapCallback implements ShellSort.SwapCallback
        {
            @Override
            public void onSwap(int a, int b)
            {
                File tmp = fileList[b];
                fileList[b] = fileList[a];
                fileList[a] = tmp;
            }
        }
        ShellSort.Comparator<String> comparator = new ShellSort.ComparatorCollection.StringComparator(true);
        ShellSort<String> sorter = new ShellSort<>(comparator);
        sorter.setSwapCallback(new swapCallback());
        sorter.sort(decryptedNames);

        decryptedFileNames = decryptedNames;
    }

    class ButtonListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            if(v == prefsButton)
            {
                invokePrefsActivity();
            }
            else if(v == addButton)
            {
                invokeFileChooserActivity();
            }
            else if(v == loginButton)
            {
                requestLoginData();
            }
            else
            {
                //int index = linearLayout.indexOfChild(v);
                invokeFileProcessor(((FileButton)v).getCorrespondingFile());
            }
        }
    }

    @SuppressLint("AppCompatCustomView")
    class FileButton extends Button
    {
        File correspondingFile;
        public FileButton(Context context, File file)
        {
            super(context);
            correspondingFile = file;
        }

        public File getCorrespondingFile()
        {
            return correspondingFile;
        }
    }

    void invokeFileChooserActivity()
    {
        FileChooserActivity.invokeFileChooser(this, OPEN_DOCUMENT_REQUEST_CODE);
    }

    void invokeFileProcessor(File info)
    {
        FileProcessingActivity.outputFile(this, info, keyBytes, ivBytes, cipherFileNames);
    }

    void deleteFile(File file)
    {
        FileProcessingActivity.deleteFile(this, file);
    }

    void invokeFileDecryptor(File file)
    {
        extracted = file;
        FileChooserActivity.invokeDirectoryChooser(this, OPEN_DIRECTORY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if(requestCode == LOGIN_REQUEST_CODE && resultCode == RESULT_OK)
        {
            keyBytes = data.getByteArrayExtra(LoginActivity.DATA_PASSWORD);
            ivBytes = data.getByteArrayExtra(LoginActivity.DATA_IV);
            loginButton.setVisibility(View.INVISIBLE);
            loginButton.setEnabled(false);
            addButton.setVisibility(View.VISIBLE);
            processFileNames(new File(storagePath));
            readStorage();
        }
        /*else if(resultCode == RESULT_CANCELED)
        {
            finish();
        }*/

        if(requestCode == OPEN_DOCUMENT_REQUEST_CODE)
        {
            Log.v(MenuActivity.class.getName(), "Document received!");
            if(resultCode == RESULT_OK)
            {
                String resultingPath = data.getStringExtra(FileChooserActivity.EXTRA_RESULT_PATH);
                File file = new File(resultingPath);
                invokeFileAdderActivity(file.getPath(),file.getName());
            }
        }

        if(requestCode == GET_PERMISSIONS_REQUEST_CODE)
        {
            if(!data.getBooleanExtra(PermissionGetter.DATA_ANSWER, false))
            {
                showMessagePermissionsNotGranted();
            }
            else
            {
                Log.v(MenuActivity.class.getName(), "Permission granted!");
            }
        }

        if(requestCode == OPEN_DIRECTORY_REQUEST_CODE)
        {
            if(resultCode == RESULT_OK)
            {
                String path = data.getStringExtra(FileChooserActivity.EXTRA_RESULT_PATH);

                File f = new File(path,extracted.getName());

                FileProcessingActivity.outputFilePermanent(this, extracted, f, DECRYPT_FILE_REQUEST_CODE, keyBytes, ivBytes, cipherFileNames);
            }
        }

        if(requestCode == PREFS_REQUEST_CODE)
        {
            getPreferences();
            processFileNames(new File(storagePath));
            Log.v("FILENAMES", "File names");
        }

    }

    private void invokeFileAdderActivity(String path, String destinationName)
    {
        File destination = new File(storagePath + "/" + destinationName);
        File sourceFile = new File(path);

        if(!destination.exists())
        {
            try
            {
                Log.w(MenuActivity.class.getName(), destination.getAbsolutePath());
                boolean created = destination.createNewFile();
                if(created)
                    FileProcessingActivity.inputFile(this, sourceFile, destination, keyBytes, ivBytes, cipherFileNames);
                else
                    showMessageFileExists(destination);
            }
            catch (IOException e)
            {
                showMessage(e);
            }
        }

    }

    void invokePrefsActivity()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, PREFS_REQUEST_CODE);
    }


    // Menu for file item

    class FileItemCallback implements ActionMode.Callback
    {
        private View sender;

        public FileItemCallback(View sender)
        {
            this.sender = sender;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_options_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            int index = linearLayout.indexOfChild(sender);
            if(index == -1)
            {
                return false;
            }
            File file = fileList[index];
            switch (item.getItemId())
            {
                case R.id.open_item:
                    invokeFileProcessor(file);
                    break;
                case R.id.decrypt_item:
                    invokeFileDecryptor(file);
                    break;
                case R.id.delete_item:
                    deleteFile(file);
                    break;
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode)
        {

        }
    }

    // Error messages

    private void sendError(Exception ex)
    {
        ErrorSenderActivity.sendErrorReport(this, ex, "");
    }


    private class onErrorFinishListener implements ErrorDialogBuilder.onFinishListener
    {
        @Override
        public void onFinish()
        {
            finish();
        }
    }

    private class onErrorNoActionListener implements ErrorDialogBuilder.onFinishListener
    {
        @Override
        public void onFinish()
        {

        }
    }

    private void showMessage(String message)
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, message, new onErrorNoActionListener());
        builder.showDialog();
    }

    private void showMessage(Exception exception)
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, exception.getMessage(), new onErrorNoActionListener());
        builder.showDialog();
    }

    private void showMessageFileExists(File file)
    {
        String template = getString(R.string.message_file_exists);
        String msg = String.format(template, file.getName());
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, msg, new onErrorNoActionListener());
        builder.showDialog();
    }

    private void showMessagePermissionsNotGranted()
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, R.string.message_permissions_not_granted, new onErrorFinishListener());
        builder.showDialog();
    }

    private void showMessageImpossibleToCreateFolder()
    {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this, "Impossible to create directory", new onErrorFinishListener());
        builder.showDialog();
    }

}
