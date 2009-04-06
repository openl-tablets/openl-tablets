package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.Undo;
import com.exigen.ie.constrainer.UndoImpl;
import com.exigen.ie.tools.FastVector;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the Undo for FastVector.add().
 *
 * @see Undo
 */
public class UndoFastVectorAdd extends UndoImpl implements java.io.Serializable
{

  private FastVector _v;


  static ReusableFactory _factory = new ReusableFactory()
  {
      protected Reusable createNewElement()
      {
        return new UndoFastVectorAdd();
      }

  };

  static public UndoFastVectorAdd getUndo(FastVector v)
  {
     UndoFastVectorAdd undo = (UndoFastVectorAdd) _factory.getElement();
     undo._v = v;
     return undo;
  }


  public void undo()
  {
    _v.removeElementAt(_v.size()-1);
    super.undo();
  }

  /**
   * Returns a String representation of this object.
   * @return a String representation of this object.
   */
  public String toString()
  {
    return "UndoFastVectorAdd " + _v;
  }

}