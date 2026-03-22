package com.screenreaders.blindroid.braillekeyboard;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class BrailleDiagnostics {
    public static final class CheckResult {
        public final String label;
        public final boolean ok;
        public final String detail;

        public CheckResult(String label, boolean ok, String detail) {
            this.label = label;
            this.ok = ok;
            this.detail = detail;
        }
    }

    private BrailleDiagnostics() {
    }

    public static List<CheckResult> run(Context context) {
        List<CheckResult> results = new ArrayList<CheckResult>();
        long initStart = System.nanoTime();
        LiblouisBridge.init(context);
        long initMs = (System.nanoTime() - initStart) / 1_000_000L;

        boolean init = LiblouisBridge.isInitialized();
        results.add(new CheckResult("Liblouis init", init, init ? "initialized" : "not initialized"));
        results.add(new CheckResult("Liblouis init time", true, initMs + " ms"));

        File tablesDir = LiblouisBridge.getTablesDir();
        boolean dirOk = tablesDir != null && tablesDir.exists() && tablesDir.isDirectory();
        results.add(new CheckResult("Tables dir", dirOk, dirOk ? tablesDir.getAbsolutePath() : "missing"));

        String tableFile = LiblouisTableRegistry.resolveTableFile("en-UEB-g1");
        boolean tableKnown = tableFile != null && tableFile.length() > 0;
        results.add(new CheckResult("Table mapping", tableKnown, tableKnown ? tableFile : "not found"));

        boolean tableExists = false;
        if (tableKnown && tablesDir != null) {
            File tablePath = new File(tablesDir, tableFile);
            tableExists = tablePath.exists();
        }
        results.add(new CheckResult("Table file", tableExists, tableExists ? "found" : "missing"));

        long translateStart = System.nanoTime();
        String sample = LiblouisBridge.backTranslate("en-UEB-g1", new byte[] { 0x01 });
        long translateMs = (System.nanoTime() - translateStart) / 1_000_000L;
        boolean translated = sample != null && sample.trim().length() > 0;
        results.add(new CheckResult("Sample translate", translated, translated ? sample : "no result"));
        results.add(new CheckResult("Sample translate time", true, translateMs + " ms"));

        return results;
    }

    public static String format(Context context, List<CheckResult> results) {
        StringBuilder out = new StringBuilder();
        String okLabel = context.getString(R.string.braille_diagnostics_ok);
        String failLabel = context.getString(R.string.braille_diagnostics_fail);
        for (int i = 0; i < results.size(); i++) {
            CheckResult result = results.get(i);
            out.append(result.ok ? okLabel : failLabel);
            out.append(" - ");
            out.append(result.label);
            if (result.detail != null && result.detail.length() > 0) {
                out.append(": ");
                out.append(result.detail);
            }
            if (i < results.size() - 1) {
                out.append("\n");
            }
        }
        return out.toString();
    }

    public static String buildReport(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("Blindroid Braille Diagnostics\n");
        report.append("Generated: ");
        report.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        report.append("\n\n");

        report.append("App: ");
        report.append(getVersionLine(context));
        report.append("\n");
        report.append("Package: ");
        report.append(context.getPackageName());
        report.append("\n");
        report.append("Device: ");
        report.append(Build.MANUFACTURER);
        report.append(" ");
        report.append(Build.MODEL);
        report.append(" (");
        report.append(Build.DEVICE);
        report.append(")\n");
        report.append("Android: ");
        report.append(Build.VERSION.RELEASE);
        report.append(" (SDK ");
        report.append(Build.VERSION.SDK_INT);
        report.append(")\n");
        report.append("Locale: ");
        report.append(Locale.getDefault().toString());
        report.append("\n");
        report.append("Memory max: ");
        report.append(Runtime.getRuntime().maxMemory() / (1024 * 1024));
        report.append(" MB\n");
        report.append("Record audio permission: ");
        boolean recordGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        report.append(recordGranted);
        report.append("\n\n");

        String imeId = new ComponentName(context, BrailleIME.class).flattenToShortString();
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_INPUT_METHODS);
        String defaultIme = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean imeEnabled = enabled != null && enabled.contains(imeId);
        boolean imeDefault = defaultIme != null && defaultIme.equals(imeId);
        report.append("IME enabled: ");
        report.append(imeEnabled);
        report.append("\n");
        report.append("IME default: ");
        report.append(imeDefault);
        report.append("\n\n");

        List<CheckResult> results = run(context);
        report.append("Checks:\n");
        report.append(format(context, results));
        report.append("\n");

        return report.toString();
    }

    private static String getVersionLine(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            String versionName = info.versionName == null ? "?" : info.versionName;
            long versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = info.getLongVersionCode();
            } else {
                versionCode = info.versionCode;
            }
            return versionName + " (" + versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }
}
