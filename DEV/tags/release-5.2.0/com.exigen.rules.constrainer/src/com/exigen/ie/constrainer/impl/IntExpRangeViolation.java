package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////

/**
 * An implementation of the expression:
 * <code>((IntExp < rangeMin)*(rangeMin - IntExp) + (IntExp > rangeMax)*(IntExp - rangeMax))</code>.
 */
public final class IntExpRangeViolation extends IntExpImpl
{
  private IntExp	 _exp;
  private int      _rangeMin, _rangeMax;
  private Observer _observer;

  private IntVar _domain;

  class ExpRangeViolationObserver extends Observer
  {
    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      _domain.setMin(calc_min());
      _domain.setMax(calc_max());
//      propagate(); //???
    }

    public String toString()
    {
      return "ExpRangeViolationObserver: ";
    }

    public Object master()
    {
      return IntExpRangeViolation.this;
    }

    public int subscriberMask()
    {
      return MIN | MAX | VALUE;
    }

  } //~ ExpRangeViolationObserver


  public IntExpRangeViolation(IntExp exp, int rangeMin, int rangeMax)
  {
    super(exp.constrainer());

    _exp = exp;
    _rangeMin = rangeMin;
    _rangeMax = rangeMax;

    if(constrainer().showInternalNames())
    {
//      _name = "("+exp.name()+" in "+domainToString(rangeMin,rangeMax)+")";
      _name = "("+"("+exp.name()+"<"+rangeMin+")*("+rangeMin+"-"+exp.name()+")+"+
                  "("+exp.name()+">"+rangeMax+")*("+exp.name()+"-"+rangeMax+")"+")";
    }

//    int trace = IntVarImplTrace.TRACE_ALL;
    int trace = 0;
    _domain = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), _name, IntVar.DOMAIN_PLAIN, trace);

    _exp.attachObserver(_observer = new ExpRangeViolationObserver());
  }

  public void attachObserver(Observer observer)
  {
    super.attachObserver(observer);
    _domain.attachObserver(observer);
  }

  public void reattachObserver(Observer observer)
  {
    super.reattachObserver(observer);
    _domain.reattachObserver(observer);
  }

  public void detachObserver(Observer observer)
  {
    super.detachObserver(observer);
    _domain.detachObserver(observer);
  }


  /**
   * Returns the value of the range violation at x.
   */
  int calc_v(int x)
  {
    if(x < _rangeMin)
      return _rangeMin - x;
    else if(x > _rangeMax)
      return x - _rangeMax;
    else
      return 0;
  }

  int calc_max()
  {
    // Max is at the one of the endpoints.
    return Math.max( calc_v(_exp.min()), calc_v(_exp.max()) );
    // Optimized?
//    return Math.max( Math.max(_rangeMin - _exp.min(), _exp.max() - _rangeMax), 0 );
  }

  int calc_min()
  {
    int expMin = _exp.min(), expMax = _exp.max();
    // exp >= _rangeMax
    if(expMin >= _rangeMax)
    {
      // v(xmin)
      return expMin - _rangeMax;
    }
    // exp <= _rangeMin
    else if(expMax <= _rangeMin)
    {
      // v(xmax)
      return _rangeMin - expMax;
    }
    else
    {
      return 0;
    }
  }

  public int max()
  {
    return _domain.max();
  }

  public int min()
  {
    return _domain.min();
  }

  public void setMax(int max) throws Failure
  {
//    System.out.println("+++IntExpRangeViolation.setMax(" +max + ") in " + this);

    if (max >= _domain.max())
      return;

    if (max < _domain.min())
      constrainer().fail("IntExpRangeViolation.setMax(): max < min()");

    _domain.setMax(max); // includes check for failure

    if(max < 0) // should be handled above
      abort("IntExpRangeViolation.setMax(): max < 0: " + max);

    _exp.setMin(_rangeMin-max);
    _exp.setMax(_rangeMax+max);

//    System.out.println("---IntExpRangeViolation.setMax(" +max + ") in " + this);
  }

  /**
   * Some expressions has to propagate their 'constraint domain'.
   */
  public void propagate() throws Failure
  {
    int min = _domain.min();
    int max = _domain.max();

    if(max >= 0)
    {
      _exp.setMin(_rangeMin-max);
      _exp.setMax(_rangeMax+max);
    }

    if(min > 0)
    {
      int holeMin = _rangeMin - min + 1;
      int holeMax = _rangeMax + min - 1;
      for(int i = holeMin; i <= holeMax; ++i)
        _exp.removeValue(i);
    }
  }

  public void setMin(int min) throws Failure
  {
//    System.out.println("+++IntExpRangeViolation.setMin(" +min + ") in " + this);

    if (min <= _domain.min())
      return;

    if (min > _domain.max())
      constrainer().fail("IntExpRangeViolation.setMin(): min > max()");

    _domain.setMin(min); // includes check for failure

    if(min <= 0) // should be handled above
      abort("IntExpRangeViolation.setMin(): min <= 0: " + min);

    int holeMin = _rangeMin - min + 1;
    int holeMax = _rangeMax + min - 1;
    for(int i = holeMin; i <= holeMax; ++i)
      _exp.removeValue(i);

//    System.out.println("---IntExpRangeViolation.setMin(" +min + ") in " + this);
  }

  public int value() throws Failure
  {
    return _domain.value();
  }

} // ~IntExpRangeViolation
