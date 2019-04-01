/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import org.openl.binding.*;
import org.openl.syntax.code.IParsedCode;

/**
 * Defines parsed code binder abstraction.
 *
 * @author snshor
 */
public interface IOpenBinder {

    IBoundCode bind(IParsedCode parsedCode);

    IBoundCode bind(IParsedCode parsedCode, IBindingContext bindingContext);

    ICastFactory getCastFactory();

    INameSpacedMethodFactory getMethodFactory();

    INodeBinderFactory getNodeBinderFactory();

    INameSpacedTypeFactory getTypeFactory();

    INameSpacedVarFactory getVarFactory();

    IBindingContext makeBindingContext();

}
