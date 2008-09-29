/*
 * Created on Apr 1, 2004
 *  
 * 
 * Developed by OpenRules, Inc. 2003, 2004
 *   
 */
package org.openl.util.print;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author snshor
 *  
 */
public class NicePrinter
{
	
	static public String print(Object obj)
	{
		StringBuffer buf = new StringBuffer(100);
		print(obj, buf);
		return buf.toString();
	}
	
	static public void print(Object obj, StringBuffer buf)
	{
		NicePrinter np = new NicePrinter(buf);
		np.print(obj, new NicePrinterAdaptor());
	} 

  int identStep = 2;

  StringBuffer buffer = null;

  int ident = 0;

  HashMap printedObjects = new HashMap();

  int printedID = 0;
  
  public NicePrinter()
  {
  	this(new StringBuffer(100));
  }
  
  public NicePrinter(StringBuffer buf)
  {
  	this.buffer = buf;
  }
  


  public void startNewLine()
  {
    buffer.append('\n');
    for(int i = 0; i < ident; ++i)
      for (int j = 0; j < identStep; j++)
    	buffer.append(' ');
  }
  
  public void incIdent()
  {
    ++ident;
  }
  
  public void decIdent()
  {
    --ident;
  }	
  	
 
 
  	

  public void print(Object obj, NicePrinterAdaptor adaptor)
  {
  	if (obj == null)
  	{
  	  adaptor.printNull(this);
  	  return;
  	}  
  
    if (adaptor.isPrimitive(obj))
    {  
      adaptor.printPrimitive(obj, this);
      return;
    }  
    
    
    Integer existingID = (Integer) printedObjects.get(obj);
    if (existingID != null)
    {
      adaptor.printReference(obj, existingID.intValue(), this);
      return;
    }
    
    int newID = printedID++;
    
    printedObjects.put(obj, new Integer(newID));
    
    if (obj instanceof  Map)
    {
    	adaptor.printMap((Map)obj, null, this);
    	return;
    }
    
    if (obj instanceof Collection)
    {
      adaptor.printCollection((Collection)obj, newID, this);
      return;
    }
    
    
    
    if (obj.getClass().isArray())
    {
      adaptor.printArray(obj, newID, this);
      return;
    }
    

    
	adaptor.printObject(obj, newID, this);
    
  }
  
  static public String getTypeName(Object obj)
  {
    return obj.getClass().getName();
  }

  /**
   * @return Returns the buffer.
   */
  public StringBuffer getBuffer()
  {
    return buffer;
  }
  
  public interface PrintableObject
  {
    void print(NicePrinter printer);
  }
  
}
