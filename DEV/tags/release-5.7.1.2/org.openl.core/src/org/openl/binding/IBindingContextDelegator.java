/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

/**
 * @author snshor
 *
 */
public interface IBindingContextDelegator extends IBindingContext {
    public void setDelegate(IBindingContext delegate);

    public void setTopDelegate(IBindingContext delegate);

}
