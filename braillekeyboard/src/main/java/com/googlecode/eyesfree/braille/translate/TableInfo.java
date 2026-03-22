package com.googlecode.eyesfree.braille.translate;

import java.util.Locale;

public class TableInfo {
    private final String id;
    private final Locale locale;
    private final boolean eightDot;
    private final int grade;

    public TableInfo(String id, Locale locale, boolean eightDot, int grade) {
        this.id = id;
        this.locale = locale;
        this.eightDot = eightDot;
        this.grade = grade;
    }

    public String getId() {
        return id;
    }

    public Locale getLocale() {
        return locale;
    }

    public boolean isEightDot() {
        return eightDot;
    }

    public int getGrade() {
        return grade;
    }
}
