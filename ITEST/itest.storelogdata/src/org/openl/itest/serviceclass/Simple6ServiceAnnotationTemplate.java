package org.openl.itest.serviceclass;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.openl.itest.cassandra.HelloEntity6;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;

public interface Simple6ServiceAnnotationTemplate {

    @StoreLogDataToCassandra(value = HelloEntity6.class, sync = true)
    @PrepareStoreLogData(value = Simple6ServiceCollectBefore.class, before = true)
    @PrepareStoreLogData(Simple6ServiceCollectorAfter.class)
    String Hello(Integer hour);

    @PrepareStoreLogData(value = Simple6ServiceRetrieverBefore.class, before = true)
    @ServiceExtraMethod(Simple6ServiceExtraMethodHandler.class)
    @Path("/response/{id}")
    @GET
    Simple6ResponseDTO getResponseById(@PathParam("id") String id);

    @ServiceExtraMethod(Simple6DoSomethingExtraMethodHandler.class)
    @RulesType("DoSomething")
        // SpreadsheetResult custom class type
    Object DoSomethingExtra();

    @StoreLogDataToCassandra(value = HelloEntity6.class, sync = true)
    @PrepareStoreLogData(value = Simple6AlwaysThrowException.class, before = true)
    @ServiceExtraMethod(Simple6ServiceExtraMethodHandler.class)
    Simple6ResponseDTO AlwaysThrowExceptionBeforeCall();

    @StoreLogDataToCassandra(value = HelloEntity6.class, sync = true)
    @PrepareStoreLogData(Simple6AlwaysThrowException.class)
    @ServiceExtraMethod(Simple6ServiceExtraMethodHandler.class)
    Simple6ResponseDTO AlwaysThrowExceptionAfterCall();
}
