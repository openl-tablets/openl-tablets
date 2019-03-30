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
public interface IPosition {

    /**
     *
     * @param info text helper class
     * @return the absolute position (number of characters from the very beginning of text) starting from 0
     * @throws {@link UnsupportedOperationException} when subclass doesn`t support this operation
     */
    int getAbsolutePosition(TextInfo info);

    /**
     *
     * @param info text helper class
     * @param tabSize the tab size as understood by caller
     * @return the column with '\t' expanded according to the tabsize starting from 0
     */
    int getColumn(TextInfo info, int tabSize);

    /**
     * @param info text helper class
     * @return the line in text starting from 0
     */
    int getLine(TextInfo info);

}
