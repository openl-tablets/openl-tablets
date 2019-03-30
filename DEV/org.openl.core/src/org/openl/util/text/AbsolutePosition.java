/*
 * Created on May 15, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.text;

/**
 * @author snshor
 *
 */
public class AbsolutePosition implements IPosition {
    int pos;

    public AbsolutePosition(int pos) {
        this.pos = pos;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.Position#getAbsolutePosition(org.openl.util.text.TextInfo)
     */
    @Override
    public int getAbsolutePosition(TextInfo info) {
        return pos;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.Position#getColumn(org.openl.util.text.TextInfo)
     */
    @Override
    public int getColumn(TextInfo info, int tabsize) {
        int line = info.getLineIdx(pos);
        int start = info.getPosition(line);

        return TextInfo.getColumn(info.getLine(line), pos - start, tabsize);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.Position#getLine(org.openl.util.text.TextInfo)
     */
    @Override
    public int getLine(TextInfo info) {
        return info.getLineIdx(pos);
    }

    @Override
    public String toString() {
        return String.valueOf(pos);
    }

}
