package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntArray;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

//
//: IntExpEnum.java
//
/**
 * An implementation of the Domain interface as a set of integer values.
 *
 */
public final class IntExpEnum extends IntExpImpl
{
  private int[]       _values;
  protected IntVar		_index;
  private ExpressionObserver _observer;

  public IntExpEnum(Constrainer c, int[] values, String name)
  {
    super(c,name);

    init(values);
  }

  public IntExpEnum(Constrainer c, IntArray values, String name)
  {
    super(c,name);

    init( (int[])values.data().clone() );
  }

  void init(int[] values)
  {
    _values = IntCalc.differentSortedValues( values );

//    int trace = IntVar.TRACE_ALL;
    int trace = 0;
    int type = IntVar.DOMAIN_BIT_FAST;

    String indexName = "";
    if(constrainer().showInternalNames())
    {
      indexName = "index_" + name();
    }

    int indexMax = _values.length - 1;

    _index = constrainer().addIntVarTraceInternal(0, indexMax, indexName, type, trace);

    _index.attachObserver(_observer = new IndexObserver());

  }

  public int size()
  {
    return _index.size();
  }

  public int max()
  {
    return _values[_index.max()];
  }

  public int min()
  {
    return _values[_index.min()];
  }

  public boolean contains(int value)
  {
    int idx = valueIndex(value, _index.min(), _index.max());
    return idx >= 0 && _index.contains(idx);
  }

  public void removeValue(int value) throws Failure
  {
    int idx = valueIndex(value, _index.min(), _index.max());
    if(idx >= 0)
    {
      _index.removeValue(idx);
    }
  }

  public void setValue(int value) throws Failure
  {
    int idx = valueIndex(value, _index.min(), _index.max());
    if(idx >= 0)
    {
      _index.setValue(idx);
    }
    else
    {
      constrainer().fail("IntExpEnum.setValue()");
    }
  }

  public void setMax(int M) throws Failure
  {
    if(M >= max())
      return;

    if(M < min())
      constrainer().fail("Max < Min ");

//    // min() <= M < max() -> indexMax is the last where M >= value[indexMax]
//    int indexMin = _index.min();
//    for(int indexMax = _index.max()-1; indexMax >= indexMin; --indexMax)
//    {
//      if(M >= _values[indexMax])
//      {
//        _index.setMax(indexMax);
//        return;
//      }
//    }
//
//    throw new RuntimeException("IntExpEnum::setMax("+M+"): " + domainToString());

    int idx = IntCalc.binarySearch(_values,M);
    if(idx >= 0)
    {
      _index.setMax(idx);
    }
    else
    {
      // idx = (-(insertion point) - 1)
      int insertion_point = -(idx + 1);
//      System.out.println("IP="+insertion_point);
      _index.setMax(insertion_point-1);
    }

  }

  public void setMin(int m) throws Failure
  {
    if(m <= min())
      return;

    if(m > max())
      constrainer().fail("Min > Max");

//    // min() < m <= max() -> indexMin is the first where values[indexMin] >= m
//    int indexMax = _index.max();
//    for(int indexMin = _index.min()+1; indexMin <= indexMax; ++indexMin)
//    {
//      if(_values[indexMin] >= m)
//      {
//        _index.setMin(indexMin);
//        return;
//      }
//    }
//
//    throw new RuntimeException("IntExpEnum::setMin("+m+"): " + domainToString());

    int idx = IntCalc.binarySearch(_values,m);
    if(idx >= 0)
    {
      _index.setMin(idx);
    }
    else
    {
      // idx = (-(insertion point) - 1)
      int insertion_point = -(idx + 1);
      //System.out.println("IP="+insertion_point);
      _index.setMin(insertion_point);
    }

  }

  public String domainToString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    int min =  _index.min();
    int max =  _index.max();
    for(int i = min; i <= max; ++i)
    {
      if (_index.contains(i))
      {
        if (i != min)
          buf.append(" ");
        buf.append(String.valueOf(_values[i]));
      }
    }
    buf.append("]");
    return buf.toString();
  }

  public void iterateDomain(IntExp.IntDomainIterator it) throws Failure
  {
    iterateDomain2(it);
  }

  void iterateDomain1(final IntExp.IntDomainIterator it) throws Failure
  {
    IntExp.IntDomainIterator indexIt = new IntExp.IntDomainIterator()
    {
      public boolean doSomethingOrStop(int idx) throws Failure
      {
        return it.doSomethingOrStop(_values[idx]);
      }
    };
    _index.iterateDomain(indexIt);
  }

  void iterateDomain2(final IntExp.IntDomainIterator it) throws Failure
  {
    int min =  _index.min();
    int max =  _index.max();
    for(int idx = min; idx <= max; ++idx)
    {
      if (_index.contains(idx))
      {
        if (!it.doSomethingOrStop(_values[idx]))
          return;
      }
    }
  }


  int valueIndex(int value, int minIndex, int maxIndex)
  {
    int index = IntCalc.binarySearch(_values, value);

    if(minIndex <= index && index <= maxIndex)
      return index;
    else
      return -1;
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_index);
  }

  class IndexObserver extends ExpressionObserver
  {
    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      IntEvent e = (IntEvent)event;

      IntEventEnum ev = IntEventEnum.getEvent(e,_values);

      notifyObservers(ev);
    }

    public String toString()
    {
      return "IndexObserver:" + _index;
    }

    public Object master()
    {
      return IntExpEnum.this;
    }

  } // ~IndexObserver


  static class IntEventEnum extends IntEvent
  {
    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new IntEventEnum();
        }

    };

    static IntEventEnum getEvent(IntEvent event, int[] values)
    {
      IntEventEnum ev = (IntEventEnum) _factory.getElement();
      ev.init(event, values);
      return ev;
    }



    int[]  _values;
    IntEvent _event;

    public String name()
    {
      return "IntEventEnum";
    }



    public void init(IntEvent e, int[] values)
    {
      _event = e;
      _values = values;
    }

    public int type()
    {
      return _event.type();
    }


    public int removed(int i)
    {
      return _values[_event.removed(i)];
    }


    public int min()
    {
      return _values[_event.min()];
    }

    public int max()
    {
      return _values[_event.max()];
    }

    public int oldmin()
    {
      return _values[_event.oldmin()];
    }


    public int oldmax()
    {
      return _values[_event.oldmax()];
    }

    public int numberOfRemoves()
    {
      return _event.numberOfRemoves();
    }

  } // ~IntEventEnum


/*
  static void main(String[] args) throws Exception
  {
    Constrainer c = new Constrainer("");

    int[] values = {4, 1, 666, 4, 7000, 4};

    IntArray ary = new IntArray(c,values);

    IntExp exp = new IntExpEnum(c,ary,"");

//    IntExp index = c.addIntVar(0,ary.size()-1);
//    IntExp exp = ary.elementAt(index);


    System.out.println(exp);
    exp.removeValue(4);
    System.out.println(exp);
    exp.setMax(8000);
    System.out.println(exp);
    exp.setMin(7000);
    System.out.println(exp);
  }
*/
} // ~IntExpEnum
