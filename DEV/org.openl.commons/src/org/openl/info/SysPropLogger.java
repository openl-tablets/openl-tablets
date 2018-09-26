package org.openl.info;

import java.util.Map;

final class SysPropLogger extends OpenLLogger {
    @Override
    protected String getName() {
        return "prop";
    }

    @Override
    protected void discover() {
        log("System properties:");
        for (Map.Entry<?, ?> prop : System.getProperties().entrySet()) {
            log("  {} = {}", prop.getKey(), prop.getValue());
        }
    }
}
