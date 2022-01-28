package org.openl.grammar;

import org.openl.util.text.IPosition;
import org.openl.util.text.TextInfo;

public class JavaCC30Position implements IPosition {

    private final int jcc30line;
    private final int jcc30col;

    public JavaCC30Position(int jcc30line, int jcc30col) {

        this.jcc30col = jcc30col;
        this.jcc30line = jcc30line;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.Position#getAbsolutePosition(org.openl.util.text.TextInfo)
     */
    @Override
    public int getAbsolutePosition(TextInfo info) {

        if (jcc30line == 0) {
            return 0;
        }

        int line = jcc30line - 1;
        int linePos = info.getPosition(line);
        int colPos = Math.min(info.getLine(line).length(), jcc30col - 1);

        return linePos + colPos;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.Position#getColumn(org.openl.util.text.TextInfo, int)
     */
    @Override
    public int getColumn(TextInfo info) {

        if (jcc30line == 0) {
            return 0;
        }

        int line = jcc30line - 1;
        // int linePos = info.getPosition(line);
        int colPos = Math.min(info.getLine(line).length(), jcc30col - 1);

        return colPos + 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.Position#getLine(org.openl.util.text.TextInfo)
     */
    @Override
    public int getLine(TextInfo info) {

        return jcc30line - 1;
    }

    @Override
    public String toString() {

        return "(" + jcc30line + "," + jcc30col + ")";
    }
}