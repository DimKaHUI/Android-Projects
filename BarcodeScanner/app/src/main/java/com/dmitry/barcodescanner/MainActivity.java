package com.dmitry.barcodescanner;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.IOException;

public class MainActivity extends Activity
{
    Camera camera;
    SurfaceView sView;
    TextView errorLabel;
    TextView resultLabel;
    final int CAMERA_REQUEST = 111;

    boolean permissionsGranted;
    boolean cameraInstantiated = false;

    int width, height;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupLocalValues();

        getPermissions();

        new Thread(new CameraInitializer()).start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        releaseCamera();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(cameraInstantiated)
            setupCamera();
    }

    private void releaseCamera()
    {
        if(camera != null)
        {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = true;
                }
                else
                {
                    permissionsGranted = false;
                }
                return;
            }
        }
    }

    private boolean getPermissions()
    {
        int flag = checkSelfPermission(Manifest.permission.CAMERA);
        if(flag == PackageManager.PERMISSION_DENIED)
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            errorLabel.setText(getResources().getString(R.string.camera_permission_denied_msg));
            errorLabel.setVisibility(View.VISIBLE);
            return false;
        }

        if(flag == PackageManager.PERMISSION_GRANTED)
        {
            errorLabel.setVisibility(View.INVISIBLE);
            permissionsGranted = true;
            Log.i("Permission check","Camera permission is granted");
            return true;
        }

        return false;
    }

    private void setupLocalValues()
    {
        sView = findViewById(R.id.surfaceView);
        errorLabel = findViewById(R.id.errorLabel);
        resultLabel = findViewById(R.id.resultView);
        Button exitButton = findViewById(R.id.exitButton);
        width = sView.getWidth();
        height = sView.getHeight();
        exitButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finishAndRemoveTask();
            }
        });
    }

    private int findFrontFacingCamera()
    {
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        int cameraId = 0;
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private void setupCamera()
    {
        if(camera == null)
        {
            try
            {
                SurfaceHolder holder = sView.getHolder();

                int camIndex = findFrontFacingCamera();
                camera = Camera.open(camIndex);
                if(camera == null)
                {
                    throw new NullPointerException("camera");
                }

                camera.setPreviewCallback(new PreviewCallback());

                camera.setDisplayOrientation(90);

                /*Camera.Parameters params = camera.getParameters();
                params.setPictureFormat(ImageFormat.);
                camera.setParameters(params);*/

                camera.setPreviewDisplay(holder);
                camera.startPreview();
                SetErrorLabel(getResources().getString(R.string.camera_io_error_msg), View.INVISIBLE);
            }
            catch (IOException ex)
            {
                SetErrorLabel(getResources().getString(R.string.camera_io_error_msg), View.VISIBLE);
            }
            catch (NullPointerException ex)
            {
                SetErrorLabel(getResources().getString(R.string.camera_io_error_msg), View.VISIBLE);
            }
            catch (RuntimeException ex)
            {
                SetErrorLabel(ex.getMessage(), View.VISIBLE);
            }
        }
        else
        {
            Log.w("warning","Camera was not null! Aborting setting camera up.");
        }
    }

    private void SetErrorLabel(final String text, final int visibility)
    {
        errorLabel.post(new Runnable()
        {
            @Override
            public void run()
            {
                errorLabel.setText(text);
                errorLabel.setVisibility(visibility);
            }
        });
    }

    class CameraInitializer implements Runnable
    {

        @Override
        public void run()
        {
            try
            {
                while(!permissionsGranted || sView.getHolder() == null)
                {
                    Thread.sleep(100);
                }
                Thread.sleep(1000);

            }
            catch (InterruptedException ex)
            {

            }

            setupCamera();
            cameraInstantiated = true;
        }
    }

    class PreviewCallback implements Camera.PreviewCallback
    {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera)
        {
            try
            {
                Image img = new Image(camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, "Y800");
                img.setData(data);
                ImageScanner scanner = new ImageScanner();
                scanner.scanImage(img);
                SymbolSet set = scanner.getResults();
                String str = "";
                for(Symbol symbol: set)
                {
                    str += symbol.getData() + " ";
                }
                resultLabel.setText(str);
            }
            catch (UnsupportedOperationException ex)
            {
                resultLabel.setText(ex.getMessage());
            }
        }
    }

}
