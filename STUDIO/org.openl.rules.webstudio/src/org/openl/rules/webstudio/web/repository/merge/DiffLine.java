package org.openl.rules.webstudio.web.repository.merge;

public class DiffLine {
    private final String text;
    private final int theirLine;
    private final int ourLine;
    private final DiffType type;
    private final boolean noEndOfFile;

    DiffLine(String text, int theirLine, int ourLine, DiffType type, boolean noEndOfFile) {
        this.text = text;
        this.theirLine = theirLine;
        this.ourLine = ourLine;
        this.type = type;
        this.noEndOfFile = noEndOfFile;
    }

    public String getText() {
        return text;
    }

    public int getTheirLine() {
        return theirLine;
    }

    public int getOurLine() {
        return ourLine;
    }

    public DiffType getType() {
        return type;
    }

    public boolean isNoEndOfFile() {
        return noEndOfFile;
    }
}
