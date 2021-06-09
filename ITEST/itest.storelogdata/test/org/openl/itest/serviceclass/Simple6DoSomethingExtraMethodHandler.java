package org.openl.itest.serviceclass;

import java.lang.reflect.Method;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataHolder;

public class Simple6DoSomethingExtraMethodHandler implements ServiceExtraMethodHandler<Object> {

    @Override
    public Object invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        Method doSomethingMethod = serviceBean.getClass().getMethod("DoSomething");
        SpreadsheetResult spr = (SpreadsheetResult) doSomethingMethod.invoke(serviceBean);
        Object object = spr.getCustomSpreadsheetResultOpenClass().createBean(spr);

        // check serialization/deserialization
        ObjectSerializer serializer = StoreLogDataHolder.get().getObjectSerializer();
        String content = serializer.writeValueAsString(object);
        return serializer.readValue(content, spr.getCustomSpreadsheetResultOpenClass().getBeanClass());
    }
}
