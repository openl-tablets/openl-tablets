package org.openl.rules.project.resolving;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Base resolving strategy class with listener support logic
 * 
 * @author Marat Kamalov
 * 
 */

public abstract class BaseResolvingStrategy implements ResolvingStrategy {
    private final Log log = LogFactory.getLog(BaseResolvingStrategy.class);

    private List<InitializingModuleListener> initializingModuleListeners = new ArrayList<InitializingModuleListener>();

    private List<InitializingProjectListener> initializingProjectListeners = new ArrayList<InitializingProjectListener>();

    /** {@inheritDoc} */
    public void setInitializingModuleListeners(List<InitializingModuleListener> initializingModuleListeners) {
        this.initializingModuleListeners = initializingModuleListeners;
    }

    /** {@inheritDoc} */
    public void setInitializingProjectListeners(List<InitializingProjectListener> initializingProjectListeners) {
        this.initializingProjectListeners = initializingProjectListeners;
    }

    /** {@inheritDoc} */
    @Override
    public List<InitializingModuleListener> getInitializingModuleListeners() {
        return Collections.unmodifiableList(initializingModuleListeners);
    }

    /** {@inheritDoc} */
    @Override
    public void addInitializingModuleListener(InitializingModuleListener initializingModuleListener) {
        if (initializingModuleListener == null) {
            throw new IllegalArgumentException("initializingModuleListeners argument can't be null");
        }
        initializingModuleListeners.add(initializingModuleListener);
        if (log.isInfoEnabled()) {
            log.info(initializingModuleListener.getClass().toString() + " listener is registered");
        }

    }

    /** {@inheritDoc} */
    @Override
    public void removeAllInitializingModuleListeners() {
        initializingModuleListeners.clear();
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeInitializingModuleListener(InitializingModuleListener initializingModuleListener) {
        if (initializingModuleListener == null) {
            throw new IllegalArgumentException("initializingModuleListener argument can't be null");
        }
        boolean result = initializingModuleListeners.remove(initializingModuleListener);
        if (result) {
            if (log.isInfoEnabled()) {
                log.info(initializingModuleListener.getClass().toString() + " listener is unregistered");
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn(initializingModuleListener.getClass().toString() + " listener wasn't unregistered!!! The listener wasn't registered");
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<InitializingProjectListener> getInitializingProjectListeners() {
        return Collections.unmodifiableList(initializingProjectListeners);
    }

    /** {@inheritDoc} */
    @Override
    public void addInitializingProjectListener(InitializingProjectListener initializingProjectListener) {
        if (initializingProjectListener == null) {
            throw new IllegalArgumentException("initializingProjectListeners argument can't be null");
        }
        initializingProjectListeners.add(initializingProjectListener);
        if (log.isInfoEnabled()) {
            log.info(initializingProjectListener.getClass().toString() + " listener is registered");
        }

    }

    /** {@inheritDoc} */
    @Override
    public void removeAllInitializingProjectListeners() {
        initializingProjectListeners.clear();
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeInitializingProjectListener(InitializingProjectListener initializingProjectListener) {
        if (initializingProjectListener == null) {
            throw new IllegalArgumentException("initializingProjectListener argument can't be null");
        }
        boolean result = initializingProjectListeners.remove(initializingProjectListener);
        if (result) {
            if (log.isInfoEnabled()) {
                log.info(initializingProjectListener.getClass().toString() + " listener is unregistered");
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn(initializingProjectListener.getClass().toString() + " listener wasn't unregistered!!! The listener wasn't registered");
            }
        }
        return result;
    }

    protected abstract ProjectDescriptor internalResolveProject(File folder) throws ProjectResolvingException;

    @Override
    public final ProjectDescriptor resolveProject(File folder) throws ProjectResolvingException {
        ProjectDescriptor projectDescriptor = internalResolveProject(folder);
        // Invoke project loader listeners
        for (InitializingProjectListener listener : getInitializingProjectListeners()) {
            try {
                listener.afterProjectLoad(projectDescriptor);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Listener invocation error!", e);
                }
            }
        }

        // Invoke module loader listeners
        for (InitializingModuleListener listener : getInitializingModuleListeners()) {
            for (Module module : projectDescriptor.getModules()) {
                try {
                    listener.afterModuleLoad(module);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Listener invocation failed!", e);
                    }
                }
            }
        }
        return projectDescriptor;
    }
}
