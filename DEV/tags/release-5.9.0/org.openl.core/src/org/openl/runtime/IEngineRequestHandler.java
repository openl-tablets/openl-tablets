package org.openl.runtime;

public interface IEngineRequestHandler<R> {

    EngineFactoryDefinition makeDefinition(R request);

}
