/*
 * Created on Jun 5, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.HashMap;

import org.openl.binding.AmbiguousMethodException;
import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.conf.Cache;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class ACastFactory implements ICastFactory
{
  IMethodFactory methodFactory;

  static final IOpenCast NO_CAST = new IOpenCast()
  {
    public Object convert(Object from)
    {
      throw new UnsupportedOperationException();
    }
    public int getDistance(IOpenClass from, IOpenClass to)
    {
      throw new UnsupportedOperationException();
    }
    public boolean isImplicit()
    {
      throw new UnsupportedOperationException();
    }

  };

  public ACastFactory()
  {
  }

  public ACastFactory(IMethodFactory methodFactory)
  {
    this.methodFactory = methodFactory;
  }

  public synchronized IOpenCast  getCast(IOpenClass from, IOpenClass to)
  {
    Object key = Cache.makeKey(from, to);

    IOpenCast cast = castCache.get(key);

    if (cast == null)
    {
      cast = findCast(from, to);
      castCache.put(key, cast);
    }

    return cast == NO_CAST ? null : cast;
  }

  /**
   * @link http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html
   *
   * The following conversions are called the narrowing reference conversions:
  
  From any class type S to any class type T, provided that S is a superclass of T. (An important special case is that there is a narrowing conversion from the class type Object to any other class type.) 
  From any class type S to any interface type K, provided that S is not final and does not implement K. (An important special case is that there is a narrowing conversion from the class type Object to any interface type.) 
  From type Object to any array type. 
  From type Object to any interface type. 
  From any interface type J to any class type T that is not final. 
  From any interface type J to any class type T that is final, provided that T implements J. 
  From any interface type J to any interface type K, provided that J is not a subinterface of K and there is no method name m such that J and K both contain a method named m with the same signature but different return types. 
  From any array type SC[] to any array type TC[], provided that SC and TC are reference types and there is a narrowing conversion from SC to TC. 
  
   * @param from
   * @param to
   * @return
   */
  static public boolean allowJavaUpcast(Class<?> from, Class<?> to)
  {
    if (from.isAssignableFrom(to))
      return true;
    
    if (!from.isPrimitive() && to.isInterface())
	return true;
    
    if (!to.isPrimitive() && from.isInterface())
	return true;

    return false;
  }

  public IOpenCast findCast(IOpenClass from, IOpenClass to)
  {

    if (from == NullOpenClass.the || from == JavaOpenClass.OBJECT)
    {
      return to.getInstanceClass().isPrimitive() ? null : JAVA_DOWNCAST;
    }

    if (
       //(from instanceof JavaOpenClass) && 
       (to instanceof JavaOpenClass))
    {
      Class<?> fromClass = from.getInstanceClass();
      Class<?> toClass = to.getInstanceClass();

      if (toClass.isAssignableFrom(fromClass))
        return JAVA_DOWNCAST;

      if (allowJavaUpcast(fromClass, toClass))
        return JAVA_UPCAST;
    }

    IOpenCast ioc = findCast(from, to, methodFactory);
    if (ioc == NO_CAST)
    	ioc = findCast(from, to, from);
    if (ioc == NO_CAST)
    	ioc = findCast(from, to, to);
    return ioc;
    
  }
    
    public IOpenCast findCast(IOpenClass from, IOpenClass to, IMethodFactory mf)
    {
    
    boolean auto = true;
    int distance = 1;
    IMethodCaller castCaller = null;

    try
    {
      castCaller =
        mf.getMatchingMethod(
          "autocast",
          new IOpenClass[] { from, to });
    }
    catch (AmbiguousMethodException ex)
    {
    }

    if (castCaller == null)
    {
      auto = false;
      distance = 2;
      try
      {
        castCaller =
          mf.getMatchingMethod(
            "cast",
            new IOpenClass[] { from, to });
      }
      catch (AmbiguousMethodException ex)
      {
      }
    }

    if (castCaller == null)
      return NO_CAST;

    IMethodCaller distanceCaller = null;
    try
    {
      distanceCaller =
        mf.getMatchingMethod(
          "distance",
          new IOpenClass[] { from, to });
    }
    catch (AmbiguousMethodException ex)
    {
    }

    if (distanceCaller != null)
    {
      distance =
        ((Integer)distanceCaller
          .invoke(null, new Object[] { from.nullObject(), to.nullObject()}, null))
          .intValue();
    }

    return new MethodBasedCast(castCaller, auto, distance, to.nullObject());
  }

  static class MethodBasedCast implements IOpenCast
  {
    IMethodCaller caller;
    boolean implicit;
    int distance;
    Object nullObject;
    MethodBasedCast(
      IMethodCaller caller,
      boolean implicit,
      int distance,
      Object nullObject)
    {
      this.caller = caller;
      this.implicit = implicit;
      this.distance = distance;
      this.nullObject = nullObject;

    }

    /* (non-Javadoc)
    * @see org.openl.types.IOpenCast#convert(java.lang.Object)
    */
    public Object convert(Object from)
    {
      return caller.invoke(null, new Object[] { from, nullObject }, null);
    }

    /* (non-Javadoc)
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass, org.openl.types.IOpenClass)
     */
    public int getDistance(IOpenClass from, IOpenClass to)
    {
      return distance;
    }

    /* (non-Javadoc)
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    public boolean isImplicit()
    {
      return implicit;
    }

  }

  HashMap<Object, IOpenCast> castCache = new HashMap<Object, IOpenCast>();

  /**
   * @return
   */
  public IMethodFactory getMethodFactory()
  {
    return methodFactory;
  }

  /**
   * @param factory
   */
  public void setMethodFactory(IMethodFactory factory)
  {
    methodFactory = factory;
  }

  static class JavaDownCast implements IOpenCast
  {

    public Object convert(Object from)
    {
      return from;
    }

    /* (non-Javadoc)
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass, org.openl.types.IOpenClass)
     */
    public int getDistance(IOpenClass from, IOpenClass to)
    {
      if (from.getInstanceClass() == null)
        return 0;
      return from.getInstanceClass().getSuperclass() == to.getInstanceClass()
        ? 1
        : 2;
    }

    /* (non-Javadoc)
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    public boolean isImplicit()
    {
      return true;
    }

  }

  static class JavaUpCast implements IOpenCast
  {

    public Object convert(Object from)
    {
      return from;
    }

    /* (non-Javadoc)
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass, org.openl.types.IOpenClass)
     */
    public int getDistance(IOpenClass from, IOpenClass to)
    {
      return 1;
    }

    /* (non-Javadoc)
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    public boolean isImplicit()
    {
      return false;
    }

  }

  static final JavaDownCast JAVA_DOWNCAST = new JavaDownCast();
  static final JavaUpCast JAVA_UPCAST = new JavaUpCast();

}
