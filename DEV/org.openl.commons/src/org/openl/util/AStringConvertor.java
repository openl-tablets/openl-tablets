/*
 * Created on Mar 29, 2004
 *  
 * 
 * Developed by OpenRules, Inc. 2003, 2004
 *   
 */
package org.openl.util;


/**
 * @author snshor
 *
 */
public abstract class AStringConvertor implements IConvertor
{

    public Object convert(Object obj)
    {
        return getStringValue(obj);
    }
    
    public abstract String getStringValue(Object obj);
    

}
