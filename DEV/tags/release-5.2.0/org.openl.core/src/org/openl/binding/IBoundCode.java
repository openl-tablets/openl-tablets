/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.syntax.*;

/**
 * @author snshor
 *
 */
public interface IBoundCode {
    ISyntaxError[] getError();

    public int getLocalFrameSize();

    IParsedCode getParsedCode();

    IBoundNode getTopNode();

}
