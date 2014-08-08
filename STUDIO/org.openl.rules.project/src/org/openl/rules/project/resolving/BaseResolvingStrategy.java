package org.openl.rules.project.resolving;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base resolving strategy class with listener support logic
 *
 * @author Marat Kamalov
 */

public abstract class BaseResolvingStrategy implements ResolvingStrategy {
    private final Logger log = LoggerFactory.getLogger(BaseResolvingStrategy.class);

    private List<InitializingModuleListener> initializingModuleListeners = new ArrayList<InitializingModuleListener>();

    private List<InitializingProjectListener> initializingProjectListeners = new ArrayList<InitializingProjectListener>();

    /**
     * {@inheritDoc}
     */
    public void setInitializingModuleListeners(List<InitializingModuleListener> initializingModuleListeners) {
        this.initializingModuleListeners = initializingModuleListeners;
    }

    /**
     * {@inheritDoc}
     */
    public void setInitializingProjectListeners(List<InitializingProjectListener> initializingProjectListeners) {
        this.initializingProjectListeners = initializingProjectListeners;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InitializingModuleListener> getInitializingModuleListeners() {
        return Collections.unmodifiableList(initializingModuleListeners);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInitializingModuleListener(InitializingModuleListener initializingModuleListener) {
        if (initializingModuleListener == null) {
            throw new IllegalArgumentException("initializingModuleListeners argument can't be null");
        }
        initializingModuleListeners.add(initializingModuleListener);
        log.info("{} listener is registered", initializingModuleListener.getClass());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllInitializingModuleListeners() {
        initializingModuleListeners.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeInitializingModuleListener(InitializingModuleListener initializingModuleListener) {
        if (initializingModuleListener == null) {
            throw new IllegalArgumentException("initializingModuleListener argument can't be null");
        }
        boolean result = initializingModuleListeners.remove(initializingModuleListener);
        if (result) {
            log.info("{} listener is unregistered", initializingModuleListener.getClass());
        } else {
            log.warn("{} listener wasn't unregistered!!! The listener wasn't registered", initializingModuleListener.getClass());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InitializingProjectListener> getInitializingProjectListeners() {
        return Collections.unmodifiableList(initializingProjectListeners);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInitializingProjectListener(InitializingProjectListener initializingProjectListener) {
        if (initializingProjectListener == null) {
            throw new IllegalArgumentException("initializingProjectListeners argument can't be null");
        }
        initializingProjectListeners.add(initializingProjectListener);
        log.info("{} listener is registered", initializingProjectListener.getClass());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllInitializingProjectListeners() {
        initializingProjectListeners.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeInitializingProjectListener(InitializingProjectListener initializingProjectListener) {
        if (initializingProjectListener == null) {
            throw new IllegalArgumentException("initializingProjectListener argument can't be null");
        }
        boolean result = initializingProjectListeners.remove(initializingProjectListener);
        if (result) {
            log.info("{} listener is unregistered", initializingProjectListener.getClass());
        } else {
            log.warn("{} listener wasn't unregistered!!! The listener wasn't registered", initializingProjectListener.getClass());
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
                log.warn("Listener invocation error!", e);
            }
        }

        // Invoke module loader listeners
        for (InitializingModuleListener listener : getInitializingModuleListeners()) {
            for (Module module : projectDescriptor.getModules()) {
                try {
                    listener.afterModuleLoad(module);
                } catch (Exception e) {
                    log.error("Listener invocation failed!", e);
                }
            }
        }
        return projectDescriptor;
    }
}
