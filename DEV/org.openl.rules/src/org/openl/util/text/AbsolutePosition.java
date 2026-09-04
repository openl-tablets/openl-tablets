/*
 * Created on May 15, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.text;

import lombok.RequiredArgsConstructor;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class AbsolutePosition implements IPosition {
    private final int pos;

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
    public int getColumn(TextInfo info) {
        var line = info.getLineIdx(pos);
        var start = info.getPosition(line);

        return pos - start + 1;
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
