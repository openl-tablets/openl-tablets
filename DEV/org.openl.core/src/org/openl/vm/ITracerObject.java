/**
 * Created Dec 3, 2006
 */
package org.openl.vm;

import java.util.ArrayList;
import java.util.Iterator;

import org.openl.base.INamedThing;
import org.openl.util.AOpenIterator;
import org.openl.util.ITreeElement;

/**
 * @author snshor
 *
 */
public interface ITracerObject extends ITreeElement, INamedThing
{

	static final public ITracerObject[] EMPTY = {};
	
	public String getUri();
	
	
	public Object getTraceObject();
	
	public ITracerObject[] getTracerObjects();
	
	public void addChild(ITracerObject child);
	
	
	public static abstract class SimpleTracerObject implements ITracerObject
	{
		Object traceObject;
		
		ArrayList children;
		
		public SimpleTracerObject()
		{
		}
		
		public SimpleTracerObject(Object traceObject)
		{
			this.traceObject = traceObject;
		}

		public Object getTraceObject()
		{
			return this.traceObject;
		}
		public abstract String getUri();
		
		public ITracerObject[] getTracerObjects()
		{
			return children == null ?  EMPTY : (ITracerObject[])children.toArray(EMPTY);
		}
		
		public void addChild(ITracerObject child)
		{
			if (children == null)
				children = new ArrayList();
			children.add(child);
		}
		
		public Iterator getChildren()
		{
			return children == null ? AOpenIterator.EMPTY : children.iterator();
		}
		
		public Object getObject()
		{
			return this;
		}

		public boolean isLeaf()
		{
			return children == null;
		}



		public String getName()
		{
			return getDisplayName(INamedThing.SHORT);
		}
		
		
		
		
	}
	
	
}
