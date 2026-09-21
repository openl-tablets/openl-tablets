/*
 * Created on May 15, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.text;

/**
 * @author snshor
 */
public class TextInterval implements ILocation {

    private final IPosition start;
    private final IPosition end;

    public TextInterval(IPosition start, IPosition end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public IPosition getEnd() {
        return end;
    }

    @Override
    public IPosition getStart() {
        return start;
    }

    @Override
    public boolean isTextLocation() {
        return true;
    }

    @Override
    public String toString() {
        return "[" + start + "," + end + "]";
    }
}
