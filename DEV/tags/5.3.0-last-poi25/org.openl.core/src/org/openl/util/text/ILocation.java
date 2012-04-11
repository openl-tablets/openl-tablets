/*
 * Created on Oct 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.text;

/**
 * @author snshor
 *
 */
public interface ILocation {
    public IPosition getEnd();

    public IPosition getStart();

    public boolean isTextLocation();

}
