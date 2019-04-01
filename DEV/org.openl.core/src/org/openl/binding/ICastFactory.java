/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;

/**
 * Interface class that defines cast factory abstraction. Cast factory implementations provide type cast methods what
 * are used by engine to convert types if it required.
 *
 * @author snshor
 */
public interface ICastFactory {
    IOpenCast getCast(IOpenClass from, IOpenClass to);

    IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2);
}
