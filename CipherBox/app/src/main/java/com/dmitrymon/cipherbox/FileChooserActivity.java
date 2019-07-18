package com.dmitrymon.cipherbox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.io.File;
import java.util.ArrayList;

public class FileChooserActivity extends Activity
{
    public static final String ACTION_PICK_FILE = BuildConfig.APPLICATION_ID + ".ACTION_PICK_FILE";
    public static final String ACTION_PICK_DIR = BuildConfig.APPLICATION_ID + ".ACTION_PICK_DIR";
    public static final String EXTRA_INITIAL_PATH = BuildConfig.APPLICATION_ID + ".EXTRA_INITIAL_PATH";
    public static final String EXTRA_RESULT_PATH = BuildConfig.APPLICATION_ID + ".EXTRA_RESULT_PATH";

    LinearLayout listContainer;

    File[] listOfFiles;
    String[] fileNames;

    File currentDirectory;

    int offset = 1;
    Type type;

    enum Type
    {
        FILE, DIRECTORY
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser_activity);

        setupLayout();
        processIntent(getIntent());
    }

    void processIntent(Intent intent)
    {
        String action = intent.getAction();
        if(action == null)
            return;

        if(action.equals(ACTION_PICK_FILE))
        {
            type = Type.FILE;
        }
        if(action.equals(ACTION_PICK_DIR))
        {
            type = Type.DIRECTORY;
        }

        String initialDirPath = intent.getStringExtra(EXTRA_INITIAL_PATH);
        if(initialDirPath != null)
        {
            File initialDir = new File(initialDirPath);
            listFiles(initialDir);
        }
        else
        {
            listFiles(Environment.getExternalStorageDirectory());
        }
    }

    public static void invokeFileChooser(Activity invoker, int requestCode)
    {
        Intent intent = new Intent(invoker, FileChooserActivity.class);
        intent.setAction(ACTION_PICK_FILE);
        invoker.startActivityForResult(intent, requestCode);
    }

    public static void invokeDirectoryChooser(Activity invoker, int requestCode)
    {
        Intent intent = new Intent(invoker, FileChooserActivity.class);
        intent.setAction(ACTION_PICK_DIR);
        invoker.startActivityForResult(intent, requestCode);
    }

    void setupLayout()
    {
        ScrollView scrollView = findViewById(R.id.listContainer);
        listContainer = (LinearLayout)scrollView.getChildAt(0);
    }

    void listFiles(File directory)
    {
        if(type == Type.FILE && directory.isFile())
        {
            sendResult(directory);
        }
        else if(directory.isDirectory())
        {
            clearList();
            addDefaultViews();
            currentDirectory = directory;
            listOfFiles = directory.listFiles();

            if(listOfFiles != null)
            {
                sortFilesByName();
                for (int i = 0; i < listOfFiles.length; i++)
                {
                    View v = createFileButton(listOfFiles[i], fileNames[i]);
                    addViewToList(v);
                }
            }
        }
    }

    void sortFilesByName()
    {
        // Using Shell sort
        //String[] names = new String[listOfFiles.length];
        ArrayList<String> names = new ArrayList<>();
        for(int i = 0; i < listOfFiles.length; i++)
            names.add(listOfFiles[i].getName());
            //names[i] = listOfFiles[i].getName();

        class swapCallback implements ShellSort.SwapCallback
        {
            @Override
            public void onSwap(int a, int b)
            {
                File tmp = listOfFiles[b];
                listOfFiles[b] = listOfFiles[a];
                listOfFiles[a] = tmp;
            }
        }
        ShellSort.Comparator<String> comparator = new ShellSort.ComparatorCollection.StringComparator(false);
        ShellSort<String> sorter = new ShellSort<>(comparator);
        sorter.setSwapCallback(new swapCallback());
        sorter.sort(names);

        fileNames = new String[names.size()];
        names.toArray(fileNames);
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

    void goUp()
    {
        if(currentDirectory != null && currentDirectory.isDirectory())
        {
            String parentPath = currentDirectory.getParent();
            if(parentPath != null)
            {
                listFiles(new File(parentPath));
            }
        }
    }

    void clearList()
    {
        listContainer.removeAllViews();
    }

    void addViewToList(View view)
    {
        listContainer.addView(view);
    }

    void addDefaultViews()
    {
        offset = 0;
        if(type == Type.DIRECTORY)
        {
            offset++;
            Button select = new Button(this);
            select.setText(R.string.file_chooser_select_this);
            select.setBackgroundColor(Color.BLUE);
            select.setTextColor(Color.GREEN);
            select.setOnClickListener(new SelectButtonListener());
            addViewToList(select);
        }

        Button upButton = new Button(this);
        upButton.setText("/..");
        upButton.setTextColor(Color.GREEN);
        upButton.setOnClickListener(new UpButtonListener());
        addViewToList(upButton);
        offset++;
    }

    View createFileButton(File file, String label)
    {
        FileButton button = new FileButton(this, file);
        if(file.isFile())
        {
            button.setText(label);
        }
        if(file.isDirectory())
        {
            String text = label + File.pathSeparator;
            button.setText(text);
            button.setTextColor(Color.BLUE);
        }
        button.setOnClickListener(new FileButtonListener());
        return button;
    }

    void sendResult(File file)
    {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_PATH, file.getPath());
        setResult(RESULT_OK, intent);
        finish();
    }


    class FileButtonListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            FileButton b = (FileButton)v;
            listFiles(b.getCorrespondingFile());
        }
    }

    class UpButtonListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            goUp();
        }
    }

    class SelectButtonListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            sendResult(currentDirectory);
        }
    }
}
