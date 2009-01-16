package com.exigen.ie.constrainer.impl;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Hashtable;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Expression;
import com.exigen.ie.constrainer.ExpressionFactory;
import com.exigen.ie.constrainer.FloatExpArray;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.Undo;
import com.exigen.ie.constrainer.UndoImpl;
import com.exigen.ie.constrainer.Undoable;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * A generic implementation of the ExpressionFactory interface.
 */
public final class ExpressionFactoryImpl extends UndoableOnceImpl implements ExpressionFactory, java.io.Serializable
{
  /**
   * Cached expressions.
   */
  private Hashtable _expressions = new Hashtable();

  /**
   * Use cache to find already created expression.
   */
  private boolean _getFromCache = false;

  /**
   * Use cache to store newly created expression.
   */
  private boolean _putInCache = false;

  /**
   * Use cache to find and store the expression.
   */
  private boolean _useCache = false;

  /**
   * Default constructor.
   */
  public ExpressionFactoryImpl(Constrainer constrainer)
  {
    super(constrainer, ExpressionFactoryImpl.class.getName());
  }

  public boolean useCache()
  {
    return _useCache;
  }

  public void useCache(boolean flag)
  {
    _useCache = flag;
    _getFromCache = flag;
    _putInCache = flag;
  }

  public Expression getExpression(Class clazz, Object[] args)
  {
    return getExpression(clazz, args, args2types(args));
  }

  public Expression getExpression(Class clazz, Object[] args, Class[] types)
  {
    ExpressionKey key = (_getFromCache || _putInCache ? new ExpressionKeyImpl(clazz, args) : null);
    Expression exp = (_getFromCache ? findExpression(key) : null);

    if(exp == null)
    {
      exp = createExpression(clazz, args, types);
//      System.out.println("Creating new expression: " + exp.name());
      if(_putInCache)
      {
        addUndo();
        _expressions.put(key, exp);
      }
    }

    return exp;
  }

  /**
   * Returns the cached expression for a given key.
   * If there is no cached expression returns null.
   */
  Expression findExpression(ExpressionKey key)
  {
    return (Expression)_expressions.get(key);
  }

  /**
   * Creates a new expression for a given class, args, and types.
   */
  Expression createExpression(Class c, Object[] args, Class[] types)
  {
    try
    {
      Constructor constr = c.getConstructor(types);
      constr.setAccessible(true);  // to create not public implementations
      return (Expression)constr.newInstance(args);
    }
    catch(Exception e)
    {
      String msg = "Error creating expression: "
                   + e.getClass().getName()
                   + ": " + e.getMessage() + ": " + c.getName();

      Constrainer.abort(msg, e);
      return null;
    }
  }

  public Undo createUndo()
  {
    return UndoExpressionFactory.getUndo();
  }

