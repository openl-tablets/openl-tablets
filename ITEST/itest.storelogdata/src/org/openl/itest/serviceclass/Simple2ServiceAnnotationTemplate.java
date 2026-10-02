package org.openl.itest.serviceclass;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.storelogdata.db.annotation.StoreLogDataToDB;

@StoreLogDataToDB
public interface Simple2ServiceAnnotationTemplate {

    // The method is named after the Hello rule it publishes; the request suites call it by this name.
    @SuppressWarnings("java:S100")
    String Hello(IRulesRuntimeContext runtimeContext, Integer hour);
}
