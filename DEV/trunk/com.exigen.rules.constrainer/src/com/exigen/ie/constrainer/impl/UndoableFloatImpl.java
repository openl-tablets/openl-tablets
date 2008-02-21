package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Undo;
import com.exigen.ie.constrainer.UndoImpl;
import com.exigen.ie.constrainer.Undoable;
import com.exigen.ie.constrainer.UndoableFloat;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * A generic implementation of the UndoableFloat.
 */
//public final class UndoableFloatImpl extends UndoableOnceImpl implements UndoableFloat
public final class UndoableFloatImpl extends UndoableOnceImpl implements UndoableFloat
{
  private double _value;

  /**
   * Constructor with a given value.
   */
  public UndoableFloatImpl(Constrainer constrainer, double value)
  {
    this(constrainer,value,"");
  }

  /**
   * Constructor with a given value and name.
   */
  public UndoableFloatImpl(Constrainer constrainer, double value, String name)
  {
    super(constrainer, name);
    _value = value;
  }

  public double value()
  {
    return _value;
  }

  public void setValue(double value)
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
  void forceValue(double value)
  {
    _value = value;
  }

  public Undo createUndo()
  {
    return UndoUndoableFloat.getUndo();
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
   * Undo Class for UndoUndoableFloat.
   */
  static class UndoUndoableFloat extends UndoImpl
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new UndoUndoableFloat();
        }

    };

    static UndoUndoableFloat getUndo()
    {
      return (UndoUndoableFloat) _factory.getElement();
    }

    private double _value;

    public void undoable(Undoable u)
    {
      super.undoable(u);
      UndoableFloat var = (UndoableFloat) u;
      _value = var.value();
    }

    public void undo()
    {
      UndoableFloatImpl var = (UndoableFloatImpl) undoable();
      var._value = _value;
      super.undo();
    }

    /**
     * Returns a String representation of this object.
     * @return a String representation of this object.
     */
    public String toString()
    {
      return "UndoUndoableFloat "+undoable();
    }

  } // ~UndoUndoableFloat

} // ~UndoableFloatImpl
