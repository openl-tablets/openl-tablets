package org.openl.rules.project.resolving;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This factory bean is used from spring configuration file for easy
 * InitializingListeners configuration. You should define all initializer
 * listeners in initializingListenerClassNames property.
 *
 * @author Marat Kamalov
 */
public class InitializingListenerFactoryBean implements FactoryBean<List<InitializingModuleListener>> {
    private final Logger log = LoggerFactory.getLogger(InitializingListenerFactoryBean.class);

    private String initializingListenerClassNames;

    public String getInitializingListenerClassNames() {
        return initializingListenerClassNames;
    }

    /**
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
                        log.warn("Listener on module load wasn't registered. Listener class name is \"{}\"", initializingListenerClassName.trim(), e);
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
