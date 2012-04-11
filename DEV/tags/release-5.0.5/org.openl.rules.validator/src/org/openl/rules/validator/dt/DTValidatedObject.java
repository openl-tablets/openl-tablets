/**
 * Created Feb 8, 2007
 */
package org.openl.rules.validator.dt;

import java.util.Map;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTCondition;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntVar;

/**
 * @author snshor
 * 
 */
public class DTValidatedObject implements IDTValidatedObject,
	IConditionTransformer
{

    DecisionTable dt;
    Map<String, IDomainDescriptor> domains;

    public DTValidatedObject(DecisionTable dt,
	    Map<String, IDomainDescriptor> domains)
    {
	this.dt = dt;
	this.domains = domains;
    }

    public DTValidatedObject(DecisionTable dt)
    {
	this.dt = dt;
    }

    public DecisionTable getDT()
    {
	return dt;
    }

    public IConditionSelector getSelector()
    {
	return null;
    }

    public IConditionTransformer getTransformer()
    {
	return this;
    }

    public IOpenClass transformSignatureType(IOpenClass class1,
	    String parameterName, int parameterDirection)
    {
	if (class1 == JavaOpenClass.STRING)
	{
	    return JavaOpenClass.getOpenClass(IntExp.class);
	}

	if (class1 == JavaOpenClass.INT)
	{
	    return JavaOpenClass.getOpenClass(IntExp.class);
	}

	return null;
    }

    public IOpenClass transformParameterType(IParameterDeclaration pd)
    {
	if (pd.getType() == JavaOpenClass.STRING)
	{
	    return JavaOpenClass.INT;
	}
	return null;
    }

    public IntVar makeSignatureVar(String parameterName, IOpenClass class1,
	    Constrainer c)
    {
	IDomainDescriptor domain = domains.get(parameterName);

	if (domain != null)
	{
	    return c.addIntVar(domain.getMin(), domain.getMax(), parameterName);
	}

	return null;

    }

    public Object transformParameterValue(String name, IDTCondition condition,
	    Object value, Constrainer C)
    {
	IDomainDescriptor domain = domains.get(name);

	if (domain != null)
	{
	    return new Integer(domain.getIndex(value));
	}

	return null;
    }

    public Object transformSignatureValueBack(String name, int i)
    {
	IDomainDescriptor domain = domains.get(name);

	if (domain != null)
	{
	    return domain.getValue(i);
	}

	return new Integer(i);
    }

}
