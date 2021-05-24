package org.openl.itest.serviceclass;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.openl.itest.cassandra.HelloEntity6;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
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
    
}
