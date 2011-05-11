package org.openl;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.openl.rules.mapping.Mapper;
import org.openl.util.Foo;

import javax.xml.namespace.QName;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

public class ClientService <T> {

    private Mapper mapper;
    private Client client;
    private Class<T> resultType;

    public ClientService() {
    }

    public ClientService(Class<T>  resultType) {
        this.resultType = resultType;
    }

    public T invoke(String operationName, Object ... params) throws Exception {
        Endpoint endpoint = client.getEndpoint();
        EndpointInfo endpointInfo = endpoint.getEndpointInfo();
        BindingInfo binding = endpointInfo.getBinding();
        Service service = endpoint.getService();
        QName operationQName = new QName(service.getName().getNamespaceURI(), operationName);
        BindingOperationInfo operation = binding.getOperation(operationQName);

        Object[] wsInputParams = mapInputParams(operation, mapInputParams(operation, params));
        Object[] wsResult = client.invoke(operation, wsInputParams);
        T result = mapOutputResult(wsResult);

        return result;
    }

    private Object[] mapInputParams(BindingOperationInfo operation, Object[] inputParams) {
        List<MessagePartInfo> messageParts = operation.getInput().getMessageParts();
        Object[] wsInputParams = new Object[inputParams.length];
        Iterator<MessagePartInfo> iterator = messageParts.iterator();
        for (int i = 0; i < inputParams.length; i++) {
            Object mappedParameter = mapper.map(inputParams[i], iterator.next().getTypeClass());
            wsInputParams[i] = (mappedParameter);
        }

        return wsInputParams;
    }

    private T mapOutputResult(Object[] outputParams) {
        return mapper.map(outputParams[0], resultType);
    }

    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
