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

  IOpenClass type;

  HashMap fields = new HashMap();

  public DynamicObject(IOpenClass type)
  {
    this.type = type;
  }
  
  
  public boolean containsField(String name)
  {
  	return fields.containsKey(name);
  }
  
  

  /**
   * @return
   */
  public IOpenClass getType()
  {
    return type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openl.types.IDynamicObject#getFieldValue(java.lang.String)
   */
  public Object getFieldValue(String name)
  {
    return fields.get(name);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openl.types.IDynamicObject#setFieldValue(java.lang.String,
   *      java.lang.Object)
   */
  public void setFieldValue(String name, Object value)
  {
    fields.put(name, value);
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

  public NicePrinterAdaptor getNicePrinterAdaptor()
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
      if (obj instanceof DynamicObject)
      {
        DynamicObject dobj = (DynamicObject) obj;
        printReference(dobj, newID, printer);
//        printer.getBuffer().append(shortTypeName(dobj.getType().getName()));
        printMap(dobj.fields, null, printer);
        return;
      }

      super.printObject(obj, newID, printer);
    }
    /* (non-Javadoc)
     * @see org.openl.commons.print.NicePrinterAdaptor#getTypeName(java.lang.Object)
     */
    protected String getTypeName(Object obj)
    {
      if (obj instanceof DynamicObject)
        return ((DynamicObject)obj).getType().getName();
      return super.getTypeName(obj);
    }
  }
  

}