  /**
   * Undo Class for UndoExpressionFactory.
   */
  static class UndoExpressionFactory extends UndoImpl
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new UndoExpressionFactory();
        }

    };

    static UndoExpressionFactory getUndo()
    {
      return (UndoExpressionFactory) _factory.getElement();
    }

    private Hashtable _expressions;

    public void undoable(Undoable u)
    {
      super.undoable(u);
      ExpressionFactoryImpl expFactory = (ExpressionFactoryImpl) u;
      _expressions = (Hashtable)expFactory._expressions.clone();
    }

    public void undo()
    {
      ExpressionFactoryImpl expFactory = (ExpressionFactoryImpl) undoable();
      expFactory._expressions = _expressions;
      super.undo();
    }

    /**
     * Returns a String representation of this object.
     * @return a String representation of this object.
     */
    public String toString()
    {
      return "UndoExpressionFactory "+undoable();
    }

  } // ~UndoExpressionFactory

  /**
   * An interface for the unique key of the expression.
   */
  interface ExpressionKey
  {
    /**
     * Returns a class of the expression.
     */
    public Class clazz();

    /**
     * Returns the arguments of the expression.
     */
    public Object[] args();
  }

  /**
   *
   */
  static class ExpressionKeyImpl implements ExpressionKey
  {
    private Class _clazz;
    private Object[] _args;

    /**
     *
     */
    public ExpressionKeyImpl(Class clazz, Object[] args)
    {
      _clazz = clazz;
      _args = args;
    }

    public Class clazz()
    {
      return _clazz;
    }

    public Object[] args()
    {
      return _args;
    }

    public int hashCode()
    {
      return _clazz.hashCode() + _args.length;//???
    }

    // Should use more of the objects' equals() methods
    public boolean equals(Object o)
    {
      if(!(o instanceof ExpressionKey)) return false;

      ExpressionKey key = (ExpressionKey)o;

      // compare classes
      if(_clazz != key.clazz())
        return false;

      // compare argumens
      if(_args.length != key.args().length)
        return false;

      for(int i=0; i < _args.length; i++)
      {
        if( !equalArgs(_args[i], key.args()[i]) )
          return false;
       }

      return true;
    }

    static boolean equalArgs(Object arg1, Object arg2)
    {
      // are references the same?
      if(arg1 == arg2)
        return true; // yes -> equal

      // are classes the same?
      if( arg1.getClass() != arg2.getClass() )
        return false; // not the same -> not equal

      // numbers in Java compare as a class + bit representation
      if(arg1 instanceof Number)
        return arg1.equals(arg2);

      // arrays
      if(arg1 instanceof IntExpArray)
        return equalArrays((IntExpArray)arg1, (IntExpArray)arg2);

      if(arg1 instanceof FloatExpArray)
        return equalArrays((FloatExpArray)arg1, (FloatExpArray)arg2);

      return false;
    }


    static boolean equalNumbers(Number arg1, Number arg2)
    {
      return arg1.equals(arg2);
    }

    static boolean equalArrays(Object[] arg1, Object[] arg2)
    {
      int size;
      if((size=arg1.length) != arg2.length)
        return false;

      for (int i=0; i < size; i++)
      {
        if( !equalArgs(arg1[i], arg2[i]) )
          return false;
      }

      return true;
    }

    static boolean equalArrays(IntExpArray arg1, IntExpArray arg2)
    {
      return equalArrays(arg1.data(), arg2.data());
    }

    static boolean equalArrays(FloatExpArray arg1, FloatExpArray arg2)
    {
      return equalArrays(arg1.data(), arg2.data());
    }

    /**
     * Returns a String representation of this object.
     * @return a String representation of this object.
     */
    public String toString()
    {
      StringBuffer s = new StringBuffer();
      s.append("class: ").append(_clazz.getName()).append(", args:(");
      for(int i=0; i<_args.length; i++)
      {
        if(i!=0) s.append(",");
        s.append(_args[i]);
      }
      s.append(")");
      return new String(s);
    }
  } // ~ExpressionKeyImpl

  /**
   * Returns a constructor with the given parameter types for a given parameter values.
   */
  static Class[] args2types(Object[] args)
  {
    int size = args.length;
    Class[] types = new Class[size];
    for(int i = 0; i < size; i++)
      types[i] = args[i].getClass();

    return types;
  }

  /**
   * Returns a String representation of this object.
   * @return a String representation of this object.
   */
  public String toString()
  {
    StringBuffer s = new StringBuffer();
    Enumeration e = _expressions.keys();
    while(e.hasMoreElements())
    {
      ExpressionKey key = (ExpressionKey)e.nextElement();
      Expression exp = (Expression)_expressions.get(key);
      s.append(exp.getClass().getName() + ", " + System.identityHashCode(exp) + ", ");
      for (int i = 0; i < key.args().length; i++)
      {
        if(i!=0) s.append(", ");
        Object o = key.args()[i];
        if(o instanceof Number)
          s.append(o.getClass().getName() + ", " + o.toString());
        else
          s.append(o.getClass().getName() + ", " + System.identityHashCode(o));
      }
      s.append("\n");
    }
    return new String(s);
  }

} // ~ExpressionFactoryImpl
