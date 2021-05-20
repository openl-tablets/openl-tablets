package org.openl.rules.ruleservice.storelogdata.hive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HiveDriverRegister {
    private static final String driverClass = "org.apache.hive.jdbc.HiveDriver";

    public void init() {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            Logger log = LoggerFactory.getLogger(HiveDriverRegister.class);
            log.error("The driver {} is not found", driverClass, e);
        }
    }
}
