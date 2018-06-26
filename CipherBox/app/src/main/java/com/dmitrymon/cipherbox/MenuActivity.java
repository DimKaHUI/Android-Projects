package com.dmitrymon.cipherbox;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import javax.crypto.Cipher;

import static com.dmitrymon.cipherbox.FileProcessingActivity.DATA_NAME_STRING;

public class MenuActivity extends Activity
{

    LinearLayout linearLayout;
    Button prefsButton;
    Button addButton;
    TextView textView;
    File[] fileInfos;
    String[] decryptedFileNames;
    File extracted;

    private static final int OPEN_DOCUMENT_REQUEST_CODE = 125;
    private static final int GET_PERMISSIONS_REQUEST_CODE = 126;
    private static final int PROCESS_FILE_REQUEST_CODE = 127;
    private static final int DECRYPT_FILE_REQUEST_CODE = 128;
    private static final int OPEN_DIRECTORY_REQUEST_CODE = 129;
    private static final int PREFS_REQUEST_CODE = 130;

    // Data for encryption/decryption
    private byte[] keyBytes;
    private byte[] ivBytes;
    String storagePath;
    boolean shouldKill = false;
    boolean cipherFileNames = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        boolean granted = processPermission();

        setupLayout();
        getPreferences();
        processIntent(getIntent());

        if(granted)
        {
            processFileNames(new File(storagePath));
            readStorage();
        }
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        shouldKill = true;
        startActivity(intent);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getPreferences();
        readStorage();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(shouldKill)
        {
            Log.v("MenuActivity","Terminating current activity");
            finish();
        }
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
        prefsButton = findViewById(R.id.prefsButton);
        prefsButton.setOnClickListener(new ButtonListener());
        linearLayout = findViewById(R.id.listOfFiles);
        addButton = findViewById(R.id.addToStorageButton);
        addButton.setOnClickListener(new ButtonListener());
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
                // TODO No such action. Aborting
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
        cipherFileNames = sharedPref.getBoolean(keyCipherFilenames, cipherFileNames);
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
            fileInfos = getFilesFromStorage(new File(storagePath));
        }
        catch (Exception ex)
        {
            showMessageImpossibleToCreateFolder();
            finish();
            return;
        }

        linearLayout.removeAllViews();

        if(fileInfos == null || fileInfos.length == 0)
        {
            ShowMessageStorageIsEmpty();
        }
        else
        {
            sortFilesByName();
            for (int i = 0; i < fileInfos.length; i++)
            {
                View element = generateView(fileInfos[i], decryptedFileNames[i]);
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


    void showMessageImpossibleToCreateFolder()
    {
        Toast.makeText(this,"Impossible to create directory", Toast.LENGTH_LONG).show();
    }

    View generateView(File fileInfo)
    {
        final FileButton button = new FileButton(this, fileInfo);
        String name;
        name = FileProcessingActivity.GetDecryptedName(fileInfo, keyBytes, ivBytes);

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
        return button;
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
        return button;
    }

    void sortFilesByName()
    {
        // Using Shell sort
        String[] decryptedNames = new String[fileInfos.length];
        for(int i = 0; i < decryptedNames.length; i++)
            decryptedNames[i] = FileProcessingActivity.GetDecryptedName(fileInfos[i], keyBytes, ivBytes);

        class swapCallback implements ShellSort.SwapCallback
        {
            @Override
            public void onSwap(int a, int b)
            {
                File tmp = fileInfos[b];
                fileInfos[b] = fileInfos[a];
                fileInfos[a] = tmp;
            }
        }
        ShellSort.Comparator<String> comparator = new ShellSort.ComparatorCollection.StringComparator(true);
        ShellSort<String> sorter = new ShellSort<>(comparator);
        sorter.setSwapCallback(new swapCallback());
        sorter.sort(decryptedNames);

        decryptedFileNames = decryptedNames;
    }

    void ShowMessageStorageIsEmpty()
    {
        /*Toast toast = Toast.makeText(this, "Storage is empty!", Toast.LENGTH_SHORT);
        toast.show();*/
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
            else
            {
                //int index = linearLayout.indexOfChild(v);
                showFileMenu(((FileButton)v).getCorrespondingFile());
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

    void showFileMenu(int index)
    {
        File fileInfo = fileInfos[index];
        invokeFileProcessor(fileInfo);
    }

    void showFileMenu(File file)
    {
        invokeFileProcessor(file);
    }

    void invokeFileChooserActivity()
    {
        FileChooserActivity.invokeFileChooser(this, OPEN_DOCUMENT_REQUEST_CODE);
    }

    void showMessagePermissionNotGranted()
    {
        Toast toast = Toast.makeText(this, "Impossible to proceed. Some permissions were not granted", Toast.LENGTH_LONG);
        toast.show();
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
        // TODO Decrypting
        extracted = file;
        FileChooserActivity.invokeDirectoryChooser(this, OPEN_DIRECTORY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
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
                permissionNotGranted();
            }
            else
            {
                Log.v(MenuActivity.class.getName(), "Permission granted!");
                readStorage();
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
        // TODO File name chooser
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
                    showMessageFileExists();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

    }

    void showMessageFileExists()
    {
        // TODO Message file exists

    }

    private void permissionNotGranted()
    {
        // TODO Permission not granted. Aborting
        Log.v(MenuActivity.class.getName(), "Permission not granted!");
        showMessagePermissionNotGranted();
        finish();
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
            File file = fileInfos[index];
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

    void sendError(Exception ex)
    {
        ErrorSenderActivity.sendErrorReport(this, ex, "");
    }
}
