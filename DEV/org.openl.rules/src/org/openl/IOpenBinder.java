/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.binding.ICastFactory;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
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

    INameSpacedTypeFactory getTypeFactory();

    INameSpacedVarFactory getVarFactory();

    IBindingContext makeBindingContext();

}
