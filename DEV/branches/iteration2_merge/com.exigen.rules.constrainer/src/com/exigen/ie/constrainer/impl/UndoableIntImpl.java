package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Undo;
import com.exigen.ie.constrainer.UndoImpl;
import com.exigen.ie.constrainer.Undoable;
import com.exigen.ie.constrainer.UndoableInt;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * A generic implementation of the UndoableInt.
 */
//public final class UndoableIntImpl extends UndoableOnceImpl implements UndoableInt
public final class UndoableIntImpl extends UndoableImpl implements UndoableInt
{
  private int _value;

  /**
   * Constructor with a given value.
   */
  public UndoableIntImpl(Constrainer constrainer, int value)
  {
    this(constrainer,value,"");
  }

  /**
   * Constructor with a given value and name.
   */
  public UndoableIntImpl(Constrainer constrainer, int value, String name)
  {
    super(constrainer, name);
    _value = value;
  }

  public int value()
  {
    return _value;
  }

  public void setValue(int value)
  {
    if(value != _value)
    {
      addUndo();
      _value = value;
    }
  }

  /**
   * Sets the current value.
   */
  void forceValue(int value)
  {
    _value = value;
  }

  public Undo createUndo()
  {
    return UndoUndoableInt.getUndo();
  }

  /**
   * Returns a String representation of this object.
   * @return a String representation of this object.
   */
  public String toString()
  {
    return name() + "[" + _value + "]";
  }

  /**
   * Undo Class for UndoUndoableInt.
   */
  static class UndoUndoableInt extends UndoImpl
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new UndoUndoableInt();
        }

    };

    static UndoUndoableInt getUndo()
    {
      return (UndoUndoableInt) _factory.getElement();
    }

    private int _value;

    public void undoable(Undoable u)
    {
      super.undoable(u);
      UndoableInt var = (UndoableInt) u;
      _value = var.value();
    }

    public void undo()
    {
      UndoableIntImpl var = (UndoableIntImpl) undoable();
      var._value = _value;
      super.undo();
    }

    /**
     * Returns a String representation of this object.
     * @return a String representation of this object.
     */
    public String toString()
    {
      return "UndoUndoableInt "+undoable();
    }

  } // ~UndoUndoableInt

} // ~UndoableIntImpl
