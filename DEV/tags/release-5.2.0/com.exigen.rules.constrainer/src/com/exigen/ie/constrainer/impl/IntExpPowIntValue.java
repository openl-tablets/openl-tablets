package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
/**
 * An implementation of the expression: <code>pow(IntExp,value)</code> for the intgeger value.
 * Value assumed to be integer and > 0.
 *  <p>
 *  Examples:
 *  <pre>
 *  IntVar var = constrainer.addIntVar(min,max,name);
 *  IntExp exp = var.pow(3);
 *  </pre>
 */
public final class IntExpPowIntValue extends IntExpImpl
{
  private IntExp	           _exp;
  private int                _value;
  private IntVar             _result;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN | MAX, MIN,
                                           MIN | MAX, MAX,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                          };


  class IntExpPowIntValueObserver extends ExpressionObserver
  {

    IntExpPowIntValueObserver()
    {
      super(event_map);
    }


    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
//      IntEvent e = (IntEvent) event;

      int min = _exp.min(), max = _exp.max();
      _result.setMin(calc_min(min,max,_value));
      _result.setMax(calc_max(min,max,_value));
    }


    public String toString()
    {
      return "IntExpPowIntValueObserver: "+"pow("+_exp+","+_value+")";
    }

    public Object master()
    {
      return IntExpPowIntValue.this;
    }

    public int subscriberMask () {
      return MIN | MAX | VALUE;
    }
  } //~ IntExpPowIntValueObserver

  public IntExpPowIntValue(IntExp exp, int value)
  {
    super(exp.constrainer());
    _exp = exp;
    _value = value;

    if(constrainer().showInternalNames())
    {
      _name = "IlcPower("+exp.name()+","+value+")";
//      _name = "("+exp.name()+"**"+value+")";
    }

    _observer = new IntExpPowIntValueObserver();
    _exp.attachObserver(_observer);
    int min = _exp.min(), max = _exp.max();
    int trace = 0;
    int domain = IntVar.DOMAIN_PLAIN;
    _result = constrainer().addIntVarTraceInternal(
                calc_min(min,max,_value), calc_max(min,max,_value), "pow", domain, trace);
  }

  static int calc_min(int min, int max, int value)
  {
    if(value%2 == 0)
    {
      // min >= 0 && max >= 0
      if(min >= 0)
        return (int)Math.pow(min,value);
      // min < 0 && max <= 0
      else if(max <= 0)
        return (int)Math.pow(max,value);
      // min < 0 && max > 0
      else
        return 0;
    }
    else
    {
      return (int)Math.pow(min,value);
    }
  }

  static int calc_max(int min, int max, int value)
  {
    if(value%2 == 0)
    {
      return Math.max((int)Math.pow(min,value),(int)Math.pow(max,value));
    }
    else
    {
      return (int)Math.pow(max,value);
    }
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public void attachObserver(Observer observer)
  {
    super.attachObserver(observer);
    _result.attachObserver(observer);
  }

  public void reattachObserver(Observer observer)
  {
    super.reattachObserver(observer);
    _result.reattachObserver(observer);
  }

  public void detachObserver(Observer observer)
  {
    super.detachObserver(observer);
    _result.detachObserver(observer);
  }

  public int max()
  {
    return _result.max();
  }

  public int min()
  {
    return _result.min();
  }

  public void setMax(int max) throws Failure
  {
    if((_value%2) == 0)
    {
      if(max < 0)
        constrainer().fail("pow(exp,value).setMax(): max < 0 for even value");
      // ???
    }
    else
    {
      // ???
    }
  }

  public void setMin(int min) throws Failure
  {
    if((_value%2) == 0)
    {
      if (min <= 0)
        return;
      // ???
    }
    else
    {
      // ???
    }
  }

  public void setValue(int value) throws Failure
  {
    setMin(value);
    setMax(value);
  }

  public String toString()
  {
    return "pow(" + _exp + "," + _value + ")" + domainToString();
  }

  public boolean isLinear(){
    return (_exp.isLinear() && ((_value == 1) || (_value == 0)));
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    if (!((_value == 0) || (_value == 1)))
      throw new NonLinearExpression(this);
    if (_value == 0){
      return 1;
    }
    else{
      return _exp.calcCoeffs(map, factor);
    }
  }



} // ~IntExpPowIntValue
