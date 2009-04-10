/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenConfigurationException;
import org.openl.binding.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionsUtil;

/**
 * @author snshor
 * 
 */
public class NameSpacedTypeConfiguration extends AConfigurationElement
{

    String namespace;

    ITypeFactoryConfigurationElement[] factories = {};

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
     */
    public void validate(IConfigurableResourceContext cxt)
	    throws OpenConfigurationException
    {
	for (int i = 0; i < factories.length; i++)
	{
	    factories[i].validate(cxt);
	}
    }

    /**
     * @return
     */
    public String getNamespace()
    {
	return namespace;
    }

    /**
     * @param string
     */
    public void setNamespace(String string)
    {
	namespace = string;
    }

    public IOpenClass getType(String name, IConfigurableResourceContext cxt)
	    throws AmbiguousTypeException
    {
	List<IOpenClass> foundTypes = new ArrayList<IOpenClass>(2);

	for (int i = 0; i < factories.length; i++)
	{
	    IOpenClass type = factories[i].getLibrary(cxt).getType(name);
	    if (type != null)
	    {
		foundTypes.add(type);
	    }
	}

	switch (foundTypes.size())
	{
	case 0:
	    return null;
	case 1:
	    return foundTypes.get(0);
	default:
	    throw new AmbiguousTypeException(name, foundTypes);
	}

    }

    public void addJavaImport(JavaImportTypeConfiguration factory)
    {
	factories = (ITypeFactoryConfigurationElement[]) CollectionsUtil.add(
		factories, factory);
    }

    public void addImport(ImportTypeConfiguration factory)
    {
	factories = (ITypeFactoryConfigurationElement[]) CollectionsUtil.add(
		factories, factory);
    }

    public void addJavaType(JavaTypeConfiguration factory)
    {
	factories = (ITypeFactoryConfigurationElement[]) CollectionsUtil.add(
		factories, factory);
    }

    public void addDynamicTypes(DynamicTypesConfiguration factory)
    {
	factories = (ITypeFactoryConfigurationElement[]) CollectionsUtil.add(
		factories, factory);
    }

    public void addAnyType(GenericTypeLibraryConfiguration glb)
    {
	factories = (ITypeFactoryConfigurationElement[]) CollectionsUtil.add(
		factories, glb);
    }

    public void addSchema(OpenSchemaConfiguration opSchema)
    {
	factories = (ITypeFactoryConfigurationElement[]) CollectionsUtil.add(
		factories, opSchema);

    }

}
