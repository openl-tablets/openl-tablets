/**
 * Created Dec 1, 2006
 */
package org.openl.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */

public class BindingDependencies
{
	
  HashMap types = new HashMap();
  HashMap methods = new HashMap();
  HashMap fields = new HashMap();
  HashMap assigned = new HashMap();
	
	
	public void addTypeDependency(IOpenClass type, IBoundNode node)
	{
		types.put(type, node);
	}

	public void addMethodDependency(IOpenMethod method, IBoundNode node)
	{
		methods.put(method, node);
	}

	public void addFieldDependency(IOpenField field, IBoundNode node)
	{
		fields.put(node, field);
	}
	
	
	public void addAssign(IBoundNode target, IBoundNode node)
	{
		target.updateAssignFieldDependency(this);
	}

	
	public void addAssignField(IOpenField field, IBoundNode node)
	{
		assigned.put(field, node);
	}
	
	
	
	
	public void visit(IBoundNode node)
	{
		if (node == null)
			return;
		node.updateDependency(this);
		visit(node.getTargetNode());
		IBoundNode[] ch = node.getChildren();
		if (ch == null)
		{
			System.out.println("null");
		}	
		for (int i = 0; i < ch.length; i++)
		{
			visit(ch[i]);
		}
		
	}

	public String toString()
	{
		return 
			"Fields:\n" + setToString(fields.keySet()) +
			"\nMethods:\n" + setToString(methods.keySet()) +
			"\nTypes:\n" + setToString(types.keySet()) +
			"\nAssignes:\n" + setToString(assigned.keySet())
			
		;
	}
	
	String setToString(Set set)
	{
		return set.toString();
	}

	public Set getAssigned()
	{
		return this.assigned.keySet();
	}

	public Map getAssignedMap()
	{
		return this.assigned;
	}

	public Set getFieldNodes()
	{
		return this.fields.keySet();
	}

	public Map getFieldsMap()
	{
		return this.fields;
	}

	public Set getMethods()
	{
		return this.methods.keySet();
	}

	public Map getMethodsMap()
	{
		return this.methods;
	}
	
	public Set getTypes()
	{
		return this.types.keySet();
	}

	public Map getTypesMap()
	{
		return this.types;
	}

	
}
