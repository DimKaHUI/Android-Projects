package com.dmitrymon.cipherbox;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageReader
{

    String path;

    public StorageReader(String storagePath)
    {
        path = storagePath;
    }

    public File[] ReadStorage() throws Exception
    {
        // TODO Reading storage
        File storageFolder = new File(path);
        Log.v(StorageReader.class.getName(), path);
        if(!storageFolder.exists())
        {
            boolean created = storageFolder.mkdir(); // TODO Error handling
            if(!created)
            {
                throw new Exception("Impossible to create directory");
            }
        }

        File[] files = storageFolder.listFiles();
        return files;
    }
}
