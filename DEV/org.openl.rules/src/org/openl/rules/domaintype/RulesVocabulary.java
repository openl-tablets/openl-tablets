package org.openl.rules.domaintype;

import java.util.ArrayList;
import java.util.HashMap;

import org.openl.binding.FieldNotFoundException;
import org.openl.binding.impl.BoundError;
import org.openl.meta.IVocabulary;
import org.openl.meta.StringValue;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class RulesVocabulary implements IVocabulary
{

	HashMap<String, IOpenClass> newTypes = new HashMap<String, IOpenClass>();

	public IOpenClass[] getVocabularyTypes() throws BoundError
	{
		ArrayList<IOpenClass> list = makeBaseTypes();

		makeDomainAttributes(list);
		
		return list.toArray(IOpenClass.EMPTY);
	}

	ArrayList<IOpenClass> makeBaseTypes()
	{

		ArrayList<IOpenClass> list = new ArrayList<IOpenClass>();
		DomainCreator[] dc = getDomains();

		for (int i = 0; i < dc.length; i++)
		{
			IOpenClass newType = dc[i].makeDomain();
			list.add(dc[i].makeDomain());
			newTypes.put(newType.getName(), newType);
		}
		
		return list;
	}


	void makeDomainAttributes(ArrayList<IOpenClass> list) throws BoundError
	{
		DomainAttribute[] attributes = getAttributes();
		
		for (int i = 0; i < attributes.length; i++)
		{
			
			StringValue fieldName = attributes[i].getName();
			IOpenField field = attributes[i].getBase().getField(fieldName.getValue());
			if (field == null)
			{
				try
				{
					throw new FieldNotFoundException("Can not find attribute", fieldName.getValue());
				} catch (FieldNotFoundException e)
				{
					throw new BoundError( e, fieldName.asSourceCodeModule());
				}
			}
			
			StringValue typeName = attributes[i].getNewType();
			
			IOpenClass newType = newTypes.get(typeName.getValue());
			
//TODO add ability to access external types, not only the ones defined in here (Excel), for example by adding another IOpenClass field			
			if (newType == null)
			{
				try
				{
					throw new Exception("Type not found: " + typeName);
				} catch (Exception e)
				{
					throw new BoundError( e, typeName.asSourceCodeModule());
				}
				
			}
				
//TODO check field type correctness
			
			
			
			String baseName = attributes[i].getBase().getName();
			
			ModifiableOpenClass modifiedBase = (ModifiableOpenClass)newTypes.get(baseName);
			
			if (modifiedBase == null)
			{
				modifiedBase = new ModifiableOpenClass(attributes[i].getBase());
				newTypes.put(baseName, modifiedBase);
				list.add(modifiedBase);
			}	
			
			ModifiedField mf = new  ModifiedField(field, newType);
			
			modifiedBase.addField(mf);
			
			
			
		}
	}
	
	
	public DomainAttribute[] getAttributes()
	{
		return new DomainAttribute[]{};
	}/**
	 * Needs to be overridden by subclasses
	 * 
	 * @return
	 */
	public DomainCreator[] getDomains()
	{
		return new DomainCreator[] {};
	}

}
