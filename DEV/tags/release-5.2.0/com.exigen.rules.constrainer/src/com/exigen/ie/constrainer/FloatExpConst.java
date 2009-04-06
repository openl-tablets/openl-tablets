package com.exigen.ie.constrainer;
import java.util.Map;

import com.exigen.ie.constrainer.impl.ConstraintFloatExpEqualsValue;
import com.exigen.ie.constrainer.impl.ConstraintFloatExpLessValue;
import com.exigen.ie.constrainer.impl.ConstraintFloatExpMoreValue;
import com.exigen.ie.constrainer.impl.FloatExpAddValue;
import com.exigen.ie.constrainer.impl.FloatExpImpl;
/**
 * An implementation of the constant floating-point expression.
 * Many methods from FloatExpImpl are overloaded with optimized implementation.
 */
public final class FloatExpConst extends FloatExpImpl
{
  private double _const;

  public FloatExpConst(Constrainer constrainer, double c)
  {
    super(constrainer,Double.toString(c));
    _const = c;
  }

  public double min()
  {
    return _const;
  }

  public double max()
  {
    return _const;
  }

  public FloatExp neg()
  {
//    return new FloatExpConst(constrainer(), -_const);
    return getFloatExp(FloatExpConst.class, -_const);
  }

  public FloatExp add(FloatExp exp)
  {
//    return new FloatExpAddValue(exp,_const);
    return getFloatExp(FloatExpAddValue.class, exp, _const);
  }

  public FloatExp add(double value)
  {
//    return new FloatExpConst(constrainer(),_const + value);
    return getFloatExp(FloatExpConst.class, _const + value);
  }

  public boolean bound()
  {
    return true;
  }

  public Constraint equals(double value) // this = value
  {
    return new ConstraintConst(constrainer(),value == _const);
  }

  public Constraint equals(FloatExp exp) // this == exp
  {
    return new ConstraintFloatExpEqualsValue(exp,_const);
  }

  public Constraint equals(FloatExp exp, double value) // _const == exp + value
  {
    return new ConstraintFloatExpEqualsValue(exp,_const - value);
  }

  public Constraint lessOrEqual(double value)
  {
    return new ConstraintConst(constrainer(),_const <= value);
  }

  public Constraint lessOrEqual(FloatExp exp)
  {
    return new ConstraintFloatExpMoreValue(exp,_const);
  }

  public Constraint moreOrEqual(double value)
  {
    return new ConstraintConst(constrainer(),_const >= value);
  }

  public Constraint moreOrEqual(FloatExp exp)
  {
    return new ConstraintFloatExpLessValue(exp,_const);
  }

  public FloatExp mul(double c)
  {
//    return new FloatExpConst(constrainer(),_const * c);
    return getFloatExp(FloatExpConst.class, _const * c);
  }

  public FloatExp div(double c)
  {
    if ( c == 0 )
      throw new IllegalArgumentException("Division by zero");
//    return new FloatExpConst(constrainer(),_const / c);
    return getFloatExp(FloatExpConst.class, _const / c);
  }

  public FloatExp sub(FloatExp exp)
  {
    return exp.neg().add(_const);
  }

  public String toString()
  {
    return "["+_const+"]";
  }

  public void setMax(double max) throws Failure
  {
    if (max < _const)
      constrainer().fail("max<const");
  }

  public void setMin(double min) throws Failure
  {
    if (min > _const)
      constrainer().fail("min>const");
  }

  public void setValue(double value) throws Failure
  {
    if (value != _const)
      constrainer().fail("value!=const");
  }

  public double value() throws Failure
  {
    return _const;
  }

  public void propagate() throws Failure
  {
  }

  public boolean isLinear(){
    return true;
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return _const*factor;
  }

} // ~FloatExpConst
