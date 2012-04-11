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
	
  HashMap<IOpenClass, IBoundNode> types = new HashMap<IOpenClass, IBoundNode>();
  HashMap<IOpenMethod, IBoundNode> methods = new HashMap<IOpenMethod, IBoundNode>();
  HashMap<IBoundNode,IOpenField> fields = new HashMap<IBoundNode, IOpenField>();
  HashMap<IOpenField, IBoundNode> assigned = new HashMap<IOpenField, IBoundNode>();
	
	
	public synchronized void addTypeDependency(IOpenClass type, IBoundNode node)
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
	
	
	public void addAssign(IBoundNode target, @SuppressWarnings("unused")
	IBoundNode node)
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
	
	String setToString(Set<?> set)
	{
		return set.toString();
	}

	public Set<IOpenField> getAssigned()
	{
		return this.assigned.keySet();
	}

	public Map<IOpenField, IBoundNode> getAssignedMap()
	{
		return this.assigned;
	}

	public Set<IBoundNode> getFieldNodes()
	{
		return this.fields.keySet();
	}

	public Map<IBoundNode,IOpenField> getFieldsMap()
	{
		return this.fields;
	}

	public Set<IOpenMethod> getMethods()
	{
		return this.methods.keySet();
	}

	public Map<IOpenMethod, IBoundNode> getMethodsMap()
	{
		return this.methods;
	}
	
	public Set<IOpenClass> getTypes()
	{
		return this.types.keySet();
	}

	public Map<IOpenClass, IBoundNode> getTypesMap()
	{
		return this.types;
	}

	
}
