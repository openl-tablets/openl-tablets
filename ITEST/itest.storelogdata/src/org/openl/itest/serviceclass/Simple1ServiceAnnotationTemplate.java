package org.openl.itest.serviceclass;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.storelogdata.db.annotation.StoreLogDataToDB;

public interface Simple1ServiceAnnotationTemplate {
    @StoreLogDataToDB
    String Hello(IRulesRuntimeContext runtimeContext, Integer hour);
}
