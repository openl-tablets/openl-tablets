package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalInstantiate;
import com.exigen.ie.constrainer.IntBoolVar;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.Undo;
import com.exigen.ie.constrainer.UndoImpl;
import com.exigen.ie.constrainer.Undoable;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the IntBoolVar interface.
 * This implementation is optimized for [0..1] domain.
 */
public class IntBoolVarImpl extends IntBoolExpImpl implements IntBoolVar
{
  protected int _max;
  protected int _min;

  public IntBoolVarImpl(Constrainer constrainer)
  {
    this(constrainer,"");
  }

  public IntBoolVarImpl(Constrainer constrainer, String name)
  {
    super(constrainer, name);
    _min = 0;
    _max = 1;
  }

  public Undo createUndo()
  {
    return UndoIntBoolVar.getBoolVarUndo();
  }

  public int min()
  {
    return _min;
  }

  public int max()
  {
    return _max;
  }

  public void setMin(int min) throws Failure
  {
    if (min > _max)
      _constrainer.fail("set min bool var");

    if (min > _min)
    {
//      addUndo();
//      if(!undone())
        constrainer().addUndo(UndoIntBoolVarValue.getUndo(this));

      _min = min;

      notifyObservers(IntEventBoolTrue.the);
//      notifyObservers(IntEventBool.getEvent(this, true));
//      addToPropagationQueue();
    }
  }

  public void setMax(int max) throws Failure
  {
    if (max < _min)
      _constrainer.fail("set max bool var");

    if (max < _max)
    {
//      addUndo();
//      if(!undone())
        constrainer().addUndo(UndoIntBoolVarValue.getUndo(this));

      _max = max;

      notifyObservers(IntEventBoolFalse.the);
//      notifyObservers(IntEventBool.getEvent(this, false));
//      addToPropagationQueue();
    }
  }

  public void propagate() throws Failure
  {
    notifyObservers(IntEventBool.getEvent(this, _min==1));
  }

  public void forceMin(int min)
  {
    _min = min;
  }

  public void forceMax(int max)
  {
    _max = max;
  }

  public void forceSize(int max)
  {
    abort("Temporary method???");
  }


   public void forceInsert(int val)
   {
    abort("Temporary method???");
   }

  public int domainType()
  {
    return DOMAIN_BOOL;
  }

  public Goal instantiate()
  {
    return new GoalInstantiate(this);
  }

  static final class IntEventBool extends IntEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new IntEventBool();
        }

    };

    static IntEventBool getEvent(IntExp exp, boolean val)
    {
      IntEventBool ev = (IntEventBool) _factory.getElement();
      ev.init(exp, val);
      return ev;
    }


    int _int_value, _type;

    public void init(IntExp exp, boolean val)
    {
      exp(exp);

      if (val)
      {
        _int_value = 1;
        _type = MIN | VALUE;
      }
      else
      {
        _int_value = 0;
        _type = MAX | VALUE;
      }

    }

    public String name()
    {
      return "IntBoolEvent";
    }

    public int oldmax()
    {
      return 1;
    }

    public int oldmin()
    {
      return 0;
    }

    public int min()
    {
      return _int_value;
    }

    public int max()
    {
      return _int_value;
    }

    public int type()
    {
      return _type;
    }


    public int removed(int i)
    {
      return -1;
    }

    public int numberOfRemoves()
    {
      return 0;
    }

  }

  /**
   * An implementation of the 'true' event.
   */
  static final class IntEventBoolTrue extends IntEvent
  {
    public static final IntEventBoolTrue the = new IntEventBoolTrue();

    public String name()
    {
      return "IntBoolEventTrue";
    }

    public int oldmax()
    {
      return 1;
    }

    public int oldmin()
    {
      return 0;
    }

    public int min()
    {
      return 1;
    }

    public int max()
    {
      return 1;
    }

    public int type()
    {
      return MIN | VALUE;
    }


    public int removed(int i)
    {
      return -1;
    }

    public int numberOfRemoves()
    {
      return 0;
    }

    public void free()
    {
    }

  } // ~IntEventBoolTrue

  /**
   * An implementation of the 'false' event.
   */
  static final class IntEventBoolFalse extends IntEvent
  {
    public static final IntEventBoolFalse the = new IntEventBoolFalse();

    public String name()
    {
      return "IntBoolEventFalse";
    }

    public int oldmax()
    {
      return 1;
    }

    public int oldmin()
    {
      return 0;
    }

    public int min()
    {
      return 0;
    }

    public int max()
    {
      return 0;
    }

    public int type()
    {
      return MAX | VALUE;
    }


    public int removed(int i)
    {
      return -1;
    }

    public int numberOfRemoves()
    {
      return 0;
    }

    public void free()
    {
    }

  } // ~IntEventBoolFalse

  /**
   * Undo Class for IntBoolVar.
   */
  static final class UndoIntBoolVar extends UndoSubject
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new UndoIntBoolVar();
        }

    };

    static UndoIntBoolVar getBoolVarUndo()
    {
      return  (UndoIntBoolVar) _factory.getElement();
    }

    int _min, _max;

    public void undoable(Undoable u)
    {
      super.undoable(u);
      IntBoolVarImpl var = (IntBoolVarImpl) u;
      _min = var.min();
      _max = var.max();

   }

    /**
     * Use to display the UndoIntVar object
     */
    public String toString()
    {
      return "UndoIntBoolVar "+undoable();
    }

    /**
     * Execute undo() operation for this UndoIntVar object
     */

    public void undo()
    {
        IntBoolVar var = (IntBoolVar) undoable();
        var.forceMin(_min);
        var.forceMax(_max);
        super.undo();
    }
  } // ~UndoIntBoolVar

  /**
   * Undo Class for IntBoolVar's "value only change".
   */
  static final class UndoIntBoolVarValue extends UndoImpl
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new UndoIntBoolVarValue();
        }

    };

    static UndoIntBoolVarValue getUndo(IntBoolVarImpl v)
    {
      UndoIntBoolVarValue undo = (UndoIntBoolVarValue) _factory.getElement();
      undo.undoable(v);
      return undo;
    }

    /**
     * Use to display the UndoIntVarValue object.
     */
    public String toString()
    {
      return "UndoIntBoolVarValue "+undoable();
    }

    /**
     * Executes undo() operation for this UndoIntVarValue object.
     */
    public void undo()
    {
        IntBoolVar var = (IntBoolVar) undoable();
        var.forceMin(0);
        var.forceMax(1);
        super.undo();
    }
  } // ~UndoIntBoolVarValue


} // ~IntBoolVarImpl
