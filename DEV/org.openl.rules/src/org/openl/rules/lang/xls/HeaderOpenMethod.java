package org.openl.rules.lang.xls;

import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class HeaderOpenMethod extends AMethod {
    public HeaderOpenMethod(IOpenMethodHeader header) {
        super(header);
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        throw new IllegalStateException("Prebinded method can't be executed!");
    }
}
