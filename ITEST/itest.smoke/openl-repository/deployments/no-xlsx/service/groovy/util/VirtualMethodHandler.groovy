package util

import java.lang.reflect.Method

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler

class VirtualMethodHandler implements ServiceExtraMethodHandler<String> {

    String invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        return "Hello!";
    }

}
