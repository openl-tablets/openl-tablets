package org.openl.rules.ruleservice.logging;

import java.lang.reflect.Method;
import java.util.Date;

import org.openl.rules.project.model.RulesDeploy.PublisherType;

public interface StoreLoggingData {
    public PublisherType getPublisherType();

    public String getRequest();

    public String getResponse();

    public String getUrl();

    public String getServiceName();

    public Date getIncomingMessageTime();

    public Date getOutcomingMessageTime();

    public String getInputName();

    public Object[] getParameters();

    public CustomData getCustomData();

    public Method getServiceMethod();

    public String getOutTopic();

    public String getInTopic();

    public boolean isIgnorable();
}
