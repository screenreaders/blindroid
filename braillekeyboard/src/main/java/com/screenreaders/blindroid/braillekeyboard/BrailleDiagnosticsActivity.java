package com.screenreaders.blindroid.braillekeyboard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class BrailleDiagnosticsActivity extends Activity {
    private TextView resultView;
    private Button runButton;
    private Button exportButton;
    private Button sendButton;
    private volatile boolean autoSendInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.braille_diagnostics_title);
        setContentView(R.layout.activity_braille_diagnostics);
        resultView = findViewById(R.id.brailleDiagnosticsResult);
        runButton = findViewById(R.id.brailleDiagnosticsRun);
        exportButton = findViewById(R.id.brailleDiagnosticsExport);
        sendButton = findViewById(R.id.brailleDiagnosticsSend);

        runButton.setOnClickListener(v -> runDiagnostics());
        exportButton.setOnClickListener(v -> exportReport());
        sendButton.setOnClickListener(v -> sendReport());
    }

    private void runDiagnostics() {
        runButton.setEnabled(false);
        exportButton.setEnabled(false);
        sendButton.setEnabled(false);
        resultView.setText(getString(R.string.braille_diagnostics_running));
        new Thread(() -> {
            List<BrailleDiagnostics.CheckResult> results = BrailleDiagnostics.run(getApplicationContext());
            final String text = BrailleDiagnostics.format(getApplicationContext(), results);
            final String report = BrailleDiagnostics.buildReport(getApplicationContext());
            BrailleReportUploader.saveReport(getApplicationContext(), report);
            autoSendReport(report);
            runOnUiThread(() -> {
                resultView.setText(text);
                runButton.setEnabled(true);
                exportButton.setEnabled(true);
                sendButton.setEnabled(true);
            });
        }).start();
    }

    private void exportReport() {
        runButton.setEnabled(false);
        exportButton.setEnabled(false);
        sendButton.setEnabled(false);
        resultView.setText(getString(R.string.braille_diagnostics_exporting));
        new Thread(() -> {
            String report = BrailleDiagnostics.buildReport(getApplicationContext());
            BrailleReportUploader.saveReport(getApplicationContext(), report);
            autoSendReport(report);
            File reportFile = writeReport(report);
            runOnUiThread(() -> {
                runButton.setEnabled(true);
                exportButton.setEnabled(true);
                sendButton.setEnabled(true);
                if (reportFile == null) {
                    Toast.makeText(this, R.string.braille_diagnostics_export_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                Uri uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".braillekeyboard.files",
                        reportFile);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, report);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.braille_diagnostics_share)));
            });
        }).start();
    }

    private void sendReport() {
        runButton.setEnabled(false);
        exportButton.setEnabled(false);
        sendButton.setEnabled(false);
        resultView.setText(getString(R.string.braille_diagnostics_sending));
        final String report = BrailleDiagnostics.buildReport(getApplicationContext());
        BrailleReportUploader.saveReport(getApplicationContext(), report);
        BrailleReportUploader.uploadAsync(getApplicationContext(), report, success -> runOnUiThread(() -> {
            runButton.setEnabled(true);
            exportButton.setEnabled(true);
            sendButton.setEnabled(true);
            int message = success ? R.string.braille_diagnostics_send_ok : R.string.braille_diagnostics_send_fail;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }));
    }

    private void autoSendReport(String report) {
        if (autoSendInProgress) return;
        autoSendInProgress = true;
        BrailleReportUploader.uploadAsync(getApplicationContext(), report, success -> runOnUiThread(() -> {
            autoSendInProgress = false;
            int message = success ? R.string.braille_diagnostics_send_ok : R.string.braille_diagnostics_send_fail;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }));
    }

    private File writeReport(String report) {
        File dir = new File(getCacheDir(), "braille-reports");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File out = new File(dir, "braille-report-" + System.currentTimeMillis() + ".txt");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            stream.write(report.getBytes("UTF-8"));
            stream.flush();
            return out;
        } catch (IOException e) {
            return null;
        }
    }
}
