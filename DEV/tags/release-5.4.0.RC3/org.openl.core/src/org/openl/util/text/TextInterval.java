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
public class TextInterval implements ILocation {
    IPosition start;
    IPosition end;

    public TextInterval(IPosition start, IPosition end) {
        this.start = start;
        this.end = end;
    }

    /**
     * @return
     */
    public IPosition getEnd() {
        return end;
    }

    /**
     * @return
     */
    public IPosition getStart() {
        return start;
    }

    /**
     *
     */

    public boolean isTextLocation() {
        return true;
    }

    /**
     * @param position
     */
    public void setEnd(IPosition position) {
        end = position;
    }

    /**
     * @param position
     */
    public void setStart(IPosition position) {
        start = position;
    }

    @Override
    public String toString() {
        return "[" + start + "," + end + "]";
    }

}
