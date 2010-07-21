/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface ICastFactory {
    IOpenCast getCast(IOpenClass from, IOpenClass to);
}
