package util

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler

import java.lang.reflect.Method

class VirtualMethodHandler implements ServiceExtraMethodHandler<String> {

    String invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        return "Hello!";
    }

}
