package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
/**
 * An implementation of the expression: <code>sum(IntExpArray)</code>.
 * This implementation "remember and propagate" setMin/Max.
 */
public final class IntExpAddArray1 extends IntExpImpl
{
  private IntExpArray	       _vars;
  private Observer _observer;

  private IntVar    _domainC; // constraint domain
  private DomainVar _domainE; // expression domain

  static final private int[] event_map = {
      MIN,              MIN,
      MAX,              MAX,
      MIN| MAX | VALUE, VALUE,
                                       //         REMOVE, REMOVE
                                           };


  class ExpAddVectorObserver extends Observer
  {

    ExpAddVectorObserver()
    {
//      super(event_map);
    }

    public int subscriberMask()
    {
      return MIN | MAX | VALUE;
    }

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      IntEvent e = (IntEvent) event;

  //    System.out.println("Event:" + e);

      _domainE.setMin(_domainE.min() + e.mindiff());
      _domainE.setMax(_domainE.max() + e.maxdiff());

      // update domainC
      _domainC.setMin(_domainE.min());
      _domainC.setMax(_domainE.max());

    }


      public String toString()
      {
        return "ExpAddVectorObserver: "+_vars;
      }

      public Object master()
      {
        return IntExpAddArray1.this;
      }


  } //~ ExpAddVectorObserver


  public IntExpAddArray1(Constrainer constrainer, IntExpArray vars)
  {
    super(constrainer);
    int size = vars.size();
    _vars = vars;
    _observer = new ExpAddVectorObserver();

    IntExp[] data = _vars.data();

    for(int i=0; i < data.length; i++)
    {
      data[i].attachObserver(_observer);
    }

    String sum_name = "";

    if (constrainer().showInternalNames())
    {
      StringBuffer s = new StringBuffer();
      s.append("(");
      for(int i=0; i<data.length; i++)
      {
        if(i!=0)
          s.append("+");
        s.append(data[i].name());
      }
      s.append(")");
      _name = s.toString();

      sum_name = "sum(" + _vars.name() + ")";
    }

    int trace = 0;
    int min = calc_min();
    int max = calc_max();
    _domainC = constrainer().addIntVarTraceInternal(min, max, sum_name, IntVar.DOMAIN_PLAIN, trace);
    _domainE = new DomainVar(constrainer(),min,max);
  }

  public void onMaskChange()
  {
//    int mask = publisherMask();
//    IntExp[] data =_vars.data();
//    for(int i=0; i < data.length; i++)
//    {
//      _observer.publish(mask,data[i]);
//    }
  }


  public void name(String name)
  {
    super.name(name);
    _domainC.name(name);
  }

  public void attachObserver(Observer observer)
  {
    super.attachObserver(observer);
    _domainC.attachObserver(observer);
  }

  public void reattachObserver(Observer observer)
  {
    super.reattachObserver(observer);
    _domainC.reattachObserver(observer);
  }

  public void detachObserver(Observer observer)
  {
    super.detachObserver(observer);
    _domainC.detachObserver(observer);
  }



  int calc_max()
  {
    int max_sum = 0;

    IntExp[] vars = _vars.data();

    for(int i=0; i < vars.length; i++)
    {
      max_sum += vars[i].max();
    }
    return max_sum;
  }

  int calc_min()
  {
    int min_sum = 0;

    IntExp[] vars = _vars.data();

    for(int i=0; i < vars.length; i++)
    {
      min_sum += vars[i].min();
    }
    return min_sum;
  }

  public int min()
  {
    return _domainC.min();
  }

  public int max()
  {
    return _domainC.max();
  }


  public void setMax(int max) throws Failure
  {

    if (max >= _domainC.max())
      return;

    _domainC.setMax(max);

//    System.out.println("++++ Set max: " + max + " in " + this);

    int min_sum = _domainE.min();


    IntExp[] vars = _vars.data();

    for(int i=0; i < vars.length; i++)
    {
      IntExp vari = vars[i];
      int maxi = max - (min_sum - vari.min());
      if (maxi < vari.max())
      {
        vari.setMax(maxi);
      }
    }
//    System.out.println("---- set max:" + max + " in " + this);
  }

  public void setMin(int min) throws Failure
  {

    if (min <= _domainC.min())
      return;

    _domainC.setMin(min);

//    System.out.println("++++ Set min: " + min + " in " + this);


    int max_sum = _domainE.max();


    IntExp[] vars = _vars.data();

    for(int i=0; i < vars.length; i++)
    {
      IntExp vari = vars[i];
      int mini = min - (max_sum - vari.max());
      if (mini > vari.min())
      {
        vari.setMin(mini);
      }
    }
//    System.out.println("---- set min:" + min + " in " + this);
  }

  public void setValue(int value) throws Failure
  {
    if(_domainC.min() == value && _domainC.max() == value)
      return;

    _domainC.setValue(value);

    //    System.out.println("++++ Set value: " + value + " in " + this);

    int sum_min = _domainE.min();
    int sum_max = _domainE.max();

    IntExp[] vars = _vars.data();

    for(int i=0; i < vars.length; i++)
    {
      IntExp vari = (IntExp)vars[i];
      int mini = vari.min();
      int maxi = vari.max();

      int new_min = value - (sum_max - maxi);
      if (new_min > mini)
      {
        vari.setMin(new_min);
      }

      int new_max = value - (sum_min - mini);
      if (new_max < maxi)
      {
        vari.setMax(new_max);
      }
    }

//    System.out.println("---- set value: " + value + " in " + this);
  }

  public void removeValue(int value) throws Failure
  {
    int Max = _domainC.max();
    if (value > Max)
      return;
    int Min = _domainC.min();
    if (value < Min)
      return;
    if (Min == Max)
      constrainer().fail("remove for IntExpAddVector");
    if (value == Max)
      setMax(value-1);
    if (value == Min)
      setMin(value+1);
  }

  public int size()
  {
    return max() - min() + 1;
  }

  void enforceDomainC() throws Failure
  {
    int minC = _domainC.min();
    int maxC = _domainC.max();
    int minE = _domainE.min();
    int maxE = _domainE.max();

    if(minC == minE && maxC == maxE)
    {
//      System.out.println("*** enforceDomainC(): ["+minE+".."+maxE+"]->["+minC+".."+maxC+"]");
      return;
    }

    IntExp[] vars = _vars.data();

    for(int i=0; i < vars.length; i++)
    {
      IntExp vari = vars[i];
      int mini = vari.min();
      int maxi = vari.max();

      int new_min = minC - (maxE - maxi);
      if (new_min > mini)
      {
        vari.setMin(new_min);
      }

      int new_max = maxC - (minE - mini);
      if (new_max < maxi)
      {
        vari.setMax(new_max);
      }
    }
  }

  final class DomainVar extends IntVarImpl
  {
    public DomainVar(Constrainer c, int min, int max)
    {
      super(c,min,max,"",DOMAIN_PLAIN);
    }

    public void propagate() throws Failure
    {
      enforceDomainC();
    }

  } // ~DomainVar

  public boolean isLinear(){
    for (int i=0;i<_vars.size();i++){
      if (!_vars.get(i).isLinear())
        return false;
    }
    return true;
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    if (!isLinear())
      throw new NonLinearExpression(this);
    double cumSum = 0;
    for (int i=0;i<_vars.size();i++){
      cumSum += _vars.get(i).calcCoeffs(map, factor);
    }
    return cumSum;
  }

} // ~IntExpAddArray1
