package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatDomain;
import com.exigen.ie.constrainer.FloatVar;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalFloatInstantiate;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Undo;
import com.exigen.ie.constrainer.Undoable;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;
/**
 * A generic implementation of the FloatVar interface.
 */
public class FloatVarImpl extends FloatExpImpl implements FloatVar
{
  private FloatDomain	_domain;

  private FloatDomainHistory _history;

  public FloatVarImpl(Constrainer constrainer, double min, double max, String name)
  {
    super(constrainer,name);
    _domain = new FloatDomainImpl(this,min,max);
    _history = new FloatDomainHistory(this);
  }

  public FloatDomainHistory history()
  {
    return _history;
  }

  public void propagate() throws Failure
  {
    _history.propagate();
  }

  public Goal instantiate()
  {
    return new GoalFloatInstantiate(this);
  }

  public double max()
  {
    return _domain.max();
  }

  public double min()
  {
    return _domain.min();
  }

  public void setMax(double max) throws Failure
  {
//    Debug.print(this+".setMax("+max+")");
    if (_domain.setMax(max))
    {
      _history.setMax(max);
      addToPropagationQueue();
    }
  }

  public void setMin(double min) throws Failure
  {
//    Debug.print(this+".setMin("+min+")");
    if (_domain.setMin(min))
    {
      _history.setMin(min);
      addToPropagationQueue();
    }
  }

  public void setValue(double value) throws Failure
  {
//    Debug.print(this+".setValue("+value+")");
    if (_domain.setValue(value))
    {
      _history.setMin(value);
      _history.setMax(value);
      addToPropagationQueue();
    }
  }

  public double size()
  {
    return _domain.size();
  }

  public Undo createUndo()
  {
    _history.saveUndo();
    return UndoFloatVar.getFloatVarUndo();
  }

  public void forceMin(double min)
  {
    _domain.forceMin(min);
  }

  public void forceMax(double max)
  {
    _domain.forceMax(max);
  }


  /**
   * Undo for FloatVar.
   */
  static class UndoFloatVar extends SubjectImpl.UndoSubject
  {
      static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new UndoFloatVar();
        }

    };

    static UndoFloatVar getFloatVarUndo()
    {
      return (UndoFloatVar) _factory.getElement();
    }

    private int _history_index;

    public void undoable(Undoable u)
    {
      super.undoable(u);
      FloatVarImpl floatvar = (FloatVarImpl) u;
      _history_index = floatvar.history().currentIndex();
//      System.out.println("++ SAVE: " + floatvar + " index:" + _history_index);
    }

    /**
     * Execute undo() operation for this UndoFloatVar object.
     */
    public void undo()
    {
      FloatVarImpl floatvar = (FloatVarImpl)undoable();
//        System.out.println("++ Undo: " + floatvar + " index:" + _history_index);
      floatvar.history().restore(_history_index);
      super.undo();
//      System.out.println("-- Undo: " + floatvar + " index:" + _history_index);
    }

    /**
     * Returns a String representation of this object.
     * @return a String representation of this object.
     */
    public String toString()
    {
      return "UndoFloatVar "+undoable();//+": min="+_min+" max="+_max;
    }

  } // ~UndoFloatVar

  public boolean isLinear(){
    return true;
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    Double coef = (Double)map.get(this);
    if (coef == null){
      map.put(this, new Double(factor));
    }
    else{
      map.put(this, new Double(factor + coef.doubleValue()));
    }
    return 0;
  }
  public double value () throws Failure {
    if (!bound ()) {
      constrainer ().fail ("Attempt to get value of the unbound float expresion " + this);
    }
    return (min() + max ()) / 2;
  }
} // ~FloatVar
