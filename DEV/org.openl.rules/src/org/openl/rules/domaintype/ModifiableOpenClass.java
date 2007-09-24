package org.openl.rules.domaintype;

import java.util.HashMap;
import java.util.Iterator;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.OpenClassDelegator;

public class ModifiableOpenClass extends OpenClassDelegator
{

	
	HashMap<String, IOpenField> modifiedFields = new HashMap<String, IOpenField>();
	
	public ModifiableOpenClass(IOpenClass baseClass)
	{
		super(baseClass.getName(),baseClass, baseClass.getMetaInfo());
	}

	
	public void addField(IOpenField f)
	{
		modifiedFields.put(f.getName(), f);
	}


	@Override
	public Iterator fields()
	{
		// TODO Auto-generated method stub
		return super.fields();
	}


	@Override
	public IOpenField getField(String name)
	{
		IOpenField f = modifiedFields.get(name);
		return f != null ? f : super.getField(name);
	}


	@Override
	public IOpenField getIndexField()
	{
		// TODO Auto-generated method stub
		return super.getIndexField();
	}
	
	
}
