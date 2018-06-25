package com.dmitrymon.cipherbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorSenderActivity extends Activity
{
    public static final String ACTION_SEND_REPORT = "com.dmitrymon.ACTION_SEND_REPORT";
    public static final String EXTRA_REPORT_TEXT = "com.dmitrymon.EXTRA_REPORT_TEXT";


    TextView textView;

    public static void sendErrorReport(Activity invoker, Exception ex, String extra)
    {
        Intent intent = new Intent(invoker, ErrorSenderActivity.class);
        intent.setAction(ACTION_SEND_REPORT);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string

        intent.putExtra(EXTRA_REPORT_TEXT, extra + "\nTRACE:\n" + sStackTrace);
        invoker.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_sender);

        setupLayout();
        processIntent(getIntent());
    }

    private void processIntent(Intent intent)
    {
        String text = intent.getStringExtra(EXTRA_REPORT_TEXT);
        textView.setText(text);
    }

    private void setupLayout()
    {
        textView = findViewById(R.id.errorView);
    }
}
