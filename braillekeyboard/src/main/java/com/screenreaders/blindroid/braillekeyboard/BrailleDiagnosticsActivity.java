package com.screenreaders.blindroid.braillekeyboard;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class BrailleDiagnosticsActivity extends Activity {
    private TextView resultView;
    private Button runButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.braille_diagnostics_title);
        setContentView(R.layout.activity_braille_diagnostics);
        resultView = findViewById(R.id.brailleDiagnosticsResult);
        runButton = findViewById(R.id.brailleDiagnosticsRun);

        runButton.setOnClickListener(v -> runDiagnostics());
    }

    private void runDiagnostics() {
        runButton.setEnabled(false);
        resultView.setText(getString(R.string.braille_diagnostics_running));
        new Thread(() -> {
            List<BrailleDiagnostics.CheckResult> results = BrailleDiagnostics.run(getApplicationContext());
            final String text = BrailleDiagnostics.format(getApplicationContext(), results);
            runOnUiThread(() -> {
                resultView.setText(text);
                runButton.setEnabled(true);
            });
        }).start();
    }
}
