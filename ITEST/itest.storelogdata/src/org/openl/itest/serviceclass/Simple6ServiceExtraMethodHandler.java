package org.openl.itest.serviceclass;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataHolder;

public class Simple6ServiceExtraMethodHandler implements ServiceExtraMethodHandler<Simple6ResponseDTO> {

    @Override
    public Simple6ResponseDTO invoke(Method interfaceMethod, Object serviceBean, Object... args) {
        Simple6ResponseDTO dest = new Simple6ResponseDTO();
        dest.response = (String) StoreLogDataHolder.get().getCustomValues().get("responseTemp");
        return dest;
    }
}
