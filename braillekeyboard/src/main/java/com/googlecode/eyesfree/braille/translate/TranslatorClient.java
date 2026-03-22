package com.googlecode.eyesfree.braille.translate;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TranslatorClient {
    public static final int SUCCESS = 0;
    public static final int ERROR = 1;

    public interface OnInitListener {
        void onInit(int status);
    }

    protected Context mContext;
    protected OnInitListener mOnInitListener;

    private final List<TableInfo> tables = new ArrayList<TableInfo>();
    private final BrailleTranslator translator = new SimpleBrailleTranslator();

    public TranslatorClient() {
    }

    protected void initTables(List<TableInfo> tableInfos) {
        tables.clear();
        if (tableInfos != null) {
            tables.addAll(tableInfos);
        }
    }

    public List<TableInfo> getTables() {
        return new ArrayList<TableInfo>(tables);
    }

    public BrailleTranslator getTranslator(String id) {
        return translator;
    }

    public void destroy() {
        // no-op
    }

    protected void notifyInit(int status) {
        if (mOnInitListener != null) {
            mOnInitListener.onInit(status);
        }
    }

    protected static Locale parseLocale(String tableId) {
        if (tableId == null || tableId.isEmpty()) {
            return Locale.getDefault();
        }
        String[] parts = tableId.split("-");
        if (parts.length >= 2) {
            String language = parts[0];
            String region = parts[1];
            if (region.length() == 2 || region.length() == 3) {
                return new Locale(language, region);
            }
            return new Locale(language);
        }
        return new Locale(parts[0]);
    }

    protected static boolean isEightDot(String tableId) {
        if (tableId == null) {
            return false;
        }
        return tableId.contains("comp8") || tableId.contains("comp");
    }

    protected static int parseGrade(String tableId) {
        if (tableId == null) {
            return 0;
        }
        if (tableId.contains("g2")) {
            return 2;
        }
        if (tableId.contains("g1")) {
            return 1;
        }
        return 0;
    }
}
