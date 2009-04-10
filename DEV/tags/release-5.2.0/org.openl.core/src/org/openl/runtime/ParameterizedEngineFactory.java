package org.openl.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author snshor
 *
 * This class provides a base class for "parameterized engine invocations" -
 * i.e. cases when Request has to be dispatched to different engine based on
 * request parameters. The examples could be engine with different rulesets
 * depending on effective date or LOB. It is not feasible to maintain all the
 * logic in one engine - it is better to have multiple engines and separate
 * dispatching logic from business logic. In this case this is done through
 * class {@link IEngineRequestHandler} that creates
 * {@link EngineFactoryDefinition} based on the request R.
 *
 *
 * @param <T>
 * @param <R>
 */

public class ParameterizedEngineFactory<T, R> {
    protected IEngineRequestHandler<R> handler;
    protected Class<T> engineInterface;
    protected String openlName;

    Map<EngineFactoryDefinition, EngineFactory<T>> factoryMap = new HashMap<EngineFactoryDefinition, EngineFactory<T>>();

    public ParameterizedEngineFactory(String openlName, IEngineRequestHandler<R> handler, Class<T> engineInterface) {
        this.openlName = openlName;
        this.handler = handler;
        this.engineInterface = engineInterface;
    }

    public synchronized EngineFactory<T> getRuleEngineFactory(R request) {

        EngineFactoryDefinition factoryDef = handler.makeDefinition(request);

        EngineFactory<T> factory = factoryMap.get(factoryDef);

        if (factory != null) {
            return factory;
        }

        factory = makeFactory(factoryDef);

        factoryMap.put(factoryDef, factory);

        return factory;

    }

    protected EngineFactory<T> makeFactory(EngineFactoryDefinition factoryDef) {
        return new EngineFactory<T>(openlName, factoryDef, engineInterface);
    }

}
