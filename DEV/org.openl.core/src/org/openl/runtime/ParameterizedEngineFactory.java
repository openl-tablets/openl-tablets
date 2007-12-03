package org.openl.runtime;

import java.util.HashMap;
import java.util.Map;

public class ParameterizedEngineFactory<T, R>
{
    protected IEngineRequestHandler<R> handler;
    protected Class<T> engineInterface;
    protected String openlName;
    
    public ParameterizedEngineFactory(String openlName, IEngineRequestHandler<R> handler, Class<T> engineInterface)
    {
	this.openlName = openlName;
	this.handler = handler;
	this.engineInterface = engineInterface;
    }
    
    public synchronized EngineFactory<T> getRuleEngineFactory(R request)
    {
	
	EngineFactoryDefinition factoryDef = handler.makeDefinition(request);
	
	EngineFactory<T> factory = factoryMap.get(factoryDef);
	
	if (factory != null)
	    return factory;
	
	
	factory = makeFactory(factoryDef);
	
	factoryMap.put(factoryDef, factory);
	
	return factory;
	
	
    }
    
    Map<EngineFactoryDefinition, EngineFactory<T>> factoryMap = new HashMap<EngineFactoryDefinition, EngineFactory<T>>();
    
    protected EngineFactory<T> makeFactory(EngineFactoryDefinition factoryDef)
    {
	return new EngineFactory<T>(openlName, factoryDef, engineInterface);
    }
    
}
