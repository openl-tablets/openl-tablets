/**
 * Created Dec 3, 2006
 */
package org.openl.vm;

import java.io.PrintStream;
import java.util.Stack;

import org.openl.base.INamedThing;
import org.openl.main.SourceCodeURLConstants;

/**
 * @author snshor
 *
 */
public class Tracer
{
	
	static ThreadLocal<Tracer> tracer = new ThreadLocal<Tracer>();
	
	static public boolean isTracerOn()
	{
		return tracer.get() != null;
	}
	
	static public Tracer getTracer()
	{
		return tracer.get();
	}
	
	static public void setTracer(Tracer t)
	{
		tracer.set(t);
	}

	Stack<ITracerObject> stack = new Stack<ITracerObject>();
	

	public void push(ITracerObject obj)
	{
		if (stack.size() == 0)
		{
			addTracerObject(obj);
		}
		else 
		{
			ITracerObject to = (ITracerObject)stack.peek();
			to.addChild(obj);
			obj.setParent(to);
		}

		stack.push(obj);

	}

	/**
	 * 
	 */
	public void pop()
	{
		stack.pop();
	}
	
	
	
	
//	ArrayList list = new ArrayList(100);
	
	void addTracerObject(ITracerObject to)
	{
		root.addChild(to);
	}


	public void reset()
	{
		root = makeRoot();
	}
	
	public ITracerObject[] getTracerObjects()
	{
		
		return root.getTracerObjects();
	}
	
	
	public void print(PrintStream ps)
	{
		ITracerObject[] tt = getTracerObjects();
		for (int i = 0; i < tt.length; i++)
		{
			printTO(tt[i], 0, ps);
		}
	}
	
	
	public void printTO(ITracerObject to, int level, PrintStream ps)
	{
		for (int i = 0; i < level * 2; i++)
		{
			ps.print(' ');
		}
		
		ps.println("TRACE: " + to.getDisplayName(INamedThing.REGULAR));
		ps.println(SourceCodeURLConstants.AT_PREFIX + to.getUri() + "&" + SourceCodeURLConstants.OPENL + "=" );
		
		
		ITracerObject[] tt = to.getTracerObjects();
		
		for (int i = 0; i < tt.length; i++)
		{
			printTO(tt[i], level+1, ps);
		}
		
	}

	
	
	ITracerObject root = makeRoot();

	/**
	 * @return
	 */
	private ITracerObject makeRoot()
	{
		return new ITracerObject.SimpleTracerObject()
		{

			public String getUri()
			{
				return null;
			}

			public String getType()
			{
				return "traceroot";
			}

			public String getDisplayName(int mode)
			{
				return "Trace";
			}
			
		};
	}

	public ITracerObject getRoot()
	{
		return this.root;
	}

	public void setRoot(ITracerObject root)
	{
		this.root = root;
	}
	
}
