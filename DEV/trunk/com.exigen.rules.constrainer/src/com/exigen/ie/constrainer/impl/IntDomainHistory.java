package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.tools.FastVectorInt;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the history for the integer domain.
 */
public final class IntDomainHistory implements java.io.Serializable
{

  final static int MIN_IDX = 0,
                   MAX_IDX = 1,
                   SIZE_IDX = 2,
                   REMOVE_IDX = 3,
                   LAST_IDX = 4;

  IntVar _var;
  FastVectorInt _history;
  FastVectorInt _remove_history;

  int _currentIndex = -1;

  int _mask;

  int _min, _max;

  public IntDomainHistory(IntVar var)
  {
    _var = var;

    int max_size = Math.min(_var.size(), 30);
    _history = new FastVectorInt(2 * max_size);
    _remove_history = new FastVectorInt(max_size);
    save();
  }

  void propagate() throws Failure
  {

//    System.out.println("+++ Propagate: " + _var + this + " pubMask:" + _var.publisherMask());


    if ((_var.publisherMask() & _mask) != 0)
    {
//      System.out.println("--- Propagate: " + _var + _history + ":" + _currentIndex );
      IntEventDomain ev = IntEventDomain.getEvent(this);
//      _mask = 0;
      save();
      _var.notifyObservers(ev);
    }
    else
    {
      save();
    }
  }

  public void saveUndo()
  {
    if (_mask != 0)
      save();
  }

  public String toString()
  {
    return "History: " +_history + ":" + _currentIndex + "(" +  _min + "-" + _max + ")" + "mask: " + _mask;
  }

  public int currentIndex()
  {
    return _currentIndex;
  }


  int save()
  {
    int old = _currentIndex;
    _currentIndex = _history.size();
    _history.add(_min = _var.min());
    _history.add(_max = _var.max());
    _history.add(_var.size());
    _history.add(_remove_history.size());
    _mask = 0;
    return old;
  }

  public int min()
  {
    return _min;
  }

  public int max()
  {
    return _max;
  }

  public int oldmin()
  {
    return _history.elementAt(_currentIndex + MIN_IDX);
  }

  public int oldmax()
  {
    return _history.elementAt(_currentIndex + MAX_IDX);
  }



  public void restore(int index)
  {
    _var.forceSize(_history.elementAt(index + SIZE_IDX));
    _var.forceMin(_min = _history.elementAt(index + MIN_IDX));
    _var.forceMax(_max = _history.elementAt(index + MAX_IDX));
    int firstRemoveIndex = _history.elementAt(index + REMOVE_IDX);

    for(int i = _remove_history.size() - 1 ; i >= firstRemoveIndex; --i)
    {
      _var.forceInsert(_remove_history.elementAt(i));
    }

    _remove_history.cutSize(firstRemoveIndex);

    _history.cutSize(index + LAST_IDX);
    _currentIndex = index;
    _mask = 0;
  }

  public int removeIndex()
  {
    return _history.elementAt(_currentIndex + REMOVE_IDX);
  }

  public int numberOfRemoves ()
  {
    return _remove_history.size() - removeIndex();
  }

  public int getRemove(int i)
  {
    return _remove_history.elementAt(i);
  }


  void remove(int val)
  {
    if (_min < val && val < _max)
    {
      _remove_history.add(val);
      _mask |= EventOfInterest.REMOVE;
    }
  }

  /**
   * The function stores range removal from Int domain.
   *
   * added by SV 20.01.03 to support removeRangeInternal in IntVarImpl
   *
   * @param range_min
   * @param range_max
   */
  void remove(int range_min, int range_max)
  {
    int t_min = Math.max (_min, range_min);
    int t_max = Math.min (_max, range_max);

    for (int i = t_min; i <= t_max; i++) {
      _remove_history.add (i);
    }
    _mask |= EventOfInterest.REMOVE;
  }

  void setMin(int val)
  {
    if (val > _min)
    {
      _min = val;
      _mask |= EventOfInterest.MIN;
      if (_min == _max)
      {
        _mask |= EventOfInterest.VALUE;
        _mask &= ~EventOfInterest.REMOVE;
      }
    }
  }


  void setMax(int val)
  {
    if (val < _max)
    {
      _max = val;
      _mask |= EventOfInterest.MAX;
      if (_min == _max)
      {
        _mask |= EventOfInterest.VALUE;
        _mask &= ~EventOfInterest.REMOVE;
      }
    }
  }

  /**
   * An implementation of the event about change in the integer domain.
   */
  static final class IntEventDomain extends IntEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new IntEventDomain();
        }

    };

    static IntEventDomain getEvent(IntDomainHistory history)
    {
      IntEventDomain ev = (IntEventDomain) _factory.getElement();
      ev.init(history);
      return ev;
    }


    protected int _min, _max, _oldmin, _oldmax;
    protected int _type_mask;
    IntDomainHistory _history;
    int _removeIndex, _numberOfRemoves;

    public String name()
    {
      return "Event Domain";
    }




    public void init(IntDomainHistory hist)
    {
      exp(hist._var);

      _min = hist.min();
      _max = hist.max();
      _oldmin = hist.oldmin();
      _oldmax = hist.oldmax();
      _type_mask = hist._mask;

      _removeIndex = hist.removeIndex();
      _numberOfRemoves = hist.numberOfRemoves();

      _history = hist;
    }



    public int min()
    {
      return _min;
    }

    public int type()
    {
      return _type_mask;
    }

    public void min(int min)
    {
      _min = min;
    }



    public int max()
    {
      return _max;
    }

    public void max(int max)
    {
      _max = max;
    }

    public int oldmin()
    {
      return _oldmin;
    }

    public void oldmin(int oldmin)
    {
      _oldmin = oldmin;
    }

    public int oldmax()
    {
      return _oldmax;
    }

    public void oldmax(int oldmax)
    {
      _oldmax = oldmax;
    }

    public int mindiff()
    {
      return _min - _oldmin;
    }

    public int maxdiff()
    {
      return _max - _oldmax;
    }

    public int removed(int i)
    {
      return _history.getRemove(_removeIndex + i);
    }

    public int numberOfRemoves()
    {
      return _numberOfRemoves;
    }


  } // ~IntEventDomain

} // ~IntDomainHistory
