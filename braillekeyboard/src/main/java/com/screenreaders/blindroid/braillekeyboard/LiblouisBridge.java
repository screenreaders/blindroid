package com.screenreaders.blindroid.braillekeyboard;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class LiblouisBridge {
    private static final String TABLES_ASSET_DIR = "liblouis/tables";
    private static final String VERSION_ASSET = "liblouis/tables_version.txt";
    private static final String VERSION_FILE = "tables_version.txt";

    private static final Object LOCK = new Object();
    private static volatile boolean initialized = false;
    private static volatile File tablesDir = null;

    private LiblouisBridge() {
    }

    public static void init(Context context) {
        if (initialized) {
            return;
        }
        synchronized (LOCK) {
            if (initialized) {
                return;
            }
            copyTablesIfNeeded(context);
            tablesDir = new File(context.getFilesDir(), "liblouis/tables");
            LiblouisNative.setDataPath(tablesDir.getAbsolutePath());
            initialized = true;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static File getTablesDir() {
        return tablesDir;
    }

    public static String backTranslate(String tableId, byte[] cells) {
        if (!initialized) {
            return null;
        }
        String tableFile = LiblouisTableRegistry.resolveTableFile(tableId);
        if (tableFile == null) {
            return null;
        }
        File localTables = tablesDir;
        if (localTables == null) {
            return null;
        }
        File tablePath = new File(localTables, tableFile);
        if (!tablePath.exists()) {
            return null;
        }
        return LiblouisNative.backTranslate(tableFile, cells);
    }

    private static void copyTablesIfNeeded(Context context) {
        File rootDir = new File(context.getFilesDir(), "liblouis");
        File tablesDir = new File(rootDir, "tables");
        String assetVersion = readAssetText(context, VERSION_ASSET);
        String currentVersion = readFileText(new File(rootDir, VERSION_FILE));
        if (tablesDir.exists() && assetVersion != null && assetVersion.equals(currentVersion)) {
            return;
        }
        if (!tablesDir.exists() && !tablesDir.mkdirs()) {
            return;
        }
        AssetManager assets = context.getAssets();
        try {
            String[] files = assets.list(TABLES_ASSET_DIR);
            if (files != null) {
                for (String name : files) {
                    copyAsset(assets, TABLES_ASSET_DIR + "/" + name, new File(tablesDir, name));
                }
            }
            if (assetVersion != null) {
                writeFileText(new File(rootDir, VERSION_FILE), assetVersion);
            }
        } catch (IOException e) {
            // ignore; IME will fall back to simple translator
        }
    }

    private static void copyAsset(AssetManager assets, String assetPath, File outFile) throws IOException {
        try (InputStream in = assets.open(assetPath); FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        }
    }

    private static String readAssetText(Context context, String assetPath) {
        try (InputStream in = context.getAssets().open(assetPath)) {
            byte[] data = new byte[in.available()];
            int read = in.read(data);
            if (read <= 0) {
                return null;
            }
            return new String(data, 0, read, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private static String readFileText(File file) {
        if (!file.exists()) {
            return null;
        }
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = in.read(data);
            if (read <= 0) {
                return null;
            }
            return new String(data, 0, read, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeFileText(File file, String value) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // ignore
        }
    }
}
