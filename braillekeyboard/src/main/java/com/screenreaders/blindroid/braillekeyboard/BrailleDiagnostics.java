package com.screenreaders.blindroid.braillekeyboard;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        LiblouisBridge.init(context);

        boolean init = LiblouisBridge.isInitialized();
        results.add(new CheckResult("Liblouis init", init, init ? "initialized" : "not initialized"));

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

        String sample = LiblouisBridge.backTranslate("en-UEB-g1", new byte[] { 0x01 });
        boolean translated = sample != null && sample.trim().length() > 0;
        results.add(new CheckResult("Sample translate", translated, translated ? sample : "no result"));

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
}
