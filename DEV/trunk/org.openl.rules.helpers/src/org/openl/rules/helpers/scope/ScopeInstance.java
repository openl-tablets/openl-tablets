package org.openl.rules.helpers.scope;

import org.openl.types.impl.DynamicObject;

public class ScopeInstance extends DynamicObject
{

    DynamicObject parent;
    
    public ScopeInstance(Scope scope)
    {
	super(scope);
//	this.parent = parent;
    }

    @Override
    public Object getFieldValue(String name)
    {
	if (isMyField(name))
	{
	    return fieldValues.get(name);
	}    
	
	return parent.getFieldValue(name);
    }
    
    


    
    public Scope getScope()
    {
	return (Scope)type;
    }

    @Override
    public void setFieldValue(String name, Object value)
    {
	if (isMyField(name))
	{
	    fieldValues.put(name, value);
	    return;
	}    
	
	parent.setFieldValue(name, value);
    }
    
    
    
    
    
}
