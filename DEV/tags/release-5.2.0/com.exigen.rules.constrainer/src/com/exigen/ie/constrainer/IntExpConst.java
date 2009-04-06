package com.exigen.ie.constrainer;
import java.util.Map;

import com.exigen.ie.constrainer.impl.ConstraintExpEqualsValue;
import com.exigen.ie.constrainer.impl.ConstraintExpLessValue;
import com.exigen.ie.constrainer.impl.ConstraintExpMoreValue;
import com.exigen.ie.constrainer.impl.IntExpBitAndExp;
import com.exigen.ie.constrainer.impl.IntExpImpl;
//
//: IntExpConst.java
//
/**
 * An implementation of the constant integer expression.
 * Many methods from IntExpImpl are overloaded with optimized implementation.
 */
public class IntExpConst extends IntExpImpl
{
  protected final int _const;

  public IntExpConst(Constrainer constrainer, int c)
  {
    super(constrainer);
    if(constrainer().showInternalNames())
    {
      _name = Integer.toString(c);
    }

    _const = c;
  }
  /**
   *
   * @return -const
   */
  final public IntExp neg()
  {
//    return new IntExpConst(constrainer(), -_const);
    return getIntExp(IntExpConst.class, -_const);
  }
  /**
   * Casts the IntExpConst to FloatExpConst
   * @return FloatExpConst
   */
  final public FloatExp asFloat()
  {
//    return new FloatExpConst(constrainer(),_const);
    return getFloatExp(FloatExpConst.class, (double)_const);
  }
  /**
   * Overrides the appropriate method of IntExpImpl
   * @return The value of expression
   */
  final public int min()
  {
    return _const;
  }
  /**
   * Overrides the appropriate method of IntExpImpl
   * @return The value of expression e.g. const
   */
  final public int max()
  {
    return _const;
  }
  /**
   * Overrides the appropriate method of IntExpImpl
   * @return <code>(IntExp)(exp + const)</code>
   */
  final public IntExp add(IntExp exp)
  {
    return (IntExp)exp.add(_const);
  }
  /**
   * @return <code>(IntExp)(value+const)</code>
   */
  final public IntExp add(int value)
  {
//    return new IntExpConst(constrainer(),_const + value);
    return getIntExp(IntExpConst.class, _const + value);
  }

  /**
   * @return true
   */
  final public boolean bound()
  {
    return true;
  }
  /**
   * @param value The value to be checked
   * @return (value == const)
   */
  final public boolean contains(int value)
  {
    return (value == _const);
  }
  /**
   * @return 1
   */
  final public int size()
  {
    return 1;
  }
  /**
   * @param value The value the IntExpConst must be equal to
   * @return <code> ConstraintConst(value == const)</code>
   */
  final public Constraint equals(int value) // this = value
  {
    return new ConstraintConst(constrainer(),value == _const);
  }
  /**
   * @param exp IntExp that must be equal to IntExpConst's value.
   * @return  <code> ConstraintConst(exp == const)</code>
   */
  final public Constraint equals(IntExp exp) // this == exp
  {
    return new ConstraintExpEqualsValue(exp,_const);
  }
  /**
   * @return <code> ConstraintConst(exp == const - value)</code>
   */
  final public Constraint equals(IntExp exp, int value) // _const == exp + value
  {
    return new ConstraintExpEqualsValue(exp,_const - value);
  }
  /**
   * @param value The value the IntExpConst must be less to.
   * @return <code> ConstraintConst(const - value)</code>
   */
  final public Constraint less(int value)
  {
    return new ConstraintConst(constrainer(),_const < value);
  }
  /**
   * @param exp The IntExp that must be greater then "const"
   * @return <code> ConstraintConst(const - value)</code>
   */

  final public Constraint less(IntExp exp)
  {
    return new ConstraintExpMoreValue(exp,_const);
  }
  /**
   * @param value The value the IntExpConst must be less or equal to.
   * @return <code> ConstraintConst(const <= value)</code>
   */
  final public Constraint lessOrEqual(int value)
  {
    return new ConstraintConst(constrainer(),_const <= value);
  }
  /**
   *
   * @param exp The IntExp that must be greater or equal to "const"
   * @return  (Constraint)(exp > const -1)
   */
  final public Constraint lessOrEqual(IntExp exp)
  {
    return new ConstraintExpMoreValue(exp,_const-1);
  }
  /**
   *
   * @param value The value the "const" must be greater then.
   * @return (Constraint)(const > value)
   */
  final public Constraint more(int value)
  {
    return new ConstraintConst(constrainer(),_const > value);
  }
  /**
   *
   * @param exp The IntExp that must be less then "const"
   * @return (COnstraint)(exp < const)
   */
  final public Constraint more(IntExp exp)
  {
    return new ConstraintExpLessValue(exp,_const);
  }
  /**
   *
   * @param value The value the "const" must be greater then or equal to
   * @return Constraint(const > value-1)
   */
  final public Constraint moreOrEqual(int value)
  {
    return more(value-1);
  }
  /**
   * @param exp The IntExp that must be less then or equal to "const"
   * @return (Constraint)(exp < const+1)
   */
  final public Constraint moreOrEqual(IntExp exp)
  {
    return new ConstraintExpLessValue(exp,_const+1);
  }
  
  public IntExp bitAnd(IntExp exp)
  {
      return getIntExp(IntExpBitAndExp.class, this, exp);
  }
  /**
   *
   * @param c the factor
   * @return (IntExp)(const*c)
   */
  final public IntExp mul(int c)
  {
//    return new IntExpConst(constrainer(),_const * c );
    return getIntExp(IntExpConst.class, _const * c);
  }
  /**
   *
   * @param exp the factor
   * @return (IntExp)(exp*const)
   */
  final public IntExp mul(IntExp exp)
  {
    return exp.mul(_const);
  }
  /**
   * @param c the denominator
   * @return IntExp(const/c)
   */
  final public IntExp div(int c)
  {
    if ( c == 0 )
      throw new IllegalArgumentException("Division by zero");
//    return new IntExpConst(constrainer(),_const / c);
    return getIntExp(IntExpConst.class, _const / c);
  }
  /**
   * @param exp the term
   * @return (IntExp)(const - exp)
   */
  final public IntExp sub(IntExp exp)
  {
    return exp.neg().add(_const);
  }
  /**
   * Checks wether the value is greater then or equal to "const". If it is not
   * throws Failure.
   * @param max the value to be checked
   */
  final public void setMax(int max) throws Failure
  {
    if (max < _const)
      constrainer().fail("max<const");
  }
  /**
   * Checks wether the value is less then or equal to "const". If it is not
   * throws Failure
   * @param min the value to be checked
   */
  final public void setMin(int min) throws Failure
  {
    if (min > _const)
      constrainer().fail("min>const");
  }
  /**
   * Actually it checks wether the value is equal to "const". If it is not
   * throws Failure.
   * @param value The value to be checked
   */
  final public void setValue(int value) throws Failure
  {
    if (value != _const)
      constrainer().fail("value!=const");
  }
  /**
   *
   * @param value the value to be compared with "const"
   * @throws Failure if value is equal to "const". Do nothing otherwise
   */
  final public void removeValue(int value) throws Failure
  {
    if(value == _const)
      constrainer().fail("remove const");
  }
  /**
   *
   * @return "const"
   */
  final public int value() throws Failure
  {
    return _const;
  }
  /**
   * Do nothig in this release
   *
   */

  final public void propagate() throws Failure
  {
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression
  {
    return _const*factor;
  }

  public boolean isLinear(){
    return true;
  }

  public boolean isInteger(){
    return true;
  }

} // ~IntExpConst
