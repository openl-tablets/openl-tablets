package org.openl.rules.calc;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindingContextDelegator;

public class SpreadsheetContext extends BindingContextDelegator {

    public SpreadsheetContext(IBindingContext delegate, SpreadsheetOpenClass type) {
        super(delegate);
    }

}
