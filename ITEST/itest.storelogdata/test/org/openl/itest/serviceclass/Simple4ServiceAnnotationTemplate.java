package org.openl.itest.serviceclass;

import org.openl.itest.cassandra.HelloEntity1;
import org.openl.itest.cassandra.HelloEntity2;
import org.openl.itest.cassandra.HelloEntity3;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;

public interface Simple4ServiceAnnotationTemplate {

    @StoreLogDataToCassandra({ HelloEntity1.class, HelloEntity2.class, HelloEntity3.class })
    @PrepareStoreLogData(PrepareStoreLogDataValue.class)
    @PrepareStoreLogData(PrepareStoreLogDataArgs.class)
    @PrepareStoreLogData(PrepareStoreLogDataResult.class)
    @PrepareStoreLogData(PrepareStoreLogDataObjectSerializerFound.class)
    String Hello(IRulesRuntimeContext runtimeContext, Integer hour);
}
