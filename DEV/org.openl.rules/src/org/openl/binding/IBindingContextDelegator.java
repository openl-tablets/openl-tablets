/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

/**
 * @author snshor
 */
public interface IBindingContextDelegator extends IBindingContext {

    void setDelegate(IBindingContext delegate);

    void setTopDelegate(IBindingContext delegate);

}
