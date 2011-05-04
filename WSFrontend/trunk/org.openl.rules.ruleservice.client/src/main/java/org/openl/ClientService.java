package org.openl;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.openl.rules.mapping.Mapper;

import javax.xml.namespace.QName;
import java.util.LinkedHashMap;

public class ClientService {

    private Mapper mapper;
    private Client client;

    public Object invoke(String operationName, Object ... params) throws Exception {
        Endpoint endpoint = client.getEndpoint();
        EndpointInfo endpointInfo = endpoint.getEndpointInfo();
        BindingInfo binding = endpointInfo.getBinding();
        Service service = endpoint.getService();
        QName operationQName = new QName(service.getName().getNamespaceURI(), operationName);
        BindingOperationInfo operation = binding.getOperation(operationQName);
        return client.invoke(operation, params);
    }

    private Object[] mapInputParams(LinkedHashMap<Object, Class> inputParams) {
        return null;
    }

    private Object[] mapOutputParams(Object ... outputParams) {
        return null;
    }

    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
