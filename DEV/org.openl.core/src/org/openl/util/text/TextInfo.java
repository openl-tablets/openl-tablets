/*
 * Created on May 15, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author snshor
 *
 */
public class TextInfo {

    String text;

    int[] lineTable;

    public static int getColumn(String line, int linePos, int tabsize) {
        int col = 0;
        for (int i = 0; i < linePos; i++) {
            if (line.charAt(i) == '\t') {
                col += tabsize - col % tabsize;
            } else {
                col++;
            }
        }
        return col;
    }

    public static int getPosition(String line, int column, int tabsize) {
        if (column == 0) {
            return 0;
        }
        int pos = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\t') {
                pos += tabsize - pos % tabsize;
            } else {
                pos++;
            }
            if (pos >= column) {
                return i + 1;
            }
        }

//        throw new RuntimeException();
        // In case we get EOF position(for some errors)
        return line.length();
    }

    public TextInfo(String text) {
        this.text = text;
    }

    public String getLine(int i) {
        int from = lines()[i];
        int to = i + 1 >= lines().length ? text.length() : lines()[i + 1];

        return text.substring(from, to);
    }

    public int getLineIdx(int absPosition) {
        int idx = Arrays.binarySearch(lines(), absPosition);
        if (idx >= 0) {
            return idx;
        }

        // return -idx + 1;
        // Zero based lineIdx == InsertionPoint - 1
        return -idx - 1 - 1; // TODO SAM: No test case yet.
    }

    public int getPosition(int line) {
        return lines()[line];
    }

    private int[] lines() {
        if (lineTable == null) {
            scanText();
        }
        return lineTable;
    }

    protected void scanText() {
        boolean isCR = false;
        boolean isLF = true;
        List<Integer> table = new ArrayList<>();

        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);

            if (isLF) {
                isLF = false;
                table.add(i);
            } else if (isCR) {
                isCR = false;
                if (c != '\n') {
                    table.add(i);
                }
            }

            if (c == '\n') {
                isLF = true;
            } else if (c == '\r') {
                isCR = true;
            }
        }

        // TODO SAM: No test case yet.
        // To have: lineIdx(text.length()) == totalLines()
        if (isLF || isCR) {
            table.add(text.length());
        }

        lineTable = new int[table.size()];
        for (int i = 0; i < lineTable.length; i++) {
            lineTable[i] = table.get(i);
        }
    }
}
