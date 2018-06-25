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
import java.util.Comparator;

public class FileChooserActivity extends Activity
{
    public static final String ACTION_PICK_FILE = "com.dmitrymon.ACTION_PICK_FILE";
    public static final String ACTION_PICK_DIR = "com.dmitrymon.ACTION_PICK_DIR";
    public static final String EXTRA_INITIAL_PATH = "com.dmitrymon.EXTRA_INITIAL_PATH";
    public static final String EXTRA_RESULT_PATH = "com.dmitrymon.EXTRA_RESULT_PATH";

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
        if(intent.getAction().equals(ACTION_PICK_FILE))
        {
            type = Type.FILE;
        }
        if(intent.getAction().equals(ACTION_PICK_DIR))
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

    public static void invokeFileChooser(Activity invoker, File initialDir, int requestCode)
    {
        Intent intent = new Intent(invoker, FileChooserActivity.class);
        intent.setAction(ACTION_PICK_FILE);
        intent.putExtra(EXTRA_INITIAL_PATH, initialDir.getPath());
        invoker.startActivityForResult(intent, requestCode);
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


            // TODO Sorting
            /*ArrayList<File> list = new ArrayList<File>();
            for(File f: listOfFiles)
                list.add(f);*/

            //list.sort(new FileComparator(FileComparator.Mode.NAME, FileComparator.Direction.FROM_SMALLER_TO_BIGGER));

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
        String[] names = new String[listOfFiles.length];
        for(int i = 0; i < names.length; i++)
            names[i] = listOfFiles[i].getName();

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

        fileNames = names;
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
            select.setText("Select this directory");
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

    View createView(File file)
    {
        Button button = new Button(this);
        if(file.isFile())
        {
            button.setText(file.getName());
        }
        if(file.isDirectory())
        {
            button.setText(file.getName() + File.separator);
            button.setTextColor(Color.BLUE);
        }
        button.setOnClickListener(new ButtonListener());
        return button;
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
            button.setText(label + File.separator);
            button.setTextColor(Color.BLUE);
        }
        button.setOnClickListener(new ButtonListener());
        return button;
    }

    void sendResult(File file)
    {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_PATH, file.getPath());
        setResult(RESULT_OK, intent);
        finish();
    }

    class ButtonListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            int index = listContainer.indexOfChild(v);
            index -= offset;
            File file = listOfFiles[index];
            listFiles(file);
        }
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

    static class FileComparator implements Comparator
    {
        enum Mode
        {
            NAME, DATE, SIZE
        }

        enum Direction
        {
            FROM_SMALLER_TO_BIGGER, FROM_BIGGER_TO_SMALLER
        }

        Mode mode;
        Direction direction;

        FileComparator(Mode mode, Direction direction)
        {
            this.mode = mode;
            this.direction = direction;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            File f1 = (File)o1;
            File f2 = (File)o2;

            switch (mode)
            {
                case NAME:
                    if(direction == Direction.FROM_SMALLER_TO_BIGGER)
                        return compareStrings(f1.getName(), f2.getName());
                    else
                        return -compareStrings(f1.getName(), f2.getName());
                case DATE:
                    throw new IllegalArgumentException();

                case SIZE:
                    throw new IllegalArgumentException();
            }

            return 0;
        }

        private int compareStrings(String s1, String s2)
        {
            return s1.compareTo(s2);
        }
    }
}
