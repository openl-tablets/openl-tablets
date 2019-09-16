package org.openl.rules.ruleservice.logging;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.kafka.publish.RequestMessage;

/**
 * Bean for data for logging to external source feature.
 *
 * @author Marat Kamalov
 *
 */
public class StoreLoggingData {
    private LoggingMessage requestMessage;
    private LoggingMessage responseMessage;

    private ConsumerRecord<String, RequestMessage> consumerRecord;
    private ProducerRecord<String, Object> producerRecord;
    private ProducerRecord<String, byte[]> dltRecord;

    private Date incomingMessageTime;
    private Date outcomingMessageTime;

    private String inputName;
    private Object[] parameters;

    private String serviceName;

    private PublisherType publisherType;

    private Method serviceMethod;

    private Map<String, Object> customValues = new HashMap<>();

    private ObjectSerializer objectSerializer;

    private boolean ignorable = false;

    public ProducerRecord<String, Object> getProducerRecord() {
        return producerRecord;
    }

    public void setProducerRecord(ProducerRecord<String, Object> producerRecord) {
        this.producerRecord = producerRecord;
    }

    public ProducerRecord<String, byte[]> getDltRecord() {
        return dltRecord;
    }

    public void setDltRecord(ProducerRecord<String, byte[]> dltRecord) {
        this.dltRecord = dltRecord;
    }

    public ConsumerRecord<String, RequestMessage> getConsumerRecord() {
        return consumerRecord;
    }

    public void setConsumerRecord(ConsumerRecord<String, RequestMessage> consumerRecord) {
        this.consumerRecord = consumerRecord;
    }

    public PublisherType getPublisherType() {
        return publisherType;
    }

    public void setPublisherType(PublisherType publisherType) {
        this.publisherType = publisherType;
    }

    public LoggingMessage getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(LoggingMessage requestMessage) {
        this.requestMessage = requestMessage;
    }

    public LoggingMessage getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(LoggingMessage responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Date getIncomingMessageTime() {
        return incomingMessageTime;
    }

    public void setIncomingMessageTime(Date incomingMessageTime) {
        this.incomingMessageTime = incomingMessageTime;
    }

    public Date getOutcomingMessageTime() {
        return outcomingMessageTime;
    }

    public void setOutcomingMessageTime(Date outcomingMessageTime) {
        this.outcomingMessageTime = outcomingMessageTime;
    }

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getCustomValues() {
        return customValues;
    }

    public Method getServiceMethod() {
        return serviceMethod;
    }

    public void setServiceMethod(Method serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    public boolean isIgnorable() {
        return ignorable;
    }

    public void setIgnorable(boolean ignorable) {
        this.ignorable = ignorable;
    }

    public void ignore() {
        this.ignorable = true;
    }

    public ObjectSerializer getObjectSerializer() {
        return objectSerializer;
    }

    public void setObjectSerializer(ObjectSerializer objectSerializer) {
        this.objectSerializer = objectSerializer;
    }
}
