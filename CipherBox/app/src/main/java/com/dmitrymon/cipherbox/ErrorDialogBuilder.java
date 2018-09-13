package com.dmitrymon.cipherbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class ErrorDialogBuilder
{
    private Activity activity;
    private String errorMessage;

    private AlertDialog dialog;
    private onFinishListener listener;


    private void buildDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.message_error_title);
        builder.setMessage(errorMessage);

        class DialogButtonListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                listener.onFinish();
            }

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                listener.onFinish();
            }
        }
        builder.setPositiveButton("OK", new DialogButtonListener());

        dialog = builder.create();
    }

    public void showDialog()
    {
        dialog.show();
    }

    public AlertDialog getDialog()
    {
        return dialog;
    }

    public ErrorDialogBuilder(Activity activity, String message, onFinishListener listener)
    {
        this.activity = activity;
        this.listener = listener;
        errorMessage = message;
        buildDialog();
    }

    public ErrorDialogBuilder(Activity activity, int messageResId, onFinishListener listener)
    {
        this.activity = activity;
        this.listener = listener;
        errorMessage = activity.getString(messageResId);
        buildDialog();
    }

    public interface onFinishListener
    {
        void onFinish();
    }
}
