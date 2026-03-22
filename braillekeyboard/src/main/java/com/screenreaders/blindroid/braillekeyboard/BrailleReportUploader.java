package com.screenreaders.blindroid.braillekeyboard;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public final class BrailleReportUploader {
    private static final String REPORT_ENDPOINT = "https://report.asteja.eu/";
    private static final String REPORT_HEADER = "X-Report-Token";
    private static final String REPORT_CLIENT_HEADER = "X-Client-Token";
    private static final String REPORT_TOKEN = "47dc28661ef7d1ac07400827508b6aa9";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 7000;
    private static final String DIR_NAME = "crash_reports";
    private static final String FILE_PREFIX = "braille_diag_";
    private static final int MAX_REPORTS = 10;

    public interface Callback {
        void onDone(boolean success);
    }

    private BrailleReportUploader() {
    }

    public static void uploadAsync(Context context, String report, Callback callback) {
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            boolean ok = upload(appContext, report);
            if (callback != null) {
                callback.onDone(ok);
            }
        }).start();
    }

    public static File saveReport(Context context, String report) {
        if (report == null) {
            return null;
        }
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File file = new File(dir, FILE_PREFIX + timestamp + ".txt");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(report.getBytes("UTF-8"));
        } catch (Exception e) {
            return null;
        }
        trimOldReports(dir);
        return file;
    }

    public static boolean upload(Context context, String report) {
        if (report == null) {
            return false;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(REPORT_ENDPOINT).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            conn.setRequestProperty(REPORT_HEADER, REPORT_TOKEN);
            conn.setRequestProperty(REPORT_CLIENT_HEADER, getClientId(context));
            byte[] bytes = report.getBytes("UTF-8");
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }
            int code = conn.getResponseCode();
            return code >= 200 && code <= 299;
        } catch (Exception e) {
            return false;
        }
    }

    private static void trimOldReports(File dir) {
        File[] files = dir.listFiles((file, name) -> name.startsWith(FILE_PREFIX));
        if (files == null || files.length <= MAX_REPORTS) {
            return;
        }
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (int i = MAX_REPORTS; i < files.length; i++) {
            files[i].delete();
        }
    }

    private static String getClientId(Context context) {
        String existing = Options.getStringPreference(
                context,
                R.string.pref_braille_report_client_id_key,
                null);
        if (existing != null && existing.length() > 0) {
            return existing;
        }
        String id = UUID.randomUUID().toString();
        Options.writeStringPreference(context, R.string.pref_braille_report_client_id_key, id);
        return id;
    }
}
