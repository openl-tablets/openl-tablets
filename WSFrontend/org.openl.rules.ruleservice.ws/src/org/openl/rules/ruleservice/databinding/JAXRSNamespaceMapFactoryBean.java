package org.openl.rules.ruleservice.databinding;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;

public class JAXRSNamespaceMapFactoryBean implements FactoryBean<Map<String, String>> {

    private String ignoredNamespace;

    public void setIgnoredNamespace(String ignoredNamespace) {
        this.ignoredNamespace = ignoredNamespace;
    }

    public String getIgnoredNamespace() {
        return ignoredNamespace;
    }

    @Override
    public Map<String, String> getObject() throws Exception {
        Map<String, String> namespaceMap = new HashMap<>();
        if (ignoredNamespace != null && ignoredNamespace.length() > 0) {
            namespaceMap.put(ignoredNamespace, "");
        }
        return namespaceMap;
    }

    @Override
    public Class<?> getObjectType() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
