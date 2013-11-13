package org.openl.rules.project.resolving;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * This factory bean is used from spring configuration file for easy
 * InitializingListeners configuration. You should define all initializer
 * listeners in initializingListenerClassNames property.
 * 
 * @author Marat Kamalov
 * 
 */
public class InitializingListenerFactoryBean implements FactoryBean<List<InitializingModuleListener>> {
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
    public List<InitializingModuleListener> getObject() throws Exception {
        List<InitializingModuleListener> initializingListeners = new ArrayList<InitializingModuleListener>();
        if (initializingListenerClassNames != null) {
            String[] initializingListenerClassNamesArray = initializingListenerClassNames.split(",");
            for (String initializingListenerClassName : initializingListenerClassNamesArray) {
                if (initializingListenerClassName != null && initializingListenerClassName.trim().length() > 0) {
                    try {
                        Class<InitializingModuleListener> clazz = (Class<InitializingModuleListener>) Class
                                .forName(initializingListenerClassName);
                        InitializingModuleListener listener = clazz.newInstance();
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
