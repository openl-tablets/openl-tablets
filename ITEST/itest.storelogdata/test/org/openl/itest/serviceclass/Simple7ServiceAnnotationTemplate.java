package org.openl.itest.serviceclass;

import org.openl.itest.cassandra.HelloEntity7;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;

public interface Simple7ServiceAnnotationTemplate {

    @StoreLogDataToCassandra(value = HelloEntity7.class, sync = true)
    @RulesType("MyType")
    Object Hello(IRulesRuntimeContext runtimeContext, Integer hour);

}
