/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.data.DataBase;
import org.openl.rules.data.IDataBase;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
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
		super(schema, name, openl);
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
				
				// Create decorator for existed method.
				//
				OpenMethodDispatcher decorator = new MatchingOpenMethodDispatcher(existedMethod, this);
				
				// Add new method to decorator as candidate.
				//
				decorator.addMethod(method);
				
				// Replace existed method with decorator using the same key.
				//
				methodMap().put(key, decorator);
			}
		} else {
			
			// Just add original method.
			//
			methodMap().put(key, method);
		}
	}

	@Override
	public void clearOddDataForExecutionMode() {
	    super.clearOddDataForExecutionMode();
	    dataBase = null;
    }

}
