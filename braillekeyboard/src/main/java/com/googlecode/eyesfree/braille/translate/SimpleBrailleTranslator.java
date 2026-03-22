package com.googlecode.eyesfree.braille.translate;

import java.util.HashMap;
import java.util.Map;

public class SimpleBrailleTranslator implements BrailleTranslator {
    private static final int NUMBER_SIGN = 0b00111100; // dots 3-4-5-6
    private static final int CAPITAL_SIGN = 0b00100000; // dot 6

    private final Map<Integer, String> letters = new HashMap<Integer, String>();
    private final Map<Integer, String> punctuation = new HashMap<Integer, String>();

    public SimpleBrailleTranslator() {
        // Letters a-z (grade 1, 6-dot)
        letters.put(0b000001, "a");
        letters.put(0b000011, "b");
        letters.put(0b001001, "c");
        letters.put(0b011001, "d");
        letters.put(0b010001, "e");
        letters.put(0b001011, "f");
        letters.put(0b011011, "g");
        letters.put(0b010011, "h");
        letters.put(0b001010, "i");
        letters.put(0b011010, "j");
        letters.put(0b000101, "k");
        letters.put(0b000111, "l");
        letters.put(0b001101, "m");
        letters.put(0b011101, "n");
        letters.put(0b010101, "o");
        letters.put(0b001111, "p");
        letters.put(0b011111, "q");
        letters.put(0b010111, "r");
        letters.put(0b001110, "s");
        letters.put(0b011110, "t");
        letters.put(0b000101 | 0b100000, "u");
        letters.put(0b000111 | 0b100000, "v");
        letters.put(0b011010 | 0b100000, "w");
        letters.put(0b001101 | 0b100000, "x");
        letters.put(0b011101 | 0b100000, "y");
        letters.put(0b010101 | 0b100000, "z");

        punctuation.put(0b000010, ",");
        punctuation.put(0b000110, ";");
        punctuation.put(0b001010 | 0b000100, "?");
        punctuation.put(0b001010 | 0b000110, "!");
        punctuation.put(0b001010 | 0b000010, ".");
        punctuation.put(0b000100, "'");
        punctuation.put(0b001000, "-");
        punctuation.put(0b001100, "/");
    }

    @Override
    public String backTranslate(byte[] cells) {
        if (cells == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        boolean numberMode = false;
        boolean capNext = false;

        for (byte cell : cells) {
            int value = cell & 0xFF;
            if (value == 0) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                numberMode = false;
                capNext = false;
                continue;
            }

            int mask = value & 0b00111111; // ignore dots 7/8

            if (mask == NUMBER_SIGN) {
                numberMode = true;
                continue;
            }
            if (mask == CAPITAL_SIGN) {
                capNext = true;
                continue;
            }

            String letter = letters.get(mask);
            String punct = punctuation.get(mask);
            if (numberMode && letter != null) {
                String digit = toDigit(letter);
                if (digit != null) {
                    out.append(digit);
                    continue;
                }
            }

            String toWrite = letter != null ? letter : punct;
            if (toWrite != null) {
                if (capNext && letter != null) {
                    toWrite = toWrite.toUpperCase();
                }
                out.append(toWrite);
            }

            // Reset modes when a non-digit or punctuation occurs.
            if (punct != null || letter == null) {
                numberMode = false;
            }
            capNext = false;
        }
        return out.toString();
    }

    private String toDigit(String letter) {
        if (letter == null || letter.length() != 1) {
            return null;
        }
        switch (letter.charAt(0)) {
            case 'a':
                return "1";
            case 'b':
                return "2";
            case 'c':
                return "3";
            case 'd':
                return "4";
            case 'e':
                return "5";
            case 'f':
                return "6";
            case 'g':
                return "7";
            case 'h':
                return "8";
            case 'i':
                return "9";
            case 'j':
                return "0";
            default:
                return null;
        }
    }
}
