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
        for (Map.Entry<String, String> prop : System.getenv().entrySet()) {
            var key = prop.getKey();
            if ("secret.key".equals(key) || "SECRET_KEY".equals(key)) {
                log("  {} = ********************************", key);
            } else {
                log("  {} = {}", key, prop.getValue());
            }
        }
    }
}
