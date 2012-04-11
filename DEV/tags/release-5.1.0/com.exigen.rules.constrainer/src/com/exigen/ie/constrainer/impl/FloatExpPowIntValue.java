package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.FloatVar;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
/**
 * An implementation of the expression: <code>pow(FloatExp,value)</code> for the intgeger value.
 * Value assumed to be integer and > 0.
 *  <p>
 *  Examples:
 *  <pre>
 *  FloatVar var = constrainer.addFloatVar(min,max,name);
 *  FloatExp exp = var.pow(3);
 *  </pre>
 */
public final class FloatExpPowIntValue extends FloatExpImpl
{
  private FloatExp	         _exp;
  private int                _value;
  private FloatVar           _result;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN | MAX, MIN,
                                           MIN | MAX, MAX,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                          };


  class FloatExpPowIntValueObserver extends ExpressionObserver
  {

    FloatExpPowIntValueObserver()
    {
      super(event_map);
    }

    public int subscriberMask()
    {
      return MIN | MAX | VALUE;
    }

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
//      FloatEvent e = (FloatEvent) event;

      double min = _exp.min(), max = _exp.max();
      _result.setMin(calc_min(min,max,_value));
      _result.setMax(calc_max(min,max,_value));
    }


    public String toString()
    {
      return "FloatExpPowIntValueObserver: "+"pow("+_exp+","+_value+")";
    }

    public Object master()
    {
      return FloatExpPowIntValue.this;
    }
  } //~ FloatExpPowIntValueObserver

  public FloatExpPowIntValue(FloatExp exp, int value)
  {
    super(exp.constrainer(),"");//"pow("+exp.name()"+","+value+")");
    _exp = exp;
    _value = value;
    _observer = new FloatExpPowIntValueObserver();
    _exp.attachObserver(_observer);
    double min = _exp.min(), max = _exp.max();
    int trace = 0;
    _result = constrainer().addFloatVarTraceInternal(
                calc_min(min,max,_value), calc_max(min,max,_value), "pow", trace);
  }

  static double calc_min(double min, double max, int value)
  {
    if(value%2 == 0)
    {
      // min >= 0 && max >= 0
      if(min >= 0)
        return Math.pow(min,value);
      // min < 0 && max <= 0
      else if(max <= 0)
        return Math.pow(max,value);
      // min < 0 && max > 0
      else
        return 0;
    }
    else
    {
      return Math.pow(min,value);
    }
  }

  static double calc_max(double min, double max, int value)
  {
    if(value%2 == 0)
    {
      return Math.max(Math.pow(min,value),Math.pow(max,value));
    }
    else
    {
      return Math.pow(max,value);
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

  public double max()
  {
    return _result.max();
  }

  public double min()
  {
    return _result.min();
  }

  public void setMax(double max) throws Failure
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

  public void setMin(double min) throws Failure
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

  public void setValue(double value) throws Failure
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

} // ~FloatExpPowIntValue
