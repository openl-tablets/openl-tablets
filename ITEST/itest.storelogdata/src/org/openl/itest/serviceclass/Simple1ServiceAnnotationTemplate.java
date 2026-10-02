package org.openl.itest.serviceclass;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.storelogdata.db.annotation.StoreLogDataToDB;

public interface Simple1ServiceAnnotationTemplate {
    // The method is named after the Hello rule it publishes; the request suites call it by this name.
    @SuppressWarnings("java:S100")
    @StoreLogDataToDB
    String Hello(IRulesRuntimeContext runtimeContext, Integer hour);
}
