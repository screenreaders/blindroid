package com.screenreaders.blindroid.braillekeyboard;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class LiblouisTableRegistry {
    private static final Map<String, String> TABLE_FILES;

    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("cs-g1", "cs-g1.ctb");
        map.put("da-comp8", "da-dk-g08.ctb");
        map.put("da-g1", "da-dk-g16.ctb");
        map.put("de-comp8", "de-de-comp8.ctb");
        map.put("de-DE-g1", "de-g1.ctb");
        map.put("el-g1", "el.ctb");
        map.put("en-UEB-g1", "en-ueb-g1.ctb");
        map.put("en-UEB-g2", "en-ueb-g2.ctb");
        map.put("en-US-comp8", "en-us-comp8.ctb");
        map.put("en-US-g1", "en-us-g1.ctb");
        map.put("en-US-g2", "en-us-g2.ctb");
        map.put("es-comp8", "Es-Es-G0.utb");
        map.put("es-g1", "es-g1.ctb");
        map.put("fi-comp8", "fi-fi-8dot.ctb");
        map.put("fr-CA-g1", "fr-bfu-g2.ctb");
        map.put("fr-comp8", "fr-bfu-comp8.utb");
        map.put("fr-FR-g1", "fr-bfu-g2.ctb");
        map.put("hr-comp8", "hr-comp8.utb");
        map.put("it-comp8", "it-it-comp8.utb");
        map.put("it-g1", "it-it-comp6.utb");
        map.put("nl-g1", "nl-comp8.utb");
        map.put("pl-comp", "pl-pl-comp8.ctb");
        map.put("pl-g1", "Pl-Pl-g1.utb");
        map.put("pt-BR-comp8", "pt-br-comp8.ctb");
        map.put("pt-BR-g1", "pt-br-g1.utb");
        map.put("pt-comp8", "pt-pt-comp8.ctb");
        map.put("pt-g1", "pt-pt-g1.utb");
        map.put("ro-comp8", "ro-g0.utb");
        map.put("ru-g1", "ru-ru-g1.ctb");
        map.put("sk-g1", "sk-g1.ctb");
        map.put("sv-comp8", "sv-g0.utb");
        map.put("sv-g1", "sv-g1.ctb");
        map.put("tr-comp8", "tr-g1.ctb");
        map.put("vi-g1", "vi-vn-g1.ctb");
        map.put("zh-comp8", "zh-chn.ctb");
        map.put("zh-TW-comp8", "zh-tw.ctb");
        TABLE_FILES = Collections.unmodifiableMap(map);
    }

    private LiblouisTableRegistry() {
    }

    static String resolveTableFile(String tableId) {
        return TABLE_FILES.get(tableId);
    }
}
