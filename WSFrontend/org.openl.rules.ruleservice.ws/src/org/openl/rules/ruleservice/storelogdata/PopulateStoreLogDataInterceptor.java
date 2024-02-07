package org.openl.rules.ruleservice.storelogdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.publish.jaxrs.storelogdata.JacksonObjectSerializer;

/**
 * CXF interceptor for collecting service data for logging to external source feature.
 *
 * @author Yury Molchan
 */
public class PopulateStoreLogDataInterceptor extends AbstractPhaseInterceptor<Message> {

    private final String serviceName;
    private final Class<?> serviceClass;
    private final ObjectSerializer objectSerializer;

    public PopulateStoreLogDataInterceptor(OpenLService service, ObjectMapper objectMapper) throws RuleServiceInstantiationException {
        super(Phase.PRE_STREAM);
        this.serviceClass = service.getServiceClass();
        this.serviceName = service.getName();
        this.objectSerializer = new JacksonObjectSerializer(objectMapper);
    }

    @Override
    public void handleMessage(Message message) {
        populateStoreLogData();
    }

    @Override
    public void handleFault(Message message) {
        populateStoreLogData();
    }

    private void populateStoreLogData() {
        var storeLogData = StoreLogDataHolder.get();
        storeLogData.setServiceClass(serviceClass);
        storeLogData.setServiceName(serviceName);
        storeLogData.setPublisherType(RulesDeploy.PublisherType.RESTFUL);
        storeLogData.setObjectSerializer(objectSerializer);
    }
}
