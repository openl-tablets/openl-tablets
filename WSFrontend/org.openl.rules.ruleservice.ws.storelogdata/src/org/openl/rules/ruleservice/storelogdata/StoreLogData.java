package org.openl.rules.ruleservice.storelogdata;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.kafka.RequestMessage;

/**
 * Bean for data for logging to external source feature.
 *
 * @author Marat Kamalov
 */
public class StoreLogData {
    @Getter
    @Setter
    private LoggingMessage requestMessage;
    @Getter
    @Setter
    private LoggingMessage responseMessage;

    @Getter
    @Setter
    private ConsumerRecord<String, RequestMessage> consumerRecord;
    @Getter
    @Setter
    private ProducerRecord<String, Object> producerRecord;
    @Getter
    @Setter
    private ProducerRecord<String, byte[]> dltRecord;

    @Getter
    @Setter
    private ZonedDateTime incomingMessageTime;
    @Getter
    @Setter
    private ZonedDateTime outcomingMessageTime;

    @Getter
    @Setter
    private Object[] parameters;

    @Getter
    @Setter
    private String serviceName;

    @Getter
    @Setter
    private PublisherType publisherType;

    @Getter
    @Setter
    private Class<?> serviceClass;

    @Getter
    @Setter
    private Method serviceMethod;

    @Getter
    private final Map<String, Object> customValues = new HashMap<>();

    @Getter
    @Setter
    private ObjectSerializer objectSerializer;

    @Getter
    @Setter
    private boolean ignorable = false;

    private Map<Class<?>, Boolean> ignorableByEntity;

    @Getter
    @Setter
    private boolean fault = true;

    public boolean isIgnorable(Class<?> entityClass) {
        if (ignorableByEntity == null) {
            return false;
        }
        return Boolean.TRUE.equals(ignorableByEntity.get(entityClass));
    }

    public void ignore() {
        this.ignorable = true;
    }

    public void ignore(Class<?> entityClass) {
        if (ignorableByEntity == null) {
            ignorableByEntity = new HashMap<>();
        }
        ignorableByEntity.put(entityClass, Boolean.TRUE);
    }

    public void fault() {
        this.fault = true;
    }
}
