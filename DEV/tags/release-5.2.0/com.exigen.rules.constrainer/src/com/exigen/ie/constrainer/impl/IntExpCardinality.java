package com.exigen.ie.constrainer.impl;
import java.util.Vector;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.FastVector;

/**
 * An implementation of the expression: <code>cardinality(IntExpArray)</code>.
 */
public final class IntExpCardinality extends IntVarImpl
{
  private FastVector	       _vars;
  private int                _card_value;
  private IntVar             _possible_required;

  static final private int[] event_map = { MIN, MAX | VALUE,
                                           MAX, MAX | VALUE,
                                           VALUE, MIN | VALUE | MAX,
                                     //      REMOVE, MAX | VALUE
                                       };


  /**
   * This is an Observer for min/max events on _possible_required variable:
   * _possible_required.min() <= this.min() <= this.max() <= _possible_required.max()
   */
  class ObserverPossibleRequired extends Observer
  {

    public void update(Subject var, EventOfInterest interest)
      throws Failure
    {
//      Debug.on();Debug.print("ObserverPossibleRequired "+interest);Debug.off();
      IntEvent event = (IntEvent)interest;
      if (event.isMinEvent())
      {
        setMin(event.min());
      }
      if (event.isMaxEvent())
      {
        setMax(event.max());
      }

      if (_possible_required.min() == max())
      {
        // remove _card_value from all unbounds
        for(int i=0; i<_vars.size(); i++)
        {
          IntExp vari = (IntVar)_vars.elementAt(i);
          if (!vari.bound())
            vari.removeValue(_card_value);
        }
      }

    }

    public int subscriberMask()
    {
      return EventOfInterest.MIN | EventOfInterest.MAX;
    }

    public String toString()
    {
      return "ObserverPossibleRequired";
    }

    public Object master()
    {
      return IntExpCardinality.this;
    }


  } //~ ObserverPossibleRequired


  public void decMax() throws Failure
  {
    _possible_required.setMax(_possible_required.max() - 1);
  }

  public void incMin() throws Failure
  {
    _possible_required.setMin(_possible_required.min() + 1);
  }

  /**
   * This is an observer to propagate value events on the cardinality to _possible_required
   */
  class ObserverCardValue extends Observer
  {
    public void update(Subject var, EventOfInterest interest)
      throws Failure
    {
//      Debug.on();Debug.print("ObserverCardValue("+var+") "+interest);Debug.off();
      IntEvent e = (IntEvent)interest;
      int cardinality = e.max();
      if (_possible_required.min() == cardinality)
      { // remove _value from all unbounds
        for(int i=0; i<_vars.size(); i++)
        {
          IntExp vari = (IntVar)_vars.elementAt(i);
          if (!vari.bound())
            vari.removeValue(_card_value);
        }
      }
    }

    public int subscriberMask()
    {
      return EventOfInterest.MAX;
    }

    public String toString()
    {
      return _name+"(ObserverCardValue)";
    }

    public Object master()
    {
      return IntExpCardinality.this;
    }



  } //~ ObserverCardValue


  public IntExpCardinality(Constrainer constrainer, Vector vars, int card_value) throws Failure
  {
    this(constrainer, new FastVector(vars), card_value);
  }

  public IntExpCardinality(Constrainer constrainer, FastVector vars, int card_value) throws Failure
  {
//		super(constrainer,0,vars.size(),"C" + card_value, IntVarImplTrace.TRACE_ALL);
    super(constrainer,0,vars.size(),"C" + card_value, DOMAIN_PLAIN);
    _card_value = card_value;
    int size = vars.size();
    _vars = vars;

    int possible_instances = 0;
    int required_instances = 0;
    for(int i=0; i<size; i++)
    {
      IntExp exp = (IntExp)vars.elementAt(i);
      if (exp.contains(card_value))
      {
        possible_instances++;
        if (exp.bound())
          required_instances++;
      }
    }
    try
    {
      setMax(possible_instances);
      setMin(required_instances);

//      int trace =  IntVarImplTrace.TRACE_ALL;
      int trace =  0;

      _possible_required = constrainer().addIntVarTraceInternal(
          required_instances,possible_instances,"PR" + card_value,IntVar.DOMAIN_PLAIN, trace);
      _possible_required.attachObserver(new ObserverPossibleRequired());
      //constrainer().trace(_possible_required);
      //constrainer().trace(this);
    }
    catch(Failure f)
    {
      Constrainer.abort("invalid cardinality parameters");
    }

    this.attachObserver(new ObserverCardValue());

  }

  public String toString()
  {
    return super.toString() + ":" + _possible_required;
  }

} // ~IntExpCardinality
