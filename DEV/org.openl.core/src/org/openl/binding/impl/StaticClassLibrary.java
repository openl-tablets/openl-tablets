/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.binding.IOpenLibrary;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass;
import org.openl.util.AOpenIterator;
import org.openl.util.ASelector;
import org.openl.util.IOpenIterator;
import org.openl.util.ISelector;
import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public class StaticClassLibrary implements IOpenLibrary {

    private IOpenClass openClass;

    public StaticClassLibrary() {
    }

    public StaticClassLibrary(IOpenClass openClass) {
        this.openClass = openClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String,
     *      java.lang.String, org.openl.types.IOpenClass[])
     */
    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params) {
        return openClass.getMethod(name, params);
    }

    public IOpenField getVar(String name, boolean strictMatch) {
        return openClass.getField(name, strictMatch);
    }

    
    
    IOpenMethod[] methods;
    
    public Iterator<IOpenMethod> methods() {
    	
    	if (methods == null)
    	{
    		synchronized (this) {
				methods = selectMethods().asList().toArray(new IOpenMethod[0]);
			}
    	}
    	
    	return OpenIterator.fromArray(methods);
    }
    
    
    
    protected IOpenIterator<IOpenMethod> selectMethods() {
        ISelector<IOpenMethod> sel = new ASelector<IOpenMethod>() {
            @Override
            public int redefinedHashCode() {
                return "static".hashCode();
            }

            public boolean select(IOpenMethod m) {
                return m.isStatic();
            }
        };

        return AOpenIterator.select(openClass.getMethods().iterator(), sel);
    }

    public void setOpenClass(IOpenClass c) {
        openClass = c;
    }
    
    Map<String, List<IOpenMethod>> methodNameMap = null;

    
	@SuppressWarnings("unchecked")
	@Override
	public Iterator<IOpenMethod> methods(String name) {
		if (methodNameMap == null)
		{
			synchronized(this)
			{
				methodNameMap = AOpenClass. buildMethodNameMap(methods());
			}
		}	
		
		List<IOpenMethod> found = methodNameMap.get(name);
		
		return found == null ? (Iterator<IOpenMethod>)OpenIterator.EMPTY : found.iterator();
	}
    
}
