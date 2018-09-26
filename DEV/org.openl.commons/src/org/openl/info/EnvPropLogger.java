package org.openl.info;

import java.util.Map;

final class EnvPropLogger extends OpenLLogger {
    @Override
    protected String getName() {
        return "env";
    }

    @Override
    protected void discover() {
        log("System environment:");
        for (Map.Entry<?, ?> prop : System.getenv().entrySet()) {
            log("  {} = {}", prop.getKey(), prop.getValue());
        }
    }
}
