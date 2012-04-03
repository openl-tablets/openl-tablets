package org.openl.rules.ruleservice.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.instantiation.InitializingListener;
import org.springframework.beans.factory.FactoryBean;

/**
 * This factory bean is used from spring configuration file for easy
 * InitializingListeners configuration. You should define all initializer
 * listeners in initializingListenerClassNames property.
 * 
 * @author Marat Kamalov
 * 
 */
public class InitializingListenerFactoryBean implements FactoryBean<List<InitializingListener>> {
    private final Log log = LogFactory.getLog(InitializingListenerFactoryBean.class);

    private String initializingListenerClassNames;

    public String getInitializingListenerClassNames() {
        return initializingListenerClassNames;
    }

    /***
     * Comma separated listener class names.
     * 
     * @param initializingListenerClassNames
     */
    public void setInitializingListenerClassNames(String initializingListenerClassNames) {
        this.initializingListenerClassNames = initializingListenerClassNames;
    }

    @SuppressWarnings("unchecked")
    public List<InitializingListener> getObject() throws Exception {
        List<InitializingListener> initializingListeners = new ArrayList<InitializingListener>();
        if (initializingListenerClassNames != null) {
            String[] initializingListenerClassNamesArray = initializingListenerClassNames.split(",");
            for (String initializingListenerClassName : initializingListenerClassNamesArray) {
                if (initializingListenerClassName != null && initializingListenerClassName.trim().length() > 0) {
                    try {
                        Class<InitializingListener> clazz = (Class<InitializingListener>) Class
                                .forName(initializingListenerClassName);
                        InitializingListener listener = clazz.newInstance();
                        initializingListeners.add(listener);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format(
                                    "Listener on module load wasn't registered. Listener class name is \"%s\"",
                                    initializingListenerClassName.trim()), e);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableList(initializingListeners);
    }

    public Class<?> getObjectType() {
        return List.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
