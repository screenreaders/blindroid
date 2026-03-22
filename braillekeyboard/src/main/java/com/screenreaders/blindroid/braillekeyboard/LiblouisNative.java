package com.screenreaders.blindroid.braillekeyboard;

final class LiblouisNative {
    static {
        System.loadLibrary("braille_liblouis_jni");
    }

    private LiblouisNative() {
    }

    static native void setDataPath(String path);

    static native String backTranslate(String table, byte[] cells);
}
