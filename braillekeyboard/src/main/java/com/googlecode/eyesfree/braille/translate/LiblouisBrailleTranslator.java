package com.googlecode.eyesfree.braille.translate;

import com.screenreaders.blindroid.braillekeyboard.LiblouisBridge;

public class LiblouisBrailleTranslator implements BrailleTranslator {
    private final String tableId;
    private final SimpleBrailleTranslator fallback = new SimpleBrailleTranslator();

    public LiblouisBrailleTranslator(String tableId) {
        this.tableId = tableId;
    }

    @Override
    public String backTranslate(byte[] cells) {
        String translated = LiblouisBridge.backTranslate(tableId, cells);
        if (translated != null) {
            return translated;
        }
        return fallback.backTranslate(cells);
    }
}
