package org.openl.studio.docs;

import java.util.ArrayList;
import java.util.List;

/**
 * The records of a {@code csv} or an {@code openl} code block of a user guide.
 *
 * <p>The records follow RFC 4180: values are separated by commas, and a value holding a comma, a quote or a line break
 * is quoted, with every quote inside it doubled.
 *
 * <p>Spaces around an unquoted value are dropped, and a blank line holds no record. A quoted value is kept as it is
 * written, so a quoted {@code <} is a value rather than a merge.
 *
 * @author Yury Molchan
 */
record CsvRecords(List<CsvRecords.Row> rows, List<Issue> issues) {

    /**
     * A value of a record.
     *
     * @param quoted whether the value is quoted, which makes it literal
     */
    record Cell(String value, boolean quoted) {

        /** Whether the cell is the given marker, written unquoted. */
        boolean is(String marker) {
            return !quoted && value.equals(marker);
        }
    }

    /**
     * A record of the block.
     *
     * @param line the line of the block the record starts at, counted from 0
     */
    record Row(int line, List<Cell> cells) {
    }

    /** Reads the records of a block. */
    static CsvRecords parse(String text) {
        return new Reader(text).read();
    }

    private static final class Reader {

        private final String text;
        private final List<Row> rows = new ArrayList<>();
        private final List<Issue> issues = new ArrayList<>();
        private int position;
        private int line;

        Reader(String text) {
            this.text = text;
        }

        CsvRecords read() {
            while (position < text.length()) {
                if (isBlankLine()) {
                    skipLine();
                } else {
                    rows.add(row());
                }
            }
            return new CsvRecords(rows, issues);
        }

        private boolean isBlankLine() {
            return text.substring(position, lineEnd()).isBlank();
        }

        private void skipLine() {
            position = Math.min(lineEnd() + 1, text.length());
            line++;
        }

        private int lineEnd() {
            var end = text.indexOf('\n', position);
            return end < 0 ? text.length() : end;
        }

        private Row row() {
            var start = line;
            var cells = new ArrayList<Cell>();
            do {
                cells.add(cell());
            } while (nextValue());
            return new Row(start, cells);
        }

        /** Steps over what ends a value, telling whether another value of the record follows. */
        private boolean nextValue() {
            if (position >= text.length()) {
                return false;
            }
            var separator = text.charAt(position++);
            if (separator == '\n') {
                line++;
            }
            return separator == ',';
        }

        private Cell cell() {
            skipSpaces();
            return position < text.length() && text.charAt(position) == '"' ? quoted() : unquoted();
        }

        private Cell unquoted() {
            var start = position;
            skipToBoundary();
            var value = text.substring(start, position).strip();
            if (value.indexOf('"') >= 0) {
                issues.add(new Issue(line,
                        "A quote inside an unquoted value. Quote the whole value and double the quote."));
            }
            return new Cell(value, false);
        }

        private Cell quoted() {
            var start = line;
            var value = new StringBuilder();
            var closed = false;
            position++;
            while (!closed && position < text.length()) {
                var c = text.charAt(position++);
                if (c == '"' && position < text.length() && text.charAt(position) == '"') {
                    value.append(c);
                    position++;
                } else if (c == '"') {
                    closed = true;
                } else {
                    line += c == '\n' ? 1 : 0;
                    value.append(c);
                }
            }
            if (!closed) {
                issues.add(new Issue(start, "A quoted value is not closed."));
            }
            skipSpaces();
            if (position < text.length() && !isBoundary()) {
                issues.add(new Issue(line, "Text after a closing quote. Quote the whole value."));
                skipToBoundary();
            }
            return new Cell(value.toString(), true);
        }

        private void skipSpaces() {
            while (position < text.length() && " \t\r".indexOf(text.charAt(position)) >= 0) {
                position++;
            }
        }

        private void skipToBoundary() {
            while (position < text.length() && !isBoundary()) {
                position++;
            }
        }

        private boolean isBoundary() {
            var c = text.charAt(position);
            return c == ',' || c == '\n';
        }
    }
}
