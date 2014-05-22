package org.openl.rules.project.instantiation;

import java.util.Map;

import org.openl.rules.project.resolving.ProjectResolvingException;

public interface ProjectEngineFactory<T> {

    T newInstance() throws RulesInstantiationException, ProjectResolvingException, ClassNotFoundException;

    boolean isSingleModuleMode();

    boolean isProvideRuntimeContext();

    Class<?> getInterfaceClass();

    Map<String, Object> getExternalParameters();

}
