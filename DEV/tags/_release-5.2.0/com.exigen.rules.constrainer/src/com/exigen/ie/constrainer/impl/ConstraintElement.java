package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.ConstraintImpl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.IntArray;
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
 * Class ConstraintElement implements an element constraint defined
 * on an IntArray and an IntExp-index.
 *
 * If the "index" in the "array" is a constrained integer expression, then the
 * "array[index]" is also the constrained integer variable. You could use the
 * method "elementAt" of IntArray to get an access to this constrained variable.
 *
 * @see IntArray
 */
public final class ConstraintElement extends ConstraintImpl
{
  private IntArray 	       _array;
  private IntExp           _index_exp;
  private IntVar           _element;
  private UndoableBits     _bits; // one bit for every index in _array

  class ObserverForIndex extends Observer
  {

    public void update(Subject var, EventOfInterest interest)
      throws Failure
    {
      IntEvent e = (IntEvent)interest;
      if (e.isValueEvent()) //------------------------------ value
      {
        int index = e.min();
        for(int i = e.oldmin(); i <= e.oldmax(); ++i)
        {
          if (i != index)
          {
            _bits.bit(i,true);
            constrainer().addUndo(UndoBits.getUndo(_bits,i));
          }
        }
        _element.setValue(_array.get(index));
      }
      else
      if (e.isRemoveEvent()) //------------------------------ removes
      {
        for(int i=0; i < e.numberOfRemoves(); ++i)
        {
          int removed_index = e.removed(i);
          removeFromIndex(removed_index);
        }
      }
      else
      if (e.isMinEvent()) //------------------------------ min
      {
        for(int i = e.oldmin(); i < e.min(); ++i)
        {
          removeFromIndex(i);
        }
      }
      else
      if (e.isMaxEvent()) //------------------------------ max
      {
        for(int i=e.oldmax(); i > e.max(); --i)
        {
          removeFromIndex(i);
        }
      }
    }

    public int subscriberMask()
    {
      return EventOfInterest.ALL;
    }

    public String toString()
    {
      return "ObserverForIndex";
    }


    public Object master()
    {
      return ConstraintElement.this;
    }



  } //~ ObserverForIndex


  class ObserverForElement extends Observer
  {

    public void update(Subject var, EventOfInterest interest)
      throws Failure
    {
      IntEvent e = (IntEvent)interest;
      if (e.isValueEvent()) //------------------------------ value
      {
        int value = e.min();
        for(int i=_index_exp.min(); i <= _index_exp.max(); i++)
        {
          if (_array.get(i) != value)
          {
            _index_exp.removeValue(i);
            if (!_bits.bit(i))
            {
              _bits.bit(i,true);
              constrainer().addUndo(UndoBits.getUndo(_bits,i));
            }
          }
        }
      }
      else
      if (e.isRemoveEvent()) //------------------------------ removes
      {
        for(int i=0; i < e.numberOfRemoves(); ++i)
        {
          int removed_value = e.removed(i); // it's better to call it e.removed(i)
          removeFromElement(removed_value);
        }
      }
      else
      if (e.isMinEvent()) //------------------------------ min
      {
        for(int i=_index_exp.min(); i < e.min(); i++)
        {
          _index_exp.removeValue(i);
          if (!_bits.bit(i))
          {
            _bits.bit(i,true);
            constrainer().addUndo(UndoBits.getUndo(_bits,i));
          }
        }
      }
      else
      if (e.isMaxEvent()) //------------------------------ max
      {
        for(int i = _index_exp.max();  i > e.max(); i--)
        {
          _index_exp.removeValue(i);
          if (!_bits.bit(i))
          {
            _bits.bit(i,true);
            constrainer().addUndo(UndoBits.getUndo(_bits,i));
          }
        }
      }
    }

    public int subscriberMask()
    {
      return EventOfInterest.ALL;
    }

    public String toString()
    {
      return "ObserverForElement";
    }

    public Object master()
    {
      return ConstraintElement.this;
    }


  } //~ ObserverForElement

  public ConstraintElement(IntArray values, IntExp index_exp, IntVar element_var)
  {
    super(values.constrainer(),"ConstraintElement");
    _index_exp = index_exp;
    _array = values;
    _element = element_var;
    int size = values.size();
    _bits = new UndoableBits(values.constrainer(), 0, size-1);
    _bits.object(this);
    // initially all bits are false meaning all indeces not removed
  }

  public Goal execute() throws Failure
  {
    // initial propagation from index_exp to element_var
    int size = _array.size();
    for(int i=0; i<size; i++)
    {
      if (!_index_exp.contains(i))
      {
        removeFromIndex(i);
      }
    }

    // attach observers
    _element.attachObserver(new ObserverForElement());
    _index_exp.attachObserver(new ObserverForIndex());

    return null;
  }

  void removeFromIndex(int index) throws Failure
  {
    if (!_bits.bit(index)) // not removed yet
    {
      int removed_value = _array.get(index);
      _bits.bit(index,true);
      constrainer().addUndo(UndoBits.getUndo(_bits,index));
      int[] data = _array.data();
      for(int i=0; i<data.length; ++i)
      {
        if (data[i] == removed_value && !_bits.bit(i))
          return;
      }
      _element.removeValue(removed_value);
    }
  }

  void removeFromElement(int value) throws Failure
  {
    for(int i=_index_exp.min(); i <= _index_exp.min(); i++)
    {
      if (_array.get(i) == value)
      {
        _index_exp.removeValue(i);
        if (!_bits.bit(i))
        {
          _bits.bit(i,true);
          constrainer().addUndo(UndoBits.getUndo(_bits,i));
        }
      }
    }
  }

  public String toString()
  {
    return "ConstraintElement: "+ _element + "=array[" + _index_exp + "]";
  }

} //eof ConstraintElement
