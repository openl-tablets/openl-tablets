/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.HashMap;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.util.print.NicePrinter;
import org.openl.util.print.NicePrinterAdaptor;

/**
 * @author snshor
 *  
 */
public class DynamicObject implements IDynamicObject
{

  protected IOpenClass type;

  protected HashMap<String, Object> fieldValues = new HashMap<String, Object>();

  public DynamicObject(IOpenClass type)
  {
    this.type = type;
  }
  
  /*
   *  Added to support deployment of OpenL project as web services
   */
  
  public DynamicObject()
  {
  }
  
  /*
   *  Added to support deployment of OpenL project as web services
   */
  
  public void setType(IOpenClass type)
  {
      this.type = type;
  }
  
  public boolean containsField(String name)
  {
  	return fieldValues.containsKey(name);
  }
  
  

  /**
   * @return
   */
  public IOpenClass getType()
  {
    return type;
  }

  public Object getFieldValue(String name)
  {
    return fieldValues.get(name);
  }

  public void setFieldValue(String name, Object value)
  {
    fieldValues.put(name, value);
  }

  /**
   *  
   */

  public String toString()
  {
    NicePrinter printer = new NicePrinter();
    printer.print(this, getNicePrinterAdaptor());
    return printer.getBuffer().toString();
  }

  static public NicePrinterAdaptor getNicePrinterAdaptor()
  {
    return new DONIcePrinterAdaptor();	
  }

  static class DONIcePrinterAdaptor extends NicePrinterAdaptor
  {

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.commons.print.NicePrinterAdaptor#printObject(java.lang.Object,
     *      int, org.openl.commons.print.NicePrinter)
     */
    public void printObject(Object obj, int newID, NicePrinter printer)
    {
      if (obj instanceof IDynamicObject)
      {
        IDynamicObject dobj = (IDynamicObject) obj;
        printReference(dobj, newID, printer);
//        printer.getBuffer().append(shortTypeName(dobj.getType().getName()));
        printMap(dobj.getFieldValues(), null, printer);
        return;
      }

      super.printObject(obj, newID, printer);
    }
    protected String getTypeName(Object obj)
    {
      if (obj instanceof DynamicObject)
        return ((DynamicObject)obj).getType().getName();
      return super.getTypeName(obj);
    }
  }
  
  
  protected boolean isMyField(String name)
  {
	return type.getField(name) != null;
  }

public HashMap<String, Object> getFieldValues() {
	return fieldValues;
} 

}
