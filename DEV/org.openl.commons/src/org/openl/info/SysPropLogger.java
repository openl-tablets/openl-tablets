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
            var key = prop.getKey();
            if ("secret.key".equals(key)) {
                log("  {} = ********************************", key);
            } else {
                log("  {} = {}", key, prop.getValue());
            }
        }
    }
}
