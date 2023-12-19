package util

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod

abstract class Service {
    static String doIt() {
        Util.ping()
    }

    @ServiceExtraMethod(VirtualMethodHandler.class)
    abstract String exec();
}
