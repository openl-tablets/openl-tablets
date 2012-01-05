/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.data.DataBase;
import org.openl.rules.data.IDataBase;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.MethodKey;

/**
 * @author snshor
 * 
 */
public class XlsModuleOpenClass extends ModuleOpenClass {

    private IDataBase dataBase = new DataBase();
	
	public XlsModuleOpenClass(IOpenSchema schema, String name, XlsMetaInfo metaInfo, OpenL openl) {
        this(schema, name, metaInfo, openl, null);
	}
	
	/**
	 * Constructor for module with dependent modules
	 *
	 */
	public XlsModuleOpenClass(IOpenSchema schema, String name, XlsMetaInfo metaInfo, OpenL openl, 
          Set<CompiledOpenClass> usingModules) {
	    super(schema, name, openl, usingModules);
	    this.metaInfo = metaInfo;
	}
	
	
    // TODO: should be placed to ModuleOpenClass
	public IDataBase getDataBase() {
		return dataBase;
	}
	
	public XlsMetaInfo getXlsMetaInfo() {
		return (XlsMetaInfo) metaInfo;
	}
	
	/**
	 * Adds method to <code>XlsModuleOpenClass</code>.
	 * 
	 * @param method
	 *            method object
	 */
	@Override
	public void addMethod(IOpenMethod method) {
	    if(method instanceof OpenMethodDispatcher){
	        addDispatcherMethod((OpenMethodDispatcher)method);
            return;
	    }
		
		// Get method key.
		//
		MethodKey key = new MethodKey(method);
		
		Map<MethodKey, IOpenMethod> methods = methodMap();
		
		// Checks that method aleready exists in method map. If it already
		// exists then "overload" it using decorator; otherwise - just add to
		// method map.
		//
		if (methods.containsKey(key)) {
			
			// Gets the existed method from map.
			// 
			IOpenMethod existedMethod = methods.get(key);
			
            if (!existedMethod.getType().equals(method.getType())) {
                throw new DuplicatedMethodException(
                    String.format("Method \"%s\" with return type \"%s\" has already been defined with another return type (\"%s\")",
                        method.getName(), method.getType().getDisplayName(0), existedMethod.getType().getDisplayName(0)), method);
            }
			
			// Checks the instance of existed method. If it's the
			// OpenMethodDecorator then just add the method-candidate to
			// decorator; otherwise - replace existed method with new instance
			// of OpenMethodDecorator for existed method and add new one.
			//
			if (existedMethod instanceof OpenMethodDispatcher) {
				OpenMethodDispatcher decorator = (OpenMethodDispatcher) existedMethod;
				decorator.addMethod(method);
		} else {
                boolean differentVersionsOfTheTable = false;
                if (existedMethod instanceof ExecutableRulesMethod && method instanceof ExecutableRulesMethod) {
                    DimensionPropertiesMethodKey existedMethodPropertiesKey = new DimensionPropertiesMethodKey((ExecutableRulesMethod) existedMethod);
                    DimensionPropertiesMethodKey newMethodPropertiesKey = new DimensionPropertiesMethodKey((ExecutableRulesMethod) method);
                    differentVersionsOfTheTable = newMethodPropertiesKey.equals(existedMethodPropertiesKey);
                }
                if (differentVersionsOfTheTable) {
                    useActiveOrNewerVersion((ExecutableRulesMethod) existedMethod, (ExecutableRulesMethod) method, key);
                } else {
                    createMethodDispatcher(method, key, existedMethod);
                }
			}
		} else {
			
			// Just add original method.
			//
			methodMap().put(key, method);
		}
	}

    /**
     * Dispatcher method should be added by adding all candidates of the
     * specified dispatcher to current XlsModuleOpenClass(it will cause adding
     * methods to dispatcher of current module or creating new dispatcher in
     * current module).
     * 
     * Previously there was problems because dispatcher from dependency was
     * either added to dispatcher of current module(dispatcher as a candidate in
     * another dispatcher) or added to current module and was modified during
     * the current module processing.
     * FIXME
     * 
     * @param dispatcher Dispatcher methods to add.
     */
    public void addDispatcherMethod(OpenMethodDispatcher dispatcher) {
        for (IOpenMethod candidate : dispatcher.getCandidates()) {
            addMethod(candidate);
        }
    }

    /**
     * In case we have several versions of one table we should add only the
     * newest or active version of table.
     * 
     * @param method The methods that we are trying to add.
     * @param key Method key of these methods based on signature.
     * @param existedMethod The existing method.
     */
    public void useActiveOrNewerVersion(ExecutableRulesMethod existedMethod,
            ExecutableRulesMethod newMethod,
            MethodKey key) {
        if (new TableVersionComparator().compare(existedMethod, newMethod) < 0) {
            methodMap().put(key, existedMethod);
        } else {
            methodMap().put(key, newMethod);
        }
    }

    /**
     * In case we have two methods overloaded by dimensional properties we
     * should create dispatcher.
     * 
     * @param method The methods that we are trying to add.
     * @param key Method key of these methods based on signature.
     * @param existedMethod The existing method.
     */
    public void createMethodDispatcher(IOpenMethod method, MethodKey key, IOpenMethod existedMethod) {
        // Create decorator for existed method.
        //
        OpenMethodDispatcher decorator;
        if (OpenLSystemProperties.isJavaDispatchingMode()) {
            decorator = new MatchingOpenMethodDispatcher(existedMethod, this);
        } else {
            decorator = new OverloadedMethodsDispatcherTable(existedMethod, this);
        }

        // Add new method to decorator as candidate.
        //
        decorator.addMethod(method);

        // Replace existed method with decorator using the same key.
        //
        methodMap().put(key, decorator);
    }    

    @Override
	public void clearOddDataForExecutionMode() {
	    super.clearOddDataForExecutionMode();
	    dataBase = null;
    }
}
