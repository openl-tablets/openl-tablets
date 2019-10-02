package org.openl.itest.serviceclass;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;

public interface Simple1ServiceAnnotationTemplate {

    @StoreLogDataToCassandra
    String Hello(IRulesRuntimeContext runtimeContext, Integer hour);
}

