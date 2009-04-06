package com.exigen.ie.constrainer.lpsolver;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public interface LPConstraint {
  public int getType();
  public double getLb();
  public double getUb();
  public int[] getLocations();
  public double[] getValues();
}