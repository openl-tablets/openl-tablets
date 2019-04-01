package org.openl;

import java.util.List;
import java.util.Set;

import org.openl.validation.IOpenLValidator;

/**
 * The <code>ICompileContext</code> defines an abstraction of the compilation time context.
 *
 * The instance used to define compile time settings what determine compilation process and can be changed by user.
 */
public interface ICompileContext {

    void addValidator(IOpenLValidator validator);

    void addValidators(List<IOpenLValidator> validators);

    void removeValidator(IOpenLValidator validator);

    void removeValidators();

    Set<IOpenLValidator> getValidators();
}
