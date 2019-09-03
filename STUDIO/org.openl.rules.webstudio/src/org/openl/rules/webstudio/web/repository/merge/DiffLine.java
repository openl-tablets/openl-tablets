package org.openl.rules.webstudio.web.repository.merge;

public class DiffLine {
    private final String text;
    private final int theirLine;
    private final int yourLine;
    private final DiffType type;
    private final boolean noEndOfFile;

    DiffLine(String text, int theirLine, int yourLine, DiffType type, boolean noEndOfFile) {
        this.text = text;
        this.theirLine = theirLine;
        this.yourLine = yourLine;
        this.type = type;
        this.noEndOfFile = noEndOfFile;
    }

    public String getText() {
        return text;
    }

    public int getTheirLine() {
        return theirLine;
    }

    public int getYourLine() {
        return yourLine;
    }

    public DiffType getType() {
        return type;
    }

    public boolean isNoEndOfFile() {
        return noEndOfFile;
    }
}
