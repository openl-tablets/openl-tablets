package org.openl.rules.webstudio.web.repository.merge;

import java.util.List;

public class DiffSet {
    private final List<DiffLine> lines;
    private final int fromLine;
    private final int toLine;

    DiffSet(List<DiffLine> lines, int fromLine, int toLine) {
        this.lines = lines;
        this.fromLine = fromLine;
        this.toLine = toLine;
    }

    public List<DiffLine> getLines() {
        return lines;
    }

    public int getFromLine() {
        return fromLine;
    }

    public int getToLine() {
        return toLine;
    }
}
